package ch.njol.skript.lang;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class SectionEvent<S extends SyntaxElement & SectionValueProvider> extends Event {

	private final S element;

	public SectionEvent(S element) {
		this.element = element;
	}

	public S getElement() {
		return element;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		throw new IllegalStateException();
	}

}
