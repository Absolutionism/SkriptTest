package ch.njol.skript.lang;

import ch.njol.skript.variables.Variables;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.lang.BukkitContext;
import org.skriptlang.skript.bukkit.lang.ContextUtils;
import org.skriptlang.skript.lang.context.TriggerItem;
import org.skriptlang.skript.lang.script.Script;

import java.util.List;

public class Trigger extends org.skriptlang.skript.bukkit.lang.Trigger implements Debuggable {

	public Trigger(@Nullable Script script, String name, SkriptEvent event, List<TriggerItem> items) {
		super(script, name, event, items);
	}

	@Override
	public boolean execute(BukkitContext<?> context) {
		return ContextUtils.eventConversion(context, this::execute, false, false);
	}

	@Override
	protected @Nullable TriggerItem walk(BukkitContext<?> context) {
		return ContextUtils.eventConversion(context, this::walk);
	}

	@Override
	public String toString(@Nullable BukkitContext<?> context, boolean debug) {
		return ContextUtils.eventConversion(context, event -> toString(event, debug), "", true);
	}

	public boolean execute(Event event) {
		boolean success = TriggerItem.walk(this, BukkitContext.fromEvent(event));

		// Clear local variables
		Variables.removeLocals(event);
		/*
		 * Local variables can be used in delayed effects by backing reference
		 * of VariablesMap up. Basically:
		 *
		 * Object localVars = Variables.removeLocals(event);
		 *
		 * ... and when you want to continue execution:
		 *
		 * Variables.setLocalVariables(event, localVars);
		 *
		 * See Delay effect for reference.
		 */

		return success;
	}

	protected @Nullable TriggerItem walk(Event event) {
		return walk(BukkitContext.fromEvent(event), true);
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return name + " (" + this.event.toString(event, debug) + ")";
	}

}
