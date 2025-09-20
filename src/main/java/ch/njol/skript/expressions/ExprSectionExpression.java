package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.expressions.base.WrapperExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionSection;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.Section;
import ch.njol.skript.lang.SectionEvent;
import ch.njol.skript.lang.SectionSkriptEvent;
import ch.njol.skript.lang.SectionValueProvider;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.SyntaxElement;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import ch.njol.util.StringUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.structure.Structure;

public class ExprSectionExpression<T> extends WrapperExpression<Object> {

	/**
	 * Registers an expression as {@link ExpressionType#EVENT} with the provided patterns.
	 * This also adds '[the]' to the start of all patterns.
	 *
	 * @param expression The class that represents this EventValueExpression.
	 * @param type The return type of the expression.
	 * @param patterns The patterns for this syntax.
	 */
	public static <T> void register(Class<? extends ExprSectionExpression<T>> expression, Class<T> type, String ... patterns) {
		for (int i = 0; i < patterns.length; i++) {
			if (!StringUtils.startsWithIgnoreCase(patterns[i], "[the] "))
				patterns[i] = "[the] " + patterns[i];
		}
		//noinspection unchecked
		Skript.registerExpression((Class<? extends Expression<T>>) expression, type, ExpressionType.EVENT, patterns);
	}

	static {
		Skript.registerExpression(ExprSectionExpression.class, Object.class, ExpressionType.PROPERTY,
			"[the] section-value", "[the] section-%*classinfo%");
	}

	private Class<?> type = Object.class;
	private ClassInfo<?> classInfo = null;

	public ExprSectionExpression() {}

	public ExprSectionExpression(ClassInfo<?> classInfo) {
		this.type = classInfo.getC();
		this.classInfo = classInfo;
	}

	public ExprSectionExpression(Class<?> type) {
		this.type = type;
		this.classInfo = Classes.getExactClassInfo(type);
	}

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		if (matchedPattern == 1) {
			//noinspection unchecked
			classInfo = ((Literal<ClassInfo<?>>) exprs[0]).getSingle();
			type = classInfo.getC();
		}
		return init(true);
	}

	public boolean init(boolean error) {
		ParserInstance parser = getParser();
		Structure structure = parser.getCurrentStructure();
		if (!(structure instanceof SectionSkriptEvent sectionSkriptEvent)) {
			if (error)
				Skript.error("There is no section to get a section value from.");
			return false;
		}
		Section section = sectionSkriptEvent.getSection();
		SyntaxElement syntaxElement = section instanceof ExpressionSection exprSec ? exprSec.getAsExpression() : section;
		boolean isEvent = parser.isCurrentEvent(SectionEvent.class);
		if (!isEvent || !(syntaxElement instanceof SectionValueProvider provider)) {
			if (error)
				Skript.error("This section does not support section values.");
			return false;
		}
		Expression<?> expr = provider.getSectionValue();
		if (!type.isAssignableFrom(expr.getReturnType())) {
			if (error) {
				assert classInfo != null;
				Skript.error("There is no " + classInfo.getName().toString(false) + " in " +
					Utils.a(sectionSkriptEvent.toString()) + " section.");
			}
			return false;
		}
		setExpr(expr);
		return true;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return getExpr().toString(event, debug);
	}

}
