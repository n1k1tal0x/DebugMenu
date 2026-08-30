package dev.n1k1tal0x.debugmenu.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * A debug parameter that can be switched on and off, together with the key that toggles it in game.
 *
 * The parameters F3 + Q advertises are a mixed bag: hitboxes and chunk borders are debug screen
 * entries, advanced tooltips and lost focus pause are plain options, the charts live on the debug
 * overlay, and the overlay itself is its own flag. KeyboardHandler wires each key to exactly one of
 * those, and this interface hides the difference behind a single on/off contract.
 */
public interface DebugToggle extends DebugRow {
	boolean isOn();

	void set(boolean on);

	@Override
	default Component buttonLabel() {
		return Component.translatable(isOn() ? "menu.debugmenu.on" : "menu.debugmenu.off");
	}

	/** Most parameters can always be flipped; entries override this when the server forbids them. */
	@Override
	default boolean enabled() {
		return true;
	}

	@Override
	default void activate() {
		set(!isOn());
	}

	/** Parameters with a hotkey come first - those are the ones the F3 + Q list is about. */
	static List<DebugToggle> all(Minecraft minecraft) {
		Map<Identifier, KeyMapping> keyed = Map.of(
				DebugScreenEntries.ENTITY_HITBOXES, minecraft.options.keyDebugShowHitboxes,
				DebugScreenEntries.CHUNK_BORDERS, minecraft.options.keyDebugShowChunkBorders);

		List<DebugToggle> toggles = new ArrayList<>();

		toggles.add(new Flag(
				"overlay",
				DebugRow.labelFor("overlay", minecraft.options.keyDebugOverlay),
				minecraft.options.keyDebugOverlay,
				() -> minecraft.debugEntries.isOverlayVisible(),
				on -> minecraft.debugEntries.setOverlayVisible(on)));

		toggles.add(new Flag(
				"advanced_tooltips",
				DebugRow.labelFor("advanced_tooltips", minecraft.options.keyDebugShowAdvancedTooltips),
				minecraft.options.keyDebugShowAdvancedTooltips,
				() -> minecraft.options.advancedItemTooltips,
				on -> {
					minecraft.options.advancedItemTooltips = on;
					minecraft.options.save();
				}));

		toggles.add(new Flag(
				"focus_pause",
				DebugRow.labelFor("focus_pause", minecraft.options.keyDebugFocusPause),
				minecraft.options.keyDebugFocusPause,
				() -> minecraft.options.pauseOnLostFocus,
				on -> {
					minecraft.options.pauseOnLostFocus = on;
					minecraft.options.save();
				}));

		toggles.add(chart(minecraft, "profiler_chart", minecraft.options.keyDebugPofilingChart,
				DebugScreenOverlay::showProfilerChart, DebugScreenOverlay::toggleProfilerChart));
		toggles.add(chart(minecraft, "fps_charts", minecraft.options.keyDebugFpsCharts,
				DebugScreenOverlay::showFpsCharts, DebugScreenOverlay::toggleFpsCharts));
		toggles.add(chart(minecraft, "network_charts", minecraft.options.keyDebugNetworkCharts,
				DebugScreenOverlay::showNetworkCharts, DebugScreenOverlay::toggleNetworkCharts));
		toggles.add(chart(minecraft, "lightmap_texture", minecraft.options.keyDebugLightmapTexture,
				DebugScreenOverlay::showLightmapTexture, DebugScreenOverlay::toggleLightmapTexture));

		List<DebugToggle> entries = new ArrayList<>();

		for (Identifier id : DebugScreenEntries.allEntries().keySet()) {
			String key = keyOf(id);
			entries.add(new Entry(key, DebugRow.labelFor(key, derivedName(id)), keyed.get(id), minecraft, id));
		}

		entries.sort(Comparator
				.comparing((DebugToggle toggle) -> toggle.hotkey() == null)
				.thenComparing(toggle -> toggle.label().getString()));
		toggles.addAll(entries);

		return List.copyOf(toggles);
	}

	/** The overlay charts only expose a toggle, so setting a state means flipping when it differs. */
	private static DebugToggle chart(Minecraft minecraft, String key, KeyMapping hotkey,
			Predicate<DebugScreenOverlay> reader, Consumer<DebugScreenOverlay> flip) {
		return new Flag(key, DebugRow.labelFor(key, hotkey), hotkey,
				() -> reader.test(minecraft.getDebugOverlay()),
				on -> {
					DebugScreenOverlay overlay = minecraft.getDebugOverlay();

					if (reader.test(overlay) != on) {
						flip.accept(overlay);
					}
				});
	}

	/** Entries from other mods keep their namespace, so their keys cannot collide with vanilla ones. */
	private static String keyOf(Identifier id) {
		return id.getNamespace().equals("minecraft") ? id.getPath() : id.getNamespace() + "." + id.getPath();
	}

	/** Debug entries carry no translation keys, so the identifier path becomes the fallback name. */
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
		 * Reduced debug info keeps an entry out of the enabled list however it is configured, so
		 * without this the button would write a status to disk and then snap straight back.
		 */
		@Override
		public boolean enabled() {
			DebugScreenEntry entry = DebugScreenEntries.getEntry(id);

			return entry != null && entry.isAllowed(minecraft.showOnlyReducedInfo());
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
