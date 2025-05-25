package ch.njol.skript.variables;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.ConfigurationSerializer;
import ch.njol.skript.config.Config;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.NewVariable;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.variables.SerializedVariable.Value;
import ch.njol.util.Kleenean;
import ch.njol.util.NonNullPair;
import ch.njol.util.Pair;
import ch.njol.util.SynchronizedReference;
import ch.njol.util.coll.iterator.EmptyIterator;
import ch.njol.yggdrasil.Yggdrasil;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.lang.converter.Converters;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

public class NewVariables {

	/**
	 * The version of {@link Yggdrasil} this class is using.
	 */
	public static final short YGGDRASIL_VERSION = 1;

	/**
	 * The {@link Yggdrasil} instance used for (de)serialization.
	 */
	public static final Yggdrasil yggdrasil = new Yggdrasil(YGGDRASIL_VERSION);

	private static final String CONFIGURATION_SERIALIZABLE_PREFIX = "ConfigurationSerializable_";

	private static final String EPHEMERAL_VARIABLE_PREFIX = "-";

	/**
	 * Whether variable names are case-sensitive.
	 */
	public static boolean caseInsensitiveVariables = true;

	/**
	 * A pattern to split variable names using {@link Variable#SEPARATOR}.
	 */
	private static final Pattern VARIABLE_NAME_SPLIT_PATTERN = Pattern.compile(Pattern.quote(NewVariable.SEPARATOR));

	/**
	 * The variable storages configured.
	 */
	static final List<VariablesStorage> STORAGES = new ArrayList<>();

	private static final Multimap<Class<? extends VariablesStorage>, String> TYPES = HashMultimap.create();

	/**
	 * A lock for reading and writing variables.
	 */
	static final ReadWriteLock variablesLock = new ReentrantReadWriteLock(true);

	/**
	 * Changes to variables that have not yet been performed.
	 */
	static final Queue<NewVariableChange> changeQueue = new ConcurrentLinkedQueue<>();

	/**
	 * The queue of serialized variables that have not yet been written
	 * to the storage.
	 */
	static final BlockingQueue<SerializedVariable> saveQueue = new LinkedBlockingQueue<>();

	private static final SynchronizedReference<Map<String, NonNullPair<Object, VariablesStorage>>> TEMP_VARIABLES =
		new SynchronizedReference<>(new HashMap<>());

	private static final int MAX_CONFLICT_WARNINGS = 50;

	private static int loadConflicts = 0;

	private static volatile boolean closed = false;

	private static final NewVariablesMap globalVariables = new NewVariablesMap();
	private static final Map<Event, NewVariablesMap> localVariables = new HashMap<>();

	static {
		registerStorage(FlatFileStorage.class, "csv", "file", "flatfile");
		registerStorage(SQLiteStorage.class, "sqlite");
		registerStorage(MySQLStorage.class, "mysql");
		yggdrasil.registerSingleClass(Kleenean.class, "Kleenean");
		// Register ConfigurationSerializable, Bukkit's serialization system
		yggdrasil.registerClassResolver(new ConfigurationSerializer<ConfigurationSerializable>() {
			{
				//noinspection unchecked
				info = (ClassInfo<? extends ConfigurationSerializable>) (ClassInfo<?>) Classes.getExactClassInfo(Object.class);
				// Info field is mostly unused in superclass, due to methods overridden below,
				//  so this illegal cast is fine
			}

			@Override
			@Nullable
			public String getID(@NotNull Class<?> c) {
				if (ConfigurationSerializable.class.isAssignableFrom(c)
					&& Classes.getSuperClassInfo(c) == Classes.getExactClassInfo(Object.class))
					return CONFIGURATION_SERIALIZABLE_PREFIX +
						ConfigurationSerialization.getAlias(c.asSubclass(ConfigurationSerializable.class));

				return null;
			}

			@Override
			@Nullable
			public Class<? extends ConfigurationSerializable> getClass(@NotNull String id) {
				if (id.startsWith(CONFIGURATION_SERIALIZABLE_PREFIX))
					return ConfigurationSerialization.getClassByAlias(
						id.substring(CONFIGURATION_SERIALIZABLE_PREFIX.length()));

				return null;
			}
		});
	}

	public static @Unmodifiable List<VariablesStorage> getStores() {
		return Collections.unmodifiableList(STORAGES);
	}

	public static <T extends VariablesStorage> boolean registerStorage(Class<T> storage, String... names) {
		if (TYPES.containsKey(storage))
			return false;
		for (String name : names) {
			if (TYPES.containsValue(name.toLowerCase(Locale.ENGLISH)))
				return false;
		}
		for (String name : names)
			TYPES.put(storage, name.toLowerCase(Locale.ENGLISH));
		return true;
	}

