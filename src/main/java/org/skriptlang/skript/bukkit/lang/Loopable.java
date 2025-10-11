package org.skriptlang.skript.bukkit.lang;

import ch.njol.skript.lang.util.SimpleExpression;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.context.TriggerContext;

import java.util.Iterator;

public interface Loopable<Type> extends org.skriptlang.skript.lang.context.Loopable<Type> {

	@Override
	default @Nullable Iterator<? extends Type> iterator(TriggerContext context) {
		if (!(context instanceof BukkitContext<?> bukkitContext))
			return null;
		return iterator(bukkitContext);
	}

	/**
	 * Returns the same as {@link Expression#getArray(BukkitContext)} but as an iterator. This method should be overridden by expressions intended to be looped to increase performance.
	 * @see SimpleExpression#iterator(Event)
	 *
	 * @param context The context to be used for evaluation
	 * @return An iterator to iterate over all values of this expression which may be empty and/or null, but must not return null elements.
	 */
	@Nullable Iterator<? extends Type> iterator(BukkitContext<?> context);

}
