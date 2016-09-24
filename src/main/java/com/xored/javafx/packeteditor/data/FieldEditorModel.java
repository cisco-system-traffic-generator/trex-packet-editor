package com.xored.javafx.packeteditor.data;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.xored.javafx.packeteditor.events.RebuildViewEvent;
import com.xored.javafx.packeteditor.events.ReloadModelEvent;
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
        if(!protocols.empty()) {
            packetDataController.removeLastProtocol();
        }
    }

    private void fireUpdateViewEvent() {
        eventBus.post(new RebuildViewEvent(protocols));
    }

    @Subscribe
    public void handleReloadModelEvent(ReloadModelEvent e) {
        protocols.clear();
        pkt = e.getPkt();
        PacketData packet = pkt.packet();
        
        binary.setBytes(packet.getPacketBytes());

        for (ProtocolData protocol: packet.getProtocols()) {
            ProtocolMetadata protocolMetadata = metadataService.getProtocolMetadataById(protocol.id);
            Protocol protocolObj = buildProtocolFromMeta(protocolMetadata);
            protocols.push(protocolObj);

            Integer protocolOffset = protocol.offset.intValue();
            for (FieldData field: protocol.fields) {
                Field fieldObj = new Field(protocolMetadata.getMetaForField(field.id), getCurrentPath(), protocolOffset, field);
                fieldObj.setOnSetCallback(newValue -> {
                    packetDataController.setFieldValue(fieldObj, newValue);
                    fireUpdateViewEvent();
                });
                fieldObj.setPath(getCurrentPath());
                protocolObj.getFields().add(fieldObj);
            }
        }
        fireUpdateViewEvent();
    }
}
