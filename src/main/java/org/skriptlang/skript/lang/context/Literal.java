package org.skriptlang.skript.lang.context;

import org.jetbrains.annotations.Nullable;

/**
 * A literal, e.g. a number, string or item. Literals are constants which do not depend on the event and can thus e.g. be used in events.
 */
public interface Literal<Type> extends Expression<Type> {

	Type[] getArray();

	Type getSingle();

	@Override
	@SuppressWarnings("unchecked")
	<ConvertType> @Nullable Literal<? extends ConvertType> getConvertedExpression(Class<ConvertType>... to);

	Type[] getAll();

}
