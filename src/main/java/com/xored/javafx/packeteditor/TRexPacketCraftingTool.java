package com.xored.javafx.packeteditor;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.xored.javafx.packeteditor.controllers.AppController;
import com.xored.javafx.packeteditor.controllers.FieldEditorController;
import com.xored.javafx.packeteditor.guice.GuiceModule;
import com.xored.javafx.packeteditor.scapy.ConnectionException;
import com.xored.javafx.packeteditor.scapy.ScapyServerClient;
import com.xored.javafx.packeteditor.service.ConfigurationService;
import com.xored.javafx.packeteditor.service.PacketDataService;
import com.xored.javafx.packeteditor.view.ConnectionErrorDialog;
import com.xored.javafx.packeteditor.view.FieldEditorView;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.xored.javafx.packeteditor.service.ConfigurationService.ApplicationMode.STANDALONE;

public class TRexPacketCraftingTool extends Application {
    
    private static TRexPacketCraftingTool instance = null;

    static Logger log = LoggerFactory.getLogger(TRexPacketCraftingTool.class);
    private Injector injector;
    private Parent parent;

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
        initAppController();
        injector.getInstance(PacketDataService.class);
        injector.getInstance(ScapyServerClient.class).connect();
        log.debug("Running app");
        FXMLLoader fxmlLoader = injector.getInstance(FXMLLoader.class);
        fxmlLoader.setLocation(ClassLoader.getSystemResource("com/xored/javafx/packeteditor/controllers/app.fxml"));
        try {
            parent = fxmlLoader.load();
        } catch (IOException e) {
            log.error("Unable to load app.fxml");
        }
    }

    public void initAppController() {
        injector.getInstance(AppController.class);
    }

    public static TRexPacketCraftingTool getInstance(Injector injector) {
        if (instance == null) {
            instance = new TRexPacketCraftingTool(injector);
            instance.initialize();
        }
        return instance;
    }
    
    @Override
    public void start(Stage stage) throws Exception {
        injector = Guice.createInjector(new GuiceModule());
        ConfigurationService configurationService = getConfigurationService();

        configurationService.setApplicationMode(STANDALONE);
        try {
            initialize();
        } catch (ConnectionException e) {
            log.error("Unable to connect to Scapy server. Critical issue. Exit application.");
            ConnectionErrorDialog dialog = new ConnectionErrorDialog();
            dialog.showAndWait();
            System.exit(0);
        }

        AppController appController = injector.getInstance(AppController.class);
        appController.setMainStage(stage);

        Scene scene = new Scene(parent);

        injector.getInstance(FieldEditorController.class).initAcceleratorsHandler(scene);
        
        FieldEditorView.initCss(scene);

        stage.setScene(scene);
        stage.setTitle("Packet Crafting Tool");
        stage.show();
        stage.setOnCloseRequest(e -> {
            appController.terminate();
        });
    }
    
    public ConfigurationService getConfigurationService() {
        return injector.getInstance(ConfigurationService.class);
    }
    
    private void doStart(Stage stage, boolean isStandalone, String ip, String port) throws Exception{
        
    }

    public void setInjector(Injector injector) {
        this.injector = injector;
    }
}
