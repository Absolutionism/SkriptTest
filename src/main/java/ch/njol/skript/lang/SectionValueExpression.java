package ch.njol.skript.lang;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;

public abstract class SectionValueExpression<S extends SyntaxElement & SectionValueProvider, T>
	extends SimpleExpression<T>
	implements DefaultExpression<T> {

	private final S element;
	private final Class<T> returnType;

	public SectionValueExpression(S element, Class<T> returnType) {
		this.element = element;
		this.returnType = returnType;
	}

	public S getElement() {
		return element;
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		return true;
	}

	@Override
	public boolean init() {
		return true;
	}

	abstract public void set(T t);

	@Override
	public Class<?> @Nullable [] acceptChange(ChangeMode mode) {
		return null;
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public boolean isDefault() {
		return true;
	}

	@Override
	public Class<T> getReturnType() {
		return returnType;
	}

	public static class BlankSectionValueExpression<S extends SyntaxElement & SectionValueProvider, T> extends SectionValueExpression<S, T> {

		public BlankSectionValueExpression(S element, Class<T> returnType) {
			super(element, returnType);
		}

		@Override
		public void set(T t) {

		}

		@Override
		protected T @Nullable [] get(Event event) {
			return null;
		}

		@Override
		public String toString(@Nullable Event event, boolean debug) {
			return "";
		}

	}

	public static class SimpleSectionValueExpression<S extends SyntaxElement & SectionValueProvider, T> extends SectionValueExpression<S, T> {

		private T value;

		public SimpleSectionValueExpression(S element, Class<T> returnType) {
			super(element, returnType);
		}

		@Override
		public void set(T value) {
			this.value = value;
		}

		@Override
		protected T @Nullable [] get(Event event) {
			//noinspection unchecked
			T[] array = (T[]) Array.newInstance(getReturnType(), 1);
			array[0] = value;
			return array;
		}

		@Override
		public String toString(@Nullable Event event, boolean debug) {
			return "";
		}
	}

}
