package com.xored.javafx.packeteditor.controllers;

import com.google.inject.Inject;
import com.xored.javafx.packeteditor.data.PacketDataController;
import com.xored.javafx.packeteditor.scapy.ScapyPkt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Stack;

/**
 * Handles Undo
 */
public class PacketUndoController {
    static Logger log = LoggerFactory.getLogger(PacketDataController.class);

    @Inject
    PacketDataController packetDataController;

    Stack<ScapyPkt> undoRecords = new Stack<>();
    Stack<ScapyPkt> redoRecords = new Stack<>();
    Stack<ScapyPkt> undoingFrom;
    Stack<ScapyPkt> undoingTo;


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
            log.debug("Nothing to undo/redo");
            return;
        }
        try {
            undoingFrom = from;
            undoingTo = to;
            ScapyPkt pkt = from.pop();
            packetDataController.replacePacket(pkt);
        } catch (Exception e) {
            log.error("undo/redo failed", e);
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

    public void clearHistory() {
        undoRecords.clear();
        redoRecords.clear();
    }

}
