package com.xored.javafx.packeteditor.data.user;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.xored.javafx.packeteditor.data.FEInstructionParameter;
import com.xored.javafx.packeteditor.data.combined.CombinedField;
import com.xored.javafx.packeteditor.metatdata.FEInstructionParameterMeta;
import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;
import com.xored.javafx.packeteditor.scapy.FieldValue;
import com.xored.javafx.packeteditor.scapy.ReconstructField;
import com.xored.javafx.packeteditor.scapy.ReconstructProtocol;

import java.io.File;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

/** Defines a document with user model of a packet
 * this model can be incorrect by user requirement
 */
public class Document {
    
    private Stack<UserProtocol> protocols = new Stack<>();

    private DocumentMetadata metadata = new DocumentMetadata();

    File currentFile;

    private UserField lastModifiedField;
    
    private JsonElement valueBeforeModification;
    
    public DocumentMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(DocumentMetadata metadata) {
        this.metadata = metadata;
    }

    public File getCurrentFile() {
        return currentFile;
    }

    public void setCurrentFile(File currentFile) {
        this.currentFile = currentFile;
    }


    public void addProtocol(ProtocolMetadata metadata) {
        List<String> currentPath = protocols.stream().map(UserProtocol::getId).collect(Collectors.toList());
        currentPath.add(metadata.getId());
        UserProtocol newProtocol = new UserProtocol(metadata, currentPath);
        
        metadata.getFields().stream().forEach(entry -> newProtocol.addField(new UserField(entry.getId())));
        
        protocols.push(newProtocol);
    }

    public void setFieldValue(List<String> path, String fieldId, String value) {
        setFieldValue(path, fieldId, new JsonPrimitive(value));
    }

    public void setFieldValue(List<String> path, String fieldId, JsonElement value) {
        UserProtocol protocol = getProtocolByPath(path);
        UserField field = protocol.getField(fieldId);
        if (field == null) {
            field = protocol.createField(fieldId);
        }
        lastModifiedField = field;
        valueBeforeModification = field.getValue();
        field.setValue(value);
    }

    public void setFEInstructionParameter(FEInstructionParameter instructionParameter, String value) {
        CombinedField field = instructionParameter.getCombinedField();
        FEInstructionParameterMeta instructionParameterMeta = instructionParameter.getMeta();
        UserProtocol protocol = instructionParameter.getCombinedField().getProtocol().getUserProtocol();
        protocol.setFieldInstruction(field.getMeta().getId(), instructionParameterMeta.getId(), value);
    }
    
    public UserProtocol getProtocolByPath(List<String> path) {
        // TODO: check path
        return protocols.get(path.size() - 1);
    }
    
    public Stack<UserProtocol> getProtocolStack() { return protocols; }

    public void clear() {
        protocols.clear();
    }

    public void deleteField(List<String> path, String fieldUniqueId) {
        getProtocolByPath(path).deleteField(fieldUniqueId);
    }

    private ReconstructField createFieldValue(UserField userField) {
        JsonElement val = userField.getValue();
        if (FieldValue.isPrimitive(val)) {
            // these values are editable as text, so let's parse them as a human value
            return ReconstructField.setHumanValue(userField.getId(), val.getAsString());
        } else {
            // bytes, expressions, objects and so on
            return ReconstructField.setRawValue(userField.getId(), val);
        }
    }

    public List<ReconstructProtocol> buildScapyModel() {
        return protocols.stream().map(
                protocol -> ReconstructProtocol.modify(protocol.getId(), protocol.getSetFields().stream()
                        .map(this::createFieldValue)
                        .collect(Collectors.toList()))
        ).collect(Collectors.toList());
    }

    private JsonElement asJson() {
        JsonArray json = new JsonArray();
        protocols.stream().forEach(protocol -> {
            JsonObject jsonProtocol = new JsonObject();
            jsonProtocol.add("id", new JsonPrimitive(protocol.getId()));
            List<UserField> fields = protocol.getSetFields();
            JsonArray jsonFields = new JsonArray();
            fields.stream().forEach(field -> {
                JsonObject jsonField = new JsonObject();
                jsonField.add("id", new JsonPrimitive(field.getId()));
                jsonField.add("value", field.getValue());
                jsonFields.add(jsonField);
            });
            jsonProtocol.add("fields", jsonFields);
            json.add(jsonProtocol);
        });
        JsonArray arr = new JsonArray();
        arr.add(json);
        return json;
    }

    public void revertLastChanges() {
        if (lastModifiedField != null) {
            lastModifiedField.setValue(valueBeforeModification);
        }
        lastModifiedField = null;
        valueBeforeModification = null;
    }

    public void createFEFieldInstruction(CombinedField combinedField) {
        combinedField.getProtocol().getUserProtocol().createFieldInstruction(combinedField.getId());
    }

    public void deleteFEFieldInstruction(CombinedField combinedField) {
        combinedField.getProtocol().getUserProtocol().deleteFieldInstruction(combinedField.getId());
    }
}
