package org.skriptlang.skript.bukkit.lang;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.classes.Changer.ChangerUtils;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.slot.Slot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.context.TriggerContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface Expression<Type> extends SyntaxElement, Debuggable, Loopable<Type>, org.skriptlang.skript.lang.context.Expression<Type> {

	@Override
	default @Nullable Type getSingle(TriggerContext context) {
		return ContextUtils.bukkitConversion(context, this::getSingle);
	}

	@Override
	default Type[] getArray(TriggerContext context) {
		return ContextUtils.bukkitConversion(context, this::getArray);
	}

	@Override
	default Type[] getAll(TriggerContext context) {
		return ContextUtils.bukkitConversion(context, this::getAll);
	}

	@Override
	default Stream<? extends @NotNull Type> stream(TriggerContext context) {
		return ContextUtils.bukkitConversion(context, this::stream);
	}

	@Override
	default Stream<? extends @NotNull Type> streamAll(TriggerContext context) {
		return ContextUtils.bukkitConversion(context, this::streamAll);
	}

	@Override
	default boolean check(TriggerContext context, Predicate<? super Type> checker, boolean negated) {
		return ContextUtils.bukkitConversion(
			context,
			bukkitContext -> check(bukkitContext, checker, negated),
			false,
			false
		);
	}

	@Override
	default boolean check(TriggerContext context, Predicate<? super Type> checker) {
		return ContextUtils.bukkitConversion(
			context,
			bukkitContext -> check(bukkitContext, checker),
			false,
			false
		);
	}

	@Override
	default void change(TriggerContext context, Object @Nullable [] delta, ChangeMode mode) {
		ContextUtils.bukkitRun(context, bukkitContext -> change(bukkitContext, delta, mode));
	}

	@Override
	default <Output> void changeInPlace(TriggerContext context, Function<Type, Output> changeFunction) {
		ContextUtils.bukkitRun(context, bukkitContext -> changeInPlace(bukkitContext, changeFunction));
	}

	@Override
	default <Output> void changeInPlace(TriggerContext context, Function<Type, Output> changeFunction, boolean getAll) {
		ContextUtils.bukkitRun(context, bukkitContext -> changeInPlace(bukkitContext, changeFunction, getAll));
	}

	@Override
	default Object @Nullable [] beforeChange(org.skriptlang.skript.lang.context.Expression<?> changed, Object @Nullable [] delta) {
		return beforeChange(
			(Expression<?>) changed,
			delta
		);
	}

	@Nullable Type getSingle(BukkitContext<?> context);

	Type[] getArray(BukkitContext<?> context);

	Type[] getAll(BukkitContext<?> context);

	default Stream<? extends @NotNull Type> stream(BukkitContext<?> context) {
		Iterator<? extends Type> iterator = iterator(context);
		if (iterator == null)
			return Stream.empty();
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
	}

	default Stream<? extends @NotNull Type> streamAll(BukkitContext<?> context) {
		Iterator<? extends Type> iterator = Arrays.stream(getAll(context)).iterator();
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
	}

	boolean check(BukkitContext<?> context, Predicate<? super Type> checker, boolean negated);

	boolean check(BukkitContext<?> context, Predicate<? super Type> checker);

	@SuppressWarnings("unchecked")
	<ConvertType> @Nullable Expression<? extends ConvertType> getConvertedExpression(Class<ConvertType>... to);

	void change(BukkitContext<?> context, Object @Nullable [] delta, ChangeMode mode);

	default <Output> void changeInPlace(BukkitContext<?> context, Function<Type, Output> function) {
		changeInPlace(context, function, false);
	}

	default <Output> void changeInPlace(BukkitContext<?> context, Function<Type, Output> function, boolean getAll) {
		Type[] values = getAll ? getAll(context) : getArray(context);
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
		change(context, newValues.toArray(), ChangeMode.SET);
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
