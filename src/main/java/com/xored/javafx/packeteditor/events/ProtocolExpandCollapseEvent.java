package com.xored.javafx.packeteditor.events;

/**
 * Requirement to expand/collapse all titled panes
 */
public class ProtocolExpandCollapseEvent {
    private boolean expanded;

    public ProtocolExpandCollapseEvent(boolean expanded) {
        this.expanded = expanded;
    }

    public boolean expandState() {
        return expanded;
    }
}

