package org.skriptlang.skript.bukkit.entity;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.registrations.Classes;
import org.bukkit.entity.Entity;
import ch.njol.skript.Skript;
import ch.njol.skript.entity.SimpleEntityData;
import ch.njol.skript.lang.util.SimpleEvent;
import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Location;
import ch.njol.skript.registrations.Classes;
import org.bukkit.entity.AbstractNautilus;
import org.bukkit.entity.Entity;
import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.HierarchicalAddonModule;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.entity.data.*;
import org.skriptlang.skript.bukkit.entity.displays.DisplayModule;
import org.skriptlang.skript.bukkit.entity.elements.expressions.ExprPathfindingLocation;
import org.skriptlang.skript.bukkit.entity.elements.expressions.ExprPathfindingTarget;
import org.skriptlang.skript.bukkit.entity.elements.effects.EffTeleport;
import org.skriptlang.skript.bukkit.entity.interactions.InteractionModule;
import org.skriptlang.skript.bukkit.entity.elements.expressions.ExprDeathMessage;
import org.skriptlang.skript.bukkit.entity.interactions.InteractionModule;
import org.skriptlang.skript.bukkit.entity.nautilus.NautilusModule;
import org.skriptlang.skript.bukkit.entity.player.PlayerModule;
import org.skriptlang.skript.docs.Origin;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;
import org.skriptlang.skript.bukkit.entity.types.TeleportFlagClassInfo;

import java.util.List;

public class EntityModule extends HierarchicalAddonModule {

	public EntityModule(AddonModule parentModule) {
		super(parentModule);
	}

	public Iterable<AddonModule> children() {
		return List.of(
			new DisplayModule(this),
			new InteractionModule(this),
			new NautilusModule(this),
			new PlayerModule(this)
		);
	}

	@Override
	public void load(SkriptAddon addon) {
		loadSelf(addon);
	}

	@Override
	protected void initSelf(SkriptAddon addon) {
		Classes.registerClass(new TeleportFlagClassInfo());
	}

	protected void loadSelf(SkriptAddon addon) {
		EntityData.register();
		EntityDataClassInfo entityDataClassInfo = new EntityDataClassInfo();
		Classes.registerClass(entityDataClassInfo);
		EntityType.register();
		registerEntityDatas();
		loadChildren(addon);
		// Must be registered after every other EntityData, due to the automatic registering of EntityTypes
		SimpleEntityData.register();

		entityDataClassInfo.defaultExpression(new SimpleLiteral<>(new SimpleEntityData(Entity.class), true));

		ClassInfo<EntityType> entityTypeClassInfo = Classes.getExactClassInfo(EntityType.class);
		assert entityTypeClassInfo != null;
		entityTypeClassInfo.defaultExpression(new SimpleLiteral<>(new EntityType(EntityData.fromClass(Entity.class), 1), true));

		SyntaxRegistry syntaxRegistry = moduleRegistry(addon);
		EventValueRegistry registry = addon.registry(EventValueRegistry.class);
		syntaxRegistry.register(BukkitSyntaxInfos.Event.KEY, BukkitSyntaxInfos.Event.builder(SimpleEvent.class, "Pathfind")
			.addDescription("Called whenever an entity tries to pathfind to a location or another entity.")
			.addExample("""
				on pathfind:
				    	broadcast "%event-entity% is about to move to %event-location%!"
				""")
			.addSince("INSERT VERSION")
			.addPattern("[entity] [start[s]] pathfind[ing]")
			.addEvent(EntityPathfindEvent.class)
			.build());

		registry.register(EventValue.builder(EntityPathfindEvent.class, Location.class)
			.getter(EntityPathfindEvent::getLoc)
			.patterns("target location")
			.build());

		registry.register(EventValue.builder(EntityPathfindEvent.class, Entity.class)
			.getter(EntityPathfindEvent::getTargetEntity)
			.patterns("target entity")
			.build());

		registry.register(EventValue.builder(EntityPathfindEvent.class, Location.class)
			.getter(event -> event.getEntity().getLocation())
			.build());

		register(addon,
			ExprDeathMessage::register,
			ExprPathfindingLocation::register,
			ExprPathfindingTarget::register,
			EffTeleport::register,
			ExprDeathMessage::register
		);
	}

	@Override
	public String name() {
		return "entity";
	}

	//<editor-fold desc="register entity datas" defaultstate="collapsed">
	private void registerEntityDatas() {
		AxolotlData.register();
		BeeData.register();
		CatData.register();
		ChickenData.register();
		CreeperData.register();
		CowData.register();
		DroppedItemData.register();
		EndermanData.register();
		FallingBlockData.register();
		FoxData.register();
		FrogData.register();
		GoatData.register();
		LlamaData.register();
		MinecartData.register();
		MooshroomData.register();
		PandaData.register();
		ParrotData.register();
		PigData.register();
		RabbitData.register();
		SalmonData.register();
		SheepData.register();
		StriderData.register();
		ThrownPotionData.register();
		TropicalFishData.register();
		VillagerData.register();
		WolfData.register();
		XpOrbData.register();
		ZombieVillagerData.register();
	}
	//</editor-fold>


}
