package dev.n1k1tal0x.debugmenu.client;

import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * A scrolling list with one row per debug screen entry: the entry name on the left, a button
 * showing and flipping its state on the right.
 */
public class DebugEntryList extends ContainerObjectSelectionList<DebugEntryList.Row> {
	/** Switching an entry on shows it in the F3 overlay rather than pinning it permanently on screen. */
	public static final DebugScreenEntryStatus ON = DebugScreenEntryStatus.IN_OVERLAY;
	public static final DebugScreenEntryStatus OFF = DebugScreenEntryStatus.NEVER;

	private static final int ROW_HEIGHT = 24;
	private static final int ROW_WIDTH = 310;
	private static final int TOGGLE_WIDTH = 50;
	private static final int LABEL_COLOR = 0xFFFFFFFF;

	private final Runnable onToggled;

	public DebugEntryList(Minecraft minecraft, int width, int height, int y, Runnable onToggled) {
		super(minecraft, width, height, y, ROW_HEIGHT);
		this.onToggled = onToggled;

		DebugScreenEntries.allEntries().keySet().stream()
				.sorted(Comparator.comparing(Identifier::toString))
				.forEach(id -> addEntry(new Row(id)));
	}

	@Override
	public int getRowWidth() {
		return ROW_WIDTH;
	}

	/** Re-reads every row after the footer buttons changed all entries at once. */
	public void refreshRows() {
		children().forEach(Row::refresh);
	}

	public class Row extends ContainerObjectSelectionList.Entry<Row> {
		private final Identifier id;
		private final Component label;
		private final Button toggle;

		Row(Identifier id) {
			this.id = id;
			this.label = labelFor(id);
			this.toggle = Button.builder(Component.empty(), ignored -> flip())
					.width(TOGGLE_WIDTH)
					.build();
			refresh();
		}

		private void flip() {
			DebugScreenEntryList entries = minecraft.debugEntries;

			entries.setStatus(id, entries.getStatus(id) == OFF ? ON : OFF);
			entries.rebuildCurrentList();
			entries.save();

			refresh();
			onToggled.run();
		}

		void refresh() {
			boolean on = minecraft.debugEntries.getStatus(id) != OFF;
			toggle.setMessage(Component.translatable(on ? "menu.debugmenu.on" : "menu.debugmenu.off"));
		}

		@Override
		public void extractContent(GuiGraphicsExtractor extractor, int mouseX, int mouseY, boolean hovered, float partialTick) {
			int textY = getContentY() + (getContentHeight() - minecraft.font.lineHeight) / 2;
			extractor.text(minecraft.font, label, getContentX(), textY, LABEL_COLOR);

			toggle.setX(getContentRight() - TOGGLE_WIDTH);
			toggle.setY(getContentY() + (getContentHeight() - toggle.getHeight()) / 2);
			toggle.extractRenderState(extractor, mouseX, mouseY, partialTick);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of(toggle);
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of(toggle);
		}
	}

	/** Debug entries carry no translation keys, so the identifier path becomes the label. */
	private static Component labelFor(Identifier id) {
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

		return Component.literal(name.toString());
	}
}
