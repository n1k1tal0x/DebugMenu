package dev.n1k1tal0x.debugmenu.client;

import java.util.Set;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Lists every debug key binding the client knows about, and switches all debug screen entries on
 * or off at once.
 *
 * The bindings come from {@code Options.debugKeys} rather than from a hardcoded table, so they
 * follow the player's own rebinds instead of showing the defaults that F3 + Q prints.
 */
public class DebugMenuScreen extends Screen {
	private static final int COLUMNS = 2;
	private static final int COLUMN_SPACING = 16;
	private static final int ROW_SPACING = 4;
	private static final int TOGGLE_WIDTH = 60;
	private static final int FOOTER_SPACING = 8;

	/** Switching an entry on shows it in the F3 overlay rather than pinning it permanently on screen. */
	private static final DebugScreenEntryStatus ON = DebugScreenEntryStatus.IN_OVERLAY;
	private static final DebugScreenEntryStatus OFF = DebugScreenEntryStatus.NEVER;

	private final Screen parent;
	private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

	private Button enableAll;
	private Button disableAll;

	public DebugMenuScreen(Screen parent) {
		super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.translatable("menu.debugmenu.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		layout.addToHeader(new StringWidget(title, font));

		GridLayout grid = new GridLayout().columnSpacing(COLUMN_SPACING).rowSpacing(ROW_SPACING);
		KeyMapping[] debugKeys = minecraft.options.debugKeys;

		for (int i = 0; i < debugKeys.length; i++) {
			grid.addChild(new StringWidget(entryFor(debugKeys[i]), font), i / COLUMNS, i % COLUMNS);
		}

		layout.addToContents(grid);

		LinearLayout footer = LinearLayout.horizontal().spacing(FOOTER_SPACING);
		enableAll = footer.addChild(Button.builder(Component.translatable("menu.debugmenu.enable_all"), ignored -> switchAll(ON))
				.width(TOGGLE_WIDTH)
				.build());
		disableAll = footer.addChild(Button.builder(Component.translatable("menu.debugmenu.disable_all"), ignored -> switchAll(OFF))
				.width(TOGGLE_WIDTH)
				.build());
		footer.addChild(Button.builder(CommonComponents.GUI_DONE, ignored -> onClose())
				.width(Button.DEFAULT_WIDTH)
				.build());
		layout.addToFooter(footer);

		refreshToggles();

		layout.visitWidgets(this::addRenderableWidget);
		repositionElements();
	}

	/**
	 * Switching on only lifts entries that are off, so an entry the player pinned with ALWAYS_ON is
	 * not quietly demoted to overlay-only.
	 */
	private void switchAll(DebugScreenEntryStatus status) {
		DebugScreenEntryList entries = minecraft.debugEntries;

		for (Identifier id : DebugScreenEntries.allEntries().keySet()) {
			if (status != ON || entries.getStatus(id) == OFF) {
				entries.setStatus(id, status);
			}
		}

		entries.rebuildCurrentList();
		entries.save();
		refreshToggles();
	}

	/**
	 * A button whose action would change nothing is switched off. An inactive button ignores clicks,
	 * so this is also what blocks the click event once everything is already on (or already off).
	 */
	private void refreshToggles() {
		DebugScreenEntryList entries = minecraft.debugEntries;
		Set<Identifier> ids = DebugScreenEntries.allEntries().keySet();

		enableAll.active = ids.stream().anyMatch(id -> entries.getStatus(id) == OFF);
		disableAll.active = ids.stream().anyMatch(id -> entries.getStatus(id) != OFF);
	}

	private Component entryFor(KeyMapping mapping) {
		return Component.translatable("menu.debugmenu.entry", Component.translatable(mapping.getName()), comboFor(mapping));
	}

	/**
	 * Debug keys are only read while the modifier (F3 by default) is held, and the modifier is not
	 * part of the mapping itself, so it has to be spelled out here.
	 */
	private Component comboFor(KeyMapping mapping) {
		if (mapping.isUnbound()) {
			return Component.translatable("menu.debugmenu.unbound");
		}

		KeyMapping modifier = minecraft.options.keyDebugModifier;

		if (modifier.isUnbound()) {
			return mapping.getTranslatedKeyMessage();
		}

		return Component.translatable("menu.debugmenu.combo",
				modifier.getTranslatedKeyMessage(),
				mapping.getTranslatedKeyMessage());
	}

	@Override
	protected void repositionElements() {
		layout.arrangeElements();
	}

	@Override
	public void onClose() {
		minecraft.setScreenAndShow(parent);
	}
}
