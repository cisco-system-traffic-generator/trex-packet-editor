package com.xored.javafx.packeteditor.metatdata;

import com.google.gson.Gson;
import com.xored.javafx.packeteditor.data.user.DocumentFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Packet template definition
 */
public class PacketTemplate {
    static Logger logger = LoggerFactory.getLogger(PacketTemplate.class);

    public static List<DocumentFile> loadTemplates() {
        List<DocumentFile> res = new ArrayList<>();
        Gson gson = new Gson();
        try {
            TemplateEntry[] templates = gson.fromJson(templateReader("templates.json"), TemplateEntry[].class);

            for (TemplateEntry template: templates) {
                DocumentFile doc = gson.fromJson(templateReader(template.file), DocumentFile.class);
                doc.metadata.caption = template.name;
                res.add(doc);
            }
        } catch (Exception e) {
            logger.error("Failed to load templates", e);
        }
        return res;
    }

    private static Reader templateReader(String templateFile) {
        return new InputStreamReader(
                PacketTemplate.class.getResourceAsStream("/templates/" + templateFile)
        );
    }

    static class TemplateEntry {
        public String name;
        public String file;
    }
}
