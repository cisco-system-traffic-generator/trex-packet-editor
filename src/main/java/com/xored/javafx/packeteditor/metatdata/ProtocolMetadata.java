package com.xored.javafx.packeteditor.metatdata;

import com.google.gson.internal.LinkedTreeMap;

import java.util.*;
import java.util.stream.Collectors;

public class ProtocolMetadata {
    private String id; // scapy class id

    private String name; // protocol name
    private LinkedTreeMap<String, FieldMetadata> fields = new LinkedTreeMap<>();

    private Map<String, FEInstructionParameterMeta> instructionParametersMetas = new LinkedHashMap<>();
    private List<String> fieldEngineAwareFields = new ArrayList<>();
    
    public ProtocolMetadata(String id, String name, List<FieldMetadata> fields, Map<String, FEInstructionParameterMeta> instructionParametersMetas, List<String> fieldEngineAwareFields) {
        this.id = id;
        this.name = name;
        if (instructionParametersMetas != null) {
            this.instructionParametersMetas.putAll(instructionParametersMetas);
        }
        
        if (fieldEngineAwareFields != null) {
            this.fieldEngineAwareFields.addAll(fieldEngineAwareFields);
        }
        
        for(FieldMetadata fieldMeta : fields) {
           this.fields.put(fieldMeta.getId(), fieldMeta);
        }
    }

    public List<FEInstructionParameterMeta> getInstructionParametersMeta(String fieldId) {
        
        if (fieldEnigineAllowed(fieldId)) {
            return instructionParametersMetas.values().stream().collect(Collectors.toList());
        } else {
            return Collections.<FEInstructionParameterMeta>emptyList();
        }
    }

    public boolean fieldEnigineAllowed(String fieldId) {
        return fieldEngineAwareFields.contains(fieldId);
    }
    
    public FEInstructionParameterMeta getInstructionParameterMeta(String parameterId) {
        return instructionParametersMetas.get(parameterId);
    }
    
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<FieldMetadata> getFields() {
        return fields.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList());
    }

    public FieldMetadata getMetaForField(String fieldId) {
        return fields.get(fieldId);
    }
    public FieldMetadata getMetaForFieldOrNull(String fieldId) { return fields.getOrDefault(fieldId, null); }

    @Override
    public String toString() {
        return name;
    }
}
