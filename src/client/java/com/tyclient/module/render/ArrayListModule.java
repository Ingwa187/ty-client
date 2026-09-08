package com.tyclient.module.render;

import com.tyclient.module.Category;
import com.tyclient.module.Module;
import com.tyclient.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.stream.Collectors;

public class ArrayListModule extends Module {
    private static final int BG = 0xA81D1E21;
    private static final int ACCENT = 0xFFFF8A26;
    private static final int TEXT_COLOR = 0xFFEAE7E2;

    public ArrayListModule() {
        super("ArrayList", Category.RENDER);
        enable();
    }

    public void render(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        Font font = mc.font;
        List<Module> enabled = ModuleManager.getInstance().getModules().stream()
                .filter(m -> m != this && m.isEnabled())
                .collect(Collectors.toList());

        int lineHeight = 10;
        int y = 2;
        int x1 = screenWidth - 4;
        for (Module module : enabled) {
            String name = module.getName();
            int textW = font.width(name);
            int x0 = x1 - textW - 14;
            graphics.fill(x0, y, x1, y + lineHeight, BG);
            graphics.fill(x0, y, x0 + 2, y + lineHeight, ACCENT);
            graphics.text(font, Component.literal(name), x0 + 7, y + 1, TEXT_COLOR, true);
            y += lineHeight + 2;
        }
    }
}