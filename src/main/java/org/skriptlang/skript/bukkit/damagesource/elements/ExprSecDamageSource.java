package org.skriptlang.skript.bukkit.damagesource.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SectionExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SectionEvent;
import ch.njol.skript.lang.SectionValueExpression;
import ch.njol.skript.lang.SectionValueExpression.BlankSectionValueExpression;
import ch.njol.skript.lang.SectionValueProvider;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.parser.DefaultValueData;
import ch.njol.skript.lang.util.SectionUtils;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import org.bukkit.Location;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.damagesource.DamageSourceExperimentSyntax;

import java.util.List;

@Name("Damage Source")
@Description({
	"Create a custom damage source and change the attributes.",
	"When setting a 'causing entity' you must also set a 'direct entity'.",
	"Attributes of a damage source cannot be changed once created, only while within the 'custom damage source' section."
})
@Example("""
	set {_source} to a custom damage source:
		set the damage type to magic
		set the causing entity to {_player}
		set the direct entity to {_arrow}
		set the damage location to location(0, 0, 10)
	damage all players by 5 using {_source}
	""")
@Example("""
	on damage:
		if the damage type of event-damage source is magic:
			set the damage to damage * 2
	""")
@Since("2.12")
@RequiredPlugins("Minecraft 1.20.4+")
@SuppressWarnings("UnstableApiUsage")
public class ExprSecDamageSource extends SectionExpression<DamageSource> implements DamageSourceExperimentSyntax, SectionValueProvider {

	static class DamageSourceSectionEvent extends SectionEvent<ExprSecDamageSource> {

		public DamageType damageType = DamageType.GENERIC;
		public @Nullable Entity causingEntity = null;
		public @Nullable Entity directEntity = null;
		public @Nullable Location damageLocation = null;

		public DamageSourceSectionEvent(ExprSecDamageSource exprSec) {
			super(exprSec);
		}

		public DamageSource buildDamageSource() {
			DamageSource.Builder builder = DamageSource.builder(damageType);
			if (damageLocation != null)
				builder = builder.withDamageLocation(damageLocation.clone());
			if (causingEntity != null)
				builder = builder.withCausingEntity(causingEntity);
			if (directEntity != null)
				builder = builder.withDirectEntity(directEntity);
			return builder.build();
		}
	}

	static {
		Skript.registerExpression(ExprSecDamageSource.class, DamageSource.class, ExpressionType.COMBINED,
			"[a] custom damage source [(with|using) [the|a] [damage type [of]] %-damagetype%]");
	}

	private @Nullable Expression<DamageType> damageType;
	private Trigger trigger = null;
	private SectionValueExpression<ExprSecDamageSource, DamageSource> sectionValue = null;

	@Override
	public boolean init(Expression<?>[] exprs, int pattern, Kleenean delayed, ParseResult result, @Nullable SectionNode node, @Nullable List<TriggerItem> triggerItems) {
		if (exprs[0] == null) {
			if (node == null) {
				Skript.error("You must contain a section for this expression.");
				return false;
			} else if (node.isEmpty()) {
				Skript.error("You must contain code inside this section.");
				return false;
			}
		} else {
			//noinspection unchecked
			damageType = (Expression<DamageType>) exprs[0];
		}

		if (node != null) {
			sectionValue = new BlankSectionValueExpression<>(this, DamageSource.class);
			trigger = SectionUtils.loadLinkedCode("custom damage source", (beforeLoading, afterLoading) ->
				loadCode(node, "custom damage source", () -> {
					beforeLoading.run();
					getParser().getData(DefaultValueData.class).addDefaultValue(DamageSource.class, sectionValue);
				}, () -> {
					afterLoading.run();
					getParser().getData(DefaultValueData.class).removeDefaultValue(DamageSource.class);
				}, DamageSourceSectionEvent.class));
			return trigger != null;
		}
		return true;
	}

	@Override
	protected DamageSource @Nullable [] get(Event event) {
		DamageSourceSectionEvent sectionEvent = new DamageSourceSectionEvent(this);
		if (damageType != null) {
			DamageType damageType = this.damageType.getSingle(event);
			if (damageType != null) {
				sectionEvent.damageType = damageType;
			}
		}
		if (trigger != null) {
			Variables.withLocalVariables(event, sectionEvent, () -> TriggerItem.walk(trigger, sectionEvent));
			if (sectionEvent.causingEntity != null && sectionEvent.directEntity == null) {
				error("You must set a 'direct entity' when setting a 'causing entity'.");
				return null;
			}
		}
		return new DamageSource[] {sectionEvent.buildDamageSource()};
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<DamageSource> getReturnType() {
		return DamageSource.class;
	}

	@Override
	public Expression<?> getSectionValue() {
		return sectionValue;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "a custom damage source";
	}

}
