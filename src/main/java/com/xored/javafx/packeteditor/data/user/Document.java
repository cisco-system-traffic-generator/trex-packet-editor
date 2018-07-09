package com.xored.javafx.packeteditor.data.user;

import com.google.common.base.Strings;
import com.google.gson.*;
import com.xored.javafx.packeteditor.data.FEInstructionParameter2;
import com.xored.javafx.packeteditor.data.FeParameter;
import com.xored.javafx.packeteditor.data.InstructionExpression;
import com.xored.javafx.packeteditor.data.combined.CombinedField;
import com.xored.javafx.packeteditor.data.user.DocumentFile.DocumentInstructionExpression;
import com.xored.javafx.packeteditor.metatdata.FeParameterMeta;
import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;
import com.xored.javafx.packeteditor.scapy.FieldValue;
import com.xored.javafx.packeteditor.scapy.ReconstructField;
import com.xored.javafx.packeteditor.scapy.ReconstructProtocol;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/** Defines a document with user model of a packet
 * this model can be incorrect by user requirement
 */
public class Document {
    
    private Stack<UserProtocol> protocols = new Stack<>();

    private DocumentMetadata metadata = new DocumentMetadata();

    public Map<String, FeParameter> feParameters = new HashMap<>();
    
    File currentFile;

    private UserField lastModifiedField;
    
    private JsonElement valueBeforeModification;
    
    private List<InstructionExpression> feInstructions = new ArrayList<>();

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

    public void setFEInstructionParameter(FEInstructionParameter2 instructionParameter, JsonElement value) {
        instructionParameter.setRawValue(value);
    }
    
    public UserProtocol getProtocolByPath(List<String> path) {
        // TODO: check path
        return protocols.get(path.size() - 1);
    }
    
    public Stack<UserProtocol> getProtocolStack() { return protocols; }

    public void clear() {
        protocols.clear();
        feInstructions.clear();
        feParameters.clear();
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

    public void createFePrarameter(FeParameterMeta meta, String value) {
        FeParameter feParameter = new FeParameter(meta, value);
        feParameters.put(meta.getId(), feParameter);
    }

    public void setFePrarameterValue(String feParameterId, String value) {
        FeParameter feParameter = getFeParameter(feParameterId);
        feParameter.setValue(value);
        feParameters.put(feParameterId, feParameter);
    }

    public FeParameter getFeParameter(String parameterId) {
        return feParameters.get(parameterId);
    }
    
    public void deleteFEFieldInstruction(CombinedField combinedField) {
        combinedField.getProtocol().getUserProtocol().deleteFieldInstruction(combinedField.getId());
    }

    public JsonElement getVmInstructionsModel() {
        Gson gson = new Gson();
        List<DocumentInstructionExpression> instructionsPOJO = feInstructions.stream()
                .map(InstructionExpression::toPOJO)
                .collect(Collectors.toList());
        
        JsonObject payload = new JsonObject();
        JsonObject fieldEngine = new JsonObject();
        fieldEngine.add("instructions", gson.toJsonTree(instructionsPOJO));
        Map<String, String> feParameters = getFePrarameters().stream()
                .filter(feParameter -> !Strings.isNullOrEmpty(feParameter.getValue()))
                .collect(Collectors.toMap(FeParameter::getId, FeParameter::getValue));
        fieldEngine.add("global_parameters", gson.toJsonTree(feParameters));
        payload.add("field_engine", fieldEngine);
        return payload;
    }

    public List<InstructionExpression> getInstructions() {
        return feInstructions;
    }

    public void addInstruction(InstructionExpression instructionExpression) {
        feInstructions.add(instructionExpression);
    }

    public void deleteInstruction(InstructionExpression instructionExpression) {
        feInstructions.remove(instructionExpression);
    }
    
    public List<FeParameter> getFePrarameters() {
        return feParameters.isEmpty() ? Collections.<FeParameter>emptyList() : feParameters.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList());
    }


    public void initFeParameters(Map<String, FeParameterMeta> feParameters) {
        feParameters.entrySet().stream().forEach(metaEntry -> {
            FeParameterMeta meta = metaEntry.getValue();
            FeParameter feParameter = new FeParameter(meta, meta.getDefault());
            this.feParameters.put(meta.getId(), feParameter);
        });
    }
    
    public void clearFeParameters() {
        feParameters.clear();
    }

    public void clearSplitByIfnecessary(String protocolId) {
        for(FeParameter parameter : getFePrarameters()) {
            if(needToClearSplitBy(parameter, protocolId)) {
                parameter.setValue("");
            }
        }
    }
    
    private boolean needToClearSplitBy(FeParameter parameter, String protocolId) {
        String value = parameter.getValue().toLowerCase();
        return parameter.getId().equals("split_by_var")
            && parameter.getId().equals("split_by_var") && value.contains(protocolId.toLowerCase());
    }

    public List<InstructionExpression> getFeInstructions() {
        return feInstructions;
    }
}
