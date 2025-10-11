package ch.njol.skript.lang;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.classes.Changer.ChangerUtils;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.slot.Slot;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.lang.BukkitContext;
import org.skriptlang.skript.bukkit.lang.ContextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Represents an expression. Expressions are used within conditions, effects and other expressions.
 *
 * @see Skript#registerExpression(Class, Class, ExpressionType, String...)
 * @see SimpleExpression
 * @see SyntaxElement
 */
public interface Expression<Type> extends SyntaxElement, Debuggable, Loopable<Type>, org.skriptlang.skript.bukkit.lang.Expression<Type> {

	@Override
	default @Nullable Type getSingle(BukkitContext<?> context) {
		return ContextUtils.eventConversion(context, this::getSingle);
	}

	@Override
	default Type[] getArray(BukkitContext<?> context) {
		return ContextUtils.eventConversion(context, this::getArray);
	}

	@Override
	default Type[] getAll(BukkitContext<?> context) {
		return ContextUtils.eventConversion(context, this::getAll);
	}

	@Override
	default Stream<? extends @NotNull Type> stream(BukkitContext<?> context) {
		return ContextUtils.eventConversion(context, this::stream);
	}

	@Override
	default Stream<? extends @NotNull Type> streamAll(BukkitContext<?> context) {
		return ContextUtils.eventConversion(context, this::streamAll);
	}

	@Override
	default boolean check(BukkitContext<?> context, Predicate<? super Type> checker, boolean negated) {
		return ContextUtils.eventConversion(
			context,
			event -> check(event, checker, negated),
			false,
			false
		);
	}

	@Override
	default boolean check(BukkitContext<?> context, Predicate<? super Type> checker) {
		return ContextUtils.eventConversion(
			context,
			event -> check(event, checker),
			false,
			false
		);
	}

	@Override
	default <Output> void changeInPlace(BukkitContext<?> context, Function<Type, Output> function) {
		ContextUtils.eventRun(context, event -> changeInPlace(event, function));
	}

	@Override
	default <Output> void changeInPlace(BukkitContext<?> context, Function<Type, Output> function, boolean getAll) {
		ContextUtils.eventRun(context, event -> changeInPlace(event, function, getAll));
	}

	@Override
	default void change(BukkitContext<?> context, Object @Nullable [] delta, ChangeMode mode) {
		ContextUtils.eventRun(context, event -> change(event, delta, mode));
	}

	@Override
	default Object @Nullable [] beforeChange(org.skriptlang.skript.bukkit.lang.Expression<?> changed, Object @Nullable [] delta) {
		return beforeChange(
			(Expression<?>) changed,
			delta
		);
	}

	Type getSingle(Event event);

	Type[] getArray(Event event);

	Type[] getAll(Event event);

	default Stream<? extends @NotNull Type> stream(Event event) {
		Iterator<? extends Type> iterator = iterator(event);
		if (iterator == null)
			return Stream.empty();
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
	}

	default Stream<? extends @NotNull Type> streamAll(Event event) {
		Iterator<? extends Type> iterator = Arrays.stream(getAll(event)).iterator();
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
	}

	boolean check(Event event, Predicate<? super Type> checker, boolean negated);

	boolean check(Event event, Predicate<? super Type> checker);

	@SuppressWarnings("unchecked")
	<ConvertType> @Nullable Expression<? extends ConvertType> getConvertedExpression(Class<ConvertType>... to);

	void change(Event event, Object @Nullable [] delta, ChangeMode mode);

	default <Output> void changeInPlace(Event event, Function<Type, Output> function) {
		changeInPlace(event, function, false);
	}

	default <Output> void changeInPlace(Event event, Function<Type, Output> function, boolean getAll) {
		Type[] values = getAll ? getAll(event) : getArray(event);
		if (values.length == 0)
			return;

		Class<?>[] validClasses = Arrays.stream(acceptChange(ChangeMode.SET))
			.map(c -> c.isArray() ? c.getComponentType() : c)
			.toArray(Class[]::new);

		List<Output> newValues = new ArrayList<>();
		for (Type value : values) {
			Output newValue = function.apply(value);
			if (newValue != null && ChangerUtils.acceptsChangeTypes(validClasses, newValue.getClass()))
				newValues.add(newValue);
		}
		change(event, newValues.toArray(), ChangeMode.SET);
	}

	default Object @Nullable [] beforeChange(Expression<?> changed, Object @Nullable [] delta) {
		if (delta == null || delta.length == 0) // Nothing to nothing
			return null;

		// Slots must be transformed to item stacks when writing to variables
		// Also, some types must be cloned
		Object[] newDelta = null;
		if (changed instanceof Variable) {
			newDelta = new Object[delta.length];
			for (int i = 0; i < delta.length; i++) {
				Object value = delta[i];
				if (value instanceof Slot) {
					ItemStack item = ((Slot) value).getItem();
					if (item != null) {
						item = item.clone(); // ItemStack in inventory is mutable
					}

					newDelta[i] = item;
				} else {
					newDelta[i] = Classes.clone(delta[i]);
				}
			}
		}
		// Everything else (inventories, actions, etc.) does not need special handling

		// Return the given delta or an Object[] copy of it, with some values transformed
		return newDelta == null ? delta : newDelta;
	}

}
