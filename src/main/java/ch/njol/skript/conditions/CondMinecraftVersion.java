package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.util.Version;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Running Minecraft")
@Description("Checks if current Minecraft version is given version or newer.")
@Example("if running minecraft \"1.14\":")
@Example("if the server is running below minecraft \"1.21.7\":")
@Example("if running exactly minecraft version \"1.20.4\":")
@Since("2.5, INSERT VERSION (exact)")
public class CondMinecraftVersion extends Condition {
	
	static {
		Skript.registerCondition(CondMinecraftVersion.class,
			"[[the] server is] running [:below|exact:[the] exact[ly]] minecraft [version] %string%");
	}

	private Expression<String> version;
	private boolean below = false;
	private boolean exact = false;

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		//noinspection unchecked
		version = (Expression<String>) exprs[0];
		below = parseResult.hasTag("below");
		exact = parseResult.hasTag("exact");
		return true;
	}
	
	@Override
	public boolean check(Event event) {
		String ver = version.getSingle(event);
		if (ver == null || ver.isEmpty())
			return false;
		Version version = new Version(ver);
		if (below) {
			return !Skript.isRunningMinecraft(version);
		} else if (exact) {
			return Skript.isRunningExactMinecraft(version);
		} else {
			return Skript.isRunningMinecraft(version);
		}
	}
	
	@Override
	public String toString(@Nullable Event event, boolean debug) {
		SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
		builder.append("running");
		if (below) {
			builder.append("below");
		} else if (exact) {
			builder.append("exact");
		}
		builder.append("minecraft", version);
		return builder.toString();
	}
	
}
