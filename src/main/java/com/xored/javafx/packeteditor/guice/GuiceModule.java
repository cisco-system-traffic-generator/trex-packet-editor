package com.xored.javafx.packeteditor.guice;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.xored.javafx.packeteditor.controllers.FieldEditorController;
import com.xored.javafx.packeteditor.controllers.PacketUndoController;
import com.xored.javafx.packeteditor.data.BinaryData;
import com.xored.javafx.packeteditor.data.FieldEditorModel;
import com.xored.javafx.packeteditor.data.IBinaryData;
import com.xored.javafx.packeteditor.data.PacketDataController;
import com.xored.javafx.packeteditor.guice.provider.FXMLLoaderProvider;
import com.xored.javafx.packeteditor.scapy.ScapyServerClient;
import com.xored.javafx.packeteditor.service.IMetadataService;
import com.xored.javafx.packeteditor.service.LocalFileMetadataService;
import com.xored.javafx.packeteditor.view.FieldEditorView;
import javafx.fxml.FXMLLoader;

public class GuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(FXMLLoader.class).toProvider(FXMLLoaderProvider.class);
        bind(IBinaryData.class).to(BinaryData.class).in(Singleton.class);
        bind(ScapyServerClient.class).in(Singleton.class);
        bind(PacketDataController.class).in(Singleton.class);
        bind(FieldEditorModel.class).in(Singleton.class);
        bind(EventBus.class).in(Singleton.class);
        bind(FieldEditorController.class).in(Singleton.class);
        bind(FieldEditorView.class).in(Singleton.class);
        bind(IMetadataService.class).to(LocalFileMetadataService.class).in(Singleton.class);
        bind(PacketUndoController.class).in(Singleton.class);
    }

}
