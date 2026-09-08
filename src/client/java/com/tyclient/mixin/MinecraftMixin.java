package com.tyclient.mixin;

import com.tyclient.module.Module;
import com.tyclient.module.ModuleManager;
import com.tyclient.module.combat.RightClicker;
import com.tyclient.module.combat.TriggerBot;
import com.tyclient.module.movement.LegitScaffold;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.IdentityHashMap;
import java.util.Map;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    private static final Map<Module, Boolean> lastKeyStates = new IdentityHashMap<>();
    private static long lastKeybindTick = -1;

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void tyclient$handleModules(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        TriggerBot triggerBot = ModuleManager.getInstance().getTriggerBot();
        if (triggerBot != null && triggerBot.isEnabled()) {
            triggerBot.onFrame(mc);
        }

        LegitScaffold scaffold = ModuleManager.getInstance().getLegitScaffold();
        if (scaffold != null && scaffold.isEnabled()) {
            scaffold.onFrame(mc);
        }

        RightClicker rightClicker = ModuleManager.getInstance().getRightClicker();
        if (rightClicker != null && rightClicker.isEnabled()) {
            rightClicker.onFrame(mc);
        }

        long tick = mc.level.getGameTime();
        if (tick == lastKeybindTick) return;
        lastKeybindTick = tick;

        com.mojang.blaze3d.platform.Window window = mc.getWindow();
        for (Module module : ModuleManager.getInstance().getModules()) {
            int code = module.getKeyCode();
            if (code < 0) {
                lastKeyStates.remove(module);
                continue;
            }
            boolean down = com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, code);
            boolean wasDown = lastKeyStates.getOrDefault(module, false);
            if (down && !wasDown) {
                module.toggle();
            }
            lastKeyStates.put(module, down);
        }
    }
}