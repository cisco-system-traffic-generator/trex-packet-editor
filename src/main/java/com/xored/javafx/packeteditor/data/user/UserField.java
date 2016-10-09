package com.xored.javafx.packeteditor.data.user;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UserField {
    
    private String id;

    private JsonElement value;
    
    private List<String> path;

    public UserField(String id, List<String> path) {
        this.id = id;
        this.path = path;
    }

    public void setValue(String value) {
        setValue(new JsonPrimitive(value));
    }

    public void setValue(JsonElement value) { this.value = value; }

    public String getId() {
        return id;
    }

    public String getStringValue() {
        return value.getAsString();
    }

    public JsonElement getValue() {
        return value;
    }

}
