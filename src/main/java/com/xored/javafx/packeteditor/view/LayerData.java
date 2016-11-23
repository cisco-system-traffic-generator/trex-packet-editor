package com.xored.javafx.packeteditor.view;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TitledPane;

import java.util.List;

public interface LayerData {
    String getLayerId();
    String getTitle();
    String getStyleClass();
    List<Node> getRows();
    ContextMenu getContextMenu();
    boolean isCollapsed();
    void setCollapsed(boolean collapsed);
    void configureLayerExpandCollapse(TitledPane layerPane);
}
