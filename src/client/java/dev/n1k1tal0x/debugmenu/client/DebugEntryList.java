package dev.n1k1tal0x.debugmenu.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.mojang.blaze3d.platform.InputConstants;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

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
 * A scrolling list of debug rows. Each cell shows a name with a help marker beside it, the key that
 * triggers it in game, and a button. Rows are laid out two to a line while both columns still have
 * room to read, and one to a line otherwise.
 */
public class DebugEntryList extends ContainerObjectSelectionList<DebugEntryList.Row> {
	private static final Component HELP_MARKER = Component.literal("?");

	private static final int ROW_HEIGHT = 24;
	private static final int ROW_MARGIN = 24;

	/** A column has to fit a name, its hotkey and a button before a second one is worth having. */
	private static final int MIN_COLUMN_WIDTH = 280;
	private static final int MAX_ROW_WIDTH_ONE_COLUMN = 360;
	private static final int MAX_ROW_WIDTH_TWO_COLUMNS = 620;
	private static final int COLUMN_GAP = 16;
	private static final int BUTTON_WIDTH = 70;
	private static final int HOTKEY_GAP = 8;
	private static final int HELP_GAP = 4;
	private static final int LABEL_COLOR = 0xFFFFFFFF;
	private static final int HOTKEY_COLOR = 0xFFA0A0A0;
	private static final int HELP_COLOR = 0xFF9BC4FF;

	/** Keeps a long description from running off the edge of the window. */
	private static final float TOOLTIP_WIDTH_SHARE = 0.6F;

	private final List<? extends DebugRow> source;
	private final Runnable onChanged;

	private int columns;

	public DebugEntryList(Minecraft minecraft, int width, int height, int y, List<? extends DebugRow> rows, Runnable onChanged) {
		super(minecraft, width, height, y, ROW_HEIGHT);
		this.source = rows;
		this.onChanged = onChanged;

		rebuild(columnsFor(width));
	}

	/** Two columns only while each one keeps its minimum; a cramped second column reads worse. */
	private static int columnsFor(int listWidth) {
		return listWidth - ROW_MARGIN >= MIN_COLUMN_WIDTH * 2 + COLUMN_GAP ? 2 : 1;
	}

	private void rebuild(int columns) {
		this.columns = columns;

		clearEntries();

		for (int i = 0; i < source.size(); i += columns) {
			addEntry(new Row(source.subList(i, Math.min(i + columns, source.size()))));
		}
	}

	/** The layout changes with the window, so the column count is decided again on every resize. */
	@Override
	public void updateSizeAndPosition(int width, int height, int y) {
		super.updateSizeAndPosition(width, height, y);

		int wanted = columnsFor(width);

		if (wanted != columns) {
			rebuild(wanted);
		}
	}

	@Override
	public int getRowWidth() {
		int max = columns > 1 ? MAX_ROW_WIDTH_TWO_COLUMNS : MAX_ROW_WIDTH_ONE_COLUMN;

		return Math.min(max, getWidth() - ROW_MARGIN);
	}

	/** Re-reads every cell, for when something outside the list changed the state behind them. */
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
		InputConstants.Key key = KeyMappingHelper.getBoundKeyOf(mapping);

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
		private final List<Cell> cells;

		Row(List<? extends DebugRow> rows) {
			this.cells = rows.stream().map(Cell::new).toList();
		}

		void refresh() {
			cells.forEach(Cell::refresh);
		}

		@Override
		public void extractContent(GuiGraphicsExtractor extractor, int mouseX, int mouseY, boolean hovered, float partialTick) {
			// The last line can be half empty, so a cell keeps its column width either way.
			int cellWidth = (getContentWidth() - COLUMN_GAP * (columns - 1)) / columns;

			for (int i = 0; i < cells.size(); i++) {
				cells.get(i).extract(extractor, getContentX() + i * (cellWidth + COLUMN_GAP), cellWidth,
						getContentY(), getContentHeight(), mouseX, mouseY, hovered, partialTick);
			}
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return buttons();
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return buttons();
		}

		private List<Button> buttons() {
			List<Button> buttons = new ArrayList<>(cells.size());

			cells.forEach(cell -> buttons.add(cell.button));

			return buttons;
		}
	}

	/** One debug row inside its column. */
	private class Cell {
		private final DebugRow row;
		private final Component hotkey;
		private final Component description;
		private final Button button;

		Cell(DebugRow row) {
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

		void extract(GuiGraphicsExtractor extractor, int x, int cellWidth, int y, int height,
				int mouseX, int mouseY, boolean hovered, float partialTick) {
			int textY = y + (height - minecraft.font.lineHeight) / 2;
			int buttonX = x + cellWidth - BUTTON_WIDTH;
			int helpX = x + minecraft.font.width(row.label()) + HELP_GAP;

			extractor.text(minecraft.font, row.label(), x, textY, LABEL_COLOR);
			extractor.text(minecraft.font, hotkey, buttonX - HOTKEY_GAP - minecraft.font.width(hotkey), textY, HOTKEY_COLOR);

			if (description != null) {
				extractor.text(minecraft.font, HELP_MARKER, helpX, textY, HELP_COLOR);

				// Only over the marker, so the description does not pop up while aiming for a button.
				if (hovered && mouseX >= helpX && mouseX <= helpX + minecraft.font.width(HELP_MARKER)) {
					int maxWidth = (int) (minecraft.getWindow().getGuiScaledWidth() * TOOLTIP_WIDTH_SHARE);

					extractor.setTooltipForNextFrame(minecraft.font, minecraft.font.split(description, maxWidth), mouseX, mouseY);
				}
			}

			button.setX(buttonX);
			button.setY(y + (height - button.getHeight()) / 2);
			button.extractRenderState(extractor, mouseX, mouseY, partialTick);
		}
	}
}
