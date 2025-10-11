package org.skriptlang.skript.bukkit.lang;

import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.context.TriggerContext;

import java.util.HashMap;
import java.util.Map;

public class BukkitContext<E extends Event> implements TriggerContext {

	private static final Map<Event, BukkitContext<?>> contexts = new HashMap<>();

	public static <E extends Event> BukkitContext<E> fromEvent(E event) {
		if (contexts.containsKey(event)) {
			//noinspection unchecked
			return (BukkitContext<E>) contexts.get(event);
		}
		BukkitContext<E> context = new BukkitContext<>(event);
		contexts.put(event, context);
		return context;
	}

	private @Nullable Class<? extends E> eventClass;
	private @Nullable E event;

	public BukkitContext(Class<? extends E> eventClass) {
		this.eventClass = eventClass;
	}

	public BukkitContext(E event) {
		this.event = event;
	}

	public @Nullable Class<? extends E> getEventClass() {
		return eventClass;
	}

	public @Nullable E getEvent() {
		return event;
	}

}
