package com.xored.javafx.packeteditor.events;

public class UpdateEtherLayerEvent {
    private String fieldName;
    
    private MacMode mode;
    
    public UpdateEtherLayerEvent(String fieldName, MacMode mode) {
        this.fieldName = fieldName;
        this.mode = mode;
    }

    public String getFieldName() {
        return fieldName;
    }

    public MacMode getMode() {
        return mode;
    }

    public enum MacMode {
        TREX_CONFIG("trex"), PACKET("packet");
        
        String modeName;
        
        MacMode(String modeName) {
            this.modeName = modeName;
        }
    }
}
