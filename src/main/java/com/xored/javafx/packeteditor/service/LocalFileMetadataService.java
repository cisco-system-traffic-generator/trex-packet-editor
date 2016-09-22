package com.xored.javafx.packeteditor.service;


import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xored.javafx.packeteditor.controllers.MenuController;
import com.xored.javafx.packeteditor.data.IField;
import com.xored.javafx.packeteditor.metatdata.FieldMetadata;
import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.xored.javafx.packeteditor.data.IField.Type.ENUM;

public class LocalFileMetadataService implements IMetadataService {

    private Logger logger= LoggerFactory.getLogger(MenuController.class); 
    
    private Gson gson = new Gson();
    
    private Map<String, ProtocolMetadata> protocols = new HashMap<>();
    
    
    
    
    @Override
    public Map<String, ProtocolMetadata> getProtocols() {
        return protocols;
    }

    @Override
    public void initialize() {
        try {
            loadMeta();
        } catch (IOException e) {
            logger.error("Unable to init LocalFileMetadataService due to: {}", e.getLocalizedMessage());
        }
    }

    public void loadMeta() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File metadataFile = new File(classLoader.getResource("protocols/protocols.json").getFile());
        BufferedReader br = new BufferedReader(new FileReader(metadataFile));
        List<JsonObject> metadata = Arrays.asList(gson.fromJson(br, JsonObject[].class));
        metadata.stream().forEach((entry) -> {
            List<FieldMetadata> fieldsMeta = new ArrayList<>();
            Iterator<JsonElement> fieldsIt = entry.get("fields").getAsJsonArray().iterator();
            while (fieldsIt.hasNext()) {
                JsonObject field = (JsonObject) fieldsIt.next();

                String fieldId = field.get("id").getAsString();
                String name = field.get("name").getAsString();
                String typeName = field.get("type").getAsString();
                IField.Type type = getTypeByName(typeName);
                Map<String, String> dict = Collections.EMPTY_MAP;

                if (type.equals(ENUM)) {
                    dict = field.getAsJsonObject("dict").entrySet().stream().collect(Collectors.toMap(e -> e.getValue().getAsString(), Map.Entry::getKey));
                }

                fieldsMeta.add(new FieldMetadata(fieldId, name, type, dict));
            }

            List<String> payload = new ArrayList<>();

            Iterator<JsonElement> payloadIt = entry.get("payload").getAsJsonArray().iterator();
            while (payloadIt.hasNext()) {
                payload.add(payloadIt.next().getAsString());
            }

            ProtocolMetadata protocol = new ProtocolMetadata(entry.get("id").getAsString(), entry.get("name").getAsString(), fieldsMeta, payload);
            protocols.put(entry.get("id").getAsString(), protocol);
        });
    }
    
    private IField.Type getTypeByName(String id) {
        switch (id) {
            case "IPv4Address":
                return IField.Type.IPV4ADDRESS;
            case "MACADDRESS":
                return IField.Type.MAC_ADDRESS;
            case "enum":
                return ENUM;
            
            default:
                return IField.Type.STRING;
        }
    }
}
