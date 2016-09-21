package com.xored.javafx.packeteditor.service;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;


public class LocalFileMetadataServiceTest {
    
    private LocalFileMetadataService metadataService = new LocalFileMetadataService();
    
    @Test
    public void testLoadFile(){
        try {
            metadataService.loadMeta();
            Assert.assertNotEquals(0, metadataService.getProtocols().size());
        } catch (IOException e) {
            Assert.fail("Unable to load metadata");
        }
    }
}
