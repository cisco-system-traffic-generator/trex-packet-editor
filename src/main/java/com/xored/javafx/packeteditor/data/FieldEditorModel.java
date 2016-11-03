package com.xored.javafx.packeteditor.data;

import com.google.common.eventbus.EventBus;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.xored.javafx.packeteditor.data.combined.CombinedField;
import com.xored.javafx.packeteditor.data.combined.CombinedProtocolModel;
import com.xored.javafx.packeteditor.data.user.Document;
import com.xored.javafx.packeteditor.data.user.DocumentFile;
import com.xored.javafx.packeteditor.data.user.UserProtocol;
import com.xored.javafx.packeteditor.events.RebuildViewEvent;
import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;
import com.xored.javafx.packeteditor.scapy.FieldData;
import com.xored.javafx.packeteditor.scapy.PacketData;
import com.xored.javafx.packeteditor.scapy.ReconstructField;
import com.xored.javafx.packeteditor.service.IMetadataService;
import com.xored.javafx.packeteditor.service.PacketDataService;
import com.xored.javafx.packeteditor.service.PacketUndoController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.xored.javafx.packeteditor.data.user.DocumentFile.toPOJO;

public class FieldEditorModel {
    private Logger logger = LoggerFactory.getLogger(FieldEditorModel.class);

    @Inject
    EventBus eventBus;

    @Inject
    IBinaryData binary;
    
    @Inject
    PacketDataService packetDataService;

    @Inject
    IMetadataService metadataService;

    /** abstract user model. contains field values */
    Document userModel = new Document();

    /**
     * Current packet representation in ScapyService format
     */
    PacketData packet = new PacketData();

    /** model, produced using userModel and information from Scapy. user for building UI structure */
    CombinedProtocolModel model = new CombinedProtocolModel();

    public void reset() {
        packet = new PacketData();
        binary.clear();
        deleteAllProtocols();
    }

    public void addFEParameters(CombinedField combinedField) {
        beforeContentReplace();
        userModel.createFEFieldInstruction(combinedField);
        fireUpdateViewEvent();
    }

    public void deleteFEParameters(CombinedField combinedField) {
        beforeContentReplace();
        userModel.deleteFEFieldInstruction(combinedField);
        fireUpdateViewEvent();
    }

    public class DocState {
        public DocumentFile userModel;
        public PacketData packet;
    }

    PacketUndoController<DocState> undoController = new PacketUndoController<>(this::loadUndoState);

