package org.skriptlang.skript.lang.context;

import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.variables.Variables;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.script.Script;

import java.util.List;

public class Trigger extends TriggerSection {

	protected final String name;
	protected final SkriptEvent event;

	protected final @Nullable Script script;
	protected int line = -1; // -1 is default: it means there is no line number available
	protected String debugLabel;

	public Trigger(@Nullable Script script, String name, SkriptEvent event, List<TriggerItem> items) {
		super(items);
		this.script = script;
		this.name = name;
		this.event = event;
		this.debugLabel = "unknown trigger";
	}

	/**
	 * Executes this trigger for a certain event.
	 * @param context The event to execute this Trigger with.
	 * @return false if an exception occurred.
	 */
	public boolean execute(TriggerContext context) {
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

	@Override
	public @Nullable TriggerItem walk(TriggerContext context) {
		return walk(context, true);
	}

	@Override
	public String toString(@Nullable TriggerContext context, boolean debug) {
		return name + " (" + this.event.toString(context, debug) + ")";
	}

	/**
	 * @return The name of this trigger.
	 */
	public String getName() {
		return name;
	}

	public SkriptEvent getEvent() {
		return event;
	}

	/**
	 * @return The script this trigger was created from.
	 */
	public @Nullable Script getScript() {
		return script;
	}

	/**
	 * Sets line number for this trigger's start.
	 * Only used for debugging.
	 * @param line Line number
	 */
	public void setLineNumber(int line) {
		this.line  = line;
	}

	/**
	 * @return The line number where this trigger starts. This should ONLY be used for debugging!
	 */
	public int getLineNumber() {
		return line;
	}

	public void setDebugLabel(String label) {
		this.debugLabel = label;
	}

	public String getDebugLabel() {
		return debugLabel;
	}

}
