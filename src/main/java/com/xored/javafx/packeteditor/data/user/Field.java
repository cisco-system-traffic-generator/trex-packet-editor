package com.xored.javafx.packeteditor.data.user;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Field {
    
    private String id;

    private String value;
    
    private List<String> path;

    public Field(String id, List<String> path) {
        this.id = id;
        this.path = path;
    }

    public String getUniqueId() {
        List<String> path = new ArrayList<>(this.path);
        path.add(id);
        return path.stream().collect(Collectors.joining("-"));
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public String getValue() {
        return value;
    }
}
