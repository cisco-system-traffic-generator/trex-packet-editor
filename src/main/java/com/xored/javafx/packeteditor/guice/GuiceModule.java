package com.xored.javafx.packeteditor.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.xored.javafx.packeteditor.data.BinaryData;
import com.xored.javafx.packeteditor.data.IBinaryData;
import com.xored.javafx.packeteditor.guice.provider.FXMLLoaderProvider;
import javafx.fxml.FXMLLoader;

public class GuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(FXMLLoader.class).toProvider(FXMLLoaderProvider.class);
        bind(IBinaryData.class).to(BinaryData.class).in(Singleton.class);
    }

}
