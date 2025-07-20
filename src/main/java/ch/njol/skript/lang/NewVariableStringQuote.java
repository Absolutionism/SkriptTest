package ch.njol.skript.lang;

import ch.njol.skript.lang.NewVariableString.NewVariableStringType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

final class NewVariableStringQuote extends NewVariableStringType {

	private static final Map<Character, BiFunction<String, Integer, Boolean>> FILTER = new HashMap<>();

	static {
		FILTER.put('"', (string, integer) -> true);
	}

	NewVariableStringQuote() {
		super();
	}

	@Override
	public String getEnclosingString() {
		return "\"";
	}

	@Override
	public boolean isQuotedCorrectly(String string, boolean withQuotes) {
		if (withQuotes && (!properlyEncased(string) || string.length() < 2))
			return false;
		boolean inQuote = false;
		boolean inExpression = false;
		if (withQuotes)
			string = string.substring(1, string.length() - 1);
		for (int i = 0; i < string.length(); i++) {
			char character = string.charAt(i);
			if (inExpression) {
				if (character == '%')
					inExpression = false;
				continue;
			}
			if (character == '\\') {
				i++;
				continue;
			}
			if (inQuote && character != '"')
				return false;
			if (character == '"') {
				inQuote = !inQuote;
			} else if (character == '%') {
				inExpression = true;
			}
		}
		return !inQuote;
	}

	@Override
	public Map<Character, BiFunction<String, Integer, Boolean>> getStringFilter() {
		return FILTER;
	}

	@Override
	public String quote(String string) {
		StringBuilder fixed = new StringBuilder();
		boolean inExpression = false;
		for (char character : string.toCharArray()) {
			if (character == '%')
				inExpression = !inExpression;
			if (!inExpression && character == '"')
				fixed.append('"');
			fixed.append(character);
		}
		return fixed.toString();
	}

	@Override
	public String unquote(String string, boolean surroundingQuotes) {
		assert isQuotedCorrectly(string, surroundingQuotes);
		if (surroundingQuotes)
			return string.substring(1, string.length() - 1).replace("\"\"", "\"");
		return string.replace("\"\"", "\"");
	}

}
