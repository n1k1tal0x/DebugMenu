package dev.n1k1tal0x.debugmenu.client;

import java.util.List;
import java.util.Locale;

import com.mojang.blaze3d.platform.InputConstants;

import org.lwjgl.glfw.GLFW;

import dev.n1k1tal0x.debugmenu.client.mixin.KeyMappingAccessor;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

/**
 * A scrolling list with one row per debug row: its name on the left, a help marker beside the name,
 * the key that triggers it in game in the middle, and a button on the right.
 */
public class DebugEntryList extends ContainerObjectSelectionList<DebugEntryList.Row> {
	private static final Component HELP_MARKER = Component.literal("?");

	private static final int ROW_HEIGHT = 24;
	private static final int ROW_WIDTH = 360;
	private static final int BUTTON_WIDTH = 70;
	private static final int HOTKEY_GAP = 8;
	private static final int HELP_GAP = 4;
	private static final int LABEL_COLOR = 0xFFFFFFFF;
	private static final int HOTKEY_COLOR = 0xFFA0A0A0;
	private static final int HELP_COLOR = 0xFF9BC4FF;

	/** Keeps a long description from running off the edge of the window. */
	private static final float TOOLTIP_WIDTH_SHARE = 0.6F;

	private final Runnable onChanged;

	public DebugEntryList(Minecraft minecraft, int width, int height, int y, List<? extends DebugRow> rows, Runnable onChanged) {
		super(minecraft, width, height, y, ROW_HEIGHT);
		this.onChanged = onChanged;

		rows.forEach(row -> addEntry(new Row(row)));
	}

	@Override
	public int getRowWidth() {
		return ROW_WIDTH;
	}

	/** Re-reads every row, for when something outside the list changed the state behind them. */
	public void refreshRows() {
		children().forEach(Row::refresh);
	}

	/**
	 * The modifier is a binding of its own and is not part of the triggering key, so it has to be
	 * spelled out here. The overlay key is bound to the modifier itself, which would otherwise read
	 * as "F3 + F3".
	 */
	private Component hotkeyText(DebugRow row) {
		KeyMapping hotkey = row.hotkey();

		if (hotkey == null || hotkey.isUnbound()) {
			return Component.empty();
		}

		KeyMapping modifier = minecraft.options.keyDebugModifier;

		if (modifier.isUnbound() || hotkey.same(modifier)) {
			return keyName(hotkey);
		}

		return Component.translatable("menu.debugmenu.combo", keyName(modifier), keyName(hotkey));
	}

	/**
	 * Asks GLFW what the key is called right now instead of reusing the translated message of the
	 * mapping: InputConstants.Key memoizes that message, so it keeps whatever keyboard layout was
	 * active when the key was first named and never follows a layout change.
	 */
	private static Component keyName(KeyMapping mapping) {
		InputConstants.Key key = ((KeyMappingAccessor) mapping).debugmenu$boundKey();

		if (key.getType() == InputConstants.Type.KEYSYM) {
			String name = GLFW.glfwGetKeyName(key.getValue(), 0);

			if (name != null && !name.isBlank()) {
				return Component.literal(name.toUpperCase(Locale.ROOT));
			}
		}

		return mapping.getTranslatedKeyMessage();
	}

	/** Not every row is described, and a missing key would otherwise show up as a raw string. */
	private static Component descriptionOf(DebugRow row) {
		String key = "debugmenu.entry." + row.key() + ".desc";
		return Language.getInstance().has(key) ? Component.translatable(key) : null;
	}

	public class Row extends ContainerObjectSelectionList.Entry<Row> {
		private final DebugRow row;
		private final Component hotkey;
		private final Component description;
		private final Button button;

		Row(DebugRow row) {
			this.row = row;
			this.hotkey = hotkeyText(row);
			this.description = descriptionOf(row);
			this.button = Button.builder(Component.empty(), ignored -> activate())
					.width(BUTTON_WIDTH)
					.build();
			refresh();
		}

		private void activate() {
			row.activate();
			refresh();
			onChanged.run();
		}

		void refresh() {
			button.setMessage(row.buttonLabel());
			button.active = row.enabled();
		}

		@Override
		public void extractContent(GuiGraphicsExtractor extractor, int mouseX, int mouseY, boolean hovered, float partialTick) {
			int textY = getContentY() + (getContentHeight() - minecraft.font.lineHeight) / 2;
			int buttonX = getContentRight() - BUTTON_WIDTH;
			int helpX = getContentX() + minecraft.font.width(row.label()) + HELP_GAP;

			extractor.text(minecraft.font, row.label(), getContentX(), textY, LABEL_COLOR);
			extractor.text(minecraft.font, hotkey, buttonX - HOTKEY_GAP - minecraft.font.width(hotkey), textY, HOTKEY_COLOR);

			if (description != null) {
				extractor.text(minecraft.font, HELP_MARKER, helpX, textY, HELP_COLOR);

				// Only over the marker, so the description does not pop up while aiming for the button.
				if (hovered && mouseX >= helpX && mouseX <= helpX + minecraft.font.width(HELP_MARKER)) {
					int maxWidth = (int) (minecraft.getWindow().getGuiScaledWidth() * TOOLTIP_WIDTH_SHARE);

					extractor.setTooltipForNextFrame(minecraft.font, minecraft.font.split(description, maxWidth), mouseX, mouseY);
				}
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
