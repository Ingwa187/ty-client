package com.tyclient.module.blatant;

import com.tyclient.module.Category;
import com.tyclient.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class Scaffold extends Module {
    private static final float SMOOTH_LAMBDA = 0.045f;
    private static final double MAX_REACH = 4.5;

    private boolean active;
    private boolean overrideActive;
    private float currentYaw;
    private float currentPitch;
    private long lastSmoothNanos;

    public Scaffold() {
        super("Scaffold", Category.BLATANT);
    }

    @Override
    protected void onEnable() {
        active = false;
        resetSmoothing();
    }

    @Override
    protected void onDisable() {
        resetSmoothing();
    }

    public boolean applyVisualAim(float realYaw, float realPitch) {
        long now = System.nanoTime();
        float dt = lastSmoothNanos == 0L ? 0.016f : Math.min(0.1f, (now - lastSmoothNanos) / 1_000_000_000f);
        lastSmoothNanos = now;

        boolean result;
        if (!isEnabled()) {
            if (!active) {
                result = false;
            } else {
                easeTo(realYaw, realPitch, dt);
                if (Math.abs(angleDelta(currentYaw, realYaw)) < 0.5f && Math.abs(currentPitch - realPitch) < 0.5f) {
                    active = false;
                    resetSmoothing();
                }
                result = active;
            }
        } else {
            if (!active) {
                currentYaw = realYaw;
                currentPitch = realPitch;
            }
            active = true;
            easeTo(backwardsYaw(realYaw), realPitch, dt);
            result = true;
        }
        overrideActive = result;
        return result;
    }

    public boolean isOverrideActive() {
        return overrideActive;
    }

    public float getVisualYaw() {
        return currentYaw;
    }

    public float getVisualPitch() {
        return currentPitch;
    }

    private static float backwardsYaw(float realYaw) {
        float wrapped = (realYaw + 180f) % 360f;
        return wrapped < -180f ? wrapped + 360f : wrapped;
    }

    private void easeTo(float targetYaw, float targetPitch, float dt) {
        float t = 1f - (float) Math.exp(-dt / SMOOTH_LAMBDA);
        currentYaw += angleDelta(currentYaw, targetYaw) * t;
        currentPitch += (targetPitch - currentPitch) * t;
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

    private void resetSmoothing() {
        currentYaw = 0f;
        currentPitch = 0f;
        lastSmoothNanos = 0L;
    }

    public void onFrame(Minecraft mc) {
        if (!isEnabled()) return;
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (mc.gui.screen() != null) return;

        mc.options.keySprint.setDown(false);
        mc.player.setSprinting(false);

        scanForBlock(mc);

        BlockPos feet = mc.player.blockPosition();
        placeTarget(mc, new BlockPos(feet.getX(), feet.getY() - 1, feet.getZ()));
    }

    private void scanForBlock(Minecraft mc) {
        if (mc.player.getMainHandItem().getItem() instanceof BlockItem) {
            return;
        }
        Inventory inventory = mc.player.getInventory();
        NonNullList<ItemStack> items = inventory.getNonEquipmentItems();
        for (int i = 0; i < 9; i++) {
            if (items.get(i).getItem() instanceof BlockItem) {
                inventory.setSelectedSlot(i);
                return;
            }
        }
    }

    private void placeTarget(Minecraft mc, BlockPos target) {
        BlockState state = mc.level.getBlockState(target);
        if (!state.isAir() && !state.canBeReplaced()) {
            return;
        }

        Vec3 eye = mc.player.getEyePosition();
        double reachSq = MAX_REACH * MAX_REACH;

        for (Direction direction : Direction.values()) {
            BlockPos support = target.relative(direction);
            if (mc.level.getBlockState(support).isAir()) {
                continue;
            }

            Direction face = direction.getOpposite();
            Vec3 center = Vec3.atCenterOf(support);
            Vec3 hitPos = center.add(
                    face.getStepX() * 0.5,
                    face.getStepY() * 0.5,
                    face.getStepZ() * 0.5);
            if (eye.distanceToSqr(hitPos) > reachSq) {
                continue;
            }
            BlockHitResult hit = new BlockHitResult(hitPos, face, support, false);

            InteractionResult result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
            if (result.consumesAction()) {
                mc.player.swing(InteractionHand.MAIN_HAND);
                return;
            }
        }
    }
}