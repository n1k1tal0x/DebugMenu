package dev.n1k1tal0x.debugmenu.client.mixin;

import com.mojang.blaze3d.platform.InputConstants;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.KeyMapping;

/**
 * Exposes the key a mapping is currently bound to.
 *
 * KeyMapping only offers getDefaultKey() and getTranslatedKeyMessage(), and that message is memoized
 * per key by InputConstants.Key, so it keeps whatever the keyboard layout was when the key object
 * was first named. Reading the key itself lets the menu ask GLFW for the name again.
 */
@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
	@Accessor("key")
	InputConstants.Key debugmenu$boundKey();
}
