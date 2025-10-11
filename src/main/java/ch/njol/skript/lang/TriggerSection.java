package ch.njol.skript.lang;

import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.lang.BukkitContext;
import org.skriptlang.skript.bukkit.lang.ContextUtils;
import org.skriptlang.skript.lang.context.TriggerItem;

/**
 * Represents a section of a trigger, e.g. a conditional or a loop
 */
public abstract class TriggerSection extends org.skriptlang.skript.bukkit.lang.TriggerSection implements Debuggable {

	@Override
	protected @Nullable TriggerItem walk(BukkitContext<?> context) {
		return ContextUtils.eventConversion(context, this::walk);
	}

	protected abstract @Nullable TriggerItem walk(Event event);

	protected final @Nullable TriggerItem walk(Event event, boolean run) {
		return super.walk(BukkitContext.fromEvent(event), run);
	}

}
