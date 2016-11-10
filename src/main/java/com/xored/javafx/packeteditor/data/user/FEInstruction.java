package com.xored.javafx.packeteditor.data.user;

import java.util.Map;

public class FEInstruction {
    // ProtocolId.FieldId
    private String id;
    
    private String fieldId;
    
    private Map<String, String> parameters;

    public FEInstruction(String id, String fieldId, Map<String, String> parameters) {
        this.id = id;
        this.fieldId = fieldId;
        this.parameters = parameters;
    }

    public String getId() {
        return id;
    }

    public String getFieldId() {
        return fieldId;
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
