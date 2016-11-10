package com.xored.javafx.packeteditor.scapy;

import java.util.Map;
import java.util.stream.Collectors;

public class InstructionExpressionData {
    public String name;
    public String free_form;
    public Map<String, String> parameters;

    public InstructionExpressionData(String name, Map<String, String> parameters, String free_form) {
        this.name = name;
        this.parameters = parameters;
        this.free_form = free_form;
    }
    
    public String toString() {
        return free_form != null ? free_form : name + "("+parameters.entrySet().stream().map(e -> e.getKey()+"="+e.getValue()).collect(Collectors.joining(", "))+")";
    }
}
