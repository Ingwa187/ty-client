package com.tyclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tyclient.module.ModuleManager;
import com.tyclient.module.blatant.Scaffold;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void tyclient$showScaffoldAim(Avatar entity, AvatarRenderState renderState, float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (entity != mc.player) return;

        Scaffold scaffold = ModuleManager.getInstance().getScaffold();
        if (scaffold == null) return;
        if (!scaffold.applyVisualAim(mc.player.getYRot(), mc.player.getXRot())) return;

        renderState.yRot = 0f;
        renderState.xRot = scaffold.getVisualPitch();
    }

    @Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V", at = @At("RETURN"))
    private void tyclient$rotateBodyToAim(AvatarRenderState renderState, PoseStack poseStack, float yBodyRot, float ageInTicks, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Scaffold scaffold = ModuleManager.getInstance().getScaffold();
        if (scaffold == null) return;
        if (!scaffold.isOverrideActive()) return;

        float delta = angleDelta(mc.player.getYRot(), scaffold.getVisualYaw());
        poseStack.mulPose(Axis.YP.rotationDegrees(-delta));
    }

    private static float angleDelta(float from, float to) {
        float delta = (to - from) % 360f;
        if (delta > 180f) {
            delta -= 360f;
        }
        if (delta < -180f) {
            delta += 360f;
        }
        return delta;
    }
}