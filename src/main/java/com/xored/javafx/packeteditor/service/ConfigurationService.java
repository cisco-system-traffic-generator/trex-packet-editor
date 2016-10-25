package com.xored.javafx.packeteditor.service;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class ConfigurationService {
    
    private String host;
    
    private String protocol;
    
    private Integer receiveTimeout;

    private String connectionPort;
    
    private ApplicationMode applicationMode;

    public boolean isStandaloneMode() {
        return ApplicationMode.STANDALONE.equals(applicationMode);
    }

    public enum ApplicationMode {
        STANDALONE,
        EMBEDDED
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
}
