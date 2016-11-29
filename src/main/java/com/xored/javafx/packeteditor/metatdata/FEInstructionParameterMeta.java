package com.xored.javafx.packeteditor.metatdata;

import java.util.Map;

import static com.xored.javafx.packeteditor.metatdata.FEInstructionParameterMeta.Type.ENUM;
import static com.xored.javafx.packeteditor.metatdata.FEInstructionParameterMeta.Type.NUMBER;
import static com.xored.javafx.packeteditor.metatdata.FEInstructionParameterMeta.Type.STRING;

public class FEInstructionParameterMeta {

    private String type;
    
    private String id;
    
    private String name;
    
    private String defaultValue;
    
    private Map<String, String> dict;
    
    private Boolean required;
    
    private Boolean editable;

    public FEInstructionParameterMeta(String type, String id, String name, String defaultValue, Map<String, String> dict, Boolean required, Boolean editable) {
        this.type = type;
        this.id = id;
        this.name = name;
        this.defaultValue = defaultValue;
        this.dict = dict;
        this.required = required == null ? Boolean.FALSE : required;
        this.editable = editable == null ? Boolean.FALSE : editable;
    }

    public Boolean editable() {
        return editable;
    }

    public Boolean isRequired() {
        return required;
    }

    public String toString() {
        return id;
    }
    
    public boolean isEnum() {
        return ENUM.equals(getType());
    }
    
    public Type getType() {
        switch (type) {
            case "ENUM":
                return ENUM;
            case "NUMBER":
                return NUMBER;
            case "STRING":
            default:
                return STRING;
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDefaultValue() {
        return defaultValue == null ? "" : defaultValue;
    }

    public Map<String, String> getDict() {
        return dict;
    }

    public enum Type {
        STRING, ENUM, NUMBER
    }
}
