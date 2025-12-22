package co.stellarskys.stella.mixins;

import co.stellarskys.stella.events.EventBus;
import co.stellarskys.stella.events.core.GuiEvent;
import co.stellarskys.stella.events.core.KeyEvent;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.deftu.omnicore.api.client.OmniClient;

@Mixin(KeyboardHandler.class)
public class MixinKeyboardHandler {
    @Inject(
            method = "keyPress",
            at = @At("HEAD"),
            cancellable = true
    )
    //#if MC >= 1.21.9
    //$$ private void stella$onKey(long window, int action, net.minecraft.client.input.KeyEvent input, CallbackInfo ci) {
    //#else
    private void stella$onKey(
            long window,
            int key,
            int scancode,
            int action,
            int modifiers,
            CallbackInfo ci
    ) {
        //#endif
        if (window == OmniClient.getWindowHandle()) {
            if (action == 1) {
                //#if MC >= 1.21.9
                //$$ if (EventBus.INSTANCE.post(new KeyEvent.Press(input.key(), input.scancode(), input.modifiers()))) ci.cancel();
                //#else
                if (EventBus.INSTANCE.post(new KeyEvent.Press(key, scancode, modifiers))) ci.cancel();
                //#endif
            } else if (action == 0) {
                //#if MC >= 1.21.9
                //$$ if (EventBus.INSTANCE.post(new KeyEvent.Release(input.key(), input.scancode(), input.modifiers()))) ci.cancel();
                //#else
                if (EventBus.INSTANCE.post(new KeyEvent.Release(key, scancode, modifiers))) ci.cancel();
                //#endif
            }
        }
    }

    @Inject(
            method = "charTyped",
            at = @At("HEAD"),
            cancellable = true
    )
    //#if MC >= 1.21.9
    //$$ private void stella$onChar(long window, net.minecraft.client.input.CharacterEvent characterEvent, CallbackInfo ci) {
    //#else
    private void stella$onChar(
            long window,
            int codePoint,
            int modifiers,
            CallbackInfo ci
    ) {
        //#endif
        Screen screen = OmniClient.get().screen;
        if (screen == null) return;

        //#if MC >= 1.21.9
        //$$ char charTyped = (char) characterEvent.codepoint();
        //#else
        char charTyped = (char) codePoint;
        //#endif
        boolean cancelled = EventBus.INSTANCE.post(new GuiEvent.Key(null, GLFW.GLFW_KEY_UNKNOWN, charTyped, 0, screen));
        if (cancelled) ci.cancel();
    }
}