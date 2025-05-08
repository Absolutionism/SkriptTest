package ch.njol.skript.classes;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.Noun;
import ch.njol.util.NonNullPair;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;

public class FieldParser<T> extends PatternedParser<T> {

	private final Class<T> fieldClass;
	private final String languageNode;
	private Field[] fields;
	private T[] types;
	private final Map<T, String> names = new HashMap<>();
	private final Map<String, T> parseMap = new HashMap<>();

	public FieldParser(Class<T> fieldClass, String languageNode) {
		assert !languageNode.isEmpty() && !languageNode.endsWith(".") : languageNode;
		this.fieldClass = fieldClass;
		this.languageNode = languageNode;

		setFields();
		refresh();
		Language.addListener(this::refresh);
	}

	void refresh() {
		parseMap.clear();
		for (Field field : fields) {
			String name = field.getName().toLowerCase(Locale.ENGLISH);
            T type;
            try {
				//noinspection unchecked
				type = (T) field.get(null);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            String key = languageNode + "." + name;

			String[] options = Language.getList(key);
			for (String option : options) {
				option = option.toLowerCase(Locale.ENGLISH);
				if (options.length == 1 && option.equals(key.toLowerCase(Locale.ENGLISH))) {
					String[] splitKey = key.split("\\.");
					String newKey = splitKey[1].replace("_", " ").toLowerCase(Locale.ENGLISH) + " " + splitKey[0];
					parseMap.put(newKey, type);
					Skript.debug("Missing lang for '" + key + "'. Using '" + newKey + "' for now.");
					continue;
				}

				NonNullPair<String, Integer> strippedOption = Noun.stripGender(option, key);
				String first = strippedOption.getFirst();
				Integer second = strippedOption.getSecond();

				names.putIfAbsent(type, first);

				parseMap.put(first, type);
				if (second != -1) {
					parseMap.put(Noun.getArticleWithSpace(second, Language.F_INDEFINITE_ARTICLE) + first, type);
				}
			}
		}
	}

	private void setFields() {
		List<T> list = new ArrayList<>();
		fields = Arrays.stream(fieldClass.getFields()).filter(field -> {
			try {
				field.setAccessible(true);
				Object object = field.get(null);
				if (fieldClass.isAssignableFrom(object.getClass())) {
					//noinspection unchecked
					list.add((T) object);
					return true;
				}
			} catch (Exception ignored) {}
			return false;
		}).toArray(Field[]::new);
		//noinspection unchecked
		types = (T[]) list.toArray();
	}

	@Override
	public @Nullable T parse(String string, ParseContext context) {
		return parseMap.get(string);
	}

	@Override
	public String toString(T object, int flags) {
		return names.get(object);
	}

	@Override
	public String toVariableNameString(T object) {
		return toString(object, 0);
	}

	@Override
	public String[] getPatterns() {
		return parseMap.keySet().toArray(String[]::new);
	}

	public T[] getTypes() {
		return types;
	}

}
