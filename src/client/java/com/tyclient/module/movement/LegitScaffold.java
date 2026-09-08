package com.tyclient.module.movement;

import com.tyclient.module.Category;
import com.tyclient.module.Module;
import com.tyclient.module.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public class LegitScaffold extends Module {
    private final FloatSetting holdMin = new FloatSetting("Hold Min", 80f, 260f, 130f, 5f);
    private final FloatSetting holdMax = new FloatSetting("Hold Max", 80f, 260f, 160f, 5f);
    private final FloatSetting edgeDist = new FloatSetting("Edge Dist", 0.05f, 0.60f, 0.25f, 0.05f);

    private double prevX;
    private double prevZ;
    private boolean sneaking;
    private long sneakingUntil;
    private long cooldownUntil;
    private int firedBlockX = Integer.MIN_VALUE;
    private int firedBlockZ = Integer.MIN_VALUE;

    public LegitScaffold() {
        super("Legit Scaffold", Category.MOVEMENT);
        addSetting(holdMin);
        addSetting(holdMax);
        addSetting(edgeDist);
    }

    @Override
    protected void onEnable() {
        sneaking = false;
        cooldownUntil = 0L;
        firedBlockX = Integer.MIN_VALUE;
        firedBlockZ = Integer.MIN_VALUE;
        prevX = Double.NaN;
        prevZ = Double.NaN;
    }

    @Override
    protected void onDisable() {
        sneaking = false;
        Minecraft.getInstance().options.keyShift.setDown(false);
    }

    public void onFrame(Minecraft mc) {
        if (!isEnabled()) return;
        if (mc.player == null || mc.level == null) return;

        long now = System.currentTimeMillis();

        if (sneaking) {
            if (now >= sneakingUntil) {
                sneaking = false;
                mc.options.keyShift.setDown(false);
                cooldownUntil = now + 150L;
            }
            return;
        }

        if (now < cooldownUntil) return;
        if (mc.gui.screen() != null) return;
        if (mc.options.keyShift.isDown()) return;
        if (!mc.player.onGround()) return;
        if (mc.player.isInWater()) return;
        if (mc.player.isFallFlying()) return;
        if (mc.player.getAbilities().flying) return;

        double x = mc.player.getX();
        double z = mc.player.getZ();
        if (Double.isNaN(prevX)) {
            prevX = x;
            prevZ = z;
            return;
        }
        double dx = x - prevX;
        double dz = z - prevZ;
        prevX = x;
        prevZ = z;

        if (dx * dx + dz * dz < 1e-6) return;

        boolean movingX = Math.abs(dx) >= Math.abs(dz);
        double coord = movingX ? x : z;
        double vel = movingX ? dx : dz;

        int curBlock = (int) Math.floor(coord);
        double edgeAt = vel > 0 ? curBlock + 1 : curBlock;
        double remaining = Math.abs(edgeAt - coord);
        if (remaining > edgeDist.getValue()) return;

        double beyond = coord + (vel > 0 ? 0.55 : -0.55);
        int stepX = (int) Math.floor(movingX ? beyond : x);
        int stepZ = (int) Math.floor(movingX ? z : beyond);
        int footY = (int) Math.floor(mc.player.getY());

        if (stepX == firedBlockX && stepZ == firedBlockZ) return;

        BlockPos ahead = new BlockPos(stepX, footY, stepZ);
        if (!mc.level.getBlockState(ahead).isAir()) return;
        BlockPos below = new BlockPos(stepX, footY - 1, stepZ);
        if (!mc.level.getBlockState(below).isAir()) return;

        firedBlockX = stepX;
        firedBlockZ = stepZ;
        sneaking = true;
        sneakingUntil = now + holdDurationMs();
        mc.options.keyShift.setDown(true);
    }

    private long holdDurationMs() {
        float min = Math.min(holdMin.getValue(), holdMax.getValue());
        float max = Math.max(holdMin.getValue(), holdMax.getValue());
        return Math.round(min + (float) Math.random() * (max - min));
    }
}