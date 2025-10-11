package org.skriptlang.skript.bukkit.lang;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.conditions.CondCompare;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Predicate;

public class ExpressionList<Type> extends org.skriptlang.skript.lang.context.ExpressionList<Type> implements Expression<Type> {

	protected final Expression<? extends Type>[] expressions;
	private final Class<Type> returnType;
	private final Class<?>[] possibleReturnTypes;
	protected boolean and;
	private final boolean single;

	private final @Nullable ExpressionList<?> source;

	public ExpressionList(Expression<? extends Type>[] expressions, Class<Type> returnType, boolean and) {
		this(expressions, returnType, and, null);
	}

	public ExpressionList(Expression<? extends Type>[] expressions, Class<Type> returnType, Class<?>[] possibleReturnTypes, boolean and) {
		this(expressions, returnType, possibleReturnTypes, and, null);
	}

	protected ExpressionList(Expression<? extends Type>[] expressions, Class<Type> returnType, boolean and, @Nullable ExpressionList<?> source) {
		this(expressions, returnType, new Class[]{returnType}, and, source);
	}

	protected ExpressionList(Expression<? extends Type>[] expressions, Class<Type> returnType, Class<?>[] possibleReturnTypes, boolean and, @Nullable ExpressionList<?> source) {
		super(expressions, returnType, possibleReturnTypes, and, source);
		assert expressions != null;
		this.expressions = expressions;
		this.returnType = returnType;
		this.possibleReturnTypes = ImmutableSet.copyOf(possibleReturnTypes).toArray(new Class[0]);
		this.and = and;
		if (and) {
			single = false;
		} else {
			boolean single = true;
			for (Expression<?> e : expressions) {
				if (!e.isSingle()) {
					single = false;
					break;
				}
			}
			this.single = single;
		}
		this.source = source;
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		throw new UnsupportedOperationException();
	}

	@Override
	public @Nullable Type getSingle(BukkitContext<?> context) {
		if (!single)
			throw new UnsupportedOperationException();
		Expression<? extends Type> expression = CollectionUtils.getRandom(expressions);
		return expression != null ? expression.getSingle(context) : null;
	}

	@Override
	public Type[] getArray(BukkitContext<?> context) {
		if (and)
			return getAll(context);
		Expression<? extends Type> expression = CollectionUtils.getRandom(expressions);
		//noinspection unchecked
		return expression != null ? expression.getArray(context) : (Type[]) Array.newInstance(returnType, 0);
	}

	@Override
	public Type[] getAll(BukkitContext<?> context) {
		List<Type> values = new ArrayList<>();
		for (Expression<? extends Type> expr : expressions)
			values.addAll(Arrays.asList(expr.getAll(context)));
		//noinspection unchecked
		return values.toArray((Type[]) Array.newInstance(returnType, values.size()));
	}

	@Override
	public @Nullable Iterator<? extends Type> iterator(BukkitContext<?> context) {
		if (!and) {
			Expression<? extends Type> expression = CollectionUtils.getRandom(expressions);
			return expression != null ? expression.iterator(context) : null;
		}
		return new Iterator<>() {
			private int i = 0;
			@Nullable
			private Iterator<? extends Type> current = null;

			@Override
			public boolean hasNext() {
				Iterator<? extends Type> iterator = current;
				while (i < expressions.length && (iterator == null || !iterator.hasNext()))
					current = iterator = expressions[i++].iterator(context);
				return iterator != null && iterator.hasNext();
			}

			@Override
			public Type next() {
				if (!hasNext())
					throw new NoSuchElementException();
				Iterator<? extends Type> iterator = current;
				if (iterator == null)
					throw new NoSuchElementException();
				Type value = iterator.next();
				assert value != null : current;
				return value;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public boolean isSingle() {
		return single;
	}

	@Override
	public boolean check(BukkitContext<?> context, Predicate<? super Type> checker, boolean negated) {
		return CollectionUtils.check(expressions, expr -> expr.check(context, checker) ^ negated, and);
	}

	@Override
	public boolean check(BukkitContext<?> context, Predicate<? super Type> checker) {
		return check(context, checker, false);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R> @Nullable Expression<? extends R> getConvertedExpression(Class<R>... to) {
		Expression<? extends R>[] exprs = new Expression[expressions.length];
		Set<Class<?>> possibleReturnTypeSet = new HashSet<>();
		for (int i = 0; i < exprs.length; i++) {
			if ((exprs[i] = expressions[i].getConvertedExpression(to)) == null)
				return null;
			possibleReturnTypeSet.addAll(Arrays.asList(exprs[i].possibleReturnTypes()));
		}
		Class<?>[] possibleReturnTypes = possibleReturnTypeSet.toArray(new Class[0]);
		return new ExpressionList<>(exprs, (Class<R>) Classes.getSuperClassInfo(possibleReturnTypes).getC(), possibleReturnTypes, and, this);
	}

	@Override
	public Class<Type> getReturnType() {
		return returnType;
	}

	@Override
	public Class<? extends Type>[] possibleReturnTypes() {
		//noinspection unchecked
		return (Class<? extends Type>[]) possibleReturnTypes;
	}

	@Override
	public boolean getAnd() {
		return and;
	}

	/**
	 * For use in {@link CondCompare} only.
	 */
	public void invertAnd() {
		and = !and;
	}

	@Override
	public Class<?> @Nullable [] acceptChange(ChangeMode mode) {

		// given X: Object.class, Y: Vector.class, Number.class, Z: Integer.class
		// output should be Integer.class.

		// get all accepted type arrays.
		List<Class<?>[]> expressionTypes = new ArrayList<>();
		for (Expression<?> expr : expressions) {
			Class<?>[] exprTypes = expr.acceptChange(mode);
			if (exprTypes == null)
				return null;
			expressionTypes.add(exprTypes);
		}

		// shortcut
		if (expressionTypes.size() == 1)
			return expressionTypes.get(0);

		// iterate over types and keep what works
		Set<Class<?>> acceptable = new LinkedHashSet<>(Arrays.asList(expressionTypes.get(0)));
		for (int i = 1; i < expressionTypes.size(); i++) {
			Set<Class<?>> newAcceptable = new LinkedHashSet<>();

			// Check if each existing acceptable types can be matched to this expr's accepted types
			for (Class<?> candidate : acceptable) {
				for (Class<?> accepted : expressionTypes.get(i)) {
					// keep the more specific version
					if (accepted.isAssignableFrom(candidate)) {
						newAcceptable.add(candidate);
						break;
					} else if (candidate.isAssignableFrom(accepted)) {
						newAcceptable.add(accepted);
						break;
					}
				}
			}

			acceptable = newAcceptable;

			if (acceptable.isEmpty()) {
				return new Class<?>[0]; // Early exit if no common types
			}
		}

		return acceptable.toArray(new Class<?>[0]);
	}

	@Override
	public void change(BukkitContext<?> context, Object @Nullable [] delta, ChangeMode mode) throws UnsupportedOperationException {
		if (and) {
			for (Expression<?> expr : expressions) {
				expr.change(context, delta, mode);
			}
		} else {
			int i = ThreadLocalRandom.current().nextInt(expressions.length);
			expressions[i].change(context, delta, mode);
		}
	}

	@Override
	public <R> void changeInPlace(BukkitContext<?> context, Function<Type, R> changeFunction, boolean getAll) {
		if (and || getAll) {
			for (Expression<?> expr : expressions) {
				//noinspection unchecked,rawtypes
				expr.changeInPlace(context, (Function) changeFunction, getAll);
			}
		} else {
			int i = ThreadLocalRandom.current().nextInt(expressions.length);
			//noinspection unchecked,rawtypes
			expressions[i].changeInPlace(context, (Function) changeFunction, false);
		}
	}

	private int time = 0;

	@Override
	public boolean setTime(int time) {
		boolean ok = false;
		for (Expression<?> e : expressions) {
			ok |= e.setTime(time);
		}
		if (ok)
			this.time = time;
		return ok;
	}

	@Override
	public int getTime() {
		return time;
	}

	@Override
	public boolean isDefault() {
		return false;
	}

	@Override
	public boolean isLoopOf(String input) {
		for (Expression<?> expression : expressions)
			if (expression.isLoopOf(input))
				return true;
		return false;
	}

	@Override
	public Expression<?> getSource() {
		ExpressionList<?> source = this.source;
		return source == null ? this : source;
	}

	@Override
	public String toString(@Nullable BukkitContext<?> context, boolean debug) {
		StringBuilder result = new StringBuilder("(");
		for (int i = 0; i < expressions.length; i++) {
			if (i != 0) {
				if (i == expressions.length - 1)
					result.append(and ? " and " : " or ");
				else
					result.append(", ");
			}
			result.append(expressions[i].toString(context, debug));
		}
		result.append(")");
		if (debug)
			result.append("[").append(returnType).append("]");
		return result.toString();
	}

	@Override
	public String toString() {
		return toString(null, false);
	}

	/**
	 * @return The internal list of expressions. Can be modified with care.
	 */
	public Expression<? extends Type>[] getExpressions() {
		return expressions;
	}

}
