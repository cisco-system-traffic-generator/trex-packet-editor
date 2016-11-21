package com.xored.javafx.packeteditor.metatdata;

import java.util.List;

public class InstructionExpressionMeta {
    private String id;
    private String help;
    private List<FEInstructionParameterMeta> parameterMetas;

    public InstructionExpressionMeta(String id, String help, List<FEInstructionParameterMeta> parameterMetas) {
        this.id = id;
        this.parameterMetas = parameterMetas;
        this.help = help;
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

    public String getHelp() {
        return help;
    }
}
