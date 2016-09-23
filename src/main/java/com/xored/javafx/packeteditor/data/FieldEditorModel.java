package com.xored.javafx.packeteditor.data;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.xored.javafx.packeteditor.events.RebuildViewEvent;
import com.xored.javafx.packeteditor.events.ReloadModelEvent;
import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;
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
        
        binary.setBytes(pkt.getBinaryData());
        Iterator it = pkt.getProtocols().iterator();
        while(it.hasNext()) {
            JsonObject protocol = (JsonObject) it.next();
            String protocolId = protocol.get("id").getAsString();
            ProtocolMetadata protocolMetadata = metadataService.getProtocolMetadataById(protocolId);
            
            Protocol protocolObj = buildProtocolFromMeta(protocolMetadata);
            protocols.push(protocolObj);
            
            JsonArray fields = protocol.getAsJsonArray("fields");
            Iterator fieldsIt = fields.iterator();
            Integer protocolOffset = protocol.get("offset").getAsInt();
            while (fieldsIt.hasNext()) {
                JsonObject field =(JsonObject) fieldsIt.next();
                String fieldId = field.get("id").getAsString();
                Integer offset = field.get("offset").getAsInt();
                Integer length = field.get("length").getAsInt();
                // field.get("value") this value can be string, numeric or potentially json object. it is a value from Scapy as is
                String hvalue = field.get("hvalue").getAsString(); // human-representation of a Scapy value. similar to what we get with show2
                JsonElement value = field.get("value"); // human-representation of a Scapy value. similar to what we get with show2
                
                Field fieldObj = new Field(protocolMetadata.getMetaForField(fieldId), getCurrentPath(), offset, length, protocolOffset, hvalue, value);
                fieldObj.setOnSetCallback(newValue -> {
                    packetDataController.modifyPacketField(fieldObj, newValue);
                    fireUpdateViewEvent();
                });
                fieldObj.setPath(getCurrentPath());
                protocolObj.getFields().add(fieldObj);
            }
            
        }
        fireUpdateViewEvent();
    }
}
