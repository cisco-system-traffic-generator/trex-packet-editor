package com.xored.javafx.packeteditor.view;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class ConnectionErrorDialog extends Alert{

    
    public ConnectionErrorDialog() {
        super(AlertType.ERROR);

        this.setTitle("Connection error");
        this.setHeaderText(null);
        this.setContentText("Check network status and try again.");

        ButtonType reconnectBtn = new ButtonType("OK");

        this.getButtonTypes().setAll(reconnectBtn);

    }
    public ConnectionErrorDialog(AlertType alertType) {
        super(alertType);
    }
}
