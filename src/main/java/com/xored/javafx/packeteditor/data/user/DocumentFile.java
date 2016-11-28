package com.xored.javafx.packeteditor.data.user;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.internal.LinkedTreeMap;
import com.xored.javafx.packeteditor.data.FEInstructionParameter2;
import com.xored.javafx.packeteditor.data.FeParameter;
import com.xored.javafx.packeteditor.data.InstructionExpression;
import com.xored.javafx.packeteditor.metatdata.FeParameterMeta;
import com.xored.javafx.packeteditor.metatdata.InstructionExpressionMeta;
import com.xored.javafx.packeteditor.service.IMetadataService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serializes Document to POJO and JSON file
 */
public class DocumentFile {
    public String fileType;
    public String version;
    public DocumentMetadata metadata;
    public List<DocumentProtocol> packet;
    public Map<String, String> fePrarameters = new HashMap<>();
    public List<DocumentInstructionExpression> feInstructions = new ArrayList<>();
    public static final String FILE_EXTENSION = ".trp";

    public static class DocumentField {
        public String id;
        public JsonElement value;

        public DocumentField(UserField field) {
            this.id = field.getId();
            this.value = field.getValue();
        }
    }

    public static class DocumentInstructionExpression {
        public String id;
        public Map<String, String> parameters = new LinkedTreeMap<>();

        public DocumentInstructionExpression(String id, Map<String, String> parameters) {
            this.id = id;
            this.parameters.putAll(parameters);
        }
    }

    public static class DocumentProtocol {
        public String id;
        public List<DocumentField> fields;

        public DocumentProtocol(String id, List<DocumentField> fields) {
            this.id = id;
            this.fields = fields;
        }
    }

    public static DocumentFile toPOJO(Document doc) {
        DocumentFile data = new DocumentFile();
        data.fileType = "trex-packet-editor";
        data.version = "1.0.0";
        data.metadata = doc.getMetadata();
        data.fePrarameters = doc.getFePrarameters().stream().collect(Collectors.toMap(FeParameter::getId, FeParameter::getValue));
        data.feInstructions = doc.getInstructions().stream().map(InstructionExpression::toPOJO).collect(Collectors.toList());
        data.packet = doc.getProtocolStack().stream().map(DocumentFile::documentProtocolToPOJO).collect(Collectors.toList());
        return data;
    }
    
    private static DocumentProtocol documentProtocolToPOJO(UserProtocol protocol) {
        List<DocumentField> documentFields = protocol.getSetFields().stream()
                .map(DocumentField::new)
                .collect(Collectors.toList());

        return new DocumentProtocol(protocol.getId(), documentFields);
    }

    public static Document fromPOJO(DocumentFile data, IMetadataService metadataService) {
        Document doc = new Document();
        doc.setMetadata(data.metadata);
        Map<String, FeParameterMeta> feParameterMetas = metadataService.getFeParameters();
        data.fePrarameters.entrySet().stream()
                .forEach(entry -> doc.createFePrarameter(feParameterMetas.get(entry.getKey()), entry.getValue()));
        
        data.feInstructions.stream().forEach(instructionPOJO -> {
            InstructionExpressionMeta meta = metadataService.getFeInstructions().get(instructionPOJO.id);

            List<FEInstructionParameter2> instructionParameters = meta.getParameterMetas().stream()
                    .map(parameterMeta -> new FEInstructionParameter2(
                                                parameterMeta,
                                                new JsonPrimitive(instructionPOJO.parameters.get(parameterMeta.getId()))))
                    .collect(Collectors.toList());
            
            doc.addInstruction(new InstructionExpression(meta, instructionParameters));
        });

        data.packet.forEach(
            documentProtocol -> {
                doc.addProtocol(metadataService.getProtocolMetadataById(documentProtocol.id));
                UserProtocol userProtocol = doc.getProtocolStack().peek();
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
