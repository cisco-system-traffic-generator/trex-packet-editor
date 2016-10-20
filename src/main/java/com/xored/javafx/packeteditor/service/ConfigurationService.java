package com.xored.javafx.packeteditor.service;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class ConfigurationService {
    
    private String connectionUrl;
    
    private String scapyDefaultConnUrl;
    
    private Integer receiveTimeout;

    private String connectionPort;
    
    private ApplicationMode applicationMode;
    
    public enum ApplicationMode {
        STANDALONE,
        EMBEDDED
    }

    @Inject
    public ConfigurationService(@Named("SCAPY_DEFAULT_CONN_URL") String scapyDefaultConnUrl,
                                @Named("SCAPY_RECEIVE_TIMEOUT") String defaultRecieveTimeout,
                                @Named("SCAPY_CONNECTION_PORT") String defaultConnectionPort) {
        this.receiveTimeout = Integer.valueOf(defaultRecieveTimeout);
        this.scapyDefaultConnUrl = scapyDefaultConnUrl;
        this.connectionPort = defaultConnectionPort;
    }
    
    public String getConnectionUrl() {
        return connectionUrl == null ? scapyDefaultConnUrl : connectionUrl + ":" + connectionPort;
    }

    public void setConnectionUrl(String scapyCornnUrl) {
        this.connectionUrl = connectionUrl;
    }

    public void setConnectionPort(String connectionPort) {
        this.connectionPort = connectionPort;
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
}
