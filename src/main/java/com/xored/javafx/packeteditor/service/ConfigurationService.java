package com.xored.javafx.packeteditor.service;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.io.File;

public class ConfigurationService {
    
    private String host;
    
    private String protocol;
    
    private Integer receiveTimeout;

    private String connectionPort;
    
    private ApplicationMode applicationMode = ApplicationMode.EMBEDDED;

    private String loadLocation = null;

    private String saveLocation = null;

    private String templatesLocation = null;

    public boolean isStandaloneMode() {
        return ApplicationMode.STANDALONE.equals(applicationMode);
    }

    public enum ApplicationMode {
        STANDALONE,
        EMBEDDED
    }

    private static final String  APP_DATA_PATH = File.separator + "TRex" + File.separator + "trex" + File.separator;
    private static final String  TEMPLATES_PATH = "templates" + File.separator;
    private static final boolean OS_IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    public static boolean isNullOrEmpty(String data) {
        return data == null || "".equals(data) || data.isEmpty() || "null".equals(data);
    }

    @Inject
    public ConfigurationService(@Named("SCAPY_CONNECTION_HOST") String defaultConnectionHost,
                                @Named("SCAPY_RECEIVE_TIMEOUT") String defaultRecieveTimeout,
                                @Named("SCAPY_CONNECTION_PORT") String defaultConnectionPort,
                                @Named("SCAPY_CONNECTION_PROTOCOL") String defaultConnectionProtocol) {
        this.receiveTimeout = Integer.valueOf(defaultRecieveTimeout);
        this.host = defaultConnectionHost;
        this.connectionPort = defaultConnectionPort;
        this.protocol = defaultConnectionProtocol;
        String scapyServerEnv = System.getenv("SCAPY_SERVER");
        if (scapyServerEnv != null && scapyServerEnv.contains(":")) {
            String[] parts = scapyServerEnv.split(":");
            host = parts[0];
            connectionPort = parts[1];
        }
    }
    
    public String getConnectionUrl() {
        return protocol + "://" + host + ":" + connectionPort;
    }

    /** deprecated, use setConnectionHost */
    public void setConnectionIP(String ip) {
        this.host = ip;
    }

    public void setConnectionHost(String host) { this.host = host; }

    public void setConnectionPort(String connectionPort) {
        this.connectionPort = connectionPort;
    }

    public String getConnectionHost() {
        return host;
    }

    public String getConnectionPort() {
        return connectionPort;
    }

    public Integer getReceiveTimeout() {
        return receiveTimeout;
    }

    public ApplicationMode getApplicationMode() {
        return applicationMode;
    }

    public void setApplicationMode(ApplicationMode applicationMode) {
        this.applicationMode = applicationMode;
    }

    public String getLoadLocation() {
        return loadLocation;
    }

    public void setLoadLocation(String loadLocation) {
        this.loadLocation = loadLocation;
    }

    public String getSaveLocation() {
        return saveLocation;
    }

    public void setSaveLocation(String saveLocation) {
        this.saveLocation = saveLocation;
    }

    public String getTemplatesLocation() {
        if (isNullOrEmpty(templatesLocation)) {
            String path = System.getProperty( "user.home" );
            if (OS_IS_WINDOWS) {
                if (isNullOrEmpty(System.getenv("LOCALAPPDATA"))) {
                    path = System.getenv("LOCALAPPDATA") ;
                }
            }
            templatesLocation = path + APP_DATA_PATH + TEMPLATES_PATH;
        }
        return templatesLocation;
    }

    public void setTemplatesLocation(String templatesLocation) {
        this.templatesLocation = templatesLocation;
    }

}
