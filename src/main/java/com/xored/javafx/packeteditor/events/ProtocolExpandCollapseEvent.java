package com.xored.javafx.packeteditor.events;

/**
 * Requirement to expand/collapse all titled panes
 */
public class ProtocolExpandCollapseEvent {
    private Action action;

    public enum Action {
        EXPAND_ALL,
        COLLAPSE_ALL,
        EXPAND_ONLY_LAST
    };

    public ProtocolExpandCollapseEvent(Action action) {
        this.action = action;
    }

    public Action getAction() {
        return action;
    }
}

