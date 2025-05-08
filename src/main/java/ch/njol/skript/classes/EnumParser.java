package ch.njol.skript.classes;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.Noun;
import ch.njol.skript.util.StringMode;
import ch.njol.util.NonNullPair;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converter;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EnumParser<E extends Enum<E>> extends PatternedParser<E> implements Converter<String, E> {

	private final Class<E> enumClass;
	private final String languageNode;
	private final boolean isLanguage;
	private String[] names;
	private final Map<String, E> parseMap = new HashMap<>();

	public EnumParser(Class<E> enumClass, String languageNode) {
		this(enumClass, languageNode, true);
	}

	public EnumParser(Class<E> enumClass, String languageNode, boolean isLanguage) {
		assert enumClass.isEnum() : enumClass;
		assert !languageNode.isEmpty() && !languageNode.endsWith(".") : languageNode;
		this.enumClass = enumClass;
		this.languageNode = languageNode;
		this.isLanguage = isLanguage;

		if (isLanguage) {
			refresh();
			Language.addListener(this::refresh);
		} else {
			readEnum();
		}
	}

	/**
	 * Refreshes the representation of this Enum based on the currently stored language entries.
	 */
	void refresh() {
		E[] constants = enumClass.getEnumConstants();
		names = new String[constants.length];
		parseMap.clear();
		for (E constant : constants) {
			String key = languageNode + "." + constant.name();
			int ordinal = constant.ordinal();

			String[] options = Language.getList(key);
			for (String option : options) {
				option = option.toLowerCase(Locale.ENGLISH);
				if (options.length == 1 && option.equals(key.toLowerCase(Locale.ENGLISH))) {
					String[] splitKey = key.split("\\.");
					String newKey = splitKey[1].replace('_', ' ').toLowerCase(Locale.ENGLISH) + " " + splitKey[0];
					parseMap.put(newKey, constant);
					Skript.debug("Missing lang enum constant for '" + key + "'. Using '" + newKey + "' for now.");
					continue;
				}

				// Isolate the gender if one is present
				NonNullPair<String, Integer> strippedOption = Noun.stripGender(option, key);
				String first = strippedOption.getFirst();
				Integer second = strippedOption.getSecond();

				if (names[ordinal] == null) { // Add to name array if needed
					names[ordinal] = first;
				}

				parseMap.put(first, constant);
				if (second != -1) { // There is a gender present
					parseMap.put(Noun.getArticleWithSpace(second, Language.F_INDEFINITE_ARTICLE) + first, constant);
				}
			}
		}
	}

	private void readEnum() {
		E[] constants = enumClass.getEnumConstants();
		names = new String[constants.length];
		parseMap.clear();
		for (E constant : constants) {
			int ordinal = constant.ordinal();
			String name = constant.name().toLowerCase(Locale.ENGLISH);
			if (names[ordinal] == null)
				names[ordinal] = name;
			parseMap.put(name, constant);
			String nameWithSpaces = name.replace("_", " ");
			if (!nameWithSpaces.equals(name))
				parseMap.put(nameWithSpaces, constant);
		}
	}

	@Override
	public @Nullable E parse(String string, ParseContext context) {
		return parseMap.get(string.toLowerCase(Locale.ENGLISH));
	}

	@Override
	public @Nullable E convert(String string) {
		E constant = parse(string, ParseContext.DEFAULT);
		if (constant != null)
			return constant;
		Skript.error("'" + string + "' is not a valid value for " + languageNode + ". Allowed values are: " + getCombinedPatterns());
		return null;
	}

	@Override
	public String toVariableNameString(E constant) {
		return toString(constant, StringMode.VARIABLE_NAME);
	}

	@Override
	public String[] getPatterns() {
		return parseMap.keySet().toArray(String[]::new);
	}

	@Override
	public String toString(E enumerator, int flags) {
		String s = names[enumerator.ordinal()];
		return s != null ? s : enumerator.name();
	}

}
