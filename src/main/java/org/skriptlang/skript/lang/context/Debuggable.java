package org.skriptlang.skript.lang.context;

import org.jetbrains.annotations.Nullable;

/**
 * Represents an element that can print details involving an event.
 */
public interface Debuggable {

	/**
	 * @param context The context to get information from. This is always null if debug == false.
	 * @param debug If true this should print more information, if false this should print what is shown to the end user
	 * @return String representation of this object
	 */
	String toString(@Nullable TriggerContext context, boolean debug);

	/**
	 * Should return <tt>{@link #toString(TriggerContext, boolean) toString}(null, false)</tt>
	 */
	@Override
	String toString();

}
