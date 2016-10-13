package com.xored.javafx.packeteditor.data;

import com.google.common.eventbus.EventBus;
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
    private Logger logger = LoggerFactory.getLogger(FieldEditorModel.class);

    /**
     * Current packet representation in ScapyService format
     */
    PacketData packet = new PacketData();

    @Inject
    EventBus eventBus;

    @Inject
    IBinaryData binary;
    
    @Inject
    PacketDataService packetDataService;

    @Inject
    IMetadataService metadataService;

    Stack<PacketData> undoRecords = new Stack<>();
    Stack<PacketData> redoRecords = new Stack<>();
    Stack<PacketData> undoingFrom;
    Stack<PacketData> undoingTo;

    /** abstract user model. contains field values */
    Document userModel = new Document();

    boolean binaryMode = false;
    File currentFile;

    /** model, produced using userModel and information from Scapy. user for building UI structure */
    CombinedProtocolModel model = new CombinedProtocolModel();

    public void deleteAllProtocols() {
        userModel.clear();
        model = new CombinedProtocolModel();
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
                setPktAndReload(packetDataService.appendProtocol(packet, meta.getId()));
            } else {
                setPktAndReload(packetDataService.buildPacket(userModel.buildScapyModel()));
            }
            logger.info("UserProtocol {} added.", meta.getName());
        }
    }


    public List<ProtocolMetadata> getAvailableProtocolsToAdd(boolean getUnsupported) {
        Map<String, ProtocolMetadata>  protocolsMetaMap = metadataService.getProtocols();

        if (model.getProtocolStack().isEmpty()) {
            return Arrays.asList(metadataService.getProtocolMetadataById("Ether"));
        }

        Set<String> suggested_extensions = metadataService.getAllowedPayloadForProtocol(model.getLastProtocolId()).stream().collect(Collectors.toSet());
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
    
    public void removeLast() {
        undoRecords.push(packet);
        if (isBinaryMode()) {
            setPktAndReload(packetDataService.removeLastProtocol(packet));
        } else {
            if (!userModel.getProtocolStack().isEmpty()) {
                userModel.getProtocolStack().pop();
                setPktAndReload(packetDataService.buildPacket(userModel.buildScapyModel()));
            }
        }
    }

    private void fireUpdateViewEvent() {
        if (isBinaryMode()) {
            model = CombinedProtocolModel.fromScapyData(metadataService, userModel, packet.getProtocols());
        } else {
            model = CombinedProtocolModel.fromUserModel(metadataService, userModel, packet.getProtocols());
        }
        eventBus.post(new RebuildViewEvent(model));
    }

    public void setPktAndReload(PacketData pkt) {
        setPktAndReload(pkt, false);
    }
    public void setPktAndReload(PacketData pkt, Boolean loadUserModel) {
        beforeContentReplace(this.packet);
        this.packet = pkt;
        reload(loadUserModel);
    }

    public void reload (Boolean loadUserModel) {
        if(loadUserModel) {
            importUserModelFromScapy(packet);
        }
        binary.setBytes(packet.getPacketBytes());
        fireUpdateViewEvent();
    }

    private void importUserModelFromScapy(PacketData packet) {
        userModel.clear();

        packet.getProtocols().forEach(protocolData -> {
            userModel.addProtocol(metadataService.getProtocolMetadataById(protocolData.id));
            UserProtocol userProtocol = userModel.getProtocolStack().peek();
            protocolData.getFields().forEach(fieldData -> {
                if (fieldData.isPrimitive()) {
                    userProtocol.addField(fieldData.id, fieldData.hvalue);
                }
            });
        });
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

        PacketData newPkt;
        if (isBinaryMode()) {
            newPkt = packetDataService.reconstructPacketField(packet, field.getProtocol().getPath(), newValue);
        } else {
            newPkt = packetDataService.buildPacket(userModel.buildScapyModel());
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
    public void beforeContentReplace(PacketData oldPkt) {
        if (undoingFrom == null) {
            // new user change
            undoRecords.push(oldPkt);
            redoRecords.clear();
        } else if (undoingFrom != null) {
            // undoing or redoing
            undoingTo.push(oldPkt);
        }
    }

    void doUndo(Stack<PacketData> from, Stack<PacketData> to) {
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

    private void clearHistory() {
        undoRecords.clear();
        redoRecords.clear();
    }

    public PacketData getPkt() {
        return packet;
    }

    public void newPacket() {
        clearHistory();
        userModel.clear();
        packet = new PacketData();
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
