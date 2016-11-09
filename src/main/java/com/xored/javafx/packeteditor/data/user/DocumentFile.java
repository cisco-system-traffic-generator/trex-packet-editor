package com.xored.javafx.packeteditor.data.user;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.internal.LinkedTreeMap;
import com.xored.javafx.packeteditor.service.IMetadataService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serializes Document to POJO and JSON file
 */
public class DocumentFile {
    public static final String FILE_EXTENSION = ".trp";
    public static class DocumentField {
        public String id;
        public JsonElement value;

        public DocumentField(UserField field) {
            this.id = field.getId();
            this.value = field.getValue();
        }
    }

    private static class DocumentFieldVmInstruction {
        public String id;
        public String fieldId;
        public Map<String, String> parameters = new LinkedTreeMap<>();

        public DocumentFieldVmInstruction(String id, String fieldId, Map<String, String> parameters) {
            this.id = id;
            this.fieldId = fieldId;
            this.parameters.putAll(parameters);
        }
    }

    public static class DocumentProtocol {
        public String id;
        public List<DocumentField> fields;
        public List<DocumentFieldVmInstruction> fieldsVmInstructions;

        public DocumentProtocol(String id, List<DocumentField> fields, List<DocumentFieldVmInstruction> fieldsVmInstructions) {
            this.id = id;
            this.fields = fields;
            this.fieldsVmInstructions = fieldsVmInstructions;
        }
    }
    public String fileType;
    public String version;
    public DocumentMetadata metadata;
    public List<DocumentProtocol> packet;

    public static DocumentFile toPOJO(Document doc) {
        DocumentFile data = new DocumentFile();
        data.fileType = "trex-packet-editor";
        data.version = "1.0.0";
        data.metadata = doc.getMetadata();
        data.packet = doc.getProtocolStack().stream().map(
                protocol -> {
                    List<DocumentField> documentFields = protocol.getSetFields().stream()
                            .map(DocumentField::new)
                            .collect(Collectors.toList());
                    
                    List<DocumentFieldVmInstruction> fieldsVmInstructions = protocol.getFieldInstructionsList().stream()
                            .map(fieldsVmInstruction -> new DocumentFieldVmInstruction(fieldsVmInstruction.getId(), fieldsVmInstruction.getFieldId(), fieldsVmInstruction.getParameters()))
                            .collect(Collectors.toList());
                    return new DocumentProtocol(protocol.getId(), documentFields, fieldsVmInstructions);
                } 
        ).collect(Collectors.toList());
        return data;
    }

    public static Document fromPOJO(DocumentFile data, IMetadataService metadataService) {
        Document doc = new Document();
        doc.setMetadata(data.metadata);
        data.packet.forEach(
                documentProtocol -> {
                    doc.addProtocol(metadataService.getProtocolMetadataById(documentProtocol.id));
                    UserProtocol userProtocol = doc.getProtocolStack().peek();
                    documentProtocol.fieldsVmInstructions.stream().forEach(docInstruction -> {
                        FEInstruction instruction = new FEInstruction(docInstruction.id, docInstruction.fieldId, docInstruction.parameters);
                        userProtocol.addFieldVmInstruction(instruction);
                    });
                    documentProtocol.fields.forEach(field -> userProtocol.getField(field.id).setValue(field.value));
                }
        );
        return doc;
    }

    public static void saveToFile(Document doc, File outFile) throws IOException {
        Files.write(outFile.toPath(), new Gson().toJson(toPOJO(doc)).getBytes());
    }

    public static Document loadFromJSON(String json, IMetadataService metadataService) {
        DocumentFile doc = new Gson().fromJson(json, DocumentFile.class);
        return fromPOJO(doc, metadataService);
    }

    public static Document loadFromFile(File jsonFile, IMetadataService metadataService) throws IOException {
        DocumentFile doc = new Gson().fromJson(Files.newBufferedReader(jsonFile.toPath()), DocumentFile.class);
        return fromPOJO(doc, metadataService);
    }

}
