package org.skriptlang.skript.bukkit.lang;

import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.context.TriggerContext;

import java.util.function.Consumer;
import java.util.function.Function;

public class ContextUtils {

	public static <O> @Nullable O bukkitConversion(TriggerContext context, Function<BukkitContext<?>, O> function) {
		return bukkitConversion(context, function, null);
	}

	public static <O> @Nullable O bukkitConversion(TriggerContext context, Function<BukkitContext<?>, O> function, boolean force) {
		return bukkitConversion(context, function, null, force);
	}

	public static <O> O bukkitConversion(TriggerContext context, Function<BukkitContext<?>, O> function, O fallback) {
		if (!(context instanceof BukkitContext<?> bukkitContext))
			return fallback;
		return function.apply(bukkitContext);
	}

	public static <O> O bukkitConversion(TriggerContext context, Function<BukkitContext<?>, O> function, O fallback, boolean force) {
		if (context instanceof BukkitContext<?> bukkitContext)
			return function.apply(bukkitContext);
		if (context == null && force)
			return function.apply(null);
		return fallback;
	}

	public static void bukkitRun(TriggerContext context, Consumer<BukkitContext<?>> consumer) {
		if (context instanceof BukkitContext<?> bukkitContext)
			consumer.accept(bukkitContext);
	}

	public static void bukkitRun(TriggerContext context, Consumer<BukkitContext<?>> consumer, boolean force) {
		if (context instanceof BukkitContext<?> bukkitContext) {
			consumer.accept(bukkitContext);
		} else if (context == null && force) {
			consumer.accept(null);
		}
	}

	public static <O> @Nullable O eventConversion(BukkitContext<?> context, Function<Event, O> function) {
		return eventConversion(context, function, null);
	}

	public static <O> @Nullable O eventConversion(BukkitContext<?> context, Function<Event, O> function, boolean force) {
		return eventConversion(context, function, null, force);
	}

	public static <O> O eventConversion(BukkitContext<?> context, Function<Event, O> function, O fallback) {
		return eventConversion(context, function, fallback, false);
	}

	public static <O> O eventConversion(BukkitContext<?> context, Function<Event, O> function, O fallback, boolean force) {
		Event event = null;
		if (context == null) {
			if (!force)
				return fallback;
		} else {
			event = context.getEvent();
		}
		if (event == null) {
			if (!force)
				return fallback;
		}
		return function.apply(event);
	}

	public static void eventRun(BukkitContext<?> context, Consumer<Event> consumer) {
		eventRun(context, consumer, false);
	}

	public static void eventRun(BukkitContext<?> context, Consumer<Event> consumer, boolean force) {
		Event event = null;
		if (context == null) {
			if (!force)
				return;
		} else {
			event = context.getEvent();
		}
		if (event == null) {
			if (!force)
				return;
		}
		consumer.accept(event);
	}

}
