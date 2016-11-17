package com.xored.javafx.packeteditor.data;

import com.xored.javafx.packeteditor.metatdata.InstructionExpressionMeta;

import java.util.List;

public class InstructionExpression {
    private InstructionExpressionMeta meta;
    private List<FEInstructionParameter> parameters;

    public InstructionExpression(InstructionExpressionMeta meta, List<FEInstructionParameter> parameters) {
        this.meta = meta;
        this.parameters = parameters;
    }
    
    public String getId() {
        return meta.getId();
    }

    public InstructionExpressionMeta getMeta() {
        return meta;
    }

    public List<FEInstructionParameter> getParameters() {
        return parameters;
    }
}
