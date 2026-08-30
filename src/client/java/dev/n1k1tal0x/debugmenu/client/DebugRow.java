package dev.n1k1tal0x.debugmenu.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

/** One line of the debug menu: a parameter that can be switched, or an action that can be run. */
public interface DebugRow {
	/** Slug behind this row's translation keys: debugmenu.entry.&lt;key&gt; and .desc. */
	String key();

	Component label();

	/** The key that triggers this row in game, or null when only this menu can reach it. */
	KeyMapping hotkey();

	/** Text on the row's button: the current state for a parameter, "Run" for an action. */
	Component buttonLabel();

	/** Whether the button takes clicks. An action can be unavailable; a parameter never is. */
	boolean enabled();

	void activate();

	/** Only rows this mod ships a translation for get a nicer name; anything else keeps its id. */
	static Component labelFor(String key, String fallback) {
		return Component.translatableWithFallback("debugmenu.entry." + key, fallback);
	}

	static Component labelFor(String key, KeyMapping fallback) {
		return labelFor(key, Component.translatable(fallback.getName()).getString());
	}
}
