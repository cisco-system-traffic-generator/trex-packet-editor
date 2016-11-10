package com.xored.javafx.packeteditor.scapy;

import java.util.Map;
import java.util.stream.Collectors;

public class InstructionExpressionData {
    public String name;
    public Map<String, String> parameters;

    public InstructionExpressionData(String name, Map<String, String> parameters) {
        this.name = name;
        this.parameters = parameters;
    }
    
    public String toString() {
        return name + "("+parameters.entrySet().stream().map(e -> e.getKey()+"="+e.getValue()).collect(Collectors.joining(", "))+")";
    }
}
