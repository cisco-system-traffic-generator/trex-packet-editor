package com.xored.javafx.packeteditor.service;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;


public class LocalFileMetadataServiceTest {
    
    private LocalFileMetadataService metadataService = new LocalFileMetadataService();
    
    @Test
    public void testLoadFile(){
        try {
            metadataService.loadMeta();
            assertFalse(metadataService.getProtocols().isEmpty());
        } catch (IOException e) {
            fail("Unable to load metadata");
        }
    }
}
