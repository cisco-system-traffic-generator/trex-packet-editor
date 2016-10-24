package com.xored.javafx.packeteditor;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.xored.javafx.packeteditor.controllers.AppController;
import com.xored.javafx.packeteditor.guice.GuiceModule;
import com.xored.javafx.packeteditor.scapy.ConnectionException;
import com.xored.javafx.packeteditor.service.ConfigurationService;
import com.xored.javafx.packeteditor.service.PacketDataService;
import com.xored.javafx.packeteditor.view.ConnectionErrorDialog;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static com.xored.javafx.packeteditor.service.ConfigurationService.ApplicationMode.EMBEDDED;
import static com.xored.javafx.packeteditor.service.ConfigurationService.ApplicationMode.STANDALONE;

public class TRexPacketCraftingTool extends Application {
    
    private static TRexPacketCraftingTool instance = null;

    static Logger log = LoggerFactory.getLogger(TRexPacketCraftingTool.class);
    private Injector injector;
    private Parent parent;
    private SplitPane fieldEditorSplitPane;
    private Insets app_padding;

    public TRexPacketCraftingTool() {
        super();
    }
    public TRexPacketCraftingTool(Injector injector) {
        this.injector = injector;
    }

    public static void main(String[] args) {
        TRexPacketCraftingTool.launch(args);
    }

    public Injector getInjector() {
        return injector;
    }

    public void initialize() throws ConnectionException {
        initServices();
        log.debug("Running app");
        FXMLLoader fxmlLoader = injector.getInstance(FXMLLoader.class);
        fxmlLoader.setLocation(ClassLoader.getSystemResource("com/xored/javafx/packeteditor/controllers/app.fxml"));
        try {
            parent = fxmlLoader.load();
        } catch (IOException e) {
            log.error("Unable to load app.fxml");
        }
    }

    public void initServices() {
        injector.getInstance(PacketDataService.class);
        injector.getInstance(AppController.class).initialize(null, null);
    }

    public static TRexPacketCraftingTool getInstance(Injector injector) {
        if (instance == null) {
            instance = new TRexPacketCraftingTool(injector);
            instance.initialize();
        }
        return instance;
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        injector = Guice.createInjector(new GuiceModule());
        doStart(primaryStage, true, null, null);
    }
    
    public void startAsEmbedded(Stage stage, String ip, String port) throws Exception {
        doStart(stage, false, ip, port);
    }
    
    public ConfigurationService getConfigurationService() {
        return injector.getInstance(ConfigurationService.class);
    }
    
    private void doStart(Stage stage, boolean isStandalone, String ip, String port) throws Exception{
        ConfigurationService configurationService = getConfigurationService();

        if (ip != null && port != null) {
            configurationService.setConnectionPort(port);
            configurationService.setConnectionIP(ip);
        }

        ConfigurationService.ApplicationMode appMode;
        if (isStandalone) {
            appMode = STANDALONE;
        } else {
            appMode = EMBEDDED;
        }
        configurationService.setApplicationMode(appMode);
        try {
            initialize();
        } catch (Exception e) {
            // it's possible to not have a connection to Scapy server.
        }

        AppController appController = injector.getInstance(AppController.class);
        appController.setMainStage(stage);
        
        Scene scene = new Scene(parent);

        if (System.getenv("DEBUG") == null) {
            scene.getStylesheets().add(ClassLoader.getSystemResource("styles/main-narrow.css").toExternalForm());
        } else {
            // use css from source file to utilize JavaFX css auto-reload
            String cssSource = "file://" + new File("src/main/resources/styles/main-narrow.css").getAbsolutePath();
            scene.getStylesheets().add(cssSource);
        }

        stage.setScene(scene);
        stage.setTitle("Packet Crafting Tool");
        stage.show();
        stage.setOnCloseRequest(e -> {
            appController.terminate();
        });
    }

    private void shutdown() {
        ConnectionErrorDialog dialog = new ConnectionErrorDialog();
        dialog.showAndWait();
        System.exit(0);
    }

    public void setInjector(Injector injector) {
        this.injector = injector;
    }
}
