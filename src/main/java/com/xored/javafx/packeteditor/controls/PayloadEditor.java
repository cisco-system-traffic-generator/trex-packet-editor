package com.xored.javafx.packeteditor.controls;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.inject.Injector;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;

public class PayloadEditor extends VBox {
    static Logger logger = LoggerFactory.getLogger(PayloadEditor.class);

    @FXML private VBox root;
    @FXML private HBox payloadEditorHboxChoice;
    @FXML private HBox payloadEditorHboxValue;
    @FXML private GridPane payloadEditorGrid;

    @FXML private HBox payloadEditorHboxSize;
    @FXML private ChoiceBox patternSizeChoice;
    @FXML private Label     patternSizeLabel;
    @FXML private Label     patternSizeLabel2;
    @FXML private TextField patternSize;

    // Choice and save button
    @FXML private ComboBox<String> payloadChoiceType;
    @FXML private Button payloadButtonSave;
    @FXML private Button payloadButtonCancel;

    // Payload from text
    @FXML private TextArea textText;

    // Payload from file
    @FXML private TextField textFilename;
    @FXML private Button    textFilenameButton;

    // Payload generation from text pattern
    @FXML private TextArea  textPatternText;

    // Payload generation from file pattern
    @FXML private TextField filePatternFilename;
    @FXML private Button    filePatternButton;

    // Payload generation from code pattern
    @FXML private TextArea  codePatternText;

    // Payload random ascii generation

    // Payload random non-ascii generation


    public enum PayloadType { // The index is equal to the combobox index !
        UNKNOWN         (-1, "UNKNOWN"),
        TEXT            (0, "Text"),
        FILE            (1, "File"),
        TEXT_PATTERN    (2, "Text pattern"),
        FILE_PATTERN    (3, "File pattern"),
        CODE_PATTERN    (4, "Code pattern"),
        RANDOM_ASCII    (5, "Random ASCII"),
        RANDOM_NON_ASCII(6, "Random non ASCII");

        private final int    index;
        private final String human;

        PayloadType(int index, String human) {
            this.index = index;
            this.human = human;
        }

        public int    index() { return index; }
        public String human() { return human; }
    }

    public enum EditorMode {
        UNKNOWN (-1, "UNKNOWN"),
        READ    (0, "Reading mode"),
        EDIT    (1, "Editing mode");

        private final int    mode;
        private final String human;

        EditorMode(int mode, String human) {
            this.mode = mode;
            this.human = human;
        }

        public int    mode()  { return mode; }
        public String human() { return human; }
    }

    private enum DataType {
        UNKNOWN (-1, "UNKNOWN"),
        TEXT    (0, "TEXT"),
        BINARY  (1, "BINARY");

        private final int    type;
        private final String human;

        DataType(int type, String human) {
            this.type = type;
            this.human = human;
        }

        public int    type()  { return type; }
        public String human() { return human; }
    }

    static private final int PAYLOAD_MAX_SIZE = 1024 * 1024;

    private EventHandler<ActionEvent> handlerActionSaveExternal;
    private PayloadType type = PayloadType.UNKNOWN;
    private EditorMode  mode = EditorMode.UNKNOWN;
    private byte[]      data;
    private File        file;
    private int         seed = 12345;
    private JsonObject  jsonData = null;

    private ChangeListener<String> onlyNumberListener = (observable, oldValue, newValue) -> {
        if (!newValue.matches("\\d*")) {
            ((StringProperty) observable).set(oldValue);
        }
    };

    private ChangeListener<String> onlyHexListener = (observable, oldValue, newValue) -> {
        if (!newValue.matches("([0-9a-fA-F]*|0x[0-9a-fA-F]*|\\s*[0-9a-fA-F]*|\\s*0x[0-9a-fA-F]*)*")) {
            ((StringProperty) observable).set(oldValue);
        }
    };

    private EventHandler<ActionEvent> handlerActionSaveInternal = (event) -> {
        boolean itsok = false;

        // First call our code
        if (type == PayloadType.TEXT) {
            itsok = text_data();
        }
        else if (type == PayloadType.FILE) {
            itsok = file_data();
        }
        else if (type == PayloadType.TEXT_PATTERN) {
            itsok = text_pattern_data();
        }
        else if (type == PayloadType.FILE_PATTERN) {
            itsok = file_pattern_data();
        }
        else if (type == PayloadType.CODE_PATTERN) {
            itsok = code_pattern_data();
        }
        else if (type == PayloadType.RANDOM_ASCII) {
            itsok = random_bytes_data(true);
        }
        else if (type == PayloadType.RANDOM_NON_ASCII) {
            itsok = random_bytes_data(false);
        }

        if (itsok) {
            if (handlerActionSaveExternal != null) {
                handlerActionSaveExternal.handle(event);
            }
        }
    };

    public PayloadEditor(Injector injector) {
        FXMLLoader fxmlLoader = injector.getInstance(FXMLLoader.class);

        fxmlLoader.setLocation(ClassLoader.getSystemResource("com/xored/javafx/packeteditor/controllers/PayloadEditor.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        payloadChoiceType.setStyle("-fx-max-width: Infinity;");
        HBox.setHgrow(patternSizeChoice, Priority.ALWAYS);
        patternSizeChoice.setStyle("-fx-min-width: 14em; -fx-pref-width: -1; -fx-max-width: -1;");
        HBox.setHgrow(patternSizeLabel, Priority.NEVER);
        patternSizeLabel.setStyle("-fx-min-width: 1.5em; -fx-pref-width: 1.5em; -fx-max-width: 1.5em;");
        HBox.setHgrow(patternSize, Priority.ALWAYS);
        patternSize.setStyle("-fx-min-width: 5em; -fx-pref-width: -1; -fx-max-width: -1;");
        HBox.setHgrow(patternSizeLabel2, Priority.NEVER);
        patternSizeLabel2.setStyle("-fx-min-width: 5em; -fx-pref-width: -1; -fx-max-width: -1;");

        payloadChoiceType.setOnAction((event) -> {
            int index = payloadChoiceType.getSelectionModel().getSelectedIndex();
            if (index >= 0) {
                payloadEditorHboxValue.setVisible(true);
                payloadEditorHboxValue.setManaged(true);
                if (index >= 2) {
                    payloadEditorHboxSize.setVisible(true);
                    payloadEditorHboxSize.setManaged(true);
                }
                else {
                    payloadEditorHboxSize.setVisible(false);
                    payloadEditorHboxSize.setManaged(false);
                }
                if (index >= 0 && index <= 4) {
                    gridSetVisible(payloadEditorGrid, index);
                }
                setType(int2type(index)); // index MUST be == PayloadType !
                setMode(EditorMode.EDIT);
            }
            else {
                setType(PayloadType.UNKNOWN);
            }
        });

        payloadButtonSave.setOnAction(handlerActionSaveInternal);

        textFilenameButton.setOnAction((event) -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select payload text file");
            File file = fileChooser.showOpenDialog(this.getScene().getWindow());
            if (file != null) {
                textFilename.setText(file.getAbsolutePath());
            }
        });

        filePatternButton.setOnAction((event) -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select payload pattern file");
            file = fileChooser.showOpenDialog(this.getScene().getWindow());
            if (file != null) {
                filePatternFilename.setText(file.getAbsolutePath());
            }
        });

        //codePatternText.textProperty().addListener(onlyHexListener);
        patternSize.textProperty().addListener(onlyNumberListener);

        setMode(EditorMode.UNKNOWN);

        this.accessibleHelpProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.contains("ERROR")) {
                showError(newValue);
                accessibleHelpProperty().setValue(null);
            }
        });

        this.getStyleClass().addListener(new ListChangeListener<String>() {
            @Override
            public void onChanged(Change<? extends String> c) {
                if (c.toString().contains("field-error")) {
                    switch (getType()) {
                        case CODE_PATTERN:
                            codePatternText.getStyleClass().add("field-error");
                            codePatternText.requestFocus();
                            break;
                    }
                }
            }
        });
    }

    public EditorMode getMode() {
        return mode;
    }

    public void setMode(EditorMode mode) {
        this.mode = mode;
        switch (mode) {
            case READ:
                logger.debug("READ mode for payload editor is disabled now");
                break;
            case EDIT:
                payloadEditorHboxChoice.setVisible(true);
                payloadEditorHboxChoice.setManaged(true);

                int index = type2int(type);
                if (index >= 0) {
                    select(index);
                } else {
                    payloadEditorHboxValue.setVisible(false);
                    payloadEditorHboxValue.setManaged(false);
                }
                break;
            case UNKNOWN:
                logger.debug("Set UNKNOWN payload editor mode");
                break;
            default:
                logger.error("Unknown payload editor mode");
        }
    }

    public JsonElement getJson() {
        JsonObject value = new JsonObject();
        value.add("vtype", new JsonPrimitive("BYTES"));

        String sz = patternSizeChoice.getSelectionModel().getSelectedItem().toString();
        if (sz.contains("Packet size")) {
            sz = "total_size";
        }
        else {
            sz = "size";
        }
        String size = patternSize.getText();
        if (size.length() > 0) {
            value.add(sz, new JsonPrimitive(Integer.parseInt(size)));
        }

        // We have already verified and got input data
        // So, just create json object
        if (type == PayloadType.TEXT) {
            String data_base64 = Base64.getEncoder().encodeToString(getText().getBytes());
            value.add("base64", new JsonPrimitive(data_base64));
        }
        else if (type == PayloadType.FILE) {
            String data_base64 = Base64.getEncoder().encodeToString(getData());
            value.add("base64", new JsonPrimitive(data_base64));
        }
        else if (type == PayloadType.TEXT_PATTERN) {
            value.add("generate", new JsonPrimitive("template"));
            String data_base64 = Base64.getEncoder().encodeToString(textPatternText.getText().getBytes());
            value.add("template_base64", new JsonPrimitive(data_base64));
        }
        else if (type == PayloadType.FILE_PATTERN) {
            value.add("generate", new JsonPrimitive("template"));
            String data_base64 = Base64.getEncoder().encodeToString(data);
            value.add("template_base64", new JsonPrimitive(data_base64));
        }
        else if (type == PayloadType.CODE_PATTERN) {
            value.add("generate", new JsonPrimitive("template_code"));
            value.add("template_code", new JsonPrimitive(codePatternText.getText()));
        }
        else if (type == PayloadType.RANDOM_ASCII) {
            value.add("generate", new JsonPrimitive("random_ascii"));
            value.add("seed", new JsonPrimitive(seed));
        }
        else if (type == PayloadType.RANDOM_NON_ASCII) {
            value.add("generate", new JsonPrimitive("random_bytes"));
            value.add("seed", new JsonPrimitive(seed));
        }
        else {
            logger.warn("Not yet implemented");
        }

        return value;
    }


    public boolean reset() {
        if (jsonData != null) {
            return setJson(jsonData);
        }
        return false;
    }

    public boolean setJson(JsonElement json) {
        if (json == null) return false;
        if (!json.isJsonObject()) return false;

        JsonObject o = json.getAsJsonObject();
        if (!o.has("vtype")) return false;
        if (!o.get("vtype").getAsString().equals("BYTES")) return false;

        jsonData = o;
        if (o.has("generate")) {
            String generate = o.get("generate").getAsString();
            int size = -1, total_size = -1;
            String template = null;
            byte[] template_base64 = null;
            String template_code = null;

            if (o.has("size")) {
                size = o.get("size").getAsInt();
            }
            if (o.has("total_size")) {
                total_size = o.get("total_size").getAsInt();
            }
            if (o.has("seed")) {
                seed = o.get("seed").getAsInt();
            }
            if (o.has("template_base64")) {
                template_base64 = o.get("template_base64").getAsString().getBytes();
            }
            else if (o.has("template_code")) {
                template_code = o.get("template_code").getAsString();
            }
            if (size != -1) {
                patternSize.setText(Integer.toString(size));
                patternSizeChoice.getSelectionModel().select(0);
            }
            else if (total_size != -1) {
                patternSize.setText(Integer.toString(total_size));
                patternSizeChoice.getSelectionModel().select(1);
            }

            if (generate.equals("template")) {
                if (template_base64 != null) {
                    template = new String(Base64.getDecoder().decode(template_base64));
                    if (getDataType(template.getBytes()) == DataType.TEXT) {
                        setType(PayloadType.TEXT_PATTERN);
                        textPatternText.setText(template);
                    }
                    else if (getDataType(template_base64) == DataType.TEXT) {
                        setType(PayloadType.FILE_PATTERN);
                    }
                    return true;
                }
            }
            else if (generate.equals("template_code")) {
                setType(PayloadType.CODE_PATTERN);
                if (template_code != null) {
                    codePatternText.setText(template_code);
                }
                return true;
            }
            else if (generate.equals("random_ascii")) {
                setType(PayloadType.RANDOM_ASCII);
                return true;
            }
            else if (generate.equals("random_bytes")) {
                setType(PayloadType.RANDOM_NON_ASCII);
                return true;
            }
        }
        else if (o.has("base64")) {
            String base64 = o.get("base64").getAsString();
            byte[] d = Base64.getDecoder().decode(base64);
            if (getDataType(d)==DataType.TEXT) {
                setText(new String(d));
                return true;
            }
            else if (getDataType(d)==DataType.BINARY) {
                setData(d);
                setType(PayloadType.FILE);
                return true;
            }
        }
        return false;
    }

    public String getText() {
        if (type==PayloadType.TEXT || type==PayloadType.TEXT_PATTERN) {
            return textProperty().get();
        } else {
            return null;
        }
    }

    public void setText(String text) {
        textProperty().set(text.substring(0, Math.min(text.length(), PAYLOAD_MAX_SIZE)));
        setType(PayloadType.TEXT);
    }


    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = Arrays.copyOf(data, PAYLOAD_MAX_SIZE);
    }

    public PayloadType getType() {
        return type;
    }

    public void setType(PayloadType type) {
        this.type = type;

        if (type != PayloadType.UNKNOWN) {
            select(type2int(type));
            setMode(EditorMode.EDIT);
        }
        else {
            getSelectionModel().select(null);
            payloadEditorHboxValue.setVisible(false);
            payloadEditorHboxValue.setManaged(false);
            logger.warn("Set UNKNOWN type for payload");
        }
    }

    public StringProperty textProperty() {
        return textText.textProperty();
    }

    public void setPrefSize(double width, double height) {
        super.setPrefSize(width, height);
    }

    public void setContextMenu(ContextMenu contextMenu) {
        textText.setContextMenu(contextMenu);
    }

    public SingleSelectionModel<String> getSelectionModel() {
        return payloadChoiceType.getSelectionModel();
    }

    public final void setOnAction(EventHandler<ActionEvent> value) {
        setOnActionSave(value);
    }

    public final void setOnActionSave(EventHandler<ActionEvent> value) {
        payloadButtonSave.setOnAction(handlerActionSaveInternal);
        handlerActionSaveExternal = value;
    }

    public final void setOnActionCancel(EventHandler<ActionEvent> value) {
        payloadButtonCancel.setOnAction(value);
    }

    public void select(int index) {
        gridSetVisible(payloadEditorGrid, index);
        getSelectionModel().select(index);
    }

    public static boolean isTextFile(String fileUrl) throws IOException {
        File f = new File(fileUrl);
        String type = Files.probeContentType(f.toPath());
        boolean res1, res2;

        if (type == null) {
            //type couldn't be determined, assume binary
            res1 = false;
        } else if (type.startsWith("text")) {
            res1 = true;
        } else {
            //type isn't text
            res1 = false;
        }

        FileInputStream in = new FileInputStream(f);
        res2 = isTextStream(in);
        in.close();

        return res1 && res2;
    }

    public static DataType getDataType(byte[] array) {
        try {
            InputStream in = new ByteArrayInputStream(array);
            boolean res2 = isTextStream(in);
            in.close();
            if (res2) {
                return DataType.TEXT;
            }
            return DataType.BINARY;
        }
        catch (Exception e) {
            logger.error(e.getMessage());
        }
        return DataType.UNKNOWN;
    }

    public static boolean isTextStream(InputStream in) throws IOException {
        int size = in.available();
        byte[] data = new byte[size];
        in.read(data);

        int ascii = 0;
        int other = 0;

        for (int i = 0; i < data.length; i++) {
            byte b = data[i];

            if( b < 0x09 ) return false;
            else if( b == 0x09 || b == 0x0A || b == 0x0C || b == 0x0D ) ascii++;
            else if( b >= 0x20  &&  b <= 0x7E ) ascii++;
            else other++;
        }

        if( other == 0 ) return true;

        return 100 * other / (ascii + other) <= 95;
    }

    private void gridSetVisible(GridPane grid, int index) {
        for (Node node : grid.getChildren()) {
            node.setVisible(false);
            node.setManaged(false);
        }
        if (index >= 0 && index <= 4) {
            Node node = grid.getChildren().get(index);
            node.setVisible(true);
            node.setManaged(true);
            payloadEditorHboxValue.setVisible(true);
            payloadEditorHboxValue.setManaged(true);
        }
        if (index >= 2 && index <= 6 ) {
            payloadEditorHboxSize.setVisible(true);
            payloadEditorHboxSize.setManaged(true);
        }
        else {
            payloadEditorHboxSize.setVisible(false);
            payloadEditorHboxSize.setManaged(false);
        }
    }

    private int type2int(PayloadType type) {
        return type.index;
    }

    private PayloadType int2type(int index) {
        for (PayloadType t : PayloadType.values()) {
            if (t.index == index) return t;
        }
        return PayloadType.UNKNOWN;
    }

    private void alert(boolean error, String title, String mess) {
        Alert alert;

        if (error) {
            logger.error("{}: {}", title, mess);
            alert = new Alert(Alert.AlertType.ERROR);
        }
        else {
            logger.warn("{}: {}", title, mess);
            alert = new Alert(Alert.AlertType.WARNING);
        }
        alert.setHeaderText(title);
        alert.initOwner(getScene().getWindow());
        if (mess != null ) {
            alert.getDialogPane().setExpandableContent(new ScrollPane(new TextArea(title + ": " + mess)));
        }
        alert.showAndWait();
    }

    private void showError(String title, String mess) {
        alert(true, title, mess);
    }

    private void showWarning(String title, String mess) {
        alert(false, title, mess);
    }

    private void showError(String title) {
        alert(true, title, null);
    }

    private void showWarning(String title) {
        alert(false, title, null);
    }

    // Input for TEXT
    private boolean text_data() {
        if (textText.getText().length() > PAYLOAD_MAX_SIZE) {
            showWarning("Raw data: text length must be less then " + PAYLOAD_MAX_SIZE + "\n Trancated to " + PAYLOAD_MAX_SIZE);
            setText(textText.getText().substring(0, PAYLOAD_MAX_SIZE));
        }
        else {
            setText(textText.getText());
        }
        return true;
    }

    // Verify input for FILE
    private boolean file_data() {
        File file = new File(textFilename.getText());

        if (file.exists() && file.canRead()) {
            try {
                byte[] temp = Files.readAllBytes(file.toPath());
                DataType datatype = getDataType(temp);
                data = Arrays.copyOf(temp, Math.min(temp.length, PAYLOAD_MAX_SIZE));

                if (temp.length > PAYLOAD_MAX_SIZE) {
                    showWarning("File length is greater then " + PAYLOAD_MAX_SIZE + "\n Trancated to " + PAYLOAD_MAX_SIZE);
                }
                if (datatype == DataType.TEXT) {
                    setText(new String(data));
                    return true;
                }
                else if (datatype == DataType.BINARY) {
                    setType(PayloadType.FILE);
                    return true;
                }

                showError("UNKNOWN file type");
            } catch (IOException e) {
                showError("Exception", e.getMessage());
            }
        }
        else {
            showError("File '" + file.getAbsolutePath() + "' not exists or is not readable");
        }
        return false;
    }

    // Verify size
    boolean verify_size(boolean may_empty) {
        String ssize = patternSize.getText();
        if (may_empty &&
                (ssize==null || ssize.isEmpty()))
        {
            return true;
        }

        int size = 0;
        try {
            size = Integer.parseInt(ssize);
            if (!(size > 0 && size <= PAYLOAD_MAX_SIZE)) {
                showError("Size may be empty OR must be >= 1 and <= " + PAYLOAD_MAX_SIZE);
                return false;
            }
            return true;
        }
        catch (NumberFormatException e) {
            showError("Size must be valid number");
            return false;
        }
    }

    // Verify input for TEXT_PATTERN
    private boolean text_pattern_data() {
        if (verify_size(false)) {
            String pattern = textPatternText.getText();
            if (pattern.length() > 0) {
                return true;
            } else {
                showError("Pattern is empty");
            }
        }
        return false;
    }

    // Verify input for CODE_PATTERN
    private boolean code_pattern_data() {
        if (verify_size(true)) {
            String pattern = codePatternText.getText();
            if (pattern.length() > 0) {
                return true;
            }
            else {
                showError("Pattern is empty");
            }
        }
        return false;
    }

    // Verify input for FILE_PATTERN
    private boolean file_pattern_data() {
        File file = new File(filePatternFilename.getText());
        byte[] datapattern = null;

        if (file.exists() && file.canRead()) {
            try {
                datapattern = Files.readAllBytes(file.toPath());
            } catch (IOException e) {
                showError(e.getMessage());
                return false;
            }
        }
        else {
            showError("File '" + file.getAbsolutePath() + "' not exists or is not readable");
            return false;
        }

        if (verify_size(false)) {
            if (datapattern.length > 0) {
                DataType patterntype = getDataType(datapattern);

                if (patterntype == DataType.TEXT) {
                    data = datapattern.clone();
                    return true;
                }
                else if (patterntype == DataType.BINARY) {
                    data = datapattern.clone();
                    return true;
                }
                showError("UNKNOWN file type");
            } else {
                showError("Pattern is empty");
            }
        }
        return false;
    }

    // Verify input for RANDOM_ASCII and RANDOM_NON_ASCII
    private boolean random_bytes_data(boolean ascii) {
        if (verify_size(false)) {
            return true;
        }
        return false;
    }

}
