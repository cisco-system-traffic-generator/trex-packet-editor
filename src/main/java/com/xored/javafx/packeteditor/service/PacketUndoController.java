package com.xored.javafx.packeteditor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Stack;
import java.util.function.Consumer;

/**
 * Manages application undo records
 */
public class PacketUndoController<T> {
    private Logger logger = LoggerFactory.getLogger(PacketUndoController.class);

    Stack<T> undoRecords = new Stack<>();
    Stack<T> redoRecords = new Stack<>();
    Stack<T> undoingFrom = null;
    Stack<T> undoingTo = null;
    Consumer<T> undoLoad;

    public PacketUndoController(Consumer<T> undoLoad) {
        this.undoLoad = undoLoad;

    }
    /** sets a callback to reload undo data */
    public void setUndoLoad(Consumer<T> undoLoad) { this.undoLoad = undoLoad; }

    private void doUndo(Stack<T> from, Stack<T> to) {
        if (from.empty()) {
            logger.debug("Nothing to undo/redo");
            return;
        }
        try {
            undoingFrom = from;
            undoingTo = to;
            undoLoad.accept(from.pop());
        } catch (Exception e) {
            logger.error("undo/redo failed", e);
        } finally {
            undoingFrom = null;
            undoingTo = null;
        }
    }

    /** should be called when modification is done */
    public void beforeContentReplace(T currentState) {
        if (undoingFrom == null) {
            // new user change
            undoRecords.push(currentState);
            redoRecords.clear();
        } else if (undoingFrom != null) {
            // undoing or redoing
            undoingTo.push(currentState);
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

