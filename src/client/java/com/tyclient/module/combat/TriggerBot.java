package com.tyclient.module.combat;

import com.mojang.blaze3d.platform.InputConstants;
import com.tyclient.module.Category;
import com.tyclient.module.Module;
import com.tyclient.module.setting.FloatSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class TriggerBot extends Module {
    private static final InputConstants.Key ATTACK_KEY = InputConstants.Type.MOUSE.getOrCreate(0);
    private static final float JITTER_BLOCKS = 0.2f;
    private static final long MIN_INTERVAL_MS = 40L;

    private final FloatSetting mode = new FloatSetting("Mode", 0f, 1f, 0f, 1f);
    private final FloatSetting reach = new FloatSetting("Reach", 1.5f, 4.5f, 3.0f, 0.1f);
    private final FloatSetting minCps = new FloatSetting("Min CPS", 8f, 20f, 12f, 1f);
    private final FloatSetting maxCps = new FloatSetting("Max CPS", 8f, 20f, 15f, 1f);

    private boolean clicking;
    private long releaseTime;
    private long nextClickTime;
    private long nextTimedClickTime;

    public TriggerBot() {
        super("TriggerBot", Category.COMBAT);
        mode.setLabels(new String[]{"Timed", "Spam"});
        addSetting(mode);
        addSetting(reach);
        addSetting(minCps);
        addSetting(maxCps);
    }

    @Override
    protected void onEnable() {
        clicking = false;
        releaseTime = 0L;
        nextClickTime = 0L;
        nextTimedClickTime = 0L;
    }

    @Override
    protected void onDisable() {
        clicking = false;
        KeyMapping.set(ATTACK_KEY, false);
    }

    public void onFrame(Minecraft mc) {
        if (!isEnabled()) return;
        if (mc.player == null || mc.level == null) return;

        long now = System.currentTimeMillis();

        if (clicking) {
            if (now >= releaseTime) {
                clicking = false;
                KeyMapping.set(ATTACK_KEY, false);
            }
            return;
        }

        if (mc.gui.screen() != null) return;
        if (mc.mouseHandler.isLeftPressed()) return;
        if (!mc.mouseHandler.isMouseGrabbed()) return;
        if (now < nextClickTime) return;

        Entity target = findTriggerTarget(mc);
        if (target == null) return;

        long interval;
        if (isModeSpam()) {
            if (now < nextClickTime) return;
            interval = clickIntervalMs();
            nextClickTime = now + interval;
        } else {
            if (now < nextTimedClickTime) return;
            if (mc.player.getAttackStrengthScale(0f) < 1.0f) return;
            interval = timedCooldownMs(mc);
            nextTimedClickTime = now + interval;
        }

        releaseTime = now + Math.max(10L, interval / 2L);

        KeyMapping.set(ATTACK_KEY, true);
        KeyMapping.click(ATTACK_KEY);
        clicking = true;
    }

    private Entity findTriggerTarget(Minecraft mc) {
        HitResult hitResult = mc.hitResult;
        if (hitResult == null || hitResult.getType() != HitResult.Type.ENTITY) return null;

        Entity target = ((EntityHitResult) hitResult).getEntity();
        if (target == null || !target.isAlive() || target.isRemoved()) return null;

        if (target instanceof Player && target.getUUID().equals(mc.player.getUUID())) return null;
        if (!(target instanceof Player || target instanceof Silverfish || target instanceof IronGolem)) return null;

        double jitteredReach = reach.getValue() + (Math.random() * 2.0 - 1.0) * JITTER_BLOCKS;
        return mc.player.distanceTo(target) <= jitteredReach ? target : null;
    }

    private boolean isModeSpam() {
        return mode.getValue() >= 0.5f;
    }

    private long clickIntervalMs() {
        float min = Math.min(minCps.getValue(), maxCps.getValue());
        float max = Math.max(minCps.getValue(), maxCps.getValue());
        float cps = min + (float) Math.random() * (max - min);
        return Math.max(MIN_INTERVAL_MS, Math.round(1000.0 / cps));
    }

    private long timedCooldownMs(Minecraft mc) {
        float delayTicks = mc.player.getCurrentItemAttackStrengthDelay();
        if (!Float.isFinite(delayTicks) || delayTicks <= 0f) {
            return 500L;
        }
        return Math.round(delayTicks * 50.0);
    }
}