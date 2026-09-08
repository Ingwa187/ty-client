package com.tyclient.module.blatant;

import com.tyclient.module.Category;
import com.tyclient.module.Module;
import com.tyclient.module.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

public class Speed extends Module {
    private final FloatSetting speed = new FloatSetting("Speed", 1f, 10f, 4f, 0.1f);

    public Speed() {
        super("Speed", Category.BLATANT);
        addSetting(speed);
    }

    public void onFrame(Minecraft mc) {
        if (!isEnabled()) return;
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        net.minecraft.world.phys.Vec2 move = player.input.getMoveVector();
        float forward = move.y;
        float strafe = move.x;

        double inputLength = Math.hypot(forward, strafe);
        if (inputLength < 1e-4) {
            return;
        }

        if (player.onGround()) {
            player.jumpFromGround();
        }

        float yaw = player.getYRot() * (float) (Math.PI / 180.0);
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double forwardUnit = forward / inputLength;
        double strafeUnit = strafe / inputLength;

        double target = player.getAttributeValue(Attributes.MOVEMENT_SPEED) * speed.getValue();
        double dx = (forwardUnit * -sin + strafeUnit * cos) * target;
        double dz = (forwardUnit * cos + strafeUnit * sin) * target;

        Vec3 delta = player.getDeltaMovement();
        player.setDeltaMovement(dx, delta.y, dz);
    }
}