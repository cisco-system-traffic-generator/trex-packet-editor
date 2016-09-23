package com.xored.javafx.packeteditor.metatdata;

import com.google.gson.JsonElement;

import java.util.Map;

public class BitFlagMetadata {
    private String name;
    private Integer mask;
    private Map<String, JsonElement> values;

    public BitFlagMetadata(String name, Integer mask, Map<String, JsonElement> values) {
        this.name = name;
        this.mask = mask;
        this.values = values;
    }

    public String getName() {
        return name;
    }

    public Integer getMask() {
        return mask;
    }

    public Map<String, JsonElement> getValues() {
        return values;
    }
    
    public String toString() {
        return name;
    }
}
