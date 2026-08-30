package dev.n1k1tal0x.debugmenu.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

import com.mojang.blaze3d.platform.TextureUtil;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.debug.DebugOptionsScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChangeGameModePacket;
import net.minecraft.server.commands.GameModeCommand;
import net.minecraft.server.commands.VersionCommand;
import net.minecraft.world.level.GameType;

/**
 * A one-shot debug command, the counterpart to the parameters in {@link DebugToggle}.
 *
 * Most of what F3 + Q lists is of this kind: Clear Chat or Reload Chunks have nothing to read back
 * as "on", so they get a Run button instead of a state. Each one calls what KeyboardHandler calls
 * for the same key.
 */
public record DebugAction(String key, Component label, KeyMapping hotkey, BooleanSupplier available, Runnable action)
		implements DebugRow {
	@Override
	public Component buttonLabel() {
		return Component.translatable("menu.debugmenu.run");
	}

	@Override
	public boolean enabled() {
		return available.getAsBoolean();
	}

	@Override
	public void activate() {
		action.run();
	}

	static List<DebugRow> all(Minecraft minecraft) {
		List<DebugRow> actions = new ArrayList<>();

		actions.add(of(minecraft, "reload_chunks", minecraft.options.keyDebugReloadChunk,
				() -> minecraft.level != null,
				() -> minecraft.levelExtractor.allChanged()));

		actions.add(of(minecraft, "reload_resource_packs", minecraft.options.keyDebugReloadResourcePacks,
				() -> true,
				minecraft::reloadResourcePacks));

		actions.add(of(minecraft, "clear_chat", minecraft.options.keyDebugClearChat,
				() -> true,
				() -> minecraft.gui.hud.getChat().clearMessages(false)));

		// Vanilla hides the coordinates behind the same check; a server may be withholding them.
		actions.add(of(minecraft, "copy_location", minecraft.options.keyDebugCopyLocation,
				() -> minecraft.player != null && !minecraft.player.isReducedDebugInfo(),
				() -> copyLocation(minecraft)));

		actions.add(of(minecraft, "dump_version", minecraft.options.keyDebugDumpVersion,
				() -> true,
				() -> VersionCommand.dumpVersion(minecraft::showDebugChat)));

		actions.add(of(minecraft, "dump_dynamic_textures", minecraft.options.keyDebugDumpDynamicTextures,
				() -> true,
				() -> minecraft.getTextureManager()
						.dumpAllSheets(TextureUtil.getDebugTexturePath(minecraft.gameDirectory.toPath()))));

		actions.add(of(minecraft, "profiling", minecraft.options.keyDebugProfiling,
				() -> true,
				() -> minecraft.debugClientMetricsStart(minecraft::showDebugChat)));

		actions.add(of(minecraft, "debug_options", minecraft.options.keyDebugDebugOptions,
				() -> true,
				() -> minecraft.setScreenAndShow(new DebugOptionsScreen())));

		actions.add(of(minecraft, "cycle_spectator", minecraft.options.keyDebugSpectate,
				() -> mayChangeGameMode(minecraft),
				() -> cycleSpectator(minecraft)));

		return actions;
	}

	private static DebugAction of(Minecraft minecraft, String key, KeyMapping hotkey, BooleanSupplier available, Runnable action) {
		return new DebugAction(key, DebugRow.labelFor(key, hotkey), hotkey, available, action);
	}

	/** The same permission the game itself checks before letting F3 + N through. */
	private static boolean mayChangeGameMode(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;

		return player != null && GameModeCommand.PERMISSION_CHECK.check(player.permissions());
	}

	private static void copyLocation(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;

		minecraft.keyboardHandler.setClipboard(String.format(Locale.ROOT,
				"/execute in %s run tp @s %.2f %.2f %.2f %.2f %.2f",
				player.level().dimension().identifier(),
				player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
	}

	/** Spectator toggles back to whatever mode the player was in before, like F3 + N does. */
	private static void cycleSpectator(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		GameType target = player.isSpectator()
				? firstNonNull(minecraft.gameMode.getPreviousPlayerMode(), GameType.CREATIVE)
				: GameType.SPECTATOR;

		minecraft.getConnection().send(new ServerboundChangeGameModePacket(target));
	}

	private static GameType firstNonNull(GameType value, GameType fallback) {
		return value != null ? value : fallback;
	}
}
