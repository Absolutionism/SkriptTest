package org.skriptlang.skript.bukkit.entity;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.YggdrasilSerializer;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.localization.Language;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.yggdrasil.YggdrasilSerializable;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * Class that represents x amount of an {@link EntityData}
 */
public class EntityType
	extends ch.njol.skript.entity.EntityType
	implements Cloneable, YggdrasilSerializable {

	static void register() {
		Classes.registerClass(new ClassInfo<>(EntityType.class, "entitytype")
				.name("Entity Type with Amount")
				.description("An <a href='#entitydata'>entity type</a> with an amount, e.g. '2 zombies'.")
				.usage("<number> <entity type>")
				.examples("spawn 5 creepers behind the player")
				.since("1.3")
				.parser(new Parser<>() {
					@Override
					public @Nullable EntityType parse(String string, ParseContext context) {
						return EntityType.parse(string);
					}

					@Override
					public String toString(EntityType entityType, int flags) {
						return entityType.toString(flags);
					}

					@Override
					public String toVariableNameString(EntityType entityType) {
						return "entitytype:" + entityType.toString();
					}
                })
				.serializer(new YggdrasilSerializer<>()));
	}

	public static @Nullable EntityType parse(String string) {
		assert string != null && !string.isEmpty();
		int amount = -1;
		if (string.matches("\\d+ .+")) {
			amount = Utils.parseInt(string.split(" ", 2)[0]);
			string = string.split(" ", 2)[1];
		} else if (string.matches("(?i)an? .+")) {
			string = string.split(" ", 2)[1];
		}
		EntityData<?> data = EntityData.parseWithoutIndefiniteArticle(string);
		if (data == null)
			return null;
		return new EntityType(data, amount);
	}

	private int amount = -1;

	private EntityData<?> data;

	/**
	 * Only used for deserialisation
	 */
	@SuppressWarnings({"unused", "null"})
	private EntityType() {
		super();
		data = null;
		amount = 1;
	}

	public EntityType(EntityData<?> data, int amount) {
		super();
		assert data != null;
		this.data = data;
		this.amount = amount;
	}

	private EntityType(EntityType other) {
		amount = other.amount;
		data = other.data;
	}

	public boolean isInstance(Entity entity) {
		return data.isInstance(entity);
	}

	@Override
	public String toString() {
		if (getAmount() == 1)
			return data.toString(0);
		return amount + " " + data.toString(Language.F_PLURAL);
	}

	public String toString(int flags) {
		if (getAmount() == 1)
			return data.toString(flags);
		return amount + " " + data.toString(flags | Language.F_PLURAL);
	}

	public int getAmount() {
		return amount == -1 ? 1 : amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}

	public EntityData<?> getData() {
		return data;
	}

	public boolean sameType(EntityType other) {
		return data.equals(other.data);
	}

	@Override
	public EntityType clone() {
		return new EntityType(this);
	}
	
	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + amount;
		result = prime * result + data.hashCode();
		return result;
	}
	
	@Override
	public boolean equals(@Nullable Object obj) {
		if (!(obj instanceof EntityType other))
			return false;
		return amount == other.amount && data.equals(other.data);
	}
	
}
