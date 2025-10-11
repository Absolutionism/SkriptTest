package org.skriptlang.skript.bukkit.lang;

import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.context.TriggerContext;

public interface Debuggable extends org.skriptlang.skript.lang.context.Debuggable {

	@Override
	default String toString(@Nullable TriggerContext context, boolean debug) {
		return ContextUtils.bukkitConversion(context, bukkitContext -> toString(bukkitContext, debug), "");
	}

	String toString(@Nullable BukkitContext<?> context, boolean debug);

}
