package com.xored.javafx.packeteditor.data;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.xored.javafx.packeteditor.events.ProtocolEvent;
import com.xored.javafx.packeteditor.metatdata.FieldMetadata;
import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FieldEditorModel {

    private Logger logger= LoggerFactory.getLogger(FieldEditorModel.class);

    @Inject
    EventBus eventBus;
    
    public void addProtocol(ProtocolMetadata meta) {
        Protocol protocol = buildProrocolFromMeta(meta);
        logger.info("Protocol {} added.", meta.getName());
        eventBus.post(new ProtocolEvent(ProtocolEvent.Action.ADD, meta, protocol));
    }
    
    private Protocol buildProrocolFromMeta(ProtocolMetadata meta) {
        Protocol proto = new Protocol(meta);
        return proto;
    }

    public void updateField(FieldMetadata meta, Object value) {
        logger.info("{} field updated.", meta.getName());
    }
}
