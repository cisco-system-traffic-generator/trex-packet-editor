package com.xored.javafx.packeteditor.data.user;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;

import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

public class Document {
    
    private Stack<Protocol> protocols = new Stack<>();
    
    public void addProtocol(ProtocolMetadata metadata) {
        List<String> currentPath = getCurrentPath();
        currentPath.add(metadata.getId());
        Protocol newProtocol = new Protocol(metadata, getCurrentPath());
        
        metadata.getFields().stream().forEach(entry -> newProtocol.addField(new Field(entry.getId(), getCurrentPath())));
        
        protocols.push(newProtocol);
    }
    
    public void setFieldValue(String fieldId, String value) {
        Protocol protocol = protocols.peek();
        Field field = protocol.getField(fieldId);
        
        if (field == null) {
            field = protocol.createField(fieldId);
        }
        field.setValue(value);
    }
    
    public List<String> getCurrentPath() {
        return protocols.stream().map(Protocol::getId).collect(Collectors.toList());
    }
    
    public Protocol getCurrentProtocol() {
        return protocols.peek();
    }
    
    public void clear() {
        protocols.clear();
    }

    public void deleteField(String fieldUniqueId) {
        getCurrentProtocol().deleteField(fieldUniqueId);
    }
    
    public JsonElement asJson() {
        JsonArray json = new JsonArray();
        protocols.stream().forEach(protocol -> {
            JsonObject jsonProtocol = new JsonObject();
            jsonProtocol.add("id", new JsonPrimitive(protocol.getId()));
            List<Field> fields = protocol.getFields();
            JsonArray jsonFields = new JsonArray();
            fields.stream().forEach(field -> {
                JsonObject jsonField = new JsonObject();
                jsonField.add("id", new JsonPrimitive(field.getId()));
                jsonField.add("hvalue", new JsonPrimitive(field.getValue()));
                jsonFields.add(jsonField);
            });
            jsonProtocol.add("fields", jsonFields);
            json.add(jsonProtocol);
        });
        JsonArray arr = new JsonArray();
        arr.add(json);
        return json;
    }
}