	public static boolean load() {
		assert STORAGES.isEmpty();

		Config config = SkriptConfig.getConfig();
		if (config == null)
			throw new SkriptAPIException("Cannot load variables before the config");

		Node databases = config.getMainNode().get("databases");
		if (!(databases instanceof SectionNode)) {
			Skript.error("The config is missing the required 'databases' section that defines where the variables are saved");
			return false;
		}

		Skript.closeOnDisable(NewVariables::close);

		// reports once per second how many variables were loaded. Useful to make clear that Skript is still doing something if it's loading many variables
		Thread loadingLoggerThread = new Thread(() -> {
			while (true) {
				try {
					Thread.sleep(Skript.logNormal() ? 1000 : 5000); // low verbosity won't disable these messages, but makes them more rare
				} catch (InterruptedException ignored) {}

				synchronized (TEMP_VARIABLES) {
					Map<String, NonNullPair<Object, VariablesStorage>> tvs = TEMP_VARIABLES.get();
					if (tvs != null)
						Skript.info("Loaded " + tvs.size() + " variables so far...");
					else
						break; // variables loaded, exit thread
				}
			}
		});
		loadingLoggerThread.start();

		try {
			boolean successful = true;

			for (Node node : (SectionNode) databases) {
				if (node instanceof SectionNode) {
					SectionNode sectionNode = (SectionNode) node;

					String type = sectionNode.getValue("type");
					if (type == null) {
						Skript.error("Missing entry 'type' in database definition");
						successful = false;
						continue;
					}

					String name = sectionNode.getKey();
					assert name != null;

					// Initiate the right VariablesStorage class
					VariablesStorage variablesStorage;
					Optional<?> optional = TYPES.entries().stream()
						.filter(entry -> entry.getValue().equalsIgnoreCase(type))
						.map(Entry::getKey)
						.findFirst();
					if (!optional.isPresent()) {
						if (!type.equalsIgnoreCase("disabled") && !type.equalsIgnoreCase("none")) {
							Skript.error("Invalid database type '" + type + "'");
							successful = false;
						}
						continue;
					}

					try {
						@SuppressWarnings("unchecked")
						Class<? extends VariablesStorage> storageClass = (Class<? extends VariablesStorage>) optional.get();
						Constructor<?> constructor = storageClass.getDeclaredConstructor(String.class);
						constructor.setAccessible(true);
						variablesStorage = (VariablesStorage) constructor.newInstance(type);
					} catch (InstantiationException | IllegalAccessException | IllegalArgumentException |
							 InvocationTargetException | NoSuchMethodException | SecurityException e) {
						Skript.error("Failed to initialize database `" + name + "`");
						successful = false;
						continue;
					}

					// Get the amount of variables currently loaded
					int totalVariablesLoaded;
					synchronized (TEMP_VARIABLES) {
						Map<String, NonNullPair<Object, VariablesStorage>> tvs = TEMP_VARIABLES.get();
						assert tvs != null;
						totalVariablesLoaded = tvs.size();
					}

					long start = System.currentTimeMillis();
					if (Skript.logVeryHigh())
						Skript.info("Loading database '" + node.getKey() + "'...");

					// Load the variables
					if (variablesStorage.load(sectionNode))
						STORAGES.add(variablesStorage);
					else
						successful = false;

					// Get the amount of variables loaded by this variables storage object
					int newVariablesLoaded;
					synchronized (TEMP_VARIABLES) {
						Map<String, NonNullPair<Object, VariablesStorage>> tvs = TEMP_VARIABLES.get();
						assert tvs != null;
						newVariablesLoaded = tvs.size() - totalVariablesLoaded;
					}

					if (Skript.logVeryHigh()) {
						Skript.info("Loaded " + newVariablesLoaded + " variables from the database " +
							"'" + sectionNode.getKey() + "' in " +
							((System.currentTimeMillis() - start) / 100) / 10.0 + " seconds");
					}
				} else {
					Skript.error("Invalid line in databases: databases must be defined as sections");
					successful = false;
				}
			}
			if (!successful)
				return false;

			if (STORAGES.isEmpty()) {
				Skript.error("No databases to store variables are defined. Please enable at least the default database, even if you don't use variables at all.");
				return false;
			}
		} finally {
			SkriptLogger.setNode(null);

			// make sure to put the loaded variables into the variables map
			int notStoredVariablesCount = onStoragesLoaded();
			if (notStoredVariablesCount != 0) {
				Skript.warning(notStoredVariablesCount + " variables were possibly discarded due to not belonging to any database " +
					"(SQL databases keep such variables and will continue to generate this warning, " +
					"while CSV discards them).");
			}

			// Interrupt the loading logger thread to make it exit earlier
			loadingLoggerThread.interrupt();

			saveThread.start();
		}
		return true;
	}

