package ch.njol.skript.classes;

import ch.njol.util.StringUtils;

/**
 * {@link Parser} class used to define that literal patterns are present.
 */
public abstract class PatternedParser<T> extends Parser<T> {

	/**
	 * Get all literal patterns that represents an {@link Object} within a 'lang' file.
	 */
	public abstract String[] getPatterns();

	/**
	 * Combine all literal patterns from {@link #getPatterns()} into a single {@link String}.
	 */
	public String getCombinedPatterns() {
		String[] patterns = getPatterns();
		return StringUtils.join(patterns, ", ");
	}

}
