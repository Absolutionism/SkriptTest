package org.skriptlang.skript.bukkit.lang;

import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.variables.Variables;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.context.TriggerContext;
import org.skriptlang.skript.lang.context.TriggerItem;
import org.skriptlang.skript.lang.script.Script;

import java.util.List;

public class Trigger extends org.skriptlang.skript.lang.context.Trigger implements Debuggable {

	public Trigger(@Nullable Script script, String name, SkriptEvent event, List<TriggerItem> items) {
		super(script, name, event, items);
	}

	@Override
	public boolean execute(TriggerContext context) {
		return ContextUtils.bukkitConversion(context, this::execute, false, false);
	}

	@Override
	public @Nullable TriggerItem walk(TriggerContext context) {
		return ContextUtils.bukkitConversion(context, this::walk);
	}

	@Override
	public String toString(@Nullable TriggerContext context, boolean debug) {
		return ContextUtils.bukkitConversion(context, bukkit -> toString(bukkit, debug), "", true);
	}

	public boolean execute(BukkitContext<?> context) {
		boolean success = TriggerItem.walk(this, context);

		// Clear local variables
		Variables.removeLocals(context);
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

	protected @Nullable TriggerItem walk(BukkitContext<?> context) {
		return walk(context, true);
	}

	@Override
	public String toString(@Nullable BukkitContext<?> context, boolean debug) {
		return name + " (" + this.event.toString(context, debug) + ")";
	}

}
