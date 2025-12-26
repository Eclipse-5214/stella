package co.stellarskys.stella.mixins;

import co.stellarskys.stella.features.msc.bars;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/*
 * Adapted from ChatListenerMixin.java in OdinFabric
 * https://github.com/odtheking/OdinFabric
 *
 * BSD 3-Clause License
 * Copyright (c) 2025, odtheking
 * See full license at: https://opensource.org/licenses/BSD-3-Clause
 */
@Mixin(ChatListener.class)
public class MixinChatListener {
    @ModifyArg(method = "handleSystemMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;setOverlayMessage(Lnet/minecraft/network/chat/Component;Z)V"), index = 0)
    private Component modifyOverlayMessage(Component component) {
        return bars.INSTANCE.cleanAB(component);
    }
}
