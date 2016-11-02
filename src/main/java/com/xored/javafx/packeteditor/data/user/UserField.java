package com.xored.javafx.packeteditor.data.user;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class UserField {
    
    private String id;
    private JsonElement value;

    public UserField(String id) {
        this.id = id;
    }

    public void setValue(String value) {
        setValue(new JsonPrimitive(value));
    }

    public void setValue(JsonElement value) { this.value = value; }

    public String getId() {
        return id;
    }

    public String getStringValue() {
        if (value instanceof JsonPrimitive) {
            return value.getAsString();
        } else  {
            return null;
        }
    }

    public boolean isSet() { return value != null; }
    public JsonElement getValue() {
        return value;
    }

}
