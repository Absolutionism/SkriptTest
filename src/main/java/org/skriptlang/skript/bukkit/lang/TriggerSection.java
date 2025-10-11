package org.skriptlang.skript.bukkit.lang;

import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.context.TriggerContext;
import org.skriptlang.skript.lang.context.TriggerItem;

public abstract class TriggerSection extends org.skriptlang.skript.lang.context.TriggerSection implements Debuggable {

	@Override
	public @Nullable TriggerItem walk(TriggerContext context) {
		return ContextUtils.bukkitConversion(context, this::walk);
	}

	protected abstract @Nullable TriggerItem walk(BukkitContext<?> context);

	protected final @Nullable TriggerItem walk(BukkitContext<?> context, boolean run) {
		return super.walk(context, run);
	}

}
