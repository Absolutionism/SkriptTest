package org.skriptlang.skript.bukkit.loottables.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.ExprSectionExpression;
import org.bukkit.loot.LootContext;

@Name("Loot Context")
@Description("The loot context involved in the context create section.")
@Examples({
	"set {_context} to a new loot context at {_location}:",
		"\tbroadcast loot context"
})
@Since("2.10")
public class ExprLootContext extends ExprSectionExpression<LootContext> {

	static {
		register(ExprLootContext.class, LootContext.class, "loot[ ]context");
	}

	public ExprLootContext() {
		super(LootContext.class);
	}

}
