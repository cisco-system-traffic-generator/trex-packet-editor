package com.xored.javafx.packeteditor.data.user;


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

    private Map<String, Map<String, String>> fieldInstructions = new HashMap<>();
    
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
        return fieldInstructions.get(fieldId) != null ? fieldInstructions.get(fieldId).get(paramId) : null;
    }

    public void setFieldInstruction(String fieldId, String parameter, String value) {
        Map<String, String> instruction = fieldInstructions.get(fieldId);
        instruction.put(parameter, value);
        fieldInstructions.put(fieldId, instruction);
    }
    
    public void createFieldInstruction(String fieldId) {
        Map<String, String> instruction = new HashMap<>();
        meta.getInstructionParametersMeta(fieldId).stream().forEach(parameterMeta -> instruction.put(parameterMeta.getId(), parameterMeta.getDefaultValue()));
        fieldInstructions.put(fieldId, instruction);
    }
    
    public void deleteFieldInstruction(String fieldId) {
        fieldInstructions.put(fieldId, Collections.<String, String>emptyMap());
    }
    
    public Map<String, String> getFieldInstruction(String fieldId) {
        return fieldInstructions.getOrDefault(fieldId, Collections.<String, String>emptyMap());
    }
}

