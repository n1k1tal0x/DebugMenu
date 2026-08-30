package dev.n1k1tal0x.debugmenu.client;

import com.mojang.blaze3d.platform.InputConstants;

import org.lwjgl.glfw.GLFW;

import dev.n1k1tal0x.debugmenu.DebugMenu;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class DebugMenuClient implements ClientModInitializer {
	// assets/debugmenu/textures/gui/sprites/icon/nvg.png, drawn by tools/make_icon.py
	public static final Identifier NVG_ICON = DebugMenu.id("icon/nvg");

	/** Bound to backslash by default: a free key that needs no modifier. */
	public static final KeyMapping OPEN_MENU = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.debugmenu.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_BACKSLASH, KeyMapping.Category.DEBUG));

	private static final int BUTTON_SIZE = 20;
	private static final int ICON_SIZE = 16;
	private static final int MARGIN = 4;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (OPEN_MENU.consumeClick()) {
				client.setScreenAndShow(new DebugMenuScreen(client.gui.screen()));
			}
		});

		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof PauseScreen)) {
				return;
			}

			Component label = Component.translatable("menu.debugmenu.open");

			SpriteIconButton button = SpriteIconButton
					.builder(label, ignored -> client.setScreenAndShow(new DebugMenuScreen(screen)), true)
					.size(BUTTON_SIZE, BUTTON_SIZE)
					.sprite(NVG_ICON, ICON_SIZE, ICON_SIZE)
					.build();

			// Bottom left corner, clear of the vanilla pause menu buttons.
			button.setX(MARGIN);
			button.setY(height - BUTTON_SIZE - MARGIN);
			button.setTooltip(Tooltip.create(label));

			Screens.getWidgets(screen).add(button);
		});
	}
}
