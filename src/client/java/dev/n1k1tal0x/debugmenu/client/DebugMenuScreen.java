package dev.n1k1tal0x.debugmenu.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Lists every debug key binding the client knows about.
 *
 * The entries come from {@code Options.debugKeys} rather than from a hardcoded table, so they
 * follow the player's own rebinds instead of showing the defaults that F3 + Q prints.
 */
public class DebugMenuScreen extends Screen {
	private static final int COLUMNS = 2;
	private static final int COLUMN_SPACING = 16;
	private static final int ROW_SPACING = 4;

	private final Screen parent;
	private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

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
		layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, ignored -> onClose())
				.width(Button.DEFAULT_WIDTH)
				.build());

		layout.visitWidgets(this::addRenderableWidget);
		repositionElements();
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
