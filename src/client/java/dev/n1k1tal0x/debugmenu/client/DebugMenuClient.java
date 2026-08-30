package dev.n1k1tal0x.debugmenu.client;

import dev.n1k1tal0x.debugmenu.DebugMenu;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class DebugMenuClient implements ClientModInitializer {
	// assets/debugmenu/textures/gui/sprites/icon/bug.png, drawn by tools/make_icon.py
	public static final Identifier BUG_ICON = DebugMenu.id("icon/bug");

	private static final int BUTTON_SIZE = 20;
	private static final int ICON_SIZE = 16;
	private static final int MARGIN = 4;

	@Override
	public void onInitializeClient() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof PauseScreen)) {
				return;
			}

			Component label = Component.translatable("menu.debugmenu.open");

			SpriteIconButton button = SpriteIconButton
					.builder(label, ignored -> client.setScreenAndShow(new DebugMenuScreen(screen)), true)
					.size(BUTTON_SIZE, BUTTON_SIZE)
					.sprite(BUG_ICON, ICON_SIZE, ICON_SIZE)
					.build();

			// Bottom left corner, clear of the vanilla pause menu buttons.
			button.setX(MARGIN);
			button.setY(height - BUTTON_SIZE - MARGIN);
			button.setTooltip(Tooltip.create(label));

			Screens.getWidgets(screen).add(button);
		});
	}
}
