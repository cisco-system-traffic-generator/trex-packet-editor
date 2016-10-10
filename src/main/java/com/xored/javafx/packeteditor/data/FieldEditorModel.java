package com.xored.javafx.packeteditor.data;

import com.google.common.eventbus.EventBus;
import com.google.gson.JsonElement;
import com.google.inject.Inject;
import com.xored.javafx.packeteditor.data.combined.CombinedField;
import com.xored.javafx.packeteditor.data.combined.CombinedProtocolModel;
import com.xored.javafx.packeteditor.data.user.Document;
import com.xored.javafx.packeteditor.data.user.UserProtocol;
import com.xored.javafx.packeteditor.events.RebuildViewEvent;
import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;
import com.xored.javafx.packeteditor.scapy.*;
import com.xored.javafx.packeteditor.service.IMetadataService;
import com.xored.javafx.packeteditor.service.PacketDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class FieldEditorModel {

    private Logger logger= LoggerFactory.getLogger(FieldEditorModel.class);
    
    private Stack<ScapyProtocol> protocols = new Stack<>();

    /**
     * Current packet representation in ScapyService format
     */
    ScapyPkt pkt = new ScapyPkt();

    File currentFile;

    @Inject
    EventBus eventBus;

    @Inject
    IBinaryData binary;
    
    @Inject
    PacketDataService packetDataService;

    @Inject
    IMetadataService metadataService;

    Document userModel = new Document();
    
    Stack<ScapyPkt> undoRecords = new Stack<>();
    
    Stack<ScapyPkt> redoRecords = new Stack<>();
    
    Stack<ScapyPkt> undoingFrom;
    
    Stack<ScapyPkt> undoingTo;

    boolean binaryMode = false;

    public void deleteAllProtocols() {
        protocols.clear();
        userModel.clear();
        fireUpdateViewEvent();
    }

    public void addProtocol(String protocolId) {
        addProtocol(metadataService.getProtocolMetadataById(protocolId));
    }

    /** if true, we rely on binary data and scapy model. Otherwise, userModel is used */
    public boolean isBinaryMode() {
        return binaryMode;
    }

    public void setBinaryMode(boolean binaryMode) {
        this.binaryMode = binaryMode;
    }

    public void addProtocol(ProtocolMetadata meta) {
        if (meta != null) {
            userModel.addProtocol(meta);
            if (isBinaryMode()) {
                setPktAndReload(packetDataService.appendProtocol(pkt, meta.getId()));
            } else {
                setPktAndReload(packetDataService.buildPacket(userModel.asJson()));
            }
            logger.info("UserProtocol {} added.", meta.getName());
        }
    }


    public List<ProtocolMetadata> getAvailableProtocolsToAdd(boolean getUnsupported) {
        Map<String, ProtocolMetadata>  protocolsMetaMap = metadataService.getProtocols();
        if (protocols.size() == 0) {
            return Arrays.asList(metadataService.getProtocolMetadataById("Ether"));
        }

        String lastProtocolId = protocols.peek().getId();
        Set<String> suggested_extensions = metadataService.getAllowedPayloadForProtocol(lastProtocolId).stream().collect(Collectors.toSet());
        List<ProtocolMetadata> res = new ArrayList<>();
        if (getUnsupported) {
            Map<Boolean, List<ProtocolMetadata>> suggested_proto = protocolsMetaMap.values().stream()
                    .sorted((p1, p2) -> p1.getId().compareTo(p2.getId()))
                    .collect(Collectors.partitioningBy(m -> suggested_extensions.contains(m.getId())));
            // stable sort
            res.addAll(suggested_proto.getOrDefault(true, Arrays.asList()));
            //res.addAll(suggested_proto.getOrDefault(false, Arrays.asList()));
        } else {
            res = protocolsMetaMap.values().stream()
                    .filter(m -> suggested_extensions.contains(m.getId()))
                    .sorted((p1, p2) -> p1.getId().compareTo(p2.getId()))
                    .collect(Collectors.toList());
        }
        return res;
    }
    
    private ScapyProtocol buildProtocolFromMeta(ProtocolMetadata meta) {
        return new ScapyProtocol(meta, getCurrentPath());
    }
    
    private List<String> getCurrentPath() {
        return protocols.stream().map(ScapyProtocol::getId).collect(Collectors.toList());
    }

    public void removeLast() {
        undoRecords.push(pkt);
        if (isBinaryMode()) {
            ScapyPkt newPkt = packetDataService.removeLastProtocol(pkt);
            setPktAndReload(newPkt);
        } else {
            if (!userModel.getProtocolStack().isEmpty()) {
                userModel.getProtocolStack().pop();
                ScapyPkt newPkt = packetDataService.buildPacket(userModel.asJson());
                setPktAndReload(newPkt);
            }
        }
    }

    private void fireUpdateViewEvent() {
        CombinedProtocolModel model;
        if (isBinaryMode()) {
            model = CombinedProtocolModel.fromScapyData(metadataService, userModel, pkt.packet().getProtocols());
        } else {
            model = CombinedProtocolModel.fromUserModel(metadataService, userModel, pkt.packet().getProtocols());
        }
        eventBus.post(new RebuildViewEvent(model));
    }

    public void setPktAndReload(ScapyPkt pkt) {
        setPktAndReload(pkt, false);
    }
    public void setPktAndReload(ScapyPkt pkt, Boolean loadUserModel) {
        beforeContentReplace(this.pkt);
        this.pkt = pkt;
        reload(loadUserModel);
    }

    public void reload (Boolean loadUserModel) {
        if(loadUserModel) {
            userModel.clear();
        }
        protocols.clear();
        PacketData packet = pkt.packet();
        
        binary.setBytes(packet.getPacketBytes());

        for (ProtocolData protocol: packet.getProtocols()) {
            ProtocolMetadata protocolMetadata = metadataService.getProtocolMetadata(protocol);
            ScapyProtocol protocolObj = buildProtocolFromMeta(protocolMetadata);
            if(loadUserModel) {
                userModel.addProtocol(protocolMetadata);
            }
            protocols.push(protocolObj);

            Integer protocolOffset = protocol.offset.intValue();
            for (FieldData field: protocol.fields) {
                if (loadUserModel) {
                    userModel.getProtocolStack().peek().addField(field.id, field.hvalue);
                }
                ScapyField fieldObj = new ScapyField(protocolMetadata.getMetaForField(field.id), getCurrentPath(), protocolOffset, field);
                protocolObj.getFields().add(fieldObj);
            }
        }

        fireUpdateViewEvent();
    }
    
    public void editField(CombinedField field, ReconstructField newValue) {
        assert(field.getMeta().getId() == newValue.id);

        List<String> protoPath = field.getProtocol().getPath();
        String fieldId = field.getMeta().getId();

        UserProtocol userProtocol = field.getProtocol().getUserProtocol();
        if (userProtocol != null) {
            if (newValue.isDeleted()) {
                userModel.deleteField(protoPath, fieldId);
            } else if (newValue.isRandom()) {
                FieldData randval = packetDataService.getRandomFieldValue(
                        field.getProtocol().getMeta().getId(),
                        field.getMeta().getId()
                );
                userModel.setFieldValue(protoPath, fieldId, randval.getValue());
            } else if (newValue.hvalue != null){
                userModel.setFieldValue(protoPath, fieldId, newValue.hvalue);
            } else {
                userModel.setFieldValue(protoPath, fieldId, newValue.value);
            }
        } else {
            logger.warn("no userProtocol");
            if (!isBinaryMode()) {
                logger.warn("missing userProtocol");
                return;
            }
        }

        ScapyPkt newPkt;
        if (isBinaryMode()) {
            newPkt = packetDataService.reconstructPacketField(pkt, field.getProtocol().getPath(), newValue);
        } else {
            newPkt = packetDataService.buildPacket(userModel.asJson());
            //newPkt = packetDataService.reconstructPacketFromBinary(newPkt.getBinaryData());
        }
        //pkt = newPkt;
        setPktAndReload(newPkt);
        fireUpdateViewEvent();
    }

    /** sets text value */
    public void editField(CombinedField field, String newValue) {
        if (field.getScapyFieldData() != null && field.getScapyFieldData().getValueExpr() != null) {
            // if original value was expression, which means there are no good representation for it,
            // new string value should be treated as an expression as well. not as a hvalue
            // at least until we do not improve support for h2i/i2h
            editField(field, ReconstructField.setExpressionValue(field.getMeta().getId(), newValue));
        } else {
            editField(field, ReconstructField.setHumanValue(field.getMeta().getId(), newValue));
        }
    }

    public void setSelected(CombinedField field) {
        int absoluteOffset = 0;
        int len = 0;
        if (field != null && field.getScapyFieldData() != null) {
            FieldData fd = field.getScapyFieldData();
            if (fd.hasPosition()) {
                int protoOffset = field.getProtocol().getScapyProtocol().offset.intValue();
                absoluteOffset = protoOffset + fd.getOffset();
                len = fd.getLength();
            }
        }
        binary.setSelected(absoluteOffset, len);
    }

    /** should be called when modification is done */
    public void beforeContentReplace(ScapyPkt oldPkt) {
        if (undoingFrom == null) {
            // new user change
            undoRecords.push(oldPkt);
            redoRecords.clear();
        } else if (undoingFrom != null) {
            // undoing or redoing
            undoingTo.push(oldPkt);
        }
    }

    void doUndo(Stack<ScapyPkt> from, Stack<ScapyPkt> to) {
        if (from.empty()) {
            logger.debug("Nothing to undo/redo");
            return;
        }
        try {
            undoingFrom = from;
            undoingTo = to;
            setPktAndReload(from.pop());
        } catch (Exception e) {
            logger.error("undo/redo failed", e);
        } finally {
            undoingFrom = null;
            undoingTo = null;
        }
        
    }

    public void undo() {
        doUndo(undoRecords, redoRecords);
    }

    public void redo() {
        doUndo(redoRecords, undoRecords);
    }

    /* Reset length and chksum fields
     * type fields can be calculated for layers with payload
     *  */
    public ScapyPkt recalculateAutoValues(ScapyPkt pkt) {
        List<ProtocolData> protocols = pkt.packet().getProtocols();
        List<ReconstructProtocol> modify = protocols.stream().map(
                protocol -> {
                    boolean is_last_layer = protocol == protocols.get(protocols.size() - 1);
                    return ReconstructProtocol.modify(protocol.id, protocol.fields.stream().filter(field ->
                                    field.id.equals("length") ||
                                            field.id.equals("chksum") ||
                                            (field.id.equals("type") && is_last_layer)
                    ).map(f -> ReconstructField.resetValue(f.id)).collect(Collectors.toList()));
                }).collect(Collectors.toList());
        return packetDataService.reconstructPacket(pkt, modify);
    }
    
    private void clearHistory() {
        undoRecords.clear();
        redoRecords.clear();
    }

    public ScapyPkt getPkt() {
        return pkt;
    }

    public void newPacket() {
        clearHistory();
        userModel.clear();
        pkt = new ScapyPkt();
        addProtocol("Ether");
    }

    public File getCurrentFile() {
        return currentFile;
    }

    public void setCurrentFile(File currentFile) {
        this.currentFile = currentFile;
        clearHistory();
    }

}
