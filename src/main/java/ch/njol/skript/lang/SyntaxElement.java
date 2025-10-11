package ch.njol.skript.lang;

import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;

import java.util.Arrays;

/**
 * Represents a general part of the syntax.
 */
public interface SyntaxElement extends org.skriptlang.skript.bukkit.lang.SyntaxElement {

	@Override
	default boolean init(org.skriptlang.skript.bukkit.lang.Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		ch.njol.skript.lang.Expression<?>[] exprs = Arrays.stream(expressions)
			.filter(expression -> expression instanceof ch.njol.skript.lang.Expression<?>)
			.toArray(ch.njol.skript.lang.Expression[]::new);
		return init(exprs, matchedPattern, isDelayed, parseResult);
	}

	boolean init(ch.njol.skript.lang.Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult);

}
