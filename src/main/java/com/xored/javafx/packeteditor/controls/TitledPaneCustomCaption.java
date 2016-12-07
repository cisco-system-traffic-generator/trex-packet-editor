package com.xored.javafx.packeteditor.controls;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Pane;

/**
 * Created by igor on 12/5/16.
 */
public class TitledPaneCustomCaption extends TitledPane {

    public TitledPaneCustomCaption() {
        super();
//        hideCaption();
    }

    public TitledPaneCustomCaption(String titleText, Node node) {
        super(titleText, node);
//        hideCaption();
    }

    private void hideCaption() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                // title region
                Node titleRegion = lookup(".title");
                if (titleRegion != null) {
                    Node label = titleRegion.lookup(".text");
                    if (label != null) {
                        label.setVisible(false);
                        label.setManaged(false);
                    }
                    titleRegion.setVisible(false);
                    titleRegion.setManaged(false);
                    ((Pane)titleRegion).setMinHeight(0.0);
                    ((Pane)titleRegion).setPrefHeight(0.0);
                    ((Pane)titleRegion).setMaxHeight(0.0);
                }
            }
        });
    }

}
