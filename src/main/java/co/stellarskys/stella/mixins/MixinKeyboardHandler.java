package co.stellarskys.stella.mixins;

import co.stellarskys.stella.api.zenith.Zenith;
import co.stellarskys.stella.events.EventBus;
import co.stellarskys.stella.events.core.KeyEvent;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class MixinKeyboardHandler {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void stella$onKey(long handle, int action, net.minecraft.client.input.KeyEvent event, CallbackInfo ci) {
        if (handle == Zenith.getWindowHandle()) {
            if (action == 1) {
                if (EventBus.INSTANCE.post(new KeyEvent.Press(event.key(), event /*? if < 26.3 {*/ .scancode() /*?} else {*/ /*.keycode() *//*?}*/, event.modifiers()))) ci.cancel();
            } else if (action == 0) {
                if (EventBus.INSTANCE.post(new KeyEvent.Release(event.key(), event /*? if < 26.3 {*/ .scancode() /*?} else {*/ /*.keycode() *//*?}*/, event.modifiers()))) ci.cancel();
            }
        }
    }
}