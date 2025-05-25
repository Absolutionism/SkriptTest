package ch.njol.skript.variables;

import ch.njol.skript.lang.NewVariable;
import ch.njol.util.StringUtils;
import ch.njol.util.coll.CollectionUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class NewVariablesMap {

	static final Comparator<String> VARIABLE_NAME_COMPARATOR = (s1, s2) -> {
		if (s1 == null)
			return s2 == null ? 0 : -1;

		if (s2 == null)
			return 1;

		int i = 0;
		int j = 0;

		boolean lastNumberNegative = false;
		boolean afterDecimalPoint = false;
		while (i < s1.length() && j < s2.length()) {
			char c1 = s1.charAt(i);
			char c2 = s2.charAt(j);

			if ('0' <= c1 && c1 <= '9' && '0' <= c2 && c2 <= '9') {
				// Numbers/digits are treated differently from other characters.

				// The index after the last digit
				int i2 = StringUtils.findLastDigit(s1, i);
				int j2 = StringUtils.findLastDigit(s2, j);

				// Amount of leading zeroes
				int z1 = 0;
				int z2 = 0;

				// Skip leading zeroes (except for the last if all 0's)
				if (!afterDecimalPoint) {
					if (c1 == '0') {
						while (i < i2 - 1 && s1.charAt(i) == '0') {
							i++;
							z1++;
						}
					}
					if (c2 == '0') {
						while (j < j2 - 1 && s2.charAt(j) == '0') {
							j++;
							z2++;
						}
					}
				}
				// Keep in mind that c1 and c2 may not have the right value (e.g. s1.charAt(i)) for the rest of this block

				// If the number is prefixed by a '-', it should be treated as negative, thus inverting the order.
				// If the previous number was negative, and the only thing separating them was a '.',
				//  then this number should also be in inverted order.
				boolean previousNegative = lastNumberNegative;

				// i - z1 contains the first digit, so i - z1 - 1 may contain a `-` indicating this number is negative
				lastNumberNegative = i - z1 > 0 && s1.charAt(i - z1 - 1) == '-';
				int isPositive = (lastNumberNegative | previousNegative) ? -1 : 1;

				// Different length numbers (99 > 9)
				if (!afterDecimalPoint && i2 - i != j2 - j)
					return ((i2 - i) - (j2 - j)) * isPositive;

				// Iterate over the digits
				while (i < i2 && j < j2) {
					char d1 = s1.charAt(i);
					char d2 = s2.charAt(j);

					// If the digits differ, return a value dependent on the sign
					if (d1 != d2)
						return (d1 - d2) * isPositive;

					i++;
					j++;
				}

				// Different length numbers (1.99 > 1.9)
				if (afterDecimalPoint && i2 - i != j2 - j)
					return ((i2 - i) - (j2 - j)) * isPositive;

				// If the numbers are equal, but either has leading zeroes,
				//  more leading zeroes is a lesser number (01 < 1)
				if (z1 != z2)
					return (z1 - z2) * isPositive;

				afterDecimalPoint = true;
			} else {
				// Normal characters
				if (c1 != c2)
					return c1 - c2;

				// Reset the last number flags if we're exiting a number.
				if (c1 != '.') {
					lastNumberNegative = false;
					afterDecimalPoint = false;
				}

				i++;
				j++;
			}
		}
		if (i < s1.length())
			return lastNumberNegative ? -1 : 1;
		if (j < s2.length())
			return lastNumberNegative ? 1 : -1;
		return 0;
	};

	private final Map<String, NewVariable<?>> variables = new HashMap<>();

	public NewVariablesMap() {}

	@Nullable NewVariable<?> getVariableObject(String name) {
		String[] split = NewVariables.splitVariableName(name);
		String baseName = split[0];
		NewVariable<?> baseVariable = variables.get(baseName);
		if (split.length == 1) {
			return baseVariable;
		} else if (baseVariable == null || !baseVariable.isList()) {
			return null;
		}

		NewVariable<?> lastVariable = baseVariable;
		for (int i = 1; i < split.length; i++) {
			String subName = split[i];
			if (subName.equals("*")) {
				return lastVariable;
			}

			NewVariable<?> subVariable = lastVariable.getVariable(subName);
			if (subVariable == null)
				return null;

			lastVariable = subVariable;
		}
		return lastVariable;
	}

	@Nullable Object getVariable(String name) {
		NewVariable<?> variable = getVariableObject(name);
		return variable == null ? null : variable.getValue();
	}

	void setVariable(String name, @Nullable Object value) {
		String[] split = NewVariables.splitVariableName(name);
		String baseName = split[0];
		NewVariable<?> baseVariable = variables.get(baseName);
		if (split.length == 1) { // 'name' is not a list
			if (value == null && baseVariable != null) {
				removeVariable(baseName);
			} else if (value != null) {
				// Create a new variable if it does not exist
				if (baseVariable == null) {
					NewVariable<?> newVariable = createVariable(baseName, false, value.getClass());
					if (newVariable == null)
						return;
					baseVariable = newVariable;
					variables.put(baseName, baseVariable);
				}
				// Set the data
				baseVariable.setValue(value);
			}
			return;
		} else if (value == null && baseVariable == null) { // no need to continue
			return;
		}
		// Create list variable
		if (baseVariable == null) {
			NewVariable<?> newVariable = createVariable(baseName, true, null);
			if (newVariable == null)
				return;
			baseVariable = newVariable;
		}
		boolean listReference = name.endsWith("*");
		NewVariable<?> lastVariable = baseVariable;
		for (int i = 1; i < split.length; i++) {
			String subName = split[i];
			if (subName.equals("*")) {
				break;
			}

			// Get sub variable
			NewVariable<?> subVariable = lastVariable.getVariable(subName);
			if (subVariable == null && value == null) { // If sub variable does not exist and data being set is null, no reason to continue
				return;
			} else if (subVariable == null) {
				// Create new sub variable
				NewVariable<?> newVariable = createVariable(subName, i < split.length - 1 || listReference, null);
				if (newVariable == null)
					return;
				subVariable = newVariable;
			}
			subVariable.setParent(lastVariable);

			lastVariable = subVariable;
		}
		if (value == null) {
			lastVariable.setValue(null);
			return;
		}
		lastVariable.setValue(value);
		variables.putIfAbsent(baseName, baseVariable);
	}

	void setVariable(NewVariable<?> newVariable) {
		variables.put(newVariable.getName().toString(null, false), newVariable);
	}

	private void removeVariable(String name) {
		NewVariable<?> variable = variables.remove(name);
		if (variable != null)
			variable.setValue(null);
	}

	public NewVariablesMap copy() {
		NewVariablesMap copyVariablesMap = new NewVariablesMap();

		variables.forEach(((string, variable) -> {
			copyVariablesMap.setVariable(string, variable.copy());
		}));

		return copyVariablesMap;
	}

	public int variablesSize() {
		int size = 0;
		for (Entry<String, NewVariable<?>> entry : variables.entrySet())
			size += entry.getValue().variablesSize();

		return size;
	}

	Map<String, NewVariable<?>> getVariables() {
		return variables;
	}

	private static @Nullable NewVariable<?> createVariable(String name, boolean list, @Nullable Class<?> valueClass) {
		Class<?>[] classes = CollectionUtils.array(valueClass != null ? valueClass : Object.class);
		NewVariable<?> newVariable = NewVariable.newInstance(name, classes);
		if (newVariable == null)
			return null;
		newVariable.setList(list);
		return newVariable;
	}

}
