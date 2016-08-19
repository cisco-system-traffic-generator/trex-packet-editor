package com.xored.javafx.packeteditor;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.xored.javafx.packeteditor.guice.GuiceModule;

import java.io.IOException;
import java.io.InputStream;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class JavaFXBinaryPacketEditor extends Application {

    public static void main(String[] args) {
        JavaFXBinaryPacketEditor.launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Injector injector = Guice.createInjector(new GuiceModule());
        FXMLLoader fxmlLoader = injector.getInstance(FXMLLoader.class);
        fxmlLoader.setLocation(ClassLoader.getSystemResource("com/xored/javafx/packeteditor/gui/JavaFXBinaryPacketEditor.fxml"));
        Parent parent = fxmlLoader.load();
        primaryStage.setScene(new Scene(parent));
        primaryStage.setTitle("JavaFX 8 Binary Packet Editor");
        primaryStage.show();
        primaryStage.setOnCloseRequest(e -> {
            System.exit(0);
        });
    }
}
