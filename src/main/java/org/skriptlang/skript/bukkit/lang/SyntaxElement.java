package org.skriptlang.skript.bukkit.lang;

import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import org.skriptlang.skript.lang.context.Expression;

import java.util.Arrays;

/**
 * Represents a general part of the syntax.
 */
public interface SyntaxElement extends org.skriptlang.skript.lang.context.SyntaxElement {

	@Override
	default boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		org.skriptlang.skript.bukkit.lang.Expression<?>[] exprs = Arrays.stream(expressions)
			.filter(expression -> expression instanceof org.skriptlang.skript.bukkit.lang.Expression<?>)
			.toArray(org.skriptlang.skript.bukkit.lang.Expression[]::new);
		return init(exprs, matchedPattern, isDelayed, parseResult);
	}

	boolean init(org.skriptlang.skript.bukkit.lang.Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult);

}
