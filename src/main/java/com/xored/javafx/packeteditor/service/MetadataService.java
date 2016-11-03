package com.xored.javafx.packeteditor.service;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.xored.javafx.packeteditor.data.FieldRules;
import com.xored.javafx.packeteditor.events.ScapyClientConnectedEvent;
import com.xored.javafx.packeteditor.metatdata.BitFlagMetadata;
import com.xored.javafx.packeteditor.metatdata.FEInstructionParameterMeta;
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
        // TODO: delete me once protocols json moved to scapy server.
    }
    
    @Subscribe
    public void handleScapyConnectedEvent(ScapyClientConnectedEvent event) {
        loadProtocolDefinitions();
    }

    public Map<String, ProtocolMetadata> getProtocols() {
        return protocols;
    }

    private void loadProtocolDefinitions() {
        localFileMetadataService.initialize();
        try {
            // Stub shold be removed once scapy server started support get_instruction_parameters_defs
            Map<String, FEInstructionParameterMeta> feInstructionParameterMetas = scapy.get_instruction_parameters_defs().feInstructionParameters.stream()
                    .filter(param -> param.id != null)
                    .collect(Collectors.toMap(param -> param.id, param -> new FEInstructionParameterMeta(param.type, param.id, param.name, param.defaultValue, param.dict)));
            
            scapy.get_definitions().protocols.forEach(proto -> {
                // merge definitions with the hand-crafted file. json has priority over metadata from scapy
                ProtocolMetadata jsonProtocol = localFileMetadataService.getProtocols().getOrDefault(proto.id, null);
                
                ProtocolMetadata protocolMeta = new ProtocolMetadata(
                    proto.id,
                    jsonProtocol != null ? jsonProtocol.getName() : proto.name,
                    proto.fields.stream().map(field -> buildFieldMetadata(field, jsonProtocol)).collect(Collectors.toList()),
                    feInstructionParameterMetas,
                    proto.fieldEngineAwareFields
                ); 
                protocols.put(proto.id, protocolMeta);
            });
        } catch (Exception e) {
            logger.error("failed to load protocol defs from scapy: {}", e);
        }
    }

    /** builds field metadata from scapy field definition */
    private FieldMetadata buildFieldMetadata(ScapyDefinitions.ScapyField field, ProtocolMetadata jsonDefaults) {
        String fieldName = field.name;
        FieldMetadata.FieldType ftype = FieldMetadata.FieldType.STRING;
        Map<String, JsonElement> dict_map = null;
        List<BitFlagMetadata> bits_meta = null;
        boolean isAuto = false;
        FieldRules rules = null;

        // merging definitions
        if (jsonDefaults != null) {
            FieldMetadata jsonFieldMeta = jsonDefaults.getMetaForFieldOrNull(field.id);
            if (jsonFieldMeta != null) {
                fieldName = jsonFieldMeta.getName();
                dict_map = jsonFieldMeta.getDictionary();
                ftype = jsonFieldMeta.getType();
                bits_meta = jsonFieldMeta.getBits();
                isAuto = jsonFieldMeta.isAuto();
                rules = jsonFieldMeta.getFieldRules();
            }
        }
        if (dict_map == null && field.values_dict instanceof JsonObject) {
            JsonObject field_dict = field.values_dict.getAsJsonObject();
            if (field_dict.size() > 0) {
                ftype = FieldMetadata.FieldType.ENUM;
                dict_map = field.values_dict.getAsJsonObject().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }
        }
        return new FieldMetadata(field.id, fieldName, ftype, dict_map, bits_meta, isAuto, rules);
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
        List<FieldMetadata> fields_metadata = protocol.fields.stream().map(
                field -> new FieldMetadata(field.id, field.id, FieldMetadata.FieldType.STRING, null, null, false)
        ).collect(Collectors.toList());
        ProtocolMetadata protocolMetadata = new ProtocolMetadata(protocol.id, protocol.name, fields_metadata, Collections.<String, FEInstructionParameterMeta>emptyMap(), new ArrayList<>());
        protocols.put(protocolMetadata.getId(), protocolMetadata);
        return protocolMetadata;
    }

    @Deprecated
    public ProtocolMetadata getProtocolMetadata(ProtocolData protocol) {
        return getProtocolMetadataById(protocol.id);
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

