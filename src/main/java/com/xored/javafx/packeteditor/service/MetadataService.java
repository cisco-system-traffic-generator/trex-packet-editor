package com.xored.javafx.packeteditor.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.xored.javafx.packeteditor.data.IField;
import com.xored.javafx.packeteditor.metatdata.FieldMetadata;
import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;
import com.xored.javafx.packeteditor.scapy.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/** provides protocol metadata based on hand-crafted protocol definition file or Scapy definitions */
public class MetadataService implements IMetadataService {
    private static Logger logger = LoggerFactory.getLogger(IMetadataService.class);
    final int max_enum_values_to_display = 100; // max sane number of choice enumeration.

    @Inject
    LocalFileMetadataService localFileMetadataService;

    @Inject
    ScapyServerClient scapy;

    Map<String, ProtocolMetadata> protocols = new HashMap<>();
    Map<String, List<String>> payload_classes_cache = new HashMap<>();

    public void initialize() {
        localFileMetadataService.initialize();
        protocols.putAll(localFileMetadataService.getProtocols());
        loadProtocolDefinitionsFromScapy();
    }

    public Map<String, ProtocolMetadata> getProtocols() {
        return protocols;
    }

    public void loadProtocolDefinitionsFromScapy() {
        try {
            scapy.get_definitions().protocols.forEach(proto -> {
                // TODO: merge definitions from json and from file ( especially enums )
                if (!this.protocols.containsKey(proto.id)) {
                    protocols.put(proto.id, new ProtocolMetadata(
                            proto.id,
                            proto.name,
                            proto.fields.stream().map(this::buildFieldMetadata).collect(Collectors.toList())
                    ));
                }
            });
        } catch (Exception e) {
            logger.error("failed to load protocol defs from scapy: {}", e);
        }
    }

    /** builds field metadata from scapy field definition */
    private FieldMetadata buildFieldMetadata(ScapyDefinitions.Field field) {
        IField.Type ftype = IField.Type.STRING;
        Map<String, JsonElement> dict_map = null;
        if (field.values_dict instanceof JsonArray) {
            ftype = IField.Type.ENUM;
            dict_map = field.values_dict.getAsJsonObject().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        return new FieldMetadata(field.id, field.name, ftype, dict_map, null);
    }

    /** normally should not be used, since we should have get_definitions */
    private ProtocolMetadata buildProtocolMetaFromScapy(String protocolId) {
        PacketData pkt = scapy.build_pkt(Arrays.asList(ReconstructProtocol.pass(protocolId)));
        if (pkt.getProtocols().isEmpty())
            return null;
        return buildMetadataFromScapyModel(pkt.getProtocols().get(0));
    }

    /** normally should not be used, since we should have get_definitions */
    private ProtocolMetadata buildMetadataFromScapyModel(ProtocolData protocol) {
        List<FieldMetadata> fields_metadata = protocol.fields.stream().map(this::buildFieldMetaFromScapy).collect(Collectors.toList());
        ProtocolMetadata protocolMetadata = new ProtocolMetadata(protocol.id, protocol.name, fields_metadata);
        protocols.put(protocolMetadata.getId(), protocolMetadata);
        return protocolMetadata;
    }

    public ProtocolMetadata getProtocolMetadata(ProtocolData protocol) {
        return getProtocolMetadataById(protocol.id);
    }

    private FieldMetadata buildFieldMetaFromScapy(FieldData field) {
        JsonObject dict = field.values_dict;
        if (dict != null && dict.size() > 0 && dict.size() < max_enum_values_to_display) {
            Map<String, JsonElement> dict_map = dict.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            return new FieldMetadata(field.id, field.id, IField.Type.ENUM, dict_map, null);

        } else {
            return new FieldMetadata(field.id, field.id, IField.Type.STRING, null, null);
        }
    }

    @Override
    public ProtocolMetadata getProtocolMetadataById(String protocolId) {
        ProtocolMetadata res = protocols.getOrDefault(protocolId, null);
        if (res == null) {
            logger.warn("Generating definition for {} protocol", protocolId);
            // TODO: parametrized get_definitions?
            res = buildProtocolMetaFromScapy(protocolId);
        }
        return res;
    }

    @Override
    public List<String> getAllowedPayloadForProtocol(String protocolId) {
        List<String> res = payload_classes_cache.getOrDefault(protocolId, null);
        if (res == null) {
            // Too slow, so getting lazily with cache
            res = scapy.get_payload_classes(protocolId);
            payload_classes_cache.put(protocolId, res);
        }
        return res;
    }
}

