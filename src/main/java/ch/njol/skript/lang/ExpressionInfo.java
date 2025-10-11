package ch.njol.skript.lang;

import org.jetbrains.annotations.Nullable;

/**
 * Represents an expression's information, for use when creating new instances of expressions.
 */
public class ExpressionInfo<E extends Expression<T>, T> extends org.skriptlang.skript.lang.context.ExpressionInfo<E, T> {

	public ExpressionInfo(String[] patterns, Class<T> returnType, Class<E> expressionClass, String originClassPath) throws IllegalArgumentException {
		this(patterns, returnType, expressionClass, originClassPath, null);
	}

	public ExpressionInfo(String[] patterns, Class<T> returnType, Class<E> expressionClass, String originClassPath, @Nullable ExpressionType expressionType) throws IllegalArgumentException {
		super(patterns, returnType, expressionClass, originClassPath, expressionType);
	}

}
