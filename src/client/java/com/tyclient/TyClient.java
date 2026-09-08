package com.tyclient;

import com.tyclient.module.ModuleManager;
import com.tyclient.profile.ProfileManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping.Category;
import com.mojang.blaze3d.platform.InputConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lwjgl.glfw.GLFW;

public class TyClient implements ClientModInitializer {
	public static final String MOD_ID = "tyclient";
	private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static int menuKeyCode = GLFW.GLFW_KEY_RIGHT_SHIFT;
	private static final KeyMapping MENU_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.tyclient.menu",
			InputConstants.Type.KEYSYM,
			menuKeyCode,
			Category.MISC
	));

	public static int getMenuKeyCode() {
		return menuKeyCode;
	}

	public static void setMenuKeyCode(int code) {
		menuKeyCode = code;
		MENU_KEY.setKey(InputConstants.Type.KEYSYM.getOrCreate(code));
	}

	@Override
	public void onInitializeClient() {
		ModuleManager.getInstance();
		ProfileManager.getInstance();
		ClientTickEvents.END_CLIENT_TICK.register(TyClient::handleClientTick);
		LOGGER.info("TY CLIENT loaded.");
	}

	private static void handleClientTick(Minecraft client) {
		while (MENU_KEY.consumeClick()) {
			if (client.gui.screen() instanceof TyClientScreen) {
				client.gui.setScreen(null);
			} else if (client.gui.screen() == null) {
				client.gui.setScreen(new TyClientScreen());
			}
		}
	}
}
