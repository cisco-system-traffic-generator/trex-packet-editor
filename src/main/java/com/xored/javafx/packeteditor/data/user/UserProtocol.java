package com.xored.javafx.packeteditor.data.user;


import com.google.gson.internal.LinkedTreeMap;
import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Serializable protocol data of user document model
 * */
public class UserProtocol {
    private ProtocolMetadata meta;
    private List<String> path = new ArrayList<>();
    private Map<String, UserField> fieldMap = new LinkedHashMap<>();
    boolean collapsed = false;

    // TODO: delete
    private Map<String, FEInstruction> fieldInstructions = new HashMap<>();
    
    public UserProtocol(ProtocolMetadata meta, List<String> path) {
        this.meta = meta;
        this.path.addAll(path);
    }
    public void addField(UserField field) {
        fieldMap.put(field.getId(), field);
    }

    public void addField(String fieldId, String value) {
        UserField field = new UserField(fieldId);
        field.setValue(value);
        fieldMap.put(field.getId(), field);
    }

    public UserField getField(String fieldId) {
        return fieldMap.get(fieldId);
    }

    public String getId() {
        return meta.getId();
    }

    public List<UserField> getSetFields() {
        return fieldMap.entrySet().stream()
                .filter(entry -> entry.getValue().getValue() != null)
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    public UserField createField(String fieldId) {
        UserField field = new UserField(fieldId);
        fieldMap.put(fieldId, field);
        return field;
    }

    public void deleteField(String fieldId) {
        fieldMap.remove(fieldId);
        createField(fieldId);
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    public String getFieldInstructionParam(String fieldId, String paramId) {
        return fieldInstructions.get(fieldId) != null ? fieldInstructions.get(fieldId).getParameterValue(paramId) : null;
    }
    
    public FEInstruction createFieldInstruction(String fieldId) {
        Map<String, String> parameters = new LinkedTreeMap<>();
        meta.getInstructionParametersMeta(fieldId).stream().forEach(parameterMeta -> parameters.put(parameterMeta.getId(), parameterMeta.getDefaultValue()));
        String instructionId = getId() + "." + fieldId;
        FEInstruction instruction = new FEInstruction(instructionId, parameters);
        fieldInstructions.put(fieldId, instruction);
        return instruction;
    }
    
    public void deleteFieldInstruction(String fieldId) {
        fieldInstructions.remove(fieldId);
    }
    
    public FEInstruction getFieldInstruction(String fieldId) {
        return fieldInstructions.get(fieldId);
    }

    public List<FEInstruction> getFieldInstructionsList() {
        return fieldInstructions.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList());
    }

    public void addFieldVmInstruction(FEInstruction instruction) {
    }

    public String getPaddedId() {
        return " " + meta.getId() + " ";
    }
}

