package com.tyclient.mixin;

import com.tyclient.module.ModuleManager;
import com.tyclient.module.combat.AimAssist;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "turnPlayer", at = @At("RETURN"))
    private void tyclient$applyAimAssist(double tickDelta, CallbackInfo ci) {
        if (this.minecraft.player == null) return;
        if (this.minecraft.level == null) return;

        LocalPlayer player = this.minecraft.player;
        AimAssist aimAssist = ModuleManager.getInstance().getAimAssist();

        if (aimAssist != null && aimAssist.isEnabled()) {
            aimAssist.onTurnPlayer(player, tickDelta);
        }
    }
}
