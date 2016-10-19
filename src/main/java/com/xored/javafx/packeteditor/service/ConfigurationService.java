package com.xored.javafx.packeteditor.service;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class ConfigurationService {
    
    private String scapyConnUrl;
    
    private String scapyDefaultConnUrl;
    
    private Integer receiveTimeout;
    
    private ApplicationMode applicationMode;
    
    public enum ApplicationMode {
        STANDALONE,
        EMBEDDED
    }

    @Inject
    public ConfigurationService(@Named("SCAPY_DEFAULT_CONN_URL") String scapyDefaultConnUrl,
                                @Named("SCAPY_RECEIVE_TIMEOUT") String defaultRecieveTimeout) {
        this.receiveTimeout = Integer.valueOf(defaultRecieveTimeout);
        this.scapyDefaultConnUrl = scapyDefaultConnUrl;
    }
    
    public String getConnectionUrl() {
        return scapyConnUrl == null ? scapyDefaultConnUrl : scapyConnUrl;
    }

    public void setScapyConnUrl(String scapyCornnUrl) {
        this.scapyConnUrl = scapyConnUrl;
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
