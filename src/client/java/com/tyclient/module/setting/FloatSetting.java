package com.tyclient.module.setting;

public class FloatSetting extends Setting {
    private final float min;
    private final float max;
    private final float step;
    private float value;
    private String[] labels;

    public FloatSetting(String name, float min, float max, float defaultValue, float step) {
        super(name);
        this.min = min;
        this.max = max;
        this.step = step;
        this.value = Math.max(min, Math.min(max, defaultValue));
    }

    public void setLabels(String[] labels) {
        this.labels = labels;
    }

    public String getValueLabel() {
        if (labels == null) return null;
        int index = Math.round((value - min) / step);
        if (index < 0 || index >= labels.length) return null;
        return labels[index];
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = Math.max(min, Math.min(max, value));
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    public float getStep() {
        return step;
    }

    public float normalizedValue() {
        return (value - min) / (max - min);
    }
}