package com.xored.javafx.packeteditor.data;

import com.xored.javafx.packeteditor.metatdata.FeParameterMeta;

public class FeParameter {
    private FeParameterMeta meta;
    private String value;

    public FeParameter(FeParameterMeta meta, String value) {
        this.meta = meta;
        this.value = value;
    }

    public FeParameterMeta getMeta() {
        return meta;
    }

    public String getId() {
        return meta.getId();
    }

    public String getValue() {
        return value;
    }

    public String getName() {
        return meta.getName();
    }

    public FeParameterMeta.FeParameterType getType() {
        return meta.getType();
    }

    public void setValue(String value) {
        this.value = value;
    }
}
