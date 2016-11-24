package com.xored.javafx.packeteditor.data;

import com.xored.javafx.packeteditor.data.user.DocumentFile.DocumentInstructionExpression;
import com.xored.javafx.packeteditor.metatdata.InstructionExpressionMeta;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InstructionExpression {
    private InstructionExpressionMeta meta;
    private List<FEInstructionParameter2> parameters;

    public InstructionExpression(InstructionExpressionMeta meta, List<FEInstructionParameter2> parameters) {
        this.meta = meta;
        this.parameters = parameters;
    }
    
    public String getId() {
        return meta.getId();
    }

    public InstructionExpressionMeta getMeta() {
        return meta;
    }

    public List<FEInstructionParameter2> getParameters() {
        return parameters;
    }
    
    public String toString() {
        return meta.getId();
    }

    public String getHelp() {
        return meta.getHelp();
    }
    
    public DocumentInstructionExpression toPOJO() {
        Map<String, String> parametersPOJO = parameters.stream()
                .collect(Collectors.toMap(FEInstructionParameter2::getId, parameter -> parameter.getValue().getAsString()));
        DocumentInstructionExpression pojo = new DocumentInstructionExpression(meta.getId(), parametersPOJO);
        return pojo;
    }
}
