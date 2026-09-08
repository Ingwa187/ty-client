package com.tyclient;

import com.tyclient.module.Category;
import com.tyclient.module.Module;
import com.tyclient.module.ModuleManager;
import com.tyclient.module.setting.FloatSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

public class TyClientScreen extends Screen {
	private static final int BACKDROP = 0x66000000;
	private static final int PANEL = 0xB82A2B2E;
	private static final int PANEL_DARK = 0xA81D1E21;
	private static final int HOVER = 0xA8444548;
	private static final int BORDER = 0xB86A6B6E;
	private static final int ORANGE = 0xFFFF8A26;
	private static final int ORANGE_DARK = 0xB85E3218;
	private static final int TEXT = 0xFFEAE7E2;
	private static final int MUTED = 0xFFAAA7A2;
	private static final int GREEN = 0xFF4CAF50;
	private static final String[] CATEGORIES = {"Overview", "Combat", "Blatant", "Movement", "Render", "Settings", "Profiles"};

	private static final int CATEGORY_COMBAT = 1;
	private static final int CATEGORY_BLATANT = 2;
	private static final int CATEGORY_MOVEMENT = 3;
	private static final int CATEGORY_RENDER = 4;
	private static final int MODULE_ROW_HEIGHT = 24;
	private static final int MODULE_ROW_GAP = 8;
	private static final int SETTING_ROW_HEIGHT = 30;

	private int selectedCategory;
	private Module selectedModule;
	private FloatSetting draggingSetting;
	private int categoryScroll;
	private Module bindingModule;

	public TyClientScreen() {
		super(Component.literal("TY CLIENT"));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.fill(0, 0, width, height, BACKDROP);

		int menuWidth = Math.min(560, width - 36);
		int menuHeight = Math.min(300, height - 36);
		int left = (width - menuWidth) / 2;
		int top = (height - menuHeight) / 2;
		int sidebarWidth = 128;
		int contentLeft = left + sidebarWidth;

		graphics.fill(left, top, left + menuWidth, top + menuHeight, PANEL);
		graphics.outline(left, top, menuWidth, menuHeight, BORDER);
		graphics.fill(left, top, left + menuWidth, top + 38, PANEL_DARK);
		graphics.fill(left, top + 37, left + menuWidth, top + 38, ORANGE);
		graphics.text(font, Component.literal("TY CLIENT"), left + 14, top + 12, TEXT, true);
		graphics.text(font, Component.literal("RIGHT SHIFT"), left + menuWidth - 78, top + 14, MUTED);

		graphics.fill(left, top + 38, contentLeft, top + menuHeight, PANEL_DARK);
		graphics.fill(contentLeft - 1, top + 38, contentLeft, top + menuHeight, BORDER);
		for (int index = 0; index < CATEGORIES.length; index++) {
			int rowTop = top + 58 + index * 34;
			boolean selected = selectedCategory == index;
			boolean hovered = inside(mouseX, mouseY, left + 8, rowTop - 4, sidebarWidth - 16, 28);
			if (selected || hovered) {
				graphics.fill(left + 8, rowTop - 4, contentLeft - 8, rowTop + 24,
						selected ? ORANGE_DARK : HOVER);
			}
			if (selected) {
				graphics.fill(left + 8, rowTop - 4, left + 11, rowTop + 24, ORANGE);
			}
			graphics.text(font, Component.literal(CATEGORIES[index]), left + 20, rowTop + 5,
					selected ? TEXT : MUTED);
		}

		int contentTop = top + 66;
		graphics.text(font, Component.literal(CATEGORIES[selectedCategory]), contentLeft + 22, contentTop,
				TEXT, true);
		graphics.fill(contentLeft + 22, contentTop + 22, left + menuWidth - 22, contentTop + 23, BORDER);

		int modulesInstalled = ModuleManager.getInstance().getModules().size();
		String modulesLabel = modulesInstalled == 1 ? "1 module" : modulesInstalled + " modules";
		graphics.fill(contentLeft + 22, top + menuHeight - 50, contentLeft + 82, top + menuHeight - 28, modulesInstalled > 0 ? ORANGE_DARK : HOVER);
		graphics.text(font, Component.literal(modulesInstalled > 0 ? "LOADED" : "EMPTY"), contentLeft + 31, top + menuHeight - 43, TEXT, true);
		graphics.text(font, Component.literal(modulesLabel), contentLeft + 94, top + menuHeight - 43, MUTED);

		if (isCategoryTab(selectedCategory)) {
			renderCategoryTab(graphics, mouseX, mouseY, left, top, menuWidth, menuHeight, contentLeft, contentTop);
		} else if (selectedCategory == 0) {
			renderOverviewTab(graphics, left, top, menuWidth, contentLeft, contentTop);
		}
	}

