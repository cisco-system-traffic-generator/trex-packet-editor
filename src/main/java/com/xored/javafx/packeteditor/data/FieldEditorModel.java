package com.xored.javafx.packeteditor.data;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.xored.javafx.packeteditor.events.RebuildViewEvent;
import com.xored.javafx.packeteditor.events.ReloadModelEvent;
import com.xored.javafx.packeteditor.metatdata.FieldMetadata;
import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;
import com.xored.javafx.packeteditor.scapy.FieldData;
import com.xored.javafx.packeteditor.scapy.PacketData;
import com.xored.javafx.packeteditor.scapy.ProtocolData;
import com.xored.javafx.packeteditor.scapy.ScapyPkt;
import com.xored.javafx.packeteditor.service.IMetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class FieldEditorModel {

    private Logger logger= LoggerFactory.getLogger(FieldEditorModel.class);
    
    private Stack<Protocol> protocols = new Stack<>();

    /**
     * Current packet representation in ScapyService format
     */
    ScapyPkt pkt = new ScapyPkt();
    
    @Inject
    EventBus eventBus;

    @Inject
    IBinaryData binary;
    
    @Inject
    PacketDataController packetDataController;
    
    private IMetadataService metadataService;

    public Protocol getCurrentProtocol() {
        try {
            return protocols.peek();
        } catch (EmptyStackException e) {
            return null;
        }
    }
    
    public void deleteAllProtocols() {
        protocols.clear();
        fireUpdateViewEvent();
    }
    
    public void addProtocol(ProtocolMetadata meta) {
        packetDataController.appendProtocol(meta.getId());
        logger.info("Protocol {} added.", meta.getName());
    }
    
    
    public List<ProtocolMetadata> getAvailableProtocolsToAdd() {
        Map<String, ProtocolMetadata>  protocolsMetaMap = metadataService.getProtocols();
        if (protocols.size() == 0) {
            return Arrays.asList(metadataService.getProtocolMetadataById("Ether"));
        }
        return protocols.peek().getMeta().getPayload()
                .stream().map(protocolsMetaMap::get)
                .filter(item -> item != null) // Filter empty entries for undefined Protocols
                .collect(Collectors.toList());
    }
    
    private Protocol buildProtocolFromMeta(ProtocolMetadata meta) {
        Protocol proto = new Protocol(meta, getCurrentPath());
        return proto;
    }
    
    private List<String> getCurrentPath() {
        return protocols.stream().map(Protocol::getId).collect(Collectors.toList());
    }

    public void setMetadataService(IMetadataService metadataService) {
        this.metadataService = metadataService;
    }

    public void removeLast() {
        packetDataController.removeLastProtocol();
    }

    private void fireUpdateViewEvent() {
        eventBus.post(new RebuildViewEvent(protocols));
    }

    public FieldMetadata buildFieldMetaFromScapy(FieldData field) {
        JsonObject dict = field.values_dict;
        final int max_enum_values_to_display = 100; // max sane number of choice enumeration.
        if (dict != null && dict.size() > 0 && dict.size() < max_enum_values_to_display) {
            Map<String, JsonElement> dict_map = dict.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            return new FieldMetadata(field.id, field.id, IField.Type.ENUM, dict_map, null);

        } else {
            return new FieldMetadata(field.id, field.id, IField.Type.STRING, null, null);
        }
    }

    public ProtocolMetadata buildMetadataFromScapyModel(ProtocolData protocol) {
        List<FieldMetadata> fields_metadata = protocol.fields.stream().map(f ->
                buildFieldMetaFromScapy(f)
        ).collect(Collectors.toList());
        List<String> payload = new ArrayList<>();
        return new ProtocolMetadata(protocol.id, protocol.name, fields_metadata, payload);
    }

    @Subscribe
    public void handleReloadModelEvent(ReloadModelEvent e) {
        protocols.clear();
        pkt = e.getPkt();
        PacketData packet = pkt.packet();
        
        binary.setBytes(packet.getPacketBytes());

        for (ProtocolData protocol: packet.getProtocols()) {
            ProtocolMetadata protocolMetadata = metadataService.getProtocolMetadataById(protocol.id);
            if (protocolMetadata == null) {
                 protocolMetadata = buildMetadataFromScapyModel(protocol);
            }
            Protocol protocolObj = buildProtocolFromMeta(protocolMetadata);
            protocols.push(protocolObj);

            Integer protocolOffset = protocol.offset.intValue();
            for (FieldData field: protocol.fields) {
                Field fieldObj = new Field(protocolMetadata.getMetaForField(field.id), getCurrentPath(), protocolOffset, field);
                fieldObj.setOnSetCallback(newValue -> {
                    packetDataController.setFieldValue(fieldObj, newValue);
                    fireUpdateViewEvent();
                });
                protocolObj.getFields().add(fieldObj);
            }
        }
        fireUpdateViewEvent();
    }
    
    public void setSelected(Field field) {
        binary.setSelected(field.getAbsOffset(), field.getLength());
    }
}
