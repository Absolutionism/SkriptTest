package org.skriptlang.skript.bukkit.lang;

import ch.njol.skript.Skript;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.context.Trigger;
import org.skriptlang.skript.lang.context.TriggerContext;
import org.skriptlang.skript.lang.script.Script;

import java.io.File;

public abstract class TriggerItem extends org.skriptlang.skript.lang.context.TriggerItem implements Debuggable {

	protected TriggerItem(TriggerSection parent) {
		super(parent);
	}

	protected @Nullable org.skriptlang.skript.lang.context.TriggerItem walk(BukkitContext<?> context) {
		if (run(context)) {
			debug(context, true);
			return next;
		} else {
			debug(context, false);
			org.skriptlang.skript.lang.context.TriggerSection parent = this.parent;
			return parent == null ? null : parent.getNext();
		}
	}

	@Override
	protected boolean run(TriggerContext context) {
		return ContextUtils.bukkitConversion(context, this::run, false);
	}

	protected abstract boolean run(BukkitContext<?> context);

	public static boolean walk(TriggerItem start, BukkitContext<?> context) {
		TriggerItem triggerItem = start;
		try {
			while (triggerItem != null)
				triggerItem = (TriggerItem) triggerItem.walk(context);

			return true;
		} catch (StackOverflowError err) {
			Trigger trigger = start.getTrigger();
			String scriptName = "<unknown>";
			if (trigger != null) {
				Script script = trigger.getScript();
				if (script != null) {
					File scriptFile = script.getConfig().getFile();
					if (scriptFile != null)
						scriptName = scriptFile.getName();
				}
			}
			Skript.adminBroadcast("<red>The script '<gold>" + scriptName + "<red>' infinitely (or excessively) repeated itself!");
			if (Skript.debug())
				err.printStackTrace();
		} catch (Exception ex) {
			if (ex.getStackTrace().length != 0) // empty exceptions have already been printed
				Skript.exception(ex, triggerItem);
		} catch (Throwable throwable) {
			// not all Throwables are Exceptions, but we usually don't want to catch them (without rethrowing)
			Skript.markErrored();
			throw throwable;
		}
		return false;
	}

}
