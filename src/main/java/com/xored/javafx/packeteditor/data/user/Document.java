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
    
    private Stack<UserProtocol> protocols = new Stack<>();
    
    public void addProtocol(ProtocolMetadata metadata) {
        List<String> currentPath = getCurrentPath();
        currentPath.add(metadata.getId());
        UserProtocol newProtocol = new UserProtocol(metadata, currentPath);
        
        metadata.getFields().stream().forEach(entry -> newProtocol.addField(new UserField(entry.getId())));
        
        protocols.push(newProtocol);
    }

    public void setFieldValue(List<String> path, String fieldId, String value) {
        UserProtocol protocol = getProtocolByPath(path);
        UserField field = protocol.getField(fieldId);
        
        if (field == null) {
            field = protocol.createField(fieldId);
        }
        field.setValue(value);
    }

    public void setFieldValue(List<String> path, String fieldId, JsonElement value) {
        // TODO: support JsonElement value
        setFieldValue(path, fieldId, value.getAsString());
    }

    public UserProtocol getProtocolByPath(List<String> path) {
        // TODO: check path
        return protocols.get(path.size() - 1);
    }
    
    public List<String> getCurrentPath() {
        return protocols.stream().map(UserProtocol::getId).collect(Collectors.toList());
    }

    public Stack<UserProtocol> getProtocolStack() { return protocols; }

    public void clear() {
        protocols.clear();
    }

    public void deleteField(List<String> path, String fieldUniqueId) {
        getProtocolByPath(path).deleteField(fieldUniqueId);
    }
    
    public JsonElement asJson() {
        JsonArray json = new JsonArray();
        protocols.stream().forEach(protocol -> {
            JsonObject jsonProtocol = new JsonObject();
            jsonProtocol.add("id", new JsonPrimitive(protocol.getId()));
            List<UserField> fields = protocol.getFields();
            JsonArray jsonFields = new JsonArray();
            fields.stream().forEach(field -> {
                JsonObject jsonField = new JsonObject();
                jsonField.add("id", new JsonPrimitive(field.getId()));
                jsonField.add("hvalue", new JsonPrimitive(field.getStringValue()));
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
