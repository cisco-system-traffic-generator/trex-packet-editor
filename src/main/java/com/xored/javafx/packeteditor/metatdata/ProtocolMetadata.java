package com.xored.javafx.packeteditor.metatdata;

import com.google.gson.internal.LinkedTreeMap;

import java.util.*;
import java.util.stream.Collectors;

public class ProtocolMetadata {
    private String id; // scapy class id

    private String name; // protocol name
    private LinkedTreeMap<String, FieldMetadata> fields = new LinkedTreeMap<>();

    private List<FEInstructionParameterMeta> instructionParametersMetas = new ArrayList<>();
    private List<String> fieldEngineAwareFields = new ArrayList<>();
    
    public ProtocolMetadata(String id, String name, List<FieldMetadata> fields, List<FEInstructionParameterMeta> instructionParametersMetas, List<String> fieldEngineAwareFields) {
        this.id = id;
        this.name = name;
        if (instructionParametersMetas != null) {
            this.instructionParametersMetas.addAll(instructionParametersMetas);
        }
        
        if (fieldEngineAwareFields != null) {
            this.fieldEngineAwareFields.addAll(fieldEngineAwareFields);
        }
        
        for(FieldMetadata fieldMeta : fields) {
           this.fields.put(fieldMeta.getId(), fieldMeta);
        }
    }

    public List<FEInstructionParameterMeta> getInstructionParametersMeta(String fieldId) {
        
        // TODO: remove this stub once scapy_server supported it
        if (id.equals("Ether")) {
            fieldEngineAwareFields = Arrays.asList("src", "dst");
        }
        return fieldEngineAwareFields.contains(fieldId) ? instructionParametersMetas : Collections.<FEInstructionParameterMeta>emptyList();
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
