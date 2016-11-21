package com.xored.javafx.packeteditor.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.xored.javafx.packeteditor.metatdata.FEInstructionParameterMeta;

import static com.xored.javafx.packeteditor.metatdata.FEInstructionParameterMeta.Type;

public class FEInstructionParameter2 {
    private FEInstructionParameterMeta meta;
    
    private JsonElement value;

    public FEInstructionParameter2(FEInstructionParameterMeta meta, JsonElement value) {
        this.meta = meta;
        this.value = value;
    }

    public String getId() {
        return meta.getId();
    }

    public JsonElement getValue() {
        return value;
    }

    public FEInstructionParameterMeta getMeta() {
        return meta;
    }

    public Type getType() {
        return meta.getType();
    }

    public String getDefaultValue() {
        return meta.getDefaultValue();
    }

    public boolean isRequired() {
        return meta.isRequired();
    }

    public void setValue(String value) {
        this.value = new JsonPrimitive(value);
    }
}