	private void renderOverviewTab(GuiGraphicsExtractor graphics, int left, int top, int menuWidth, int contentLeft, int contentTop) {
		int row = contentTop + 36;
		graphics.text(font, Component.literal("Modules: " + ModuleManager.getInstance().getModules().size()), contentLeft + 22, row, TEXT);
		graphics.text(font, Component.literal("Select the Combat tab to tweak modules."), contentLeft + 22, row + 21, MUTED);
	}

	private void renderCategoryTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int left, int top, int menuWidth, int menuHeight, int contentLeft, int contentTop) {
		Category category = categoryForIndex(selectedCategory);
		List<Module> modules = ModuleManager.getInstance().getModulesByCategory(category);
		if (selectedModule == null || selectedModule.getCategory() != category) {
			selectedModule = modules.isEmpty() ? null : modules.get(0);
		}

		int rowX = contentLeft + 22;
		int rowWidth = (left + menuWidth - 22) - rowX;
		int pillX0 = rowX + rowWidth - 44;
		int pillX1 = rowX + rowWidth;

		int viewportTop = contentTop + 36;
		int viewportBottom = top + menuHeight - 58;

		int rowsContent = modules.isEmpty() ? 0 : modules.size() * (MODULE_ROW_HEIGHT + MODULE_ROW_GAP) - MODULE_ROW_GAP;
		List<FloatSetting> settings = selectedModule == null ? List.of() : floatSettingsOf(selectedModule);
		boolean hasSettings = selectedModule != null && !selectedModule.getSettings().isEmpty();
		int settingsTop = viewportTop + rowsContent + 8;
		int contentBottom = hasSettings ? settingsTop + 24 + settings.size() * SETTING_ROW_HEIGHT : settingsTop + 16;
		int maxScroll = Math.max(0, contentBottom - viewportBottom);
		categoryScroll = Mth.clamp(categoryScroll, 0, maxScroll);

		graphics.enableScissor(contentLeft, viewportTop, left + menuWidth, viewportBottom);

		int rowTop = viewportTop - categoryScroll;

		int bindBtnW = 44;
		int bindBtnX0 = pillX0 - bindBtnW - 4;

		for (Module module : modules) {
			boolean enabled = module.isEnabled();
			boolean selected = module == selectedModule;
			boolean hoveringBind = bindingModule == module || inside(mouseX, mouseY, bindBtnX0, rowTop - 2, bindBtnW, MODULE_ROW_HEIGHT);
			boolean hovered = inside(mouseX, mouseY, rowX, rowTop - 2, rowWidth, MODULE_ROW_HEIGHT);
			boolean pillHovered = inside(mouseX, mouseY, pillX0, rowTop - 2, pillX1 - pillX0, MODULE_ROW_HEIGHT);

			if (selected) {
				graphics.fill(rowX, rowTop - 2, rowX + 4, rowTop + MODULE_ROW_HEIGHT - 1, ORANGE);
			}
			if (hovered && !pillHovered && !hoveringBind) {
				graphics.fill(rowX + 4, rowTop - 2, pillX0, rowTop + MODULE_ROW_HEIGHT - 1, HOVER);
			} else if (selected) {
				graphics.fill(rowX + 4, rowTop - 2, pillX0, rowTop + MODULE_ROW_HEIGHT - 1, ORANGE_DARK);
			} else {
				graphics.fill(rowX + 4, rowTop - 2, pillX0, rowTop + MODULE_ROW_HEIGHT - 1, PANEL_DARK);
			}
			graphics.fill(rowX, rowTop + MODULE_ROW_HEIGHT - 1, pillX1, rowTop + MODULE_ROW_HEIGHT, BORDER);
			graphics.text(font, Component.literal(module.getName()), rowX + 14, rowTop + 4, TEXT);

			graphics.fill(bindBtnX0 + 1, rowTop - 1, pillX0 - 1, rowTop + MODULE_ROW_HEIGHT - 3,
					bindingModule == module ? ORANGE_DARK : (hoveringBind ? HOVER : PANEL_DARK));
			String bindText = bindingModule == module ? "..." : keyName(module.getKeyCode());
			int bindTextW = font.width(bindText);
			graphics.text(font, Component.literal(bindText), bindBtnX0 + (bindBtnW - bindTextW) / 2, rowTop + 4,
					bindingModule == module ? TEXT : MUTED, true);

			int pillColor = enabled ? (pillHovered ? BORDER : ORANGE_DARK) : (pillHovered ? HOVER : PANEL_DARK);
			graphics.fill(pillX0 + 1, rowTop - 1, pillX1 - 1, rowTop + MODULE_ROW_HEIGHT - 3, pillColor);
			graphics.text(font, Component.literal(enabled ? "ON" : "OFF"), pillX0 + 10, rowTop + 4,
					enabled ? GREEN : MUTED);

			rowTop += MODULE_ROW_HEIGHT + MODULE_ROW_GAP;
		}

		int settingsDrawTop = rowTop + 8;
		if (hasSettings) {
			graphics.text(font, Component.literal("SETTINGS  -  " + selectedModule.getName().toUpperCase()), rowX,
					settingsDrawTop, ORANGE, true);
			graphics.fill(rowX, settingsDrawTop + 14, pillX1, settingsDrawTop + 15, BORDER);

			int sliderRow = settingsDrawTop + 24;
			for (FloatSetting setting : settings) {
				renderSlider(graphics, mouseX, mouseY, rowX, sliderRow, pillX0 - 70, pillX1, setting);
				sliderRow += SETTING_ROW_HEIGHT;
			}
		} else if (modules.isEmpty()) {
			String tabName = category.getDisplayName().toLowerCase();
			graphics.text(font, Component.literal("No " + tabName + " modules installed"), rowX, settingsDrawTop, TEXT);
			graphics.text(font, Component.literal("Your client is ready for new modules."), rowX, settingsDrawTop + 21, MUTED);
		}

		graphics.disableScissor();

		if (maxScroll > 0) {
			int sbX = left + menuWidth - 9;
			int trackH = viewportBottom - viewportTop;
			int contentH = contentBottom - viewportTop;
			int thumbH = Math.max(20, trackH * trackH / contentH);
			int thumbY = viewportTop + (trackH - thumbH) * categoryScroll / maxScroll;
			graphics.fill(sbX, viewportTop, sbX + 3, viewportBottom, 0xFF101112);
			graphics.fill(sbX, thumbY, sbX + 3, thumbY + thumbH, ORANGE_DARK);
		}
	}

	private void renderSlider(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int rowX, int rowTop, int trackX1, int containerX1, FloatSetting setting) {
		int trackX0 = rowX + 130;
		int trackW = trackX1 - trackX0;
		if (trackW < 60) {
			trackW = 60;
		}
		int trackY = rowTop + 12;
		int trackH = 6;
		int handleHalfY = 9;
		int fillW = (int) (trackW * setting.normalizedValue());

		boolean hovered = inside(mouseX, mouseY, trackX0 - 3, rowTop + 3, trackW + 6, 18);

		graphics.text(font, Component.literal(setting.getName()), rowX, rowTop + 3, MUTED);

		graphics.fill(trackX0, trackY, trackX0 + trackW, trackY + trackH, 0xFF101112);
		graphics.fill(trackX0, trackY, trackX0 + fillW, trackY + trackH, ORANGE);
		graphics.outline(trackX0, trackY, trackW, trackH, hovered ? BORDER : 0xFF3E4042);

		graphics.fill(trackX0 + fillW - 2, trackY - handleHalfY, trackX0 + fillW + 2, trackY + trackH + handleHalfY,
				hovered ? ORANGE : ORANGE_DARK);

		String label = formatValue(setting);
		graphics.text(font, Component.literal(label), containerX1 + 6, rowTop + 3, TEXT);
	}

	private List<FloatSetting> floatSettingsOf(Module module) {
		return module.getSettings().stream()
				.filter(s -> s instanceof FloatSetting)
				.map(s -> (FloatSetting) s)
				.toList();
	}

