package com.xored.javafx.packeteditor.data.user;

import java.util.Map;

public class FEInstruction {
    private String id;
    
    private Map<String, String> parameters;

    public FEInstruction(String id, Map<String, String> parameters) {
        this.id = id;
        this.parameters = parameters;
    }

    public String getId() {
        return id;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }
    
    public String getParameterValue(String parameterId) {
        return parameters.get(parameterId);
    }

    public void putValue(String parameterId, String value) {
        parameters.put(parameterId, value);
    }
}
