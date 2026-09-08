package com.tyclient.module.combat;

import com.tyclient.module.Category;
import com.tyclient.module.Module;
import com.tyclient.module.setting.FloatSetting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

import java.util.List;

public class AimAssist extends Module {
    private static final float ERROR_EPS = 0.08f;
    private static final float PROPORTIONAL_PER_TICK = 0.85f;
    private static final float PITCH_MIN = -90.0f;
    private static final float PITCH_MAX = 90.0f;

    private final FloatSetting smoothness = new FloatSetting("Smoothness", 1f, 100f, 75f, 1f);
    private final FloatSetting speed = new FloatSetting("Speed", 1f, 100f, 65f, 1f);
    private final FloatSetting range = new FloatSetting("Range", 2f, 8f, 4.5f, 0.5f);

    private Entity currentTarget;
    private int tickCounter;
    private float yawSmooth;
    private float pitchSmooth;

    public AimAssist() {
        super("AimAssist", Category.COMBAT);
        addSetting(smoothness);
        addSetting(speed);
        addSetting(range);
    }

    @Override
    protected void onEnable() {
        currentTarget = null;
        tickCounter = 0;
        yawSmooth = 0f;
        pitchSmooth = 0f;
    }

    @Override
    protected void onDisable() {
        currentTarget = null;
        yawSmooth = 0f;
        pitchSmooth = 0f;
    }

    public void onTurnPlayer(LocalPlayer player, double tickDelta) {
        if (!isEnabled()) return;
        if (player == null || player.level() == null) return;
        if (player.isDeadOrDying()) return;

        tickCounter++;
        float range = this.range.getValue();
        if (tickCounter % 4 == 0 || currentTarget == null || !isValidTarget(player, currentTarget, range)) {
            currentTarget = findBestTarget(player, range);
        }

        if (currentTarget == null) {
            yawSmooth *= 0.6f;
            pitchSmooth *= 0.6f;
            return;
        }

        applySmoothAim(player, currentTarget, tickDelta);
    }

    private void applySmoothAim(LocalPlayer player, Entity target, double tickDelta) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 targetPos = aimPointFor(target);

        double dx = targetPos.x - eyePos.x;
        double dy = targetPos.y - eyePos.y;
        double dz = targetPos.z - eyePos.z;

        double desiredYaw = -Math.toDegrees(Math.atan2(dx, dz));
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        double desiredPitch = -Math.toDegrees(Math.atan2(dy, horizontalDist));

        float currentYaw = player.getYRot();
        float currentPitch = player.getXRot();

        float yawError = (float) Mth.wrapDegrees(desiredYaw - currentYaw);
        float pitchError = (float) Mth.wrapDegrees(desiredPitch - currentPitch);

        float smooth = smoothness.getValue() / 100f;
        float spd = speed.getValue() / 100f;

        double frameTicks = Mth.clamp(tickDelta * 20.0, 0.05, 4.0);

        float proportional = 1f - (float) Math.pow(1f - PROPORTIONAL_PER_TICK, frameTicks);
        float combinedError = (float) Math.sqrt(yawError * yawError + pitchError * pitchError);
        float errorFactor = Mth.clamp(1.0f + combinedError / 30.0f, 1.0f, 5.0f);
        float maxStep = (maxStepPerSecond(spd) / 20f) * (float) frameTicks * errorFactor;
        float alpha = 1f - (float) Math.pow(1f - emaAlphaPerTick(smooth), frameTicks);

        boolean inDeadzone = Math.abs(yawError) < ERROR_EPS && Math.abs(pitchError) < ERROR_EPS;

        if (!inDeadzone) {
            float yawStep = Mth.clamp(yawError * proportional, -maxStep, maxStep);
            float pitchStep = Mth.clamp(pitchError * proportional, -maxStep, maxStep);

            yawSmooth += (yawStep - yawSmooth) * alpha;
            pitchSmooth += (pitchStep - pitchSmooth) * alpha;
        } else {
            yawSmooth *= 0.5f;
            pitchSmooth *= 0.5f;
        }

        if (Math.abs(yawSmooth) < 0.004f && Math.abs(pitchSmooth) < 0.004f) return;

        pitchSmooth = Mth.clamp(currentPitch + pitchSmooth, PITCH_MIN, PITCH_MAX) - currentPitch;

        player.turn(yawSmooth / 0.15f, pitchSmooth / 0.15f);
    }

    private float maxStepPerSecond(float spd) {
        return 20f + spd * 160f;
    }

    private float emaAlphaPerTick(float smooth) {
        return 1f - smooth * 0.6f;
    }

    private Entity findBestTarget(LocalPlayer player, float range) {
        Vec3 eyePos = player.getEyePosition();
        ClientLevel level = (ClientLevel) player.level();

        AABB searchBox = new AABB(
                eyePos.x - range, eyePos.y - range, eyePos.z - range,
                eyePos.x + range, eyePos.y + range, eyePos.z + range
        );

        List<Entity> entities = level.getEntities(player, searchBox, e -> {
            if (!e.isAlive()) return false;
            if (e instanceof Player || e instanceof Silverfish || e instanceof IronGolem) {
                if (e instanceof Player && e.getUUID().equals(player.getUUID())) return false;
                return hasLineOfSight(player, e);
            }
            return false;
        });

        Entity best = null;
        double bestDist = Double.MAX_VALUE;

        for (Entity entity : entities) {
            double dist = aimPointFor(entity).distanceTo(eyePos);
            if (dist < bestDist) {
                bestDist = dist;
                best = entity;
            }
        }

        return best;
    }

    private Vec3 aimPointFor(Entity target) {
        AABB box = target.getBoundingBox();
        return new Vec3(
                target.getX(),
                box.minY + (box.maxY - box.minY) * 0.5,
                target.getZ()
        );
    }

    private boolean hasLineOfSight(LocalPlayer player, Entity target) {
        Vec3 from = player.getEyePosition();
        Vec3 to = aimPointFor(target);

        ClipContext context = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        BlockHitResult result = player.level().clip(context);

        if (result.getType() == HitResult.Type.MISS) return true;

        double hitDist = result.getLocation().distanceTo(from);
        double targetDist = from.distanceTo(to);

        return hitDist >= targetDist;
    }

    private boolean isValidTarget(LocalPlayer player, Entity target, float range) {
        if (target == null || !target.isAlive()) return false;
        if (target.isRemoved()) return false;
        double dist = aimPointFor(target).distanceTo(player.getEyePosition());
        if (dist > range) return false;
        if (target instanceof Player && target.getUUID().equals(player.getUUID())) return false;
        return hasLineOfSight(player, target);
    }
}