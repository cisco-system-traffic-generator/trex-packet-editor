package com.xored.javafx.packeteditor.data.user;


import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;
import javafx.scene.control.TitledPane;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UserProtocol {
    private ProtocolMetadata meta;
    private List<String> path = new ArrayList<>();
    private Map<String, UserField> fieldMap = new LinkedHashMap<>();
    private TitledPane titledPane = null;
    
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

    public TitledPane getTitledPane() {
        return titledPane;
    }

    public void setTitledPane(TitledPane titledPane) {
        this.titledPane = titledPane;
    }
}
