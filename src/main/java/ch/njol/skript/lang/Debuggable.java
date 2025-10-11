package ch.njol.skript.lang;

import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.lang.BukkitContext;

/**
 * Represents an element that can print details involving an event.
 */
public interface Debuggable extends org.skriptlang.skript.bukkit.lang.Debuggable {

	@Override
	default String toString(@Nullable BukkitContext<?> context, boolean debug) {
		if (context == null)
			return toString((Event) null, debug);
		return toString(context.getEvent(), debug);
	}

	/**
	 * @param event The event to get information from. This is always null if debug == false.
	 * @param debug If true this should print more information, if false this should print what is shown to the end user
	 * @return String representation of this object
	 */
	String toString(@Nullable Event event, boolean debug);

}
