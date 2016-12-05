package com.xored.javafx.packeteditor.service;

import com.xored.javafx.packeteditor.metatdata.InstructionExpressionMeta;

import java.util.List;

public class InstructionsTemplate {
    private String id;
    private String name;
    private List<InstructionExpressionMeta> instructions;

    public InstructionsTemplate(String id, String name, List<InstructionExpressionMeta> instructions) {
        this.id = id;
        this.name = name;
        this.instructions = instructions;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<InstructionExpressionMeta> getInstructions() {
        return instructions;
    }
}
