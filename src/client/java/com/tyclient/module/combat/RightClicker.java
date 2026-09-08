package com.tyclient.module.combat;

import com.mojang.blaze3d.platform.InputConstants;
import com.tyclient.module.Category;
import com.tyclient.module.Module;
import com.tyclient.module.setting.FloatSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public class RightClicker extends Module {
    private static final InputConstants.Key USE_KEY = InputConstants.Type.MOUSE.getOrCreate(1);

    private final FloatSetting minCps = new FloatSetting("Min CPS", 6f, 20f, 12f, 1f);
    private final FloatSetting maxCps = new FloatSetting("Max CPS", 6f, 20f, 13f, 1f);

    private boolean simRightDown;
    private long simUntil;
    private long nextClickTime;

    public RightClicker() {
        super("RightClicker", Category.COMBAT);
        addSetting(minCps);
        addSetting(maxCps);
    }

    @Override
    protected void onEnable() {
        simRightDown = false;
        simUntil = 0L;
        nextClickTime = 0L;
    }

    @Override
    protected void onDisable() {
        simRightDown = false;
        simUntil = 0L;
        KeyMapping.set(USE_KEY, false);
        KeyMapping.click(USE_KEY);
    }

    public void onFrame(Minecraft mc) {
        if (!isEnabled()) return;
        if (mc.player == null || mc.level == null) return;
        if (mc.gui.screen() != null) return;
        if (!mc.mouseHandler.isMouseGrabbed()) return;

        long now = System.currentTimeMillis();

        if (simRightDown) {
            if (now >= simUntil) {
                simRightDown = false;
                KeyMapping.set(USE_KEY, false);
                KeyMapping.click(USE_KEY);
            }
            return;
        }

        if (!mc.mouseHandler.isRightPressed()) return;
        if (now < nextClickTime) return;

        long interval = clickIntervalMs();
        nextClickTime = now + interval;
        simUntil = now + Math.max(8L, interval / 2L);

        simRightDown = true;
        KeyMapping.set(USE_KEY, true);
        KeyMapping.click(USE_KEY);
    }

    private long clickIntervalMs() {
        float min = Math.min(minCps.getValue(), maxCps.getValue());
        float max = Math.max(minCps.getValue(), maxCps.getValue());
        float cps = min + (float) Math.random() * (max - min);
        return Math.round(1000.0 / cps);
    }
}