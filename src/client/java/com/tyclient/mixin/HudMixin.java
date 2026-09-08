package com.tyclient.mixin;

import com.tyclient.module.ModuleManager;
import com.tyclient.module.render.ArrayListModule;
import com.tyclient.module.render.EspModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class HudMixin {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void tyclient$renderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        ArrayListModule arrayList = ModuleManager.getInstance().getArrayList();
        if (arrayList != null && arrayList.isEnabled()) {
            arrayList.render(graphics);
        }
        EspModule esp = ModuleManager.getInstance().getEsp();
        if (esp != null && esp.isEnabled()) {
            esp.render(graphics);
        }
    }
}