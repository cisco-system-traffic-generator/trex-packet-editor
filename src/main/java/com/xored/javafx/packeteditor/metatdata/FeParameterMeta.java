package com.xored.javafx.packeteditor.metatdata;

public class FeParameterMeta {
    private String id;
    
    private String name;

    private FeParameterType type;
    
    private String defaultValue;

    public String getDefault() {
        return defaultValue == null ? "" : defaultValue;
    }

    public enum FeParameterType {
        STRING, ENUM
    }

    public FeParameterMeta(String id, String name, String type, String defaultValue) {
        this.id = id;
        this.name = name;
        this.defaultValue = defaultValue;
        switch (type) {
            case "NUMBER":
            case "STRING":
                this.type = FeParameterType.STRING;
                break;
            case "ENUM":
                this.type = FeParameterType.ENUM;
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public FeParameterType getType() {
        return type;
    }
}