	public static String[] splitVariableName(String name) {
		return VARIABLE_NAME_SPLIT_PATTERN.split(name);
	}

	static NewVariablesMap getVariables() {
		return globalVariables;
	}

	/**
	 * Gets the lock for reading variables.
	 *
	 * @return the lock.
	 *
	 * @see #variablesLock
	 */
	static Lock getReadLock() {
		return variablesLock.readLock();
	}

	public static @Nullable NewVariablesMap removeLocals(Event event) {
		return localVariables.remove(event);
	}

	public static void setLocalVariables(Event event, @Nullable NewVariablesMap map) {
		if (map != null) {
			localVariables.put(event, map);
		} else {
			removeLocals(event);
		}
	}

	public static @Nullable NewVariablesMap copyLocalVariables(Event event) {
		NewVariablesMap variablesMap = localVariables.get(event);
		if (variablesMap == null)
			return null;

		return variablesMap.copy();
	}

	public static void withLocalVariables(Event provider, Event user, @NotNull Runnable action) {
		setLocalVariables(user, copyLocalVariables(provider));
		action.run();
		setLocalVariables(provider, copyLocalVariables(user));
		removeLocals(user);
	}

	public static @Nullable NewVariable<?> getVariableObject(String name, @Nullable Event event, boolean local) {
		String finalName;
		if (caseInsensitiveVariables) {
			finalName = name.toLowerCase(Locale.ENGLISH);
		} else {
			finalName = name;
		}

		if (local) {
			NewVariablesMap variableMap = localVariables.get(event);
			if (variableMap == null)
				return null;
			return variableMap.getVariableObject(name);
		} else {
			if (!changeQueue.isEmpty()) {
				NewVariableChange variableChange = changeQueue.stream()
					.filter(change -> change.name.equals(finalName))
					.reduce((first, second) -> second)
					.orElse(null);

				if (variableChange != null) {
					return globalVariables.getVariableObject(name);
				}
			}
		}

		try {
			variablesLock.readLock().lock();
			return globalVariables.getVariableObject(finalName);
		} finally {
			variablesLock.readLock().unlock();
		}
	}

	public static @Nullable Object getVariable(String name, @Nullable Event event, boolean local) {
		NewVariable<?> variable = getVariableObject(name, event, local);
		if (variable == null)
			return null;
		return variable.getValue();
	}

	public static void deleteVariable(String name, @Nullable Event event, boolean local) {
		setVariable(name, null, event, local);
	}

	public static void setVariable(String name, @Nullable Object value, @Nullable Event event, boolean local) {
		if (caseInsensitiveVariables)
			name = name.toLowerCase(Locale.ENGLISH);

		if (value != null) {
			assert !name.endsWith("::*");

			ClassInfo<?> classInfo = Classes.getSuperClassInfo(value.getClass());
			Class<?> serializeAs = classInfo.getSerializeAs();

			if (serializeAs != null) {
				value = Converters.convert(value, serializeAs);
				assert value != null : classInfo + ", " + serializeAs;
			}
		}

		if (local) {
			assert event != null : name;
			localVariables.computeIfAbsent(event, event1 -> new NewVariablesMap()).setVariable(name, value);
		} else {
			setVariable(name, value);
		}
	}

	static void setVariable(String name, @Nullable Object value) {
		boolean gotLock = variablesLock.writeLock().tryLock();
		if (gotLock) {
			try {
				globalVariables.setVariable(name, value);
				saveVariableChange(name, value);
				processChangeQueue();
			} finally {
				variablesLock.writeLock().unlock();
			}
		}  else {
			queueVariableChange(name, value);
		}
	}

	static void setVariable(NewVariable<?> variable) {
		globalVariables.setVariable(variable);
	}

	public static Iterator<Pair<String, Object>> getVariableIterator(String name, boolean local, @Nullable Event event) {
		assert name.endsWith("*");
		NewVariable<?> variable = getVariableObject(name, event, local);
		if (variable == null)
			return new EmptyIterator<>();
		return getVariableIterator(variable, event);
	}

