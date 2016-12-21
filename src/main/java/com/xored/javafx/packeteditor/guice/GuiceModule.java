package com.xored.javafx.packeteditor.guice;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.xored.javafx.packeteditor.TRexPacketCraftingTool;
import com.xored.javafx.packeteditor.controllers.*;
import com.xored.javafx.packeteditor.data.BinaryData;
import com.xored.javafx.packeteditor.data.IBinaryData;
import com.xored.javafx.packeteditor.data.PacketEditorModel;
import com.xored.javafx.packeteditor.guice.provider.FXMLLoaderProvider;
import com.xored.javafx.packeteditor.scapy.ScapyServerClient;
import com.xored.javafx.packeteditor.service.ConfigurationService;
import com.xored.javafx.packeteditor.service.IMetadataService;
import com.xored.javafx.packeteditor.service.MetadataService;
import com.xored.javafx.packeteditor.service.PacketDataService;
import com.xored.javafx.packeteditor.view.FieldEditorView;
import com.xored.javafx.packeteditor.view.FieldEngineView;
import javafx.fxml.FXMLLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;
import java.util.ResourceBundle;

public class GuiceModule extends AbstractModule {
    public static Logger logger = LoggerFactory.getLogger(GuiceModule.class);

    @Override
    protected void configure() {
        bind(FXMLLoader.class).toProvider(FXMLLoaderProvider.class);
        bind(IBinaryData.class).to(BinaryData.class).in(Singleton.class);
        bind(ConfigurationService.class).in(Singleton.class);
        bind(ScapyServerClient.class).in(Singleton.class);
        bind(PacketDataService.class).in(Singleton.class);
        bind(PacketEditorModel.class).in(Singleton.class);
        bind(EventBus.class).in(Singleton.class);
        bind(MenuControllerEditor.class).in(Singleton.class);
        bind(MenuControllerEngine.class).in(Singleton.class);
        bind(FieldEditorController.class).in(Singleton.class);
        bind(FieldEngineController.class).in(Singleton.class);
        bind(FieldEditorView.class).in(Singleton.class);
        bind(FieldEngineView.class).in(Singleton.class);
        bind(AppController.class).in(Singleton.class);
        bind(IMetadataService.class).to(MetadataService.class).in(Singleton.class);

        bind(ResourceBundle.class)
                .annotatedWith(Names.named("resources"))
                .toInstance(ResourceBundle.getBundle(TRexPacketCraftingTool.class.getName()));
        
        Names.bindProperties(binder(), loadProperties());
    }

    private Properties loadProperties() {
        Properties properties = new Properties();
        try {
            properties.load(ScapyServerClient.class.getResourceAsStream("scapy_config.properties"));
        } catch (IOException e) {
            logger.error("Unable to load config file. Due to: {}", e);
        }
        return properties;
    }
}
