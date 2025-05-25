package ch.njol.skript.effects;

import ch.njol.skript.variables.NewVariables;
import ch.njol.skript.variables.NewVariablesMap;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.util.Timespan;

/**
 * @author Peter Güttinger
 */
public class IndeterminateDelay extends Delay {
	
	@Override
	@Nullable
	protected TriggerItem walk(Event event) {
		debug(event, true);

		long start = Skript.debug() ? System.nanoTime() : 0;
		TriggerItem next = getNext();

		if (next != null && Skript.getInstance().isEnabled()) { // See https://github.com/SkriptLang/Skript/issues/3702
			Delay.addDelayedEvent(event);
			Timespan duration = this.duration.getSingle(event);
			if (duration == null)
				return null;
			
			// Back up local variables
			NewVariablesMap localVars = NewVariables.removeLocals(event); // CHANGE
			
			Bukkit.getScheduler().scheduleSyncDelayedTask(Skript.getInstance(), () -> {
				Skript.debug(getIndentation() + "... continuing after " + (System.nanoTime() - start) / 1_000_000_000. + "s");

				// Re-set local variables
				if (localVars != null)
					NewVariables.setLocalVariables(event, localVars); // CHANGE

				TriggerItem.walk(next, event);
			}, duration.getAs(Timespan.TimePeriod.TICK));
		}

		return null;
	}
	
	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "wait for operation to finish";
	}
	
}
