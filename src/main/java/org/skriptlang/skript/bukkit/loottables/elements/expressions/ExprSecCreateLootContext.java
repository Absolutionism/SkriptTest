package org.skriptlang.skript.bukkit.loottables.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SectionExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SectionValueExpression;
import ch.njol.skript.lang.SectionValueExpression.BlankSectionValueExpression;
import ch.njol.skript.lang.SectionValueProvider;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.parser.DefaultValueData;
import ch.njol.skript.lang.util.SectionUtils;
import ch.njol.skript.util.Direction;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.loot.LootContext;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.loottables.LootContextCreateEvent;
import org.skriptlang.skript.bukkit.loottables.LootContextWrapper;

import java.util.List;

@Name("Create Loot Context")
@Description("Create a loot context.")
@Examples({
	"set {_player} to player",
	"set {_context} to a loot context at player:",
		"\tset loot luck value to 10",
		"\tset looter to {_player}",
		"\tset looted entity to last spawned pig",
	"give player loot items of loot table \"minecraft:entities/iron_golem\" with loot context {_context}"
})
@Since("2.10")
public class ExprSecCreateLootContext extends SectionExpression<LootContext> implements SectionValueProvider {

	static {
		Skript.registerExpression(ExprSecCreateLootContext.class, LootContext.class, ExpressionType.COMBINED,
			"[a] loot context %direction% %location%");
	}

	private Trigger trigger;
	private Expression<Location> location;
	private SectionValueExpression<ExprSecCreateLootContext, LootContext> sectionValue = null;

	@Override
	public boolean init(Expression<?>[] exprs, int pattern, Kleenean isDelayed, ParseResult result, @Nullable SectionNode node, @Nullable List<TriggerItem> triggerItems) {
		//noinspection unchecked
		location = Direction.combine((Expression<Direction>) exprs[0], (Expression<Location>) exprs[1]);

		if (node != null) {
			sectionValue = new BlankSectionValueExpression<>(this, LootContext.class);
			trigger = SectionUtils.loadLinkedCode("create loot context", (beforeLoading, afterLoading) ->
				loadCode(node, "create loot context", () -> {
					beforeLoading.run();
					getParser().getData(DefaultValueData.class).addDefaultValue(LootContext.class, sectionValue);
				}, () -> {
					afterLoading.run();
					getParser().getData(DefaultValueData.class).removeDefaultValue(LootContext.class);
				}, LootContextCreateEvent.class));
			return trigger != null;
		}

		return true;
	}

	@Override
	protected LootContext @Nullable [] get(Event event) {
		Location loc = location.getSingle(event);
		if (loc == null)
			return new LootContext[0];

		LootContextWrapper wrapper = new LootContextWrapper(loc);
		if (trigger != null) {
			LootContextCreateEvent contextEvent = new LootContextCreateEvent(this, wrapper);
			Variables.withLocalVariables(event, contextEvent, () ->
				TriggerItem.walk(trigger, contextEvent)
			);
		}
		return new LootContext[]{wrapper.getContext()};
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends LootContext> getReturnType() {
		return LootContext.class;
	}

	@Override
	public Expression<?> getSectionValue() {
		return sectionValue;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "a loot context " + location.toString(event, debug);
	}

}
