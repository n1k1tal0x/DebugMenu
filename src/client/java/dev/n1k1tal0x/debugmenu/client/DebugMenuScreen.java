package dev.n1k1tal0x.debugmenu.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import net.minecraft.client.gui.components.debug.DebugScreenProfile;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Debug parameters and one-shot debug actions, split across two tabs. */
public class DebugMenuScreen extends Screen {
	private static final int RESET_WIDTH = 80;
	private static final int FOOTER_SPACING = 8;

	private final Screen parent;
	private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
	private final TabManager tabManager = new TabManager(this::addRenderableWidget, this::removeWidget);

	private MenuTabBar tabBar;
	private DebugEntryList parameters;
	private DebugEntryList actions;
	private Button restoreDefaults;

	public DebugMenuScreen(Screen parent) {
		super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.translatable("menu.debugmenu.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		parameters = new DebugEntryList(minecraft, width, layout.getContentHeight(), layout.getHeaderHeight(),
				DebugToggle.all(minecraft), this::refreshFooter);
		actions = new DebugEntryList(minecraft, width, layout.getContentHeight(), layout.getHeaderHeight(),
				DebugAction.all(minecraft), this::refreshFooter);

		tabBar = MenuTabBar.builder(tabManager, width)
				.addTabs(new ListTab(Component.translatable("menu.debugmenu.section.parameters"), parameters),
						new ListTab(Component.translatable("menu.debugmenu.section.actions"), actions))
				.build();
		addRenderableWidget(tabBar);

		LinearLayout footer = LinearLayout.horizontal().spacing(FOOTER_SPACING);
		restoreDefaults = footer.addChild(Button.builder(Component.translatable("menu.debugmenu.restore_defaults"), ignored -> restoreDefaults())
				.width(RESET_WIDTH)
				.build());
		footer.addChild(Button.builder(CommonComponents.GUI_DONE, ignored -> onClose())
				.width(Button.DEFAULT_WIDTH)
				.build());
		layout.addToFooter(footer);
		layout.visitWidgets(this::addRenderableWidget);

		tabBar.selectTab(0, false);
		refreshFooter();
		repositionElements();
	}

	/**
	 * Puts the debug entries back to the vanilla defaults. Every status is written to disk as it
	 * changes, so without this a player who switches entries off is left with an empty F3 overlay
	 * and no way back. Only entries follow the profile, so the option-backed parameters and the
	 * actions are left alone. loadProfile does not persist on its own.
	 */
	private void restoreDefaults() {
		DebugScreenEntryList entries = minecraft.debugEntries;

		entries.loadProfile(DebugScreenProfile.DEFAULT);
		entries.save();

		parameters.refreshRows();
		actions.refreshRows();
		refreshFooter();
	}

	/**
	 * A button whose action would change nothing is switched off, and an inactive button ignores
	 * clicks: there is nothing to restore while the entries already match the default profile.
	 */
	private void refreshFooter() {
		restoreDefaults.active = !minecraft.debugEntries.isUsingProfile(DebugScreenProfile.DEFAULT);
	}

	@Override
	protected void repositionElements() {
		tabBar.setWidth(width);
		tabBar.arrangeElements(width);

		int tabBarBottom = tabBar.getRectangle().bottom();

		layout.setHeaderHeight(tabBarBottom);
		layout.arrangeElements();
		tabManager.setTabArea(new ScreenRectangle(0, tabBarBottom, width, layout.getContentHeight()));
	}

	@Override
	public void onClose() {
		minecraft.setScreenAndShow(parent);
	}

	/** A tab whose whole content is one scrolling list, sized to the area the tab manager hands it. */
	private static class ListTab extends GridLayoutTab {
		private final DebugEntryList list;

		ListTab(Component title, DebugEntryList list) {
			super(title);
			this.list = list;
		}

		@Override
		public void visitChildren(java.util.function.Consumer<net.minecraft.client.gui.components.AbstractWidget> visitor) {
			super.visitChildren(visitor);
			visitor.accept(list);
		}

		@Override
		public void doLayout(ScreenRectangle area) {
			list.updateSizeAndPosition(area.width(), area.height(), area.top());
			super.doLayout(area);
		}
	}
}
