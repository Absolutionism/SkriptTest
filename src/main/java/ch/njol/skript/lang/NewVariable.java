package ch.njol.skript.lang;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.classes.Changer.ChangerUtils;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.structures.StructVariables.DefaultVariables;
import ch.njol.skript.util.StringMode;
import ch.njol.skript.util.Utils;
import ch.njol.skript.variables.*;
import ch.njol.util.Kleenean;
import ch.njol.util.Pair;
import ch.njol.util.StringUtils;
import ch.njol.util.coll.CollectionUtils;
import ch.njol.util.coll.iterator.SingleItemIterator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.arithmetic.Arithmetics;
import org.skriptlang.skript.lang.arithmetic.OperationInfo;
import org.skriptlang.skript.lang.arithmetic.Operator;
import org.skriptlang.skript.lang.comparator.Comparators;
import org.skriptlang.skript.lang.comparator.Relation;
import org.skriptlang.skript.lang.converter.Converters;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.lang.script.ScriptWarning;

import java.lang.reflect.Array;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class NewVariable<T> implements Expression<T>, KeyReceiverExpression<T>, KeyProviderExpression<T> {

	private final static String SINGLE_SEPARATOR_CHAR = ":";
	public final static String SEPARATOR = SINGLE_SEPARATOR_CHAR + SINGLE_SEPARATOR_CHAR;
	public final static String LOCAL_VARIABLE_TOKEN = "_";
	private static final char[] reservedTokens = {'~', '.', '+', '$', '!', '&', '^', '*', '-'};

	private @Nullable Script script;
	private VariableString name;
	private Class<T> superType;
	private Class<? extends T>[] types;
	private boolean local;
	private boolean list;
	private @Nullable NewVariable<?> source;

	private @Nullable NewVariable<?> parent;
	private Map<String, NewVariable<?>> variables = new HashMap<>();
	private @Nullable Object value = null;

	private NewVariable(VariableString name, Class<? extends T>[] types, boolean local, boolean list, @Nullable NewVariable<?> source) {
		assert types.length > 0;
		assert name.isSimple() || name.getMode() == StringMode.VARIABLE_NAME;

		ParserInstance parser = getParser();
		this.script = parser.isActive() ? parser.getCurrentScript() : null;
		this.name = name;
		//noinspection unchecked
		this.superType = (Class<T>) Utils.getSuperType(types);
		this.types = types;
		this.local = local;
		this.list = list;
		this.source = source;
	}

	private NewVariable(VariableString name, boolean list) {
		assert name.isSimple() || name.getMode() == StringMode.VARIABLE_NAME;

		ParserInstance parser = getParser();
		this.script = parser.isActive() ? parser.getCurrentScript() : null;
		this.name = name;
		this.list = list;
	}

	public @Nullable Script getScript() {
		return script;
	}

	public VariableString getName() {
		return name;
	}

	public Class<T> getSuperType() {
		return superType;
	}

	public Class<? extends T>[] getTypes() {
		return types;
	}

	public boolean isLocal() {
		return local;
	}

	public boolean isList() {
		return list;
	}

	public void setList(boolean list) {
		this.list = list;
	}

	public @Nullable Object getValue() {
		if (list)
			return variables;
		return value;
	}

	public void setValue(@Nullable Object value) {
		setValue(value, null);
	}

	private void setValue(@Nullable Object value, @Nullable NewVariable<?> caller) {
		if (list) {
			if (value == null) {
				clearVariables();
				if (parent != null && caller != parent)
					parent.removeVariable(this, this);
				parent = null;
			} else {
				if (value instanceof Map<?, ?> rawMap) {
					//noinspection unchecked
					Map<String, NewVariable<?>> map = (Map<String, NewVariable<?>>) rawMap;
					setVariables(map);
				}
			}
		} else {
			this.value = value;
			if (value == null) {
				if (parent != null && caller != parent)
				    parent.removeVariable(this, this);
				parent = null;
			}
		}
	}

	public @Nullable NewVariable<?> getParent() {
		return parent;
	}

	public void setParent(@Nullable NewVariable<?> parent) {
		if (this.parent != null && this.parent != parent)
			this.parent.removeVariable(this, this);
		this.parent = parent;
	}

	public void setVariable(NewVariable<?> other) {
		assert list;
		VariableString otherName = other.getName();
		other.setParent(this);
		variables.put(otherName.original, other);
	}

	public void setVariables(Map<String, NewVariable<?>> variables) {
		assert list;
		clearVariables();
		variables.forEach((string, variable) ->  variable.setParent(this));
		this.variables = variables;
	}

	public void setVariables(Object... objects) {
		assert list;
		clearVariables();
		if (objects.length == 1) {
			Object object = objects[0];
			Map<String, NewVariable<?>> newVariables = null;
			try {
				//noinspection unchecked
				newVariables = (Map<String, NewVariable<?>>) object;
			} catch (Exception ignored) {}
			if (newVariables != null) {
				setVariables(newVariables);
				return;
			}
		}
		int index = 1;
		for (Object value : objects) {
			if (value instanceof Object[] values) {
				for (int sub = 0; sub < values.length; sub++) {
					setIndex(null, index + SEPARATOR + sub, values[sub]);
				}
			} else {
				setIndex(null, "" + index, value);
			}
			index++;
		}
	}

	public @Nullable NewVariable<?> getVariable(String name) {
		assert this.list;
		return variables.get(name);
	}

	public void clearVariables() {
		variables.forEach(((string, newVariable) -> {
			newVariable.setValue(null, this);
		}));
		variables.clear();
	}

	public void removeVariable(NewVariable<?> other) {
		removeVariable(other.name.original);
	}

	private void removeVariable(NewVariable<?> other, @Nullable NewVariable<?> caller) {
		removeVariable(other.name.original, caller);
	}

	public void removeVariable(String name) {
		removeVariable(name, null);
	}

	private void removeVariable(String name, @Nullable NewVariable<?> caller) {
		assert list;
		NewVariable<?> variable = variables.get(name);
		if (variable == null)
			return;
		if (variable != caller)
			variable.setValue(null, this);
		variables.remove(name);
	}

	public <R> @Nullable R convertIfOldPlayer() {
		//noinspection unchecked
		return (R) convertIfOldPlayer(value);
	}

	public <R> @Nullable R convertIfOldPlayer(@Nullable R object) {
		if (!list && SkriptConfig.enablePlayerVariableFix.value() && object instanceof Player oldPlayer) {
			if (!oldPlayer.isValid() && oldPlayer.isOnline()) {
				Player newPlayer = Bukkit.getPlayer(oldPlayer.getUniqueId());
				value = newPlayer;
				//noinspection unchecked
				return (R) newPlayer;
			}
		}
		return object;
	}

	private @Nullable Object get(Event event) {
		Object rawValue = getRaw(event);
		if (!list)
			return rawValue;
		if (rawValue == null)
			return Array.newInstance(types[0], 0);
		List<Object> convertedValues = new ArrayList<>();
		//noinspection unchecked
		for (Entry<String, NewVariable<?>> entry : ((Map<String, NewVariable<?>>) rawValue).entrySet()) {
			NewVariable<?> otherVariable = entry.getValue();
			Object value;
			if (otherVariable.isList()) {
				value = otherVariable.get(null);
			} else {
				value = otherVariable.getValue();
			}
			if (value != null)
				convertedValues.add(otherVariable.convertIfOldPlayer());
		}
		return convertedValues.toArray();
	}

	public @Nullable Object getRaw(Event event) {
		DefaultVariables data = script == null ? null : script.getData(DefaultVariables.class);
		if (data != null)
			data.enterScope();
		try {
			String name = this.name.toString(event);

			if (name.endsWith(NewVariable.SEPARATOR + "*") != list)
				return null;
			Object value = !list ? convertIfOldPlayer() : getValue();
			if (value != null) {
				return value;
			} else if (data == null || !data.hasDefaultVariables()) {
				return null;
			}

			for (String typeHint : this.name.getDefaultVariableNames(name, event)) {
				value = NewVariables.getVariable(typeHint, event, false);
				if (value != null)
					return value;
			}
		} finally {
			if (data != null)
				data.exitScope();
		}
		return null;
	}

	public Iterator<Pair<String, Object>> variablesIterator(Event event)  {
		return NewVariables.getVariableIterator(this, event);
	}

	private @Nullable T getConverted(Event event) {
		assert !list;
		return Converters.convert(get(event), types);
	}

	private T[] getConvertedArray(Event event)  {
		assert list;
		return Converters.convert(((Object[]) get(event)), types, superType);
	}

	private void set(Event event, @Nullable Object value) {
		setValue(value);
	}

	private void set(@Nullable Object value) {
		setValue(value);
	}

	private void setIndex(Event event, String index, @Nullable Object value) {
		assert list;
		Class<?> valueClass = value == null ? Object.class : value.getClass();
		if (index.contains(SEPARATOR) && value != null) {
			String[] split = index.split(SEPARATOR);
			NewVariable<?> lastVariable = this;
			for (int i = 0; i < split.length; i++) {
				String subName = split[i];

				NewVariable<?> subVariable = lastVariable.getVariable(subName);
				if (subVariable == null)
					subVariable = newInstance(subName, CollectionUtils.array(valueClass), i < split.length - 1, lastVariable);
				lastVariable = subVariable;
			}
			lastVariable.setValue(this);
		} else {
			NewVariable<?> subVariable = variables.get(index);
			if (value != null) {
				if (subVariable == null)
					subVariable = newInstance(index, CollectionUtils.array(valueClass), false, this);
				subVariable.setValue(value);
			} else if (subVariable != null) {
				subVariable.setValue(value);
			}
		}
	}

	public boolean isIndexLoop(String input) {
		return input.equalsIgnoreCase("index");
	}

	public NewVariable<?> copy() {
		NewVariable<?> copyVariable = new NewVariable<>(name, types, local, list, source);

		if ((!list && value != null) || (list && !variables.isEmpty())) {
			if (list) {
				variables.forEach((string, variable) -> {
					copyVariable.setVariable(variable.copy());
				});
			} else {
				ClassInfo classInfo = Classes.getSuperClassInfo(value.getClass());
				//noinspection unchecked
				copyVariable.value = classInfo.clone(value);
			}
		}

		return copyVariable;
	}

	public int variablesSize() {
		if (!list)
			return 1;

		int size = 1;
		for (Entry<String, NewVariable<?>> entry : variables.entrySet())
			size += entry.getValue().variablesSize();
		return size;
	}

	// -- STATIC -- //

	/**
	 * Checks whether a string is a valid variable name. This is used to verify variable names as well as command and function arguments.
	 *
	 * @param name The name to test
	 * @param allowListVariable Whether to allow a list variable
	 * @param printErrors Whether to print errors when they are encountered
	 * @return true if the name is valid, false otherwise.
	 */
	public static boolean isValidVariableName(String name, boolean allowListVariable, boolean printErrors) {
		assert !name.isEmpty(): "Variable name should not be empty";
		char nameToken = name.charAt(0);
		check_reserved_tokens:
		for (char token : reservedTokens) {
			if (nameToken == token && printErrors) {
				/*
				A lot of people already use '-' so we want to skip this warning iff they're using it here
				*/
				if (nameToken == '-') {
					for (VariablesStorage storage : NewVariables.getStores()) {
						Pattern pattern = storage.getNamePattern();
						if (pattern != null && pattern.pattern().equals("(?!-).*"))
							continue check_reserved_tokens;
					}
				}
				Skript.warning("The character '" + token + "' is reserved at the start of variable names, "
					+ "and may be restricted in future versions.");
			}
		}

		name = name.startsWith(LOCAL_VARIABLE_TOKEN) ? name.substring(LOCAL_VARIABLE_TOKEN.length()).trim() : name.trim();
		if (!allowListVariable && name.contains(SEPARATOR)) {
			if (printErrors)
				Skript.error("List variables are not allowed here (error in variable {" + name + "})");
			return false;
		} else if (name.startsWith(SEPARATOR) || name.endsWith(SEPARATOR)) {
			if (printErrors)
				Skript.error("A variable's name must neither start nor end with the separator '"
					+ SEPARATOR + "' (error in variable {" + name + "})");
			return false;
		} else if (name.contains(SEPARATOR + SEPARATOR)) {
			if (printErrors)
				Skript.error("A variable's name must not contain the separator '" + SEPARATOR + "' multiple times in a row "
					+ "(error in variable {" + name + "})");
			return false;
		} else if (name.replace(SEPARATOR, "").contains(SINGLE_SEPARATOR_CHAR)) {
			if (printErrors)
				Skript.warning("If you meant to make the variable {" + name + "} a list, its name should contain'"
					+ SEPARATOR + "'. Having a single '" + SINGLE_SEPARATOR_CHAR + "' does nothing!");
		} else if (name.contains("*") && (!allowListVariable || name.indexOf("*") != name.length() - 1 || !name.endsWith(SEPARATOR + "*"))) {
			List<Integer> asterisks = new ArrayList<>();
			List<Integer> percents = new ArrayList<>();
			for (int i = 0; i < name.length(); i++) {
				char character = name.charAt(i);
				if (character == '*') {
					asterisks.add(i);
				} else if (character == '%') {
					percents.add(i);
				}
			}
			int count = asterisks.size();
			int index = 0;
			for (int i = 0; i < percents.size(); i += 2) {
				if (index == asterisks.size() || i+1 == percents.size()) // Out of bounds
					break;
				int lowerBound = percents.get(i), upperBound = percents.get(i+1);
				// Continually decrement asterisk count by checking if any asterisks in current range
				while (index < asterisks.size() && lowerBound < asterisks.get(index) && asterisks.get(index) < upperBound) {
					count--;
					index++;
				}
			}
			if (!(count == 0 || (count == 1 && name.endsWith(SEPARATOR + "*")))) {
				if (printErrors) {
					Skript.error("A variable's name must not contain any asterisks except at the end after '"
						+ SEPARATOR + "' to denote a list variable, e.g. {variable" + SEPARATOR + "*} (error in variable {" + name + "})");
				}
				return false;
			}
		}
		return true;
	}

	/**
	 * Prints errors
	 */
	public static <T> @Nullable NewVariable<T> newInstance(String name, Class<? extends T>[] types) {
		name = name.trim();
		if (!isValidVariableName(name, true, true))
			return null;
		VariableString variableString = VariableString.newInstance(
			name.startsWith(LOCAL_VARIABLE_TOKEN) ? name.substring(LOCAL_VARIABLE_TOKEN.length()).trim() : name, StringMode.VARIABLE_NAME);
		if (variableString == null)
			return null;

		boolean isLocal = name.startsWith(LOCAL_VARIABLE_TOKEN);
		boolean isList = name.endsWith(SEPARATOR + "*");

		ParserInstance parser = ParserInstance.get();
		Script currentScript = parser.isActive() ? parser.getCurrentScript() : null;
		if (currentScript != null
			&& !SkriptConfig.disableVariableStartingWithExpressionWarnings.value()
			&& !currentScript.suppressesWarning(ScriptWarning.VARIABLE_STARTS_WITH_EXPRESSION)
			&& (isLocal ? name.substring(LOCAL_VARIABLE_TOKEN.length()) : name).startsWith("%")) {
			Skript.warning("Starting a variable's name with an expression is discouraged ({" + name + "}). " +
				"You could prefix it with the script's name: " +
				"{" + StringUtils.substring(currentScript.getConfig().getFileName(), 0, -3) + SEPARATOR + name + "}");
		}

		// Check for local variable type hints
		if (isLocal && variableString.isSimple()) { // Only variable names we fully know already
			Class<?> hint = TypeHints.get(variableString.toString());
			if (hint != null && !hint.equals(Object.class)) { // Type hint available
				// See if we can get correct type without conversion
				for (Class<? extends T> type : types) {
					assert type != null;
					if (type.isAssignableFrom(hint)) {
						// Hint matches, use variable with exactly correct type
						return new NewVariable<>(variableString, CollectionUtils.array(type), true, isList, null);
					}
				}

				// Or with conversion?
				for (Class<? extends T> type : types) {
					if (Converters.converterExists(hint, type)) {
						// Hint matches, even though converter is needed
						return new NewVariable<>(variableString, CollectionUtils.array(type), true, isList, null);
					}

					// Special cases
					if (type.isAssignableFrom(World.class) && hint.isAssignableFrom(String.class)) {
						// String->World conversion is weird spaghetti code
						return new NewVariable<>(variableString, types, true, isList, null);
					} else if (type.isAssignableFrom(Player.class) && hint.isAssignableFrom(String.class)) {
						// String->Player conversion is not available at this point
						return new NewVariable<>(variableString, types, true, isList, null);
					}
				}

				// Hint exists and does NOT match any types requested
				ClassInfo<?>[] infos = new ClassInfo[types.length];
				for (int i = 0; i < types.length; i++) {
					infos[i] = Classes.getExactClassInfo(types[i]);
				}
				Skript.warning("Variable '{_" + name + "}' is " + Classes.toString(Classes.getExactClassInfo(hint))
					+ ", not " + Classes.toString(infos, false));
				// Fall back to not having any type hints
			}
		}

		return new NewVariable<>(variableString, types, isLocal, isList, null);
	}

	static <T> NewVariable<T> newInstance(String name, Class<? extends T>[] types, boolean local, boolean list) {
		name = name.trim();
		assert isValidVariableName(name, true, false);
		VariableString variableString = VariableString.newInstance(name, StringMode.VARIABLE_NAME);
		assert variableString != null;

		return new NewVariable<>(variableString, types, local, list, null);
	}

	static NewVariable<?> newInstance(String name, Class<?>[] types, boolean list, NewVariable<?> parent) {
		NewVariable<?> variable = newInstance(name, types, parent.local, list);
		parent.setVariable(variable);
		return variable;
	}

	public static <R> @Nullable R convertIfOldPlayer(NewVariable<?> newVariable, @Nullable R object) {
		if (object == null)
			return newVariable.convertIfOldPlayer();
		return newVariable.convertIfOldPlayer(object);
	}

	// ----- EXPRESSION ------ //

	@Override
	public Expression<?> getSource() {
		return source == null ? this : source;
	}

	@Override
	public @Nullable T getSingle(Event event) {
		if (list)
			throw new SkriptAPIException("Invalid call to getSingle");
		return getConverted(event);
	}

	@Override
	public T[] getArray(Event event) {
		return getAll(event);
	}

	@Override
	public T[] getAll(Event event) {
		if (list)
			return getConvertedArray(event);
		T value = getConverted(event);
		if (value == null)
			//noinspection unchecked
			return (T[]) Array.newInstance(superType, 0);
		//noinspection unchecked
		T[] valueArray = (T[]) Array.newInstance(superType, 1);
		valueArray[0] = value;
		return valueArray;
	}

	@Override
	public Expression<? extends T> simplify() {
		return this;
	}

	@Override
	public Class<?> @Nullable [] acceptChange(ChangeMode mode) {
		if (!list && mode == ChangeMode.SET)
			return CollectionUtils.array(Object.class);
		return CollectionUtils.array(Object[].class);
	}

	@Override
	public void change(Event event, Object @Nullable [] delta, ChangeMode mode) {
		switch (mode) {
			case DELETE -> {
				set(event, null);
			}
			case SET -> {
				assert delta != null;
				if (list) {
					set(event, null);
					int index = 1;
					for (Object value : delta) {
						if (value instanceof Object[] objects) {
							for (int sub = 0; sub < objects.length; sub++) {
								setIndex(event, index + SEPARATOR + sub, objects[sub]);
							}
						} else {
							setIndex(event, "" + index, value);
						}
						index++;
					}
				} else if (delta.length > 0) {
					set(event, delta[0]);
				}
			}
			case ADD -> {
				assert delta != null;
				if (list) {
					int index = 1;
					for (Object value : delta) {
						while (variables.containsKey("" + index))
							index++;
						setIndex(event, "" + index, value);
						index++;
					}
				}
			}
			case REMOVE -> {
				assert delta != null;
				if (list) {
					Set<String> toRemove = new HashSet<>();
					Set<Entry<String, NewVariable<?>>> entries = variables.entrySet();
					for (Object value : delta) {
						for (Entry<String, NewVariable<?>> entry : entries) {
							String key = entry.getKey();
							if (key == null || toRemove.contains(key)) {
								continue;
							} else if (Relation.EQUAL.isImpliedBy(Comparators.compare(entry.getValue(), value))) {
								toRemove.add(key);
								break;
							}
						}
					}
					for (String index : toRemove) {
						assert index != null;
						removeVariable(index);
					}
				}
			}
			case REMOVE_ALL -> {
				assert delta != null;
				if (list) {
					Set<String> toRemove = new HashSet<>();
					Set<Entry<String, NewVariable<?>>> entries = variables.entrySet();
					for (Object value : delta) {
						for (Entry<String, NewVariable<?>> entry : entries) {
							String key = entry.getKey();
							if (key == null) {
								continue;
							} else if (Relation.EQUAL.isImpliedBy(Comparators.compare(entry.getValue(), value))) {
								toRemove.add(key);
							}
						}
					}
					for (String index : toRemove) {
						assert index != null;
						removeVariable(index);
					}
				}
			}
			case RESET -> {
				if (!list && value == null)
					return;
				for (Object values : list ? variables.values() : List.of(value)) {
					Class<?> type = values.getClass();
					assert type != null;
					ClassInfo<?> classInfo = Classes.getSuperClassInfo(type);
					Changer<?> changer = classInfo.getChanger();
					if (changer != null && changer.acceptChange(ChangeMode.RESET) != null) {
						Object[] valueArray = (Object[]) Array.newInstance(type, 1);
						valueArray[0] = values;
						//noinspection rawtypes,unchecked
						((Changer) changer).change(valueArray, null, ChangeMode.RESET);
					}
				}
			}
		}
		if (!list && (mode == ChangeMode.ADD || mode == ChangeMode.REMOVE || mode == ChangeMode.REMOVE_ALL)) {
			Object originalValue = value;
			Class<?> valueClass = originalValue == null ? null : originalValue.getClass();
			Operator operator = mode == ChangeMode.ADD ? Operator.ADDITION : Operator.SUBTRACTION;
			Changer<?> changer;
			Class<?>[] classes;
			if (valueClass == null || !Arithmetics.getOperations(operator, valueClass).isEmpty()) {
				boolean changed = false;
				for (Object newValue : delta) {
					//noinspection rawtypes,unchecked
					OperationInfo info = Arithmetics.getOperationInfo(
						operator,
						valueClass != null ? (Class) valueClass : newValue.getClass(),
						newValue.getClass());
					if (info == null)
						continue;

					//noinspection unchecked
					Object value = originalValue == null ? Arithmetics.getDefaultValue(info.getLeft()) : originalValue;
					if (value == null)
						continue;

					//noinspection unchecked
					originalValue = info.getOperation().calculate(value, newValue);
					changed = true;
				}
				if (changed)
					set(event, originalValue);
			} else if ((changer = Classes.getSuperClassInfo(valueClass).getChanger()) != null && (classes = changer.acceptChange(mode)) != null) {
				Object[] originalArray = (Object[]) Array.newInstance(valueClass, 1);
				originalArray[0] = originalValue;

				Class<?>[] classes2 = new Class<?>[classes.length];
				for (int i = 0; i < classes.length; i++)
					classes2[i] = classes[i].isArray() ? classes[i].getComponentType() : classes[i];

				List<Object> convertedDelta = new ArrayList<>();
				for (Object value : delta) {
					Object convertedValue = Converters.convert(value, classes2);
					if (convertedValue != null)
						convertedDelta.add(convertedValue);
				}
				ChangerUtils.change(changer, originalArray, convertedDelta.toArray(), mode);
			}
		}
	}

	@Override
	public <R> void changeInPlace(Event event, Function<T, R> changeFunction, boolean getAll) {
		changeInPlace(event, changeFunction);
	}

	@Override
	public <R> void changeInPlace(Event event, Function<T, R> changeFunction) {
		if (!list) {
			T value = getSingle(event);
			if (value == null)
				return;
			set(event, changeFunction.apply(value));
			return;
		}
		variablesIterator(event).forEachRemaining(pair -> {
			String index = pair.getKey();
			T value = Converters.convert(pair.getValue(), types);
			if (value == null)
				return;
			Object newValue = changeFunction.apply(value);
			setIndex(event, index, newValue);
		});
	}

	// -- SYNTAX ELEMENT -- //

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean check(Event event, Predicate<? super T> checker, boolean negated) {
		return SimpleExpression.check(getAll(event), checker, negated, getAnd());
	}

	@Override
	public boolean check(Event event, Predicate<? super T> checker) {
		return SimpleExpression.check(getAll(event), checker, false, getAnd());
	}

	@Override
	@SafeVarargs
	public final <R> NewVariable<R> getConvertedExpression(Class<R>... to) {
		return new NewVariable<>(name, to, local, list, this);
	}

	@Override
	public Class<? extends T> getReturnType() {
		return superType;
	}

	@Override
	public boolean getAnd() {
		return true;
	}

	@Override
	public boolean isSingle() {
		return !list;
	}

	@Override
	public boolean setTime(int time) {
		return false;
	}

	@Override
	public int getTime() {
		return 0;
	}

	@Override
	public boolean isDefault() {
		return false;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		StringBuilder stringBuilder = new StringBuilder()
			.append("{");
		if (local)
			stringBuilder.append(LOCAL_VARIABLE_TOKEN);
		stringBuilder.append(StringUtils.substring(name.toString(event, debug), 1, -1))
			.append("}");

		if (debug) {
			stringBuilder.append(" (");
			if (event != null) {
				stringBuilder.append(Classes.toString(get(event)))
					.append(", ");
			}
			stringBuilder.append("as ")
				.append(superType.getName())
				.append(")");
		}
		return stringBuilder.toString();
	}

	@Override
	public String toString() {
		return toString(null, false);
	}

	// -- KEY PROVIDER EXPRESSION -- //

	@Override
	public @NotNull String @NotNull [] getArrayKeys(Event event) throws IllegalStateException {
		if (!list)
			throw new SkriptAPIException("Invalid call to getArrayKeys on non-list");
		return variables.keySet().toArray(String[]::new);
	}

	@Override
	public @NotNull String @NotNull [] getAllKeys(Event event) {
		return getArrayKeys(event);
	}

	@Override
	public boolean areKeysRecommended() {
		return false;
	}

	// -- KEY RECEIVER EXPRESSION -- //

	@Override
	public void change(Event event, Object @NotNull [] delta, ChangeMode mode, @NotNull String @NotNull [] keys) {
		if (!list) {
			this.change(event, delta, mode);
			return;
		}
		if (mode == ChangeMode.SET) {
			assert delta.length == keys.length;
			this.set(event, null);
			int length = delta.length;
			for (int index = 0; index < length; index++) {
				Object value = delta[index];
				String key = keys[index];
				if (value instanceof Object[] objects) {
					for (int j = 0; j < objects.length; j++)
						this.setIndex(event, key + SEPARATOR + (j + 1), objects[j]);
				} else {
					this.setIndex(event, key, value);
				}
			}
		} else {
			this.change(event, delta, mode);
		}
	}

	// -- LOOPABLE -- //

	@Override
	public @Nullable Iterator<? extends T> iterator(Event event) {
		if (!list) {
			T value = getSingle(event);
			return value != null ? new SingleItemIterator<>(value) : null;
		}
		Iterator<String> keys = new ArrayList<>(variables.keySet()).iterator();
		return new Iterator<>() {

			private @Nullable String key = null;
			private @Nullable T nextObject = null;
			private @Nullable NewVariable<?> nextVariable = null;

			@Override
			public boolean hasNext() {
				if (nextObject != null)
					return true;
				while (keys.hasNext()) {
					key = keys.next();
					if (key != null) {
						nextVariable = variables.get(key);
						if (nextVariable != null) {
							nextObject = Converters.convert(nextVariable.getValue(), types);
							nextObject = nextVariable.convertIfOldPlayer();
							if (nextObject != null && !nextVariable.isList())
								return true;
						}
					}
				}
				key = null;
				nextObject = null;
				nextVariable = null;
				return false;
			}

			@Override
			public T next() {
				if (!hasNext())
					throw new NoSuchElementException();
				T next = nextObject;
				assert next != null;
				key = null;
				nextObject = null;
				nextVariable = null;
				return next;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public boolean isLoopOf(String input) {
		return input.equalsIgnoreCase("var")
			|| input.equalsIgnoreCase("variable")
			|| input.equalsIgnoreCase("value")
			|| input.equalsIgnoreCase("index");
	}

	@Override
	public boolean supportsLoopPeeking() {
		return true;
	}

}
