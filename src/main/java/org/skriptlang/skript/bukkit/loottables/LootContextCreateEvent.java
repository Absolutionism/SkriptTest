package org.skriptlang.skript.bukkit.loottables;

import ch.njol.skript.lang.SectionEvent;
import org.skriptlang.skript.bukkit.loottables.elements.expressions.ExprSecCreateLootContext;

/**
 * The event used in the {@link ExprSecCreateLootContext} section.
 */
public class LootContextCreateEvent extends SectionEvent<ExprSecCreateLootContext> {

	private final LootContextWrapper contextWrapper;

	public LootContextCreateEvent(ExprSecCreateLootContext exprSec, LootContextWrapper context) {
		super(exprSec);
		this.contextWrapper = context;
	}

	public LootContextWrapper getContextWrapper() {
		return contextWrapper;
	}

}
