package dev.n1k1tal0x.debugmenu.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Every debug parameter in one scrolling column, each with its hotkey and its own On/Off button. */
public class DebugMenuScreen extends Screen {
	private static final int TOGGLE_WIDTH = 80;
	private static final int FOOTER_SPACING = 8;

	private final Screen parent;
	private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

	private DebugEntryList list;
	private Button restoreDefaults;

	public DebugMenuScreen(Screen parent) {
		super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.translatable("menu.debugmenu.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		layout.addToHeader(new StringWidget(title, font));

		list = layout.addToContents(new DebugEntryList(minecraft, width, layout.getContentHeight(),
				layout.getHeaderHeight(), this::refreshToggles));

		LinearLayout footer = LinearLayout.horizontal().spacing(FOOTER_SPACING);
		restoreDefaults = footer.addChild(Button.builder(Component.translatable("menu.debugmenu.restore_defaults"), ignored -> restoreDefaults())
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

	private void restoreDefaults() {
		list.restoreDefaults();
		refreshToggles();
	}

	/**
	 * A button whose action would change nothing is switched off, and an inactive button ignores
	 * clicks: there is nothing to restore while the entries already match the default profile.
	 */
	private void refreshToggles() {
		restoreDefaults.active = !list.isDefaultProfile();
	}

	@Override
	protected void repositionElements() {
		layout.arrangeElements();

		if (list != null) {
			list.updateSize(width, layout);
		}
	}

	@Override
	public void onClose() {
		minecraft.setScreenAndShow(parent);
	}
}
