package com.xored.javafx.packeteditor;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.xored.javafx.packeteditor.guice.GuiceModule;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TRexPacketCraftingTool extends Application {
    static Logger log = LoggerFactory.getLogger(TRexPacketCraftingTool.class);

    public static void main(String[] args) {
        TRexPacketCraftingTool.launch(args);
    }

    Injector injector = Guice.createInjector(new GuiceModule());

    public Injector getInjector() {
        return injector;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        log.debug("Running app");
        FXMLLoader fxmlLoader = injector.getInstance(FXMLLoader.class);
        fxmlLoader.setLocation(ClassLoader.getSystemResource("com/xored/javafx/packeteditor/controllers/app.fxml"));
        Parent parent = fxmlLoader.load();
        Scene scene = new Scene(parent);
        scene.getStylesheets().add(ClassLoader.getSystemResource("styles/main-narrow.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle("Packet Crafting Tool");
        primaryStage.show();
        primaryStage.setOnCloseRequest(e -> System.exit(0));
    }
}