    /** compatibility flag. to be removed later */
    boolean binaryMode = false;

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
            try{
                beforeContentReplace();
                userModel.addProtocol(meta);
                if (isBinaryMode()) {
                    setPktAndReload(packetDataService.appendProtocol(packet, meta.getId()));
                } else {
                    setPktAndReload(packetDataService.buildPacket(userModel.buildScapyModel()));
                }
            } catch(Exception e) {
                undo();
                logger.error("Unable to add protocol due to: {}", e);
                userModel.getProtocolStack().pop();
                throw e;
            }
            logger.info("UserProtocol {} added.", meta.getName());
        }
    }


    public List<ProtocolMetadata> getAvailableProtocolsToAdd(boolean getUnsupported) {
        Map<String, ProtocolMetadata>  protocolsMetaMap = metadataService.getProtocols();

        if (model.getProtocolStack().isEmpty()) {
            return Collections.singletonList(metadataService.getProtocolMetadataById("Ether"));
        }

        Set<String> suggested_extensions = metadataService.getAllowedPayloadForProtocol(model.getLastProtocolId()).stream().collect(Collectors.toSet());
        List<ProtocolMetadata> res = new ArrayList<>();
        if (getUnsupported) {
            Map<Boolean, List<ProtocolMetadata>> suggested_proto = protocolsMetaMap.values().stream()
                    .sorted((p1, p2) -> p1.getId().compareTo(p2.getId()))
                    .collect(Collectors.partitioningBy(m -> suggested_extensions.contains(m.getId())));
            // stable sort
            res.addAll(suggested_proto.getOrDefault(true, Collections.emptyList()));
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
        beforeContentReplace();
        if (isBinaryMode()) {
            setPktAndReload(packetDataService.removeLastProtocol(packet));
        } else {
            if (userModel.getProtocolStack().size() > 1) {
                userModel.getProtocolStack().pop();
                setPktAndReload(packetDataService.buildPacket(userModel.buildScapyModel()));
            }
        }
    }

    private void fireUpdateViewEvent() {
        binary.setBytes(packet.getPacketBytes());
        if (isBinaryMode()) {
            model = CombinedProtocolModel.fromScapyData(metadataService, userModel, packet.getProtocols());
        } else {
            model = CombinedProtocolModel.fromUserModel(metadataService, userModel, packet.getProtocols());
        }
        logger.debug("Rebuilding UI model");
        eventBus.post(new RebuildViewEvent(model));
    }

    private void setPktAndReload(PacketData pkt) {
        this.packet = pkt;
        fireUpdateViewEvent();
    }

    public void saveDocumentToFile(File outFile) throws IOException {
        DocumentFile.saveToFile(userModel, outFile);
    }

    /** replace current new user model document with a different(from file/json/template) */
    private void setNewUserModel(Document userModel) {
        this.userModel = userModel;
        userModel.getProtocolStack().forEach(protocol -> protocol.setCollapsed(true));
        setPktAndReload(packetDataService.buildPacket(userModel.buildScapyModel()));
    }

    public void loadTemplate(DocumentFile outFile) {
        setNewUserModel(DocumentFile.fromPOJO(outFile, metadataService));
    }

    public void loadDocumentFromPcapData(PacketData pkt) {
        beforeContentReplace();
        this.packet = pkt;
        importUserModelFromScapy(packet);
        fireUpdateViewEvent();
    }

    public void loadDocumentFromJSON(String jsonBase64) {
        String userModelJSON = new String(Base64.getDecoder().decode(jsonBase64.getBytes()));
        Document newUserModel = DocumentFile.loadFromJSON(userModelJSON, metadataService);
        setNewUserModel(newUserModel);
    }

    public void loadDocumentFromFile(File outFile) throws IOException {
        Document newUserModel = DocumentFile.loadFromFile(outFile, metadataService);
        newUserModel.setCurrentFile(outFile);
        setNewUserModel(newUserModel);
    }

    public void removeLayer(UserProtocol protocol) {
        Stack<UserProtocol>  protocolStack = userModel.getProtocolStack();
        int idx = protocolStack.indexOf(protocol);
        if (idx > 0 && !isBinaryMode()) { // can't remove 1st layer
            beforeContentReplace();
            protocolStack.remove(idx);
            setPktAndReload(packetDataService.buildPacket(userModel.buildScapyModel()));
        }
    }

    public void moveLayerUp(UserProtocol protocol) {
        Stack<UserProtocol>  protocolStack = userModel.getProtocolStack();
        int idx = protocolStack.indexOf(protocol);
        if (idx > 1 && !isBinaryMode()) { // can't remove 1st layer
            beforeContentReplace();
            protocolStack.remove(idx);
            protocolStack.insertElementAt(protocol, idx - 1);
            setPktAndReload(packetDataService.buildPacket(userModel.buildScapyModel()));
        }
    }

    public void moveLayerDown(UserProtocol protocol) {
        Stack<UserProtocol>  protocolStack = userModel.getProtocolStack();
        int idx = protocolStack.indexOf(protocol);
        if (idx > 0 && !isBinaryMode() && idx + 1 < protocolStack.size()) { // can't remove 1st layer
            beforeContentReplace();
            protocolStack.remove(idx);
            protocolStack.insertElementAt(protocol, idx + 1);
            setPktAndReload(packetDataService.buildPacket(userModel.buildScapyModel()));
        }
    }

    private void importUserModelFromScapy(PacketData packet) {
        userModel.clear();

        packet.getProtocols().forEach(protocolData -> {
            userModel.addProtocol(metadataService.getProtocolMetadataById(protocolData.id));
            UserProtocol userProtocol = userModel.getProtocolStack().peek();
            userProtocol.setCollapsed(true);
            protocolData.getFields().forEach(fieldData -> {
                if (fieldData.isPrimitive()) {
                    userProtocol.addField(fieldData.id, fieldData.hvalue);
                }
            });
        });
    }
    
    public void setVmInstructionParameter(FEInstructionParameter instructionParameter, String value) {
        beforeContentReplace();
        userModel.setFEInstructionParameter(instructionParameter, value);

        // TODO: rebuild_pkt with new data;
        PacketData newPkt = packet;
        
        setPktAndReload(newPkt);
    }
    
    public void editField(CombinedField field, ReconstructField newValue) {
        beforeContentReplace();
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
        
        try {
            if (isBinaryMode()) {
                newPkt = packetDataService.reconstructPacketField(packet, field.getProtocol().getPath(), newValue);
            } else {
                newPkt = packetDataService.buildPacket(userModel.buildScapyModel());
            }
        } catch (Exception e) {
            logger.error("Fail to update field {} with new value: {} due to: \"{}\"", field.getId(), newValue.value, e.getMessage());
            userModel.revertLastChanges();
            throw e;
        }
        
        //pkt = newPkt;
        setPktAndReload(newPkt);
    }

    /** sets text value */
    public void editField(CombinedField field, String newValue) {
        if (field.getScapyFieldData() != null && field.getScapyFieldData().getValueExpr() != null) {
            // if original value was expression, which means there are no good representation for it,
            // new string value should be treated as an expression as well. not as a hvalue
            // at least until we do not improve support for h2i/i2h
            editField(field, ReconstructField.setExpressionValue(field.getId(), newValue));
        } else if ("".equals(newValue)) {
            editField(field, ReconstructField.resetValue(field.getId()));
        } else {
            editField(field, ReconstructField.setHumanValue(field.getId(), newValue));
        }
    }

    /** packet bytes were changed with the binary editor */
    public void editPacketBytes(byte[] newBytes) {
        importUserModelFromScapy(packetDataService.reconstructPacketFromBinary(newBytes));
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

    public PacketData getPkt() {
        return packet;
    }

    public void newPacket() {
        userModel.clear();
        packet = new PacketData();
        addProtocol("Ether");
        clearHistory();
    }

    public File getCurrentFile() {
        return userModel.getCurrentFile();
    }

    public void setCurrentFile(File currentFile) {
        this.userModel.setCurrentFile(currentFile);
        clearHistory();
    }

    public void clearAutoFields() {

    }

    public void undo() {
        undoController.undo();
    }

    public void redo() {
        undoController.redo();
    }

    private void clearHistory() {
        undoController.clearHistory();
    }

    /** should be called before changing data in this class. it writes UNDO records */
    private void beforeContentReplace() {
        DocState ds = new DocState();
        ds.packet = packet;
        ds.userModel = toPOJO(userModel);
        undoController.beforeContentReplace(ds);
    }

    /** called by undoController to restore state from undo records */
    private void loadUndoState(DocState docState) {
        beforeContentReplace(); // save data for reverse undo/redo while processing undo/redo
        packet = docState.packet;
        userModel = DocumentFile.fromPOJO(docState.userModel, metadataService);
        fireUpdateViewEvent();
    }

    public Document getUserModel() {
        return userModel;
    }

    public String serialize() {
        return Base64.getEncoder().encodeToString(new Gson().toJson(toPOJO(userModel)).getBytes());
    }
}