private String formatValue(FloatSetting setting) {
        String label = setting.getValueLabel();
        if (label != null) {
            return label;
        }
        float step = setting.getStep();
        if (step >= 1f) {
            return String.valueOf((int) Math.round(setting.getValue()));
        }
        return String.format("%.1f", setting.getValue());
    }

	private boolean isCategoryTab(int index) {
		return index == CATEGORY_COMBAT || index == CATEGORY_BLATANT
				|| index == CATEGORY_MOVEMENT || index == CATEGORY_RENDER;
	}

	private Category categoryForIndex(int index) {
		if (index == CATEGORY_COMBAT) return Category.COMBAT;
		if (index == CATEGORY_BLATANT) return Category.BLATANT;
		if (index == CATEGORY_MOVEMENT) return Category.MOVEMENT;
		if (index == CATEGORY_RENDER) return Category.RENDER;
		return null;
	}

	private Module firstCombatModule() {
		List<Module> modules = ModuleManager.getInstance().getModulesByCategory(Category.COMBAT);
		return modules.isEmpty() ? null : modules.get(0);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != 0) {
			return super.mouseClicked(event, doubleClick);
		}
		int menuWidth = Math.min(560, width - 36);
		int menuHeight = Math.min(300, height - 36);
		int left = (width - menuWidth) / 2;
		int top = (height - menuHeight) / 2;
		int sidebarWidth = 128;
		int contentLeft = left + sidebarWidth;

		for (int index = 0; index < CATEGORIES.length; index++) {
			int rowTop = top + 54 + index * 34;
			if (inside(event.x(), event.y(), left + 8, rowTop, sidebarWidth - 16, 28)) {
				selectedCategory = index;
Category cat = categoryForIndex(index);
				if (cat != null) {
					List<Module> mods = ModuleManager.getInstance().getModulesByCategory(cat);
					selectedModule = mods.isEmpty() ? null : mods.get(0);
				} else {
					selectedModule = null;
				}
				categoryScroll = 0;
				draggingSetting = null;
				bindingModule = null;
				return true;
			}
		}

		if (isCategoryTab(selectedCategory)) {
			Category cat = categoryForIndex(selectedCategory);
			int contentTop = top + 66;
			int rowX = contentLeft + 22;
			int rowWidth = (left + menuWidth - 22) - rowX;
			int pillX0 = rowX + rowWidth - 44;
			int pillX1 = rowX + rowWidth;
			int bindBtnX0 = pillX0 - 48;
			int bindBtnX1 = pillX0 - 4;
			int rowTop = contentTop + 36 - categoryScroll;
			List<Module> modules = ModuleManager.getInstance().getModulesByCategory(cat);

			for (Module module : modules) {
				if (inside(event.x(), event.y(), bindBtnX0, rowTop - 2, bindBtnX1 - bindBtnX0, MODULE_ROW_HEIGHT)) {
					bindingModule = bindingModule == module ? null : module;
					return true;
				}
				if (inside(event.x(), event.y(), pillX0, rowTop - 2, pillX1 - pillX0, MODULE_ROW_HEIGHT)) {
					module.toggle();
					return true;
				}
				if (inside(event.x(), event.y(), rowX, rowTop - 2, pillX0 - rowX, MODULE_ROW_HEIGHT)) {
					selectedModule = module;
					draggingSetting = null;
					return true;
				}
				rowTop += MODULE_ROW_HEIGHT + MODULE_ROW_GAP;
			}

			if (selectedModule != null) {
				int settingsTop = rowTop + 8;
				int sliderRow = settingsTop + 24;
				for (FloatSetting setting : floatSettingsOf(selectedModule)) {
					int trackX0 = rowX + 130;
					int trackX1 = pillX0 - 70;
					int trackW = trackX1 - trackX0;
					if (trackW < 60) {
						trackW = 60;
					}
					if (inside(event.x(), event.y(), trackX0 - 3, sliderRow + 3, trackW + 6, 18)) {
						draggingSetting = setting;
						updateSliderFromX(setting, event.x(), trackX0, trackW);
						return true;
					}
					sliderRow += SETTING_ROW_HEIGHT;
				}
			}
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (event.button() != 0) {
			return super.mouseDragged(event, dragX, dragY);
		}
		if (draggingSetting == null) {
			return super.mouseDragged(event, dragX, dragY);
		}
		int menuWidth = Math.min(560, width - 36);
		int left = (width - menuWidth) / 2;
		int sidebarWidth = 128;
		int contentLeft = left + sidebarWidth;
		int rowX = contentLeft + 22;
		int pillX0 = rowX + (left + menuWidth - 22) - rowX - 44;
		int trackX0 = rowX + 130;
		int trackX1 = pillX0 - 70;
		int trackW = trackX1 - trackX0;
		if (trackW < 60) {
			trackW = 60;
		}
		updateSliderFromX(draggingSetting, event.x(), trackX0, trackW);
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		draggingSetting = null;
		return super.mouseReleased(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (bindingModule != null) {
			int code = event.key();
			if (code == 256) {
				bindingModule.setKeyCode(-1);
			} else {
				bindingModule.setKeyCode(code);
			}
			bindingModule = null;
			return true;
		}
		return super.keyPressed(event);
	}

	private String keyName(int code) {
		if (code < 0) {
			return "KEY";
		}
		try {
			net.minecraft.network.chat.Component display = com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM
					.getOrCreate(code).getDisplayName();
			String text = display.getString().toUpperCase();
			if (!text.isEmpty()) {
				return text;
			}
		} catch (Exception ignored) {
		}
		return "KEY";
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (isCategoryTab(selectedCategory)) {
			int menuWidth = Math.min(560, width - 36);
			int menuHeight = Math.min(300, height - 36);
			int left = (width - menuWidth) / 2;
			int top = (height - menuHeight) / 2;
			int contentLeft = left + 128;
			int viewportTop = top + 66 + 36;
			int viewportBottom = top + menuHeight - 58;
			if (inside(mouseX, mouseY, contentLeft, viewportTop, (left + menuWidth) - contentLeft, viewportBottom - viewportTop)) {
				categoryScroll -= (int) (verticalAmount * 20);
				return true;
			}
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	private void updateSliderFromX(FloatSetting setting, double mouseX, int trackX0, int trackW) {
		float ratio = (float) ((mouseX - trackX0) / (double) trackW);
		ratio = Mth.clamp(ratio, 0f, 1f);
		float raw = setting.getMin() + ratio * (setting.getMax() - setting.getMin());
		float stepped = Math.round(raw / setting.getStep()) * setting.getStep();
		setting.setValue(stepped);
	}

	private static boolean inside(double mouseX, double mouseY, int left, int top, int width, int height) {
		return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
	}
}