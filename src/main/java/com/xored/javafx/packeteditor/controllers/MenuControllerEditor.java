package com.xored.javafx.packeteditor.controllers;

import com.google.common.eventbus.EventBus;
import com.google.common.io.Files;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.xored.javafx.packeteditor.data.PacketEditorModel;
import com.xored.javafx.packeteditor.data.user.DocumentFile;
import com.xored.javafx.packeteditor.events.NeedToUpdateTemplateMenu;
import com.xored.javafx.packeteditor.events.ProtocolExpandCollapseEvent;
import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;
import com.xored.javafx.packeteditor.service.ConfigurationService;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class MenuControllerEditor implements Initializable {

    public static final String EXIT_MENU_ITEM = "exit";
    private static Logger logger= LoggerFactory.getLogger(MenuControllerEditor.class);

    @Inject
    private FieldEditorController controller;

    @Inject
    ConfigurationService configurations;

    @Inject
    private EventBus eventBus;

    @FXML
    MenuBar applicationMenu;

    @FXML
    Menu fileMenu;
    
    @FXML
    Menu newTemplateMenu;
    
    @FXML
    MenuButton newTemplateMenuButton;

    @FXML
    Menu debugMenu;

    @FXML
    Button binaryModeOnBtn;

    @FXML
    Button abstractModeOnBtn;

    @FXML
    ComboBox<ProtocolMetadata> protocolSelector;
    
    @FXML
    Button appendProtocolBtn;
    
    @Inject
    @Named("resources")
    ResourceBundle resourceBundle;

    @Inject
    AppController appController;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initTemplateMenu();

        if (System.getenv("DEBUG") != null) {
            debugMenu.setVisible(true);
            binaryModeOnBtn.setVisible(true);
            abstractModeOnBtn.setVisible(true);
        }
    }

    public void initTemplateMenu() {
        if (newTemplateMenu != null) {
            newTemplateMenu.getItems().clear();
            addTemplates(newTemplateMenu.getItems());
        }
        if (newTemplateMenuButton != null) {
            newTemplateMenuButton.getItems().clear();
            addTemplates(newTemplateMenuButton.getItems());
        }
    }

    private void addTemplates(ObservableList<MenuItem> topMenu) {
        // Predefined templates from scapy server
        addScapyTemplates(topMenu);

        // Add templates from user dir to menu list
        addUserTemplates(topMenu, configurations.getTemplatesLocation(), configurations.getTemplatesLocation());

        // Add "Save as template..." item
        topMenu.add(new SeparatorMenuItem());
        MenuItem menuItem = new MenuItem("Save as template...");
        topMenu.add(menuItem);
        menuItem.setOnAction(event -> {
            if (controller.getModel().getUserModel().getProtocolStack().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText("Save as template");
                alert.setContentText("Can't save empty template. \nPlease add at least one protocol.");
                alert.showAndWait();
                return;
            }
            try {
                File repo = new File(configurations.getTemplatesLocation());
                boolean tryagain = true;
                while (tryagain) {
                    String templ = controller.createNewTemplateDialog();
                    if (templ != null) {
                        if (isFilenameValid(repo.getCanonicalPath(), templ)) {
                            templ = templ.replace('/', File.separatorChar);
                            File file = new File(repo.getCanonicalPath(), templ + DocumentFile.FILE_EXTENSION);
                            boolean ok2write = true;
                            if (file.exists()) {
                                ok2write = controller.createFileOverwriteDialog(file);
                            }
                            if (ok2write) {
                                File dir = new File(file.getParent());
                                if (!dir.exists()) {
                                    dir.mkdirs();
                                }
                                controller.getModel().saveDocumentToFile(file);
                                eventBus.post(new NeedToUpdateTemplateMenu());
                                tryagain = false;
                            }
                        } else {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setHeaderText("Save as template");
                            alert.setContentText("Template name '" + templ + "' is invalid for local filesystem.\nPlease change template name and try again.");
                            alert.showAndWait();
                        }
                    }
                    else {
                        tryagain = false;
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        });
    }

    @FXML
    private void handleCloseAction() {
        appController.shutDown();
    }

    @FXML
    public void handleDeleteProtocolAction(ActionEvent actionEvent) {
        getModel().removeLast();
    }

    private PacketEditorModel getModel() { return controller.getModel(); }

    @FXML
    public void handleNewDocument(ActionEvent actionEvent) {
        getModel().newPacket();
    }

    @FXML
    public void handleOpenAction(ActionEvent actionEvent) {
        controller.showLoadDialog();
    }

    @FXML
    public void handleSaveAction(ActionEvent event) {
        controller.showSaveDialog();
    }

    @FXML
    public void handleRecalculateValues(ActionEvent actionEvent) {
        getModel().clearAutoFields();
    }

    @FXML
    public void handleUndo(ActionEvent actionEvent) {
        getModel().undo();
    }

    @FXML
    public void handleRedo(ActionEvent actionEvent){
        getModel().redo();
    }

    @FXML
    public void handleModeBinary(ActionEvent actionEvent) {
        getModel().setBinaryMode(true);
    }

    @FXML
    public void handleModeAbstract(ActionEvent actionEvent) {
        getModel().setBinaryMode(false);
    }

    @FXML
    public void handleCopyInstructions(ActionEvent event) {
        controller.copyInstructionsToClipboard();
    }
    
    public void handleExpandAll(ActionEvent actionEvent) {
        eventBus.post(new ProtocolExpandCollapseEvent(ProtocolExpandCollapseEvent.Action.EXPAND_ALL));
    }

    public void handleCollapseAll(ActionEvent actionEvent) {
        eventBus.post(new ProtocolExpandCollapseEvent(ProtocolExpandCollapseEvent.Action.COLLAPSE_ALL));
    }

    private static File[] getFnames(String rootDir, String ext){
        Iterable<File> fileTraverser = Files.fileTreeTraverser().postOrderTraversal(new File(rootDir));
        
        Stream<File> filesStream = StreamSupport.stream(fileTraverser.spliterator(), false);
        
        return filesStream.filter(file -> file.isFile() && file.getName().toLowerCase().endsWith(ext))
                   .collect(Collectors.toList()).toArray(new File[0]);
    }

    private static boolean isFilenameValid(String parent, String file) {
        final int length = file.length();
        if (length == 0)
            return false;
        final char firstChar = file.charAt(0);
        final char lastChar = file.charAt(length-1);
        // filenames starting/ending in dot are not valid
        if (lastChar == '.')
            return false;
        if (firstChar == '.')
            return false;
        // file names starting/ending with whitespace are bad
        if (Character.isWhitespace(lastChar))
            return false;
        if (Character.isWhitespace(firstChar))
            return false;
        // poison symbols
        if (Pattern.compile("[\\\\\'\"^$:*?<>|\\t\\n\\x0B\\f\\r\\a\\e]+").matcher(file).find()) {
            return false;
        }

        try {
            File f = new File(parent);
            Path parentPath = f.toPath();
            Path filePath = parentPath.resolve(file);

            return filePath.toFile().getCanonicalPath().equals(new File(parent, file).getAbsolutePath());
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return false;
    }

    private void addScapyTemplates(ObservableList<MenuItem> topMenu) {
        List<JsonObject> templates = controller.getTemplates();

        if (templates !=null && templates.size() > 0) {
            Collections.sort(templates, (o1, o2) -> {
                try {
                    String[] s1 = o1.get("id").getAsString().split("/");
                    String[] s2 = o2.get("id").getAsString().split("/");

                    if (s1.length > 1 && s2.length > 1) {
                        int ret = 0;
                        for (int i = 0; i < s1.length && i < s2.length; i++) {
                            ret = ret==0 ? s1[i].compareTo(s2[i]) : ret;
                        }
                        return ret;
                    }

                    return s2.length - s1.length;
                }
                catch (Exception e) {
                    return 0;
                }
            });

            Map<String, Menu> menuMap = new HashMap<>();
            for (JsonObject t : templates) {
                String templateId = t.get("id").getAsString();
                String[] parts = templateId.split(Pattern.quote("/"));
                MenuItem menuItem = new MenuItem(parts[parts.length - 1]);

                if (parts.length == 1) {
                    topMenu.add(menuItem);
                } else {
                    Menu menu = addMenuChain(topMenu, menuMap, Arrays.copyOfRange(parts, 0, parts.length - 1));
                    menu.getItems().add(menuItem);
                }

                menuItem.setOnAction(event -> {
                    try {
                        controller.loadTemplateFromScapy(templateId);
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                    }
                });
            }
        }
    }

    private void addUserTemplates(ObservableList<MenuItem> topMenu, String repoDir, String rootDir) {
        try {
            File dir = new File(rootDir);
            if (dir.isDirectory()) {
                String dirName = dir.getCanonicalPath();
                File[] fileList = getFnames(dirName, DocumentFile.FILE_EXTENSION);
                if (fileList.length > 0) {
                    topMenu.add(new SeparatorMenuItem());
                }

                Arrays.sort(fileList, (o1, o2) -> {
                    try {
                        String[] s1 = o1.getCanonicalPath().replace(repoDir, "").substring(1).split(File.separator);
                        String[] s2 = o2.getCanonicalPath().replace(repoDir, "").substring(1).split(File.separator);

                        if (s1.length > 1 && s2.length > 1) {
                            int ret = 0;
                            for (int i = 0; i < s1.length && i < s2.length; i++) {
                                ret = ret==0 ? s1[i].compareTo(s2[i]) : ret;
                            }
                            if (s2.length != s1.length) {
                                return ret + s2.length - s1.length;
                            }
                            return ret;
                        }

                        return s2.length - s1.length;
                    }
                    catch (Exception e) {
                        return 0;
                    }
                });

                Map<String, Menu> menuMap = new HashMap<>();
                for (File f : fileList) {
                    String fileName = f.getCanonicalPath();

                    if (!f.exists()) {
                        break;
                    }
                    if (!fileName.startsWith(dirName)) {
                        break;
                    }
                    fileName = fileName.replace(dirName, "");
                    if (fileName.startsWith(dirName)) {
                        break;
                    }
                    if (fileName.startsWith(File.separator)) {
                        fileName = fileName.substring(1);
                    }
                    if (!fileName.endsWith(DocumentFile.FILE_EXTENSION)) {
                        break;
                    }
                    int index = fileName.lastIndexOf(DocumentFile.FILE_EXTENSION);
                    if (index == -1) {
                        break;
                    }

                    fileName = fileName.substring(0, index).replace(File.separatorChar, '/');
                    String[] parts = fileName.split(Pattern.quote("/"));
                    MenuItem menuItem = new MenuItem(parts[parts.length - 1]);

                    if (parts.length == 1) {
                        topMenu.add(menuItem);
                    }
                    else {
                        Menu menu = addMenuChain(topMenu, menuMap, Arrays.copyOfRange(parts, 0, parts.length - 1));
                        menu.getItems().add(menuItem);
                    }

                    menuItem.setOnAction(event -> {
                        try {
                            controller.getModel().loadDocumentFromFile(f.getCanonicalFile());
                        } catch (IOException e) {
                            logger.error(e.getMessage());
                        }
                    });
                }
            }
        } catch (IOException e) {
            logger.warn("Exception was thrown while reading user's template dir: " + e.getMessage());
        }
    }

    private Menu addMenuChain(ObservableList<MenuItem> topMenu, Map<String, Menu> hmap, String[] submenu) {
        Menu menu;
        String menuKey = String.join("/", submenu);

        menu = hmap.get(menuKey);
        if (menu == null) {
            if (submenu.length == 1) {
                menu = new Menu(submenu[0]);
                topMenu.add(menu);
            }
            else {
                String[] psubmenu= Arrays.copyOfRange(submenu, 0, submenu.length - 1);
                Menu pmenu = hmap.get(String.join("/", psubmenu));
                if (pmenu == null) {
                    pmenu = addMenuChain(topMenu, hmap, psubmenu);
                }
                menu = new Menu(submenu[submenu.length - 1]);
                pmenu.getItems().add(menu);
            }
            hmap.put(menuKey, menu);
        }
        return menu;
    }

}
