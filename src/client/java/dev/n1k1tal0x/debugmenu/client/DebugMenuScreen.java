package dev.n1k1tal0x.debugmenu.client;

import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Every debug screen entry in one scrolling column, each with its own On/Off button. */
public class DebugMenuScreen extends Screen {
	private static final int TOGGLE_WIDTH = 80;
	private static final int FOOTER_SPACING = 8;

	private final Screen parent;
	private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

	private DebugEntryList list;
	private Button enableAll;
	private Button disableAll;

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
		enableAll = footer.addChild(Button.builder(Component.translatable("menu.debugmenu.enable_all"), ignored -> switchAll(DebugEntryList.ON))
				.width(TOGGLE_WIDTH)
				.build());
		disableAll = footer.addChild(Button.builder(Component.translatable("menu.debugmenu.disable_all"), ignored -> switchAll(DebugEntryList.OFF))
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
			if (status != DebugEntryList.ON || entries.getStatus(id) == DebugEntryList.OFF) {
				entries.setStatus(id, status);
			}
		}

		entries.rebuildCurrentList();
		entries.save();

		list.refreshRows();
		refreshToggles();
	}

	/**
	 * A button whose action would change nothing is switched off. An inactive button ignores clicks,
	 * so this is also what blocks the click event once everything is already on (or already off).
	 */
	private void refreshToggles() {
		DebugScreenEntryList entries = minecraft.debugEntries;
		Set<Identifier> ids = DebugScreenEntries.allEntries().keySet();

		enableAll.active = ids.stream().anyMatch(id -> entries.getStatus(id) == DebugEntryList.OFF);
		disableAll.active = ids.stream().anyMatch(id -> entries.getStatus(id) != DebugEntryList.OFF);
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