	public static Iterator<Pair<String, Object>> getVariableIterator(NewVariable<?> variable, @Nullable Event event) {
		assert variable.isList();
		//noinspection unchecked
		Map<String, NewVariable<?>> variables = (Map<String, NewVariable<?>>) variable.getValue();
		assert variables != null;
		if (variables.isEmpty())
			return new EmptyIterator<>();

		Iterator<String> keys = new ArrayList<>(variables.keySet()).iterator();
		return new Iterator<>() {

			private @Nullable String key;
			private @Nullable NewVariable<?> nextVariable = null;
			private @Nullable Object nextObject = null;

			@Override
			public boolean hasNext() {
				if (nextObject != null)
					return true;
				while (keys.hasNext()) {
					key = keys.next();
					if (key != null) {
						nextVariable = variables.get(key);
						if (nextVariable != null) {
							nextObject = variables.get(key).convertIfOldPlayer();
							if (nextObject != null && !nextVariable.isList())
								return true;
						}
					}
				}
				nextObject = null;
				nextVariable = null;
				return false;
			}

			@Override
			public Pair<String, Object> next() {
				if (!hasNext())
					throw new NoSuchElementException();
				Pair<String, Object> pair = new Pair<>(key, nextObject);
				nextObject = null;
				nextVariable = null;
				return pair;
			}

			@Override
			public void remove() {
				if (key == null)
					throw new NoSuchElementException();
				variable.removeVariable(key);
			}
		};
	}


	public static class NewVariableChange {
		/**
		 * The name of the changed variable.
		 */
		public final String name;

		/**
		 * The (possibly {@code null}) value of the variable change.
		 */
		@Nullable
		public final Object value;

		/**
		 * Creates a new {@link NewVariableChange} with the given name and value.
		 *
		 * @param name the variable name.
		 * @param value the new variable value.
		 */
		public NewVariableChange(String name, @Nullable Object value) {
			this.name = name;
			this.value = value;
		}
	}

	/**
	 * Queues a variable change. Only to be called when direct write is not
	 * possible, but thread cannot be allowed to block.
	 *
	 * @param name the variable name.
	 * @param value the new value.
	 */
	private static void queueVariableChange(String name, @Nullable Object value) {
		changeQueue.add(new NewVariableChange(name, value));
	}

	/**
	 * Processes all entries in variable change queue.
	 * <p>
	 * Note that caller must acquire write lock before calling this,
	 * then release it.
	 */
	static void processChangeQueue() {
		while (true) { // Run as long as we still have changes
			NewVariableChange change = changeQueue.poll();
			if (change == null)
				break;

			// Set and save variable
			globalVariables.setVariable(change.name, change.value);
			saveVariableChange(change.name, change.value);
		}
	}

	static boolean variableLoaded(String name, @Nullable Object value, VariablesStorage source) {
		assert Bukkit.isPrimaryThread(); // required by serialisation

		if (value == null)
			return false;

		synchronized (TEMP_VARIABLES) {
			Map<String, NonNullPair<Object, VariablesStorage>> tvs = TEMP_VARIABLES.get();
			if (tvs != null) {
				NonNullPair<Object, VariablesStorage> existingVariable = tvs.get(name);

				// Check for conflicts with other storages
				conflict: if (existingVariable != null) {
					VariablesStorage existingVariableStorage = existingVariable.getSecond();

					if (existingVariableStorage == source) {
						// No conflict if from the same storage
						break conflict;
					}

					// Variable already loaded from another database, conflict
					loadConflicts++;

					// Warn if needed
					if (loadConflicts <= MAX_CONFLICT_WARNINGS) {
						Skript.warning("The variable {" + name + "} was loaded twice from different databases (" +
							existingVariableStorage.getUserConfigurationName() + " and " + source.getUserConfigurationName() +
							"), only the one from " + source.getUserConfigurationName() + " will be kept.");
					} else if (loadConflicts == MAX_CONFLICT_WARNINGS + 1) {
						Skript.warning("[!] More than " + MAX_CONFLICT_WARNINGS +
							" variables were loaded more than once from different databases, " +
							"no more warnings will be printed.");
					}

					// Remove the value from the existing variable's storage
					existingVariableStorage.save(name, null, null);
				}

				// Add to the loaded variables
				tvs.put(name, new NonNullPair<>(value, source));

				return false;
			}
		}

		variablesLock.writeLock().lock();
		try {
			globalVariables.setVariable(name, value);
		} finally {
			variablesLock.writeLock().unlock();
		}

		// Move the variable to the right storage
		try {
			for (VariablesStorage variablesStorage : STORAGES) {
				if (variablesStorage.accept(name)) {
					if (variablesStorage != source) {
						// Serialize and set value in new storage
						Value serializedValue = serialize(value);
						if (serializedValue == null) {
							variablesStorage.save(name, null, null);
						} else {
							variablesStorage.save(name, serializedValue.type, serializedValue.data);
						}

						// Remove from old storage
						if (value != null)
							source.save(name, null, null);
					}
					return true;
				}
			}
		} catch (Exception e) {
			//noinspection ThrowableNotThrown
			Skript.exception(e, "Error saving variable named " + name);
		}

		return false;
	}

