package com.tyclient.module.combat;

import com.mojang.blaze3d.platform.InputConstants;
import com.tyclient.module.Category;
import com.tyclient.module.Module;
import com.tyclient.module.setting.FloatSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public class AutoClicker extends Module {
    private static final InputConstants.Key ATTACK_KEY = InputConstants.Type.MOUSE.getOrCreate(0);
    private static final InputConstants.Key USE_KEY = InputConstants.Type.MOUSE.getOrCreate(1);

    private final FloatSetting minCps = new FloatSetting("Min CPS", 8f, 20f, 12f, 1f);
    private final FloatSetting maxCps = new FloatSetting("Max CPS", 8f, 20f, 14f, 1f);
    private final FloatSetting rightClick = new FloatSetting("Rightclick", 0f, 1f, 0f, 1f);

    private int phase;
    private long phaseUntil;
    private long nextClickTime;

    public AutoClicker() {
        super("AutoClicker", Category.COMBAT);
        rightClick.setLabels(new String[]{"Off", "On"});
        addSetting(minCps);
        addSetting(maxCps);
        addSetting(rightClick);
    }

    @Override
    protected void onEnable() {
        phase = 0;
        phaseUntil = 0L;
        nextClickTime = 0L;
    }

    @Override
    protected void onDisable() {
        phase = 0;
        releaseKeys();
    }

    public void onFrame(Minecraft mc) {
        if (!isEnabled()) return;
        if (mc.player == null || mc.level == null) return;
        if (mc.gui.screen() != null) return;
        if (!mc.mouseHandler.isMouseGrabbed()) return;

        long now = System.currentTimeMillis();

        if (phase != 0) {
            advancePhase(mc, now);
            return;
        }

        if (!mc.mouseHandler.isLeftPressed()) return;
        if (now < nextClickTime) return;

        startAttack(mc, now);
    }

    private void startAttack(Minecraft mc, long now) {
        long interval = clickIntervalMs();
        long hold = Mth.clamp(interval / 3, 25L, 60L);

        phase = 1;
        phaseUntil = now + hold;
        KeyMapping.set(ATTACK_KEY, true);
        KeyMapping.click(ATTACK_KEY);
    }

    private void advancePhase(Minecraft mc, long now) {
        long interval = clickIntervalMs();

        switch (phase) {
            case 1 -> {
                if (now >= phaseUntil) {
                    releaseAttack();
                    if (isRightClickEnabled()) {
                        phase = 2;
                        phaseUntil = now + randomGapMs();
                    } else {
                        phase = 0;
                        nextClickTime = now + interval;
                    }
                }
            }
            case 2 -> {
                if (now >= phaseUntil) {
                    startUse(mc, now);
                }
            }
            case 3 -> {
                if (now >= phaseUntil) {
                    releaseUse();
                    phase = 0;
                    nextClickTime = now + interval;
                }
            }
        }
    }

    private void startUse(Minecraft mc, long now) {
        long interval = clickIntervalMs();
        long hold = Mth.clamp(interval / 3, 25L, 60L);

        phase = 3;
        phaseUntil = now + hold;
        KeyMapping.set(USE_KEY, true);
        KeyMapping.click(USE_KEY);
    }

    private void releaseAttack() {
        KeyMapping.set(ATTACK_KEY, false);
        KeyMapping.click(ATTACK_KEY);
    }

    private void releaseUse() {
        KeyMapping.set(USE_KEY, false);
        KeyMapping.click(USE_KEY);
    }

    private void releaseKeys() {
        KeyMapping.set(ATTACK_KEY, false);
        KeyMapping.set(USE_KEY, false);
    }

    private boolean isRightClickEnabled() {
        return rightClick.getValue() >= 0.5f;
    }

    private long randomGapMs() {
        return 8L + (long) (Math.random() * 22.0);
    }

    private long clickIntervalMs() {
        float min = Math.min(minCps.getValue(), maxCps.getValue());
        float max = Math.max(minCps.getValue(), maxCps.getValue());
        float cps = min + (float) Math.random() * (max - min);
        return Math.max(30L, Math.round(1000.0 / cps));
    }
}