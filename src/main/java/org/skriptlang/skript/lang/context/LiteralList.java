package org.skriptlang.skript.lang.context;

import ch.njol.skript.lang.UnparsedLiteral;
import ch.njol.skript.registrations.Classes;
import org.jetbrains.annotations.Nullable;

/**
 * A list of literals. Can contain {@link UnparsedLiteral}s.
 */
public class LiteralList<Type> extends ExpressionList<Type> implements Literal<Type> {

	public LiteralList(Literal<? extends Type>[] literals, Class<Type> returnType, boolean and) {
		super(literals, returnType, and);
	}

	public LiteralList(Literal<? extends Type>[] literals, Class<Type> returnType, Class<?>[] possibleReturnTypes, boolean and) {
		super(literals, returnType, possibleReturnTypes, and);
	}

	public LiteralList(Literal<? extends Type>[] literals, Class<Type> returnType, boolean and, LiteralList<?> source) {
		super(literals, returnType, and, source);
	}

	public LiteralList(Literal<? extends Type>[] literals, Class<Type> returnType, Class<?>[] possibleReturnTypes, boolean and, LiteralList<?> source) {
		super(literals, returnType, possibleReturnTypes, and, source);
	}

	@Override
	public Type[] getArray() {
		return getArray(null);
	}

	@Override
	public Type getSingle() {
		return getSingle(null);
	}

	@Override
	public Type[] getAll() {
		return getAll(null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <ConvertType> @Nullable Literal<? extends ConvertType> getConvertedExpression(final Class<ConvertType>... to) {
		Literal<? extends ConvertType>[] exprs = new Literal[expressions.length];
		Class<?>[] returnTypes = new Class[expressions.length];
		for (int i = 0; i < exprs.length; i++) {
			if ((exprs[i] = (Literal<? extends ConvertType>) expressions[i].getConvertedExpression(to)) == null)
				return null;
			returnTypes[i] = exprs[i].getReturnType();
		}
		return new LiteralList<>(exprs, (Class<ConvertType>) Classes.getSuperClassInfo(returnTypes).getC(), returnTypes, and, this);
	}

	@Override
	public Literal<? extends Type>[] getExpressions() {
		return (Literal<? extends Type>[]) super.getExpressions();
	}

	@Override
	public Expression<Type> simplify() {
		return this;
	}

}
