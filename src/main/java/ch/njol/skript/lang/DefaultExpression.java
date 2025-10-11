package ch.njol.skript.lang;

/**
 * Represents an expression that can be used as the default value of a certain type or event.
 */
public interface DefaultExpression<Type> extends Expression<Type>, org.skriptlang.skript.lang.context.DefaultExpression<Type> {

}
