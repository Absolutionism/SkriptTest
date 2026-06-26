package org.skriptlang.skript.bukkit.entity;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.lang.ParseContext;
import org.jetbrains.annotations.Nullable;

import static org.skriptlang.skript.bukkit.entity.EntityData.ALL_ENTITY_DATAS;

@SuppressWarnings("rawtypes")
public class EntityDataClassInfo extends ClassInfo<EntityData> {

	EntityDataClassInfo() {
		super(EntityData.class, "entitydata");
		this.user("entity ?types?")
			.name("Entity Type")
			.description("The type of an <a href='#entity'>entity</a>, e.g. player, wolf, powered creeper, etc.")
			.usage("")
			.examples("victim is a cow",
					"spawn a creeper")
			.since("1.3")
			.before("entitytype")
			.supplier(ALL_ENTITY_DATAS::iterator)
			.parser(new Parser<>() {
				@Override
				public String toString(EntityData entityData, int flags) {
					return entityData.toString(flags);
				}

				@Override
				public @Nullable EntityData parse(String string, ParseContext context) {
					return EntityData.parse(string);
				}

				@Override
				public String toVariableNameString(EntityData entityData) {
					return "entitydata:" + entityData.toString();
				}
			}).serializer(EntityData.serializer);
	}

}
