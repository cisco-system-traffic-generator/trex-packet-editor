package com.xored.javafx.packeteditor.data;

import com.xored.javafx.packeteditor.data.combined.CombinedField;
import com.xored.javafx.packeteditor.metatdata.FEInstructionParameterMeta;
import com.xored.javafx.packeteditor.metatdata.FEInstructionParameterMeta.Type;

public class FEInstructionParameter {
    
    private CombinedField combinedField;
    
    private FEInstructionParameterMeta meta;
    
    private String value;

    public FEInstructionParameter(CombinedField combinedField, FEInstructionParameterMeta meta, String value) {
        this.combinedField = combinedField;
        this.meta = meta;
        this.value = value;
    }   

    public CombinedField getCombinedField() {
        return combinedField;
    }

    public FEInstructionParameterMeta getMeta() {
        return meta;
    }

    public String getValue() {
        return value;
    }

    public String getId() {
        return meta.getId();
    }

    public Type getType() {
        return meta.getType();
    }
}
