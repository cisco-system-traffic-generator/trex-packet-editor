package com.xored.javafx.packeteditor.data.user;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.xored.javafx.packeteditor.service.IMetadataService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serializes Document to POJO and JSON file
 */
public class DocumentFile {
    public static final String FILE_EXTENSION = ".trp";
    public static class DocumentField {
        public String id;
        public JsonElement value;

        public DocumentField(String id, JsonElement value) {
            this.id = id;
            this.value = value;
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
                protocol -> new DocumentProtocol(protocol.getId(), protocol.getSetFields().stream().map(
                        field -> new DocumentField(field.getId(), field.getValue())
                ).collect(Collectors.toList()))
        ).collect(Collectors.toList());
        return data;
    }

    public static Document fromPOJO(DocumentFile data, IMetadataService metadataService) {
        Document doc = new Document();
        doc.setMetadata(data.metadata);
        data.packet.forEach(
                protocol -> {
                    doc.addProtocol(metadataService.getProtocolMetadataById(protocol.id));
                    protocol.fields.forEach(
                            field -> {
                                doc.getProtocolStack().peek().getField(field.id).setValue(field.value);
                            }
                    );
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
