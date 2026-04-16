package ch.njol.skript.patterns;

import ch.njol.skript.Skript;
import ch.njol.skript.test.runner.SkriptJUnitTest;
import org.junit.Test;
import org.skriptlang.skript.registration.SyntaxInfo;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

public class PatternCounterTest extends SkriptJUnitTest {

	private void info(String message) {
		Skript.debug(message);
		Skript.adminBroadcast(message);
	}

	@Test
	public void test() {
		Collection<SyntaxInfo<?>> elements = Skript.instance().syntaxRegistry().elements();
		info("Running Regex Counter Test");
		info("Total Elements: " + elements.size());
		int patternRegexes = 0;
		int elementRegexes = 0;
		for (SyntaxInfo<?> syntaxInfo : elements) {
			Collection<String> patterns = syntaxInfo.patterns();
			Class<?> elementClass = syntaxInfo.type();

			boolean hasRegex = false;
			for (String pattern : patterns) {
				PatternElement patternElement = PatternCompiler.compile(pattern, new AtomicInteger());
				if (patternElement.containsRegex()) {
					patternRegexes++;
					info(elementClass.getSimpleName() + " - " + pattern);
					hasRegex = true;
				}
			}
			if (hasRegex)
				elementRegexes++;
		}
		info("Total Element Regexes: " + elementRegexes);
		info("Total Pattern Regexes: " + patternRegexes);
	}

}
