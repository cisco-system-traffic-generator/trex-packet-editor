package com.xored.javafx.packeteditor.metatdata;

import java.util.List;

public class InstructionExpressionMeta {
    private String id;
    private List<FEInstructionParameterMeta> parameterMetas;

    public InstructionExpressionMeta(String id, List<FEInstructionParameterMeta> parameterMetas) {
        this.id = id;
        this.parameterMetas = parameterMetas;
    }

    public String getId() {
        return id;
    }

    public List<FEInstructionParameterMeta> getParameterMetas() {
        return parameterMetas;
    }
    
    public String toString() {
        return id;
    }
}
