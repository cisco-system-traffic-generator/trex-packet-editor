package com.xored.javafx.packeteditor.metatdata;

import java.util.Map;

import static com.xored.javafx.packeteditor.metatdata.FEInstructionParameterMeta.Type.ENUM;
import static com.xored.javafx.packeteditor.metatdata.FEInstructionParameterMeta.Type.STRING;

public class FEInstructionParameterMeta {

    // "string" or "enum"
    private String type;
    
    private String id;
    
    private String name;
    
    private String defaultValue;
    
    private Map<String, String> dict;

    public FEInstructionParameterMeta(String type, String id, String name, String defaultValue, Map<String, String> dict) {
        this.type = type;
        this.id = id;
        this.name = name;
        this.defaultValue = defaultValue;
        this.dict = dict;
    }

    public boolean isEnum() {
        return "enum".equals(type);
    }
    
    public Type getType() {
        return "string".equals(type) ? STRING : ENUM;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public Map<String, String> getDict() {
        return dict;
    }

    public enum Type {
        STRING, ENUM
    }
}
