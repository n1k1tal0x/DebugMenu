package dev.n1k1tal0x.debugmenu.client;

import java.util.List;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import net.minecraft.client.gui.components.debug.DebugScreenProfile;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

/**
 * A scrolling list with one row per debug parameter: its name on the left, the key that toggles it
 * in game in the middle, and a button showing and flipping its state on the right.
 */
public class DebugEntryList extends ContainerObjectSelectionList<DebugEntryList.Row> {
	private static final int ROW_HEIGHT = 24;
	private static final int ROW_WIDTH = 340;
	private static final int TOGGLE_WIDTH = 50;
	private static final int HOTKEY_GAP = 8;
	private static final int LABEL_COLOR = 0xFFFFFFFF;
	private static final int HOTKEY_COLOR = 0xFFA0A0A0;

	private final List<DebugToggle> toggles;
	private final Runnable onToggled;

	public DebugEntryList(Minecraft minecraft, int width, int height, int y, Runnable onToggled) {
		super(minecraft, width, height, y, ROW_HEIGHT);
		this.toggles = DebugToggle.all(minecraft);
		this.onToggled = onToggled;

		toggles.forEach(toggle -> addEntry(new Row(toggle)));
	}

	@Override
	public int getRowWidth() {
		return ROW_WIDTH;
	}

	/**
	 * Puts the debug entries back to the vanilla defaults, undoing whatever the rows switched off.
	 * Every status is written to disk as it changes, so without this the player can end up with an
	 * empty F3 overlay and no way back. Only entries follow the profile, so the option-backed
	 * parameters are left alone. loadProfile does not persist on its own.
	 */
	public void restoreDefaults() {
		DebugScreenEntryList entries = minecraft.debugEntries;

		entries.loadProfile(DebugScreenProfile.DEFAULT);
		entries.save();
		children().forEach(Row::refresh);
	}

	public boolean isDefaultProfile() {
		return minecraft.debugEntries.isUsingProfile(DebugScreenProfile.DEFAULT);
	}

	/**
	 * The modifier is a binding of its own and is not part of the toggling key, so it has to be
	 * spelled out here. The overlay key is bound to the modifier itself, which would otherwise read
	 * as "F3 + F3".
	 */
	private Component hotkeyText(DebugToggle toggle) {
		KeyMapping hotkey = toggle.hotkey();

		if (hotkey == null || hotkey.isUnbound()) {
			return Component.empty();
		}

		KeyMapping modifier = minecraft.options.keyDebugModifier;

		if (modifier.isUnbound() || hotkey.same(modifier)) {
			return hotkey.getTranslatedKeyMessage();
		}

		return Component.translatable("menu.debugmenu.combo",
				modifier.getTranslatedKeyMessage(),
				hotkey.getTranslatedKeyMessage());
	}

	/** Not every parameter is described, and a missing key would otherwise show up as a raw string. */
	private static Component descriptionOf(DebugToggle toggle) {
		String key = "debugmenu.entry." + toggle.key() + ".desc";
		return Language.getInstance().has(key) ? Component.translatable(key) : null;
	}

	public class Row extends ContainerObjectSelectionList.Entry<Row> {
		private final DebugToggle toggle;
		private final Component hotkey;
		private final Component description;
		private final Button button;

		Row(DebugToggle toggle) {
			this.toggle = toggle;
			this.hotkey = hotkeyText(toggle);
			this.description = descriptionOf(toggle);
			this.button = Button.builder(Component.empty(), ignored -> flip())
					.width(TOGGLE_WIDTH)
					.build();
			refresh();
		}

		private void flip() {
			toggle.set(!toggle.isOn());
			refresh();
			onToggled.run();
		}

		void refresh() {
			button.setMessage(Component.translatable(toggle.isOn() ? "menu.debugmenu.on" : "menu.debugmenu.off"));
		}

		@Override
		public void extractContent(GuiGraphicsExtractor extractor, int mouseX, int mouseY, boolean hovered, float partialTick) {
			int textY = getContentY() + (getContentHeight() - minecraft.font.lineHeight) / 2;
			int buttonX = getContentRight() - TOGGLE_WIDTH;

			int labelWidth = minecraft.font.width(toggle.label());

			extractor.text(minecraft.font, toggle.label(), getContentX(), textY, LABEL_COLOR);
			extractor.text(minecraft.font, hotkey, buttonX - HOTKEY_GAP - minecraft.font.width(hotkey), textY, HOTKEY_COLOR);

			// Only over the name itself, so the tooltip does not cover the button the player is aiming for.
			if (hovered && description != null && mouseX <= getContentX() + labelWidth) {
				extractor.setTooltipForNextFrame(minecraft.font, description, mouseX, mouseY);
			}

			button.setX(buttonX);
			button.setY(getContentY() + (getContentHeight() - button.getHeight()) / 2);
			button.extractRenderState(extractor, mouseX, mouseY, partialTick);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of(button);
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of(button);
		}
	}
}
