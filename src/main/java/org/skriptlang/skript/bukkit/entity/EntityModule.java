package org.skriptlang.skript.bukkit.entity;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.registrations.Classes;
import org.bukkit.entity.Entity;
import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.HierarchicalAddonModule;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.entity.data.*;
import org.skriptlang.skript.bukkit.entity.displays.DisplayModule;
import org.skriptlang.skript.bukkit.entity.elements.expressions.ExprDeathMessage;
import org.skriptlang.skript.bukkit.entity.interactions.InteractionModule;
import org.skriptlang.skript.bukkit.entity.nautilus.NautilusModule;
import org.skriptlang.skript.bukkit.entity.player.PlayerModule;
import org.skriptlang.skript.docs.Origin;

import java.util.List;

public class EntityModule extends HierarchicalAddonModule {

	static Origin origin;

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

	protected void loadSelf(SkriptAddon addon) {
		EntityData.register();
		EntityDataClassInfo entityDataClassInfo = new EntityDataClassInfo();
		Classes.registerClass(entityDataClassInfo);
		EntityType.register();
		registerEntityDatas();
		loadChildren(addon);
		SimpleEntityData.register();

		entityDataClassInfo.defaultExpression(new SimpleLiteral<>(new SimpleEntityData(Entity.class), true));

		ClassInfo<EntityType> entityTypeClassInfo = Classes.getExactClassInfo(EntityType.class);
		assert entityTypeClassInfo != null;
		entityTypeClassInfo.defaultExpression(new SimpleLiteral<>(new EntityType(EntityData.fromClass(Entity.class), 1), true));

		register(addon,
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
