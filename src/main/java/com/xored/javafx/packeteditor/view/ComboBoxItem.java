package com.xored.javafx.packeteditor.view;

import com.google.gson.JsonElement;

public class ComboBoxItem {
    private final String displayValue;
    private final JsonElement value;

    public ComboBoxItem(String displayValue, JsonElement value) {
        this.displayValue = displayValue;
        this.value = value;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public JsonElement getValue() {
        return value;
    }
    
    @Override
    public String toString() { return displayValue; }

    public boolean equalsTo(JsonElement value) {
        return value != null && this.value.toString().equals(value.toString());
    }
}
