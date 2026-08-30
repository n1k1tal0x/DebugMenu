package dev.n1k1tal0x.debugmenu.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * A debug parameter that can be switched on and off, together with the key that toggles it in game.
 *
 * The parameters F3 + Q advertises are a mixed bag: hitboxes and chunk borders are debug screen
 * entries, advanced tooltips and lost focus pause are plain options, and the overlay is its own
 * flag. KeyboardHandler wires each key to exactly one of those, and this interface hides the
 * difference behind a single on/off contract.
 */
public interface DebugToggle {
	/** Slug behind this parameter's translation keys: debugmenu.entry.<key> and .desc. */
	String key();

	Component label();

	/** The key that flips this parameter in game, or null when only this menu can reach it. */
	KeyMapping hotkey();

	boolean isOn();

	void set(boolean on);

	/** Parameters with a hotkey come first - those are the ones the F3 + Q list is about. */
	static List<DebugToggle> all(Minecraft minecraft) {
		Map<Identifier, KeyMapping> keyed = Map.of(
				DebugScreenEntries.ENTITY_HITBOXES, minecraft.options.keyDebugShowHitboxes,
				DebugScreenEntries.CHUNK_BORDERS, minecraft.options.keyDebugShowChunkBorders);

		List<DebugToggle> toggles = new ArrayList<>();

		toggles.add(new Flag(
				"overlay",
				labelFor("overlay", minecraft.options.keyDebugOverlay),
				minecraft.options.keyDebugOverlay,
				() -> minecraft.debugEntries.isOverlayVisible(),
				on -> minecraft.debugEntries.setOverlayVisible(on)));

		toggles.add(new Flag(
				"advanced_tooltips",
				labelFor("advanced_tooltips", minecraft.options.keyDebugShowAdvancedTooltips),
				minecraft.options.keyDebugShowAdvancedTooltips,
				() -> minecraft.options.advancedItemTooltips,
				on -> {
					minecraft.options.advancedItemTooltips = on;
					minecraft.options.save();
				}));

		toggles.add(new Flag(
				"focus_pause",
				labelFor("focus_pause", minecraft.options.keyDebugFocusPause),
				minecraft.options.keyDebugFocusPause,
				() -> minecraft.options.pauseOnLostFocus,
				on -> {
					minecraft.options.pauseOnLostFocus = on;
					minecraft.options.save();
				}));

		List<DebugToggle> entries = new ArrayList<>();

		for (Identifier id : DebugScreenEntries.allEntries().keySet()) {
			String key = keyOf(id);
			entries.add(new Entry(key, labelFor(key, derivedName(id)), keyed.get(id), minecraft, id));
		}

		entries.sort(Comparator
				.comparing((DebugToggle toggle) -> toggle.hotkey() == null)
				.thenComparing(toggle -> toggle.label().getString()));
		toggles.addAll(entries);

		return List.copyOf(toggles);
	}

	/** Only entries this mod ships a translation for get a nicer name; anything else keeps its id. */
	private static Component labelFor(String key, String fallback) {
		return Component.translatableWithFallback("debugmenu.entry." + key, fallback);
	}

	private static Component labelFor(String key, KeyMapping fallback) {
		return labelFor(key, Component.translatable(fallback.getName()).getString());
	}

	/** Entries from other mods keep their namespace, so their keys cannot collide with vanilla ones. */
	private static String keyOf(Identifier id) {
		return id.getNamespace().equals("minecraft") ? id.getPath() : id.getNamespace() + "." + id.getPath();
	}

	/** Debug entries carry no translation keys, so the identifier path becomes the label. */
	private static String derivedName(Identifier id) {
		StringBuilder name = new StringBuilder();

		for (String word : id.getPath().split("_")) {
			if (word.isEmpty()) {
				continue;
			}

			if (!name.isEmpty()) {
				name.append(' ');
			}

			name.append(Character.toUpperCase(word.charAt(0))).append(word, 1, word.length());
		}

		return name.toString();
	}

	/** A parameter backed by a boolean somewhere in the client. */
	record Flag(String key, Component label, KeyMapping hotkey, BooleanSupplier reader, Consumer<Boolean> writer)
			implements DebugToggle {
		@Override
		public boolean isOn() {
			return reader.getAsBoolean();
		}

		@Override
		public void set(boolean on) {
			writer.accept(on);
		}
	}

	/** A parameter backed by a debug screen entry status. */
	record Entry(String key, Component label, KeyMapping hotkey, Minecraft minecraft, Identifier id)
			implements DebugToggle {
		/**
		 * Whether the entry is being drawn right now, not merely how it is configured: an entry set
		 * to IN_OVERLAY stays invisible while the F3 overlay is closed, and reporting that as on
		 * would make the button lie.
		 */
		@Override
		public boolean isOn() {
			return minecraft.debugEntries.isCurrentlyEnabled(id);
		}

		/**
		 * ALWAYS_ON rather than IN_OVERLAY, so switching a parameter on shows it immediately instead
		 * of waiting for the player to also open the F3 overlay. setStatus rebuilds the enabled list
		 * and saves on its own.
		 */
		@Override
		public void set(boolean on) {
			minecraft.debugEntries.setStatus(id, on ? DebugScreenEntryStatus.ALWAYS_ON : DebugScreenEntryStatus.NEVER);
		}
	}
}