	private static int onStoragesLoaded() {
		if (loadConflicts > MAX_CONFLICT_WARNINGS)
			Skript.warning("A total of " + loadConflicts + " variables were loaded more than once from different databases");

		Skript.debug("Databases loaded, setting variables...");

		synchronized (TEMP_VARIABLES) {
			Map<String, NonNullPair<Object, VariablesStorage>> tvs = TEMP_VARIABLES.get();
			TEMP_VARIABLES.set(null);
			assert tvs != null;

			variablesLock.writeLock().lock();
			try {
				// Calculate the amount of variables that don't have a storage
				int unstoredVariables = 0;
				for (Entry<String, NonNullPair<Object, VariablesStorage>> tv : tvs.entrySet()) {
					if (!variableLoaded(tv.getKey(), tv.getValue().getFirst(), tv.getValue().getSecond()))
						unstoredVariables++;
				}

				for (VariablesStorage variablesStorage : STORAGES)
					variablesStorage.allLoaded();

				Skript.debug("Variables set. Queue size = " + saveQueue.size());

				return unstoredVariables;
			} finally {
				variablesLock.writeLock().unlock();
			}
		}
	}

	/**
	 * Creates a {@link SerializedVariable} from the given variable name
	 * and value.
	 * <p>
	 * Must be called from Bukkit's main thread.
	 *
	 * @param name the variable name.
	 * @param value the value.
	 * @return the serialized variable.
	 */
	public static SerializedVariable serialize(String name, @Nullable Object value) {
		assert Bukkit.isPrimaryThread();

		// First, serialize the variable.
		SerializedVariable.Value var;
		try {
			var = serialize(value);
		} catch (Exception e) {
			throw Skript.exception(e, "Error saving variable named " + name);
		}

		return new SerializedVariable(name, var);
	}

	/**
	 * Serializes the given value.
	 * <p>
	 * Must be called from Bukkit's main thread.
	 *
	 * @param value the value to serialize.
	 * @return the serialized value.
	 */
	public static SerializedVariable.@Nullable Value serialize(@Nullable Object value) {
		assert Bukkit.isPrimaryThread();

		return Classes.serialize(value);
	}

	/**
	 * Serializes and adds the variable change to the {@link #saveQueue}.
	 *
	 * @param name the variable name.
	 * @param value the value of the variable.
	 */
	private static void saveVariableChange(String name, @Nullable Object value) {
		if (name.startsWith(EPHEMERAL_VARIABLE_PREFIX))
			return;
		saveQueue.add(serialize(name, value));
	}

	/**
	 * The thread that saves variables, i.e. stores in the appropriate storage.
	 */
	private static final Thread saveThread = Skript.newThread(() -> {
		while (!closed) {
			try {
				// Save one variable change
				SerializedVariable variable = saveQueue.take();

				for (VariablesStorage variablesStorage : STORAGES) {
					if (variablesStorage.accept(variable.name)) {
						variablesStorage.save(variable);

						break;
					}
				}
			} catch (InterruptedException ignored) {}
		}
	}, "Skript variable save thread");

	/**
	 * Closes the variable systems:
	 * <ul>
	 *     <li>Process all changes left in the {@link #changeQueue}.</li>
	 *     <li>Stops the {@link #saveThread}.</li>
	 * </ul>
	 */
	public static void close() {
		try { // Ensure that all changes are to save soon
			variablesLock.writeLock().lock();
			processChangeQueue();
		} finally {
			variablesLock.writeLock().unlock();
		}

		// First, make sure all variables are saved
		while (saveQueue.size() > 0) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException ignored) {}
		}

		// Then we can safely interrupt and stop the thread
		closed = true;
		saveThread.interrupt();
	}

	/**
	 * Gets the amount of variables currently on the server.
	 *
	 * @return the amount of variables.
	 */
	public static int numVariables() {
		try {
			variablesLock.readLock().lock();
			return globalVariables.variablesSize();
		} finally {
			variablesLock.readLock().unlock();
		}
	}

}
