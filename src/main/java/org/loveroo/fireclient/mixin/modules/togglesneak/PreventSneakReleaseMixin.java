package org.loveroo.fireclient.mixin.modules.togglesneak;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.loveroo.fireclient.modules.ToggleSneakModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyBinding.class)
public abstract class PreventSneakReleaseMixin {

    @Inject(method = "setPressed", at = @At("HEAD"), cancellable = true)
    private void preventSneakRelease(boolean pressed, CallbackInfo info) {
        if (pressed) {
            return;
        }

        if (!ToggleSneakModule.isToggleSneaking() && !ToggleSneakModule.isWaitingForRelease()) {
            return;
        }

        var client = MinecraftClient.getInstance();
        if (client == null || client.options == null) {
            return;
        }

        if ((Object) this == client.options.sneakKey) {
            info.cancel();
        }
    }
}

