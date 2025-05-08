package ch.njol.skript.classes;

import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.DefaultExpression;
import ch.njol.util.coll.iterator.ArrayIterator;
import org.skriptlang.skript.lang.comparator.Comparators;
import org.skriptlang.skript.lang.comparator.Relation;

public class FieldClassInfo<T> extends ClassInfo<T> {

	public FieldClassInfo(Class<T> fieldClass, String codeName, String languageNode) {
		this(fieldClass, codeName, languageNode, new EventValueExpression<>(fieldClass), true);
	}

	public FieldClassInfo(Class<T> fieldClass, String codeName, String languageNode, boolean registerComparator) {
		this(fieldClass, codeName, languageNode, new EventValueExpression<>(fieldClass), registerComparator);
	}

	public FieldClassInfo(Class<T> fieldClass, String codeName, String languageNode, DefaultExpression<T> defaultExpression) {
		this(fieldClass, codeName, languageNode, defaultExpression, true);
	}

	public FieldClassInfo(Class<T> fieldClass, String codeName, String languageNode, DefaultExpression<T> defaultExpression, boolean registerComparator) {
		super(fieldClass, codeName);
		FieldParser<T> fieldParser = new FieldParser<>(fieldClass, languageNode);
		usage(fieldParser.getCombinedPatterns())
			.defaultExpression(defaultExpression)
			.supplier(() -> new ArrayIterator<>(fieldParser.getTypes()))
			.parser(fieldParser);
		if (registerComparator)
			Comparators.registerComparator(fieldClass, fieldClass, (o1, o2) -> Relation.get(o1.equals(o2)));
	}

}
