package com.xored.javafx.packeteditor.events;

import com.xored.javafx.packeteditor.data.combined.CombinedProtocolModel;

public class RebuildViewEvent {
    private CombinedProtocolModel model;

    public RebuildViewEvent(CombinedProtocolModel model) {
        this.model = model;
    }

    public CombinedProtocolModel getModel() {
        return model;
    }
}
