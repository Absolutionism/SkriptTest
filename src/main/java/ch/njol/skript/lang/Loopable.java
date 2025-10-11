package ch.njol.skript.lang;

import ch.njol.skript.lang.util.SimpleExpression;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.lang.BukkitContext;

import java.util.Iterator;

public interface Loopable<Type> extends org.skriptlang.skript.bukkit.lang.Loopable<Type> {

	@Override
	default @Nullable Iterator<? extends Type> iterator(BukkitContext<?> context) {
		return iterator(context.getEvent());
	}

	/**
	 * Returns the same as {@link Expression#getArray(Event)} but as an iterator. This method should be overridden by expressions intended to be looped to increase performance.
	 * @see SimpleExpression#iterator(Event)
	 *
	 * @param event The event to be used for evaluation
	 * @return An iterator to iterate over all values of this expression which may be empty and/or null, but must not return null elements.
	 */
	@Nullable Iterator<? extends Type> iterator(Event event);

}
