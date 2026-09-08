package com.tyclient.module;

import com.tyclient.module.setting.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {
    private final String name;
    private final Category category;
    private final List<Setting> settings = new ArrayList<>();
    private boolean enabled;
    private int keyCode = -1;

    protected Module(String name, Category category) {
        this.name = name;
        this.category = category;
        this.enabled = false;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    protected void addSetting(Setting setting) {
        settings.add(setting);
    }

    public List<Setting> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    public String getName() {
        return name;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        if (enabled) {
            disable();
        } else {
            enable();
        }
    }

    public void enable() {
        enabled = true;
        onEnable();
    }

    public void disable() {
        enabled = false;
        onDisable();
    }

    protected void onEnable() {}

    protected void onDisable() {}
}