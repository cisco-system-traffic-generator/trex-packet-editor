package com.xored.javafx.packeteditor.data;

import com.xored.javafx.packeteditor.data.combined.CombinedField;
import com.xored.javafx.packeteditor.metatdata.FEInstructionParameterMeta;
import com.xored.javafx.packeteditor.metatdata.FEInstructionParameterMeta.Type;

public class FEInstructionParameter {
    
    private CombinedField combinedField;
    
    private FEInstructionParameterMeta meta;
    
    public FEInstructionParameter(CombinedField combinedField, FEInstructionParameterMeta meta) {
        this.combinedField = combinedField;
        this.meta = meta;
    }   

    public CombinedField getCombinedField() {
        return combinedField;
    }

    public FEInstructionParameterMeta getMeta() {
        return meta;
    }

    public String getValue() {
        return combinedField.getProtocol().getUserProtocol().getFieldInstructionParam(getFieldId(), getId());
    }

    public String getId() {
        return meta.getId();
    }

    public Type getType() {
        return meta.getType();
    }

    public String getDefaultValue() {
        return getMeta().getDefaultValue();
    }

    public String getFieldId() {
        return combinedField.getId();
    }
}
