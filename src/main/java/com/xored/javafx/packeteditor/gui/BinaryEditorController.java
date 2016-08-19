package com.xored.javafx.packeteditor.gui;

import java.net.URL;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;

import com.xored.javafx.packeteditor.data.BinaryData;
import com.xored.javafx.packeteditor.data.IBinaryData;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import javax.inject.Inject;

public class BinaryEditorController implements Initializable, Observer {
    public static final String BINARY_FIELD_CLASS = "binary-field";
    public static final String SELECTED_CLASS = "selected";

    //@FXML private GridPane binaryEditorPane;

    @FXML private Group beGroup;
    @Inject private IBinaryData binaryData;

    boolean updating = false;

    private Text[][] texts;
    private Text[] lineNums;
    private Text[] lineHex;
    private Rectangle selRect = new Rectangle();
    private Rectangle editingRect = new Rectangle();

    double xOffset = 10;
    double yOffset = 20;
    double numLineLength = 40;
    double byteLength = 15;

    int idxEditing = -1;
    int editingStep = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        //binaryEditorPane.setStyle("-fx-background-color: #FFFFFF");
        beGroup.setStyle("-fx-background-color: #FFFFFF");

        int len = binaryData.getLength();
        final int w = 16;
        final int h = len/w;

        texts = new Text[h][];
        lineNums = new Text[h];
        lineHex = new Text[h];

        beGroup.getChildren().add(selRect);
        beGroup.getChildren().add(editingRect);
        for (int i = 0; i < h; i++) {
            texts[i] = new Text[w];
            lineNums[i] = new Text(String.format("%04X", i * w) + ':');
            lineHex[i] = new Text(convertHexToString(binaryData.getBytes(i*w, w)));

            lineNums[i].setTranslateX(xOffset);
            lineNums[i].setTranslateY(yOffset * (i+1));

            lineHex[i].setTranslateX(numLineLength + xOffset + (xOffset + byteLength) * w);
            lineHex[i].setTranslateY(yOffset * (i+1));

            beGroup.getChildren().addAll(lineNums[i], lineHex[i]);

            for (int j = 0; j < w; j++) {
                final int f_i = i;
                final int f_j = j;
                final int idx = i * w + j;

                final Text text = new Text();
                final char[] symbols = new char[2];
                String.format("%02X", (int) binaryData.getByte(idx)).getChars(0, 2, symbols, 0);
                text.setText(new String(symbols));


                text.setTranslateX(numLineLength + xOffset * (j + 1) + byteLength * j);
                text.setTranslateY(yOffset * (i+1));
                text.setTranslateZ(100);

                texts[i][j] = text;

                text.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent mouseEvent) {
                        if(mouseEvent.getButton().equals(MouseButton.PRIMARY)){
                            if(mouseEvent.getClickCount() == 2){
                                startEditing(idx);
                                beGroup.requestFocus();
                            }
                        }
                    }
                });

                text.setOnKeyPressed((KeyEvent ke) -> {
                    System.out.println("123");
                });


                beGroup.getChildren().add(text);
                beGroup.setFocusTraversable(true);
            }
        }

        beGroup.setOnKeyPressed((KeyEvent ke) -> {
            if (-1 != idxEditing) {
                try {
                    Integer val = Integer.parseInt(ke.getText(), 16);

                    int b = binaryData.getByte(idxEditing);
                    if (0 == editingStep) {
                        b &= 0xFFFFF0F;
                    } else {
                        b &= 0xFFFFFF0;
                    }
                    b |= val << (1 - editingStep) * 4;
                    binaryData.setByte(idxEditing, (byte)b);

                    int i = idxEditing / texts[0].length;
                    int j = idxEditing % texts[0].length;



                    final char[] symbols = new char[2];
                    updating = true;
                    String.format("%02X", b).getChars(0, 2, symbols, 0);
                    texts[i][j].setText(new String(symbols));
                    lineHex[i].setText(convertHexToString(binaryData.getBytes(i*texts[i].length,  texts[i].length)));
                    updating = false;


                    editingStep ++;
                    if (editingStep == 2) {
                        editingStep = 0;
                        idxEditing = -1;
                        editingRect.setWidth(0);
                        editingRect.setHeight(0);
                    }
                } catch (Exception e) {
                    //slip
                }
            }
        });

        //convertToHex(binaryData.getBytes(0, binaryData.getLength()));

        /*fields = new TextField[h][];
        for (int i = 0; i < h; i++) {
            texts[i] = new TextField[w];
            for (int j = 0; j < w; j++) {
                final int f_i = i;
                final int f_j = j;
                final int idx = i * len/16 + j;
                final TextField textField = new TextField();
                fields[i][j] = textField;

                textField.getStyleClass().add(BINARY_FIELD_CLASS);
                final char[] symbols = new char[2];
                String.format("%02X", (int) binaryData.getByte(idx)).getChars(0, 2, symbols, 0);
                textField.setText(new String(symbols));
                textField.textProperty().addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                        if (updating) {
                            return;
                        }
                        int caretPosition = textField.getCaretPosition();
                        if (caretPosition < 2) {
                            symbols[caretPosition] = newValue.charAt(caretPosition);
                            String newHexValue = new String(symbols);
                            textField.setText(newHexValue);
                            binaryData.setByte(idx, Integer.valueOf(newHexValue, 16).byteValue());
                            caretPosition++;
                        }
                        if (caretPosition >= 2) {
                            goNext(fields, h, f_i, w, f_j);
                        } else {
                            textField.positionCaret(caretPosition);
                        }
                    }
                });
                binaryEditorPane.add(textField, j, i);

                binaryData.getObservable().addObserver(this);
            }
        }*/

        binaryData.getObservable().addObserver(this);
    }




    /*private void convertToHex(byte[] rawData) {
        int splitLine = 16;
        int counter = 0;
        StringBuilder myString = new StringBuilder("");
        int index = 0;
        binaryEditorPane.getChildren().clear();
        StringBuilder hexData = new StringBuilder("");
        StringBuilder indexBuffer = new StringBuilder("");
        StringBuilder rowHex = new StringBuilder("");
        StringBuilder convertedHexBuffer = new StringBuilder("");
        int spacing = 0;
        for (byte b : rawData) {
            String formatedByte = String.format("%02X", b);
            myString.append(formatedByte);
            rowHex.append(formatedByte).append(" ");
            spacing++;
            if (spacing == 4) {
                spacing = 0;
                rowHex.append("  ");
            }
            counter++;

            if (counter % splitLine == 0) {
                indexBuffer.append(String.format("%04X", index)).append(':').append('\n');
                hexData.append(rowHex.toString()).append('\n');
                convertedHexBuffer.append(convertHexToString(myString.toString())).append('\n');
                myString.setLength(0);
                rowHex.setLength(0);
                index = index + 16;
            }
        }
        if (myString.length() > 0) {
            indexBuffer.append(String.format("%04X", index)).append(':').append('\n');
            hexData.append(rowHex.toString()).append('\n');
            convertedHexBuffer.append(convertHexToString(myString.toString())).append('\n');
        }
        binaryEditorPane.add(new Label(indexBuffer.toString()), 0, 0);
        binaryEditorPane.add(new Label(hexData.toString()), 1, 0);
        binaryEditorPane.add(new Label(convertedHexBuffer.toString()), 2, 0);
        rowHex.setLength(0);
        myString.setLength(0);
        hexData.setLength(0);
    }*/

    private String convertHexToString(String hex) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hex.length() - 1; i += 2) {
            String output = hex.substring(i, i + 2);
            int decimal = Integer.parseInt(output, 16);
            if (!Character.isISOControl(decimal)) {
                sb.append((char) decimal);
            } else {
                sb.append('.');
            }
        }
        return sb.toString();
    }

    private String convertHexToString(byte[] hex) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hex.length; i ++) {
            int decimal = hex[i];
            if (!Character.isISOControl(decimal)) {
                sb.append((char) decimal);
            } else {
                sb.append('.');
            }
        }
        return sb.toString();
    }

    @Override
    public void update(Observable o, Object arg) {
        if ((o == binaryData) && (BinaryData.OP.SET_BYTES.equals(arg))) {
            updating = true;
            for (int i = 0; i < texts.length; i++) {
                for (int j = 0; j < texts[i].length; j++) {
                    int idx = i * texts[i].length + j;
                    final char[] symbols = new char[2];
                    String.format("%02X", (int) binaryData.getByte(idx)).getChars(0, 2, symbols, 0);
                    texts[i][j].setText(new String(symbols));
                }
                lineHex[i].setText(convertHexToString(binaryData.getBytes(i*texts[i].length,  texts[i].length)));
            }
            updating = false;
        }
        if ((o == binaryData) && (BinaryData.OP.SELECTION.equals(arg))) {
            int offset = binaryData.getSelOffset();
            int length = binaryData.getSelLength();
            int l = binaryData.getSelOffset() / texts[0].length;

            double x = numLineLength + xOffset * (offset + 1) + byteLength * offset - xOffset/2;

            selRect.setTranslateX(x);
            selRect.setWidth(length * (byteLength + xOffset));
            selRect.setHeight(yOffset);
            selRect.setTranslateY(l * yOffset + 5);
            selRect.setTranslateZ(0);

            selRect.setFill(Color.BLUE);
        }
    }

    private void startEditing(int idx) {
        idxEditing = idx;
        int ty = idx / texts[0].length;
        int tx = idx % texts[0].length;

        double x = numLineLength + xOffset * (tx + 1) + byteLength * tx - xOffset/2;

        editingRect.setTranslateX(x);
        editingRect.setWidth(byteLength + xOffset);
        editingRect.setHeight(yOffset);
        editingRect.setTranslateY(ty * yOffset + 5);
        editingRect.setTranslateZ(0);

        editingRect.setFill(Color.WHITE);
        editingRect.setStroke(Color.BLACK);
    }

    /*
    private void goNext(TextField[][] fields, int h, int i, int w, int j) {
        int n_i = i;
        int n_j = j + 1;
        if (n_j > w - 1) {
            n_j = w - 1;
            if (i + 1 < h) {
                n_i = i + 1;
                n_j = 0;
            }
        }
        fields[n_i][n_j].requestFocus();
    }
    */

    /*public void setBinaryEditorPane(GridPane binaryEditorPane) {
        this.binaryEditorPane = binaryEditorPane;
    }

    public void setBinaryData(IBinaryData binaryData) {
        this.binaryData = binaryData;
    }

    @Override
    public void update(Observable o, Object arg) {
        if ((o == binaryData) && (BinaryData.OP.SET_BYTES.equals(arg))) {
            convertToHex(binaryData.getBytes(0, binaryData.getLength()));
        }
    }*/

    /*
    @Override
    public void update(Observable o, Object arg) {
        if ((o == binaryData) && (BinaryData.OP.SET_BYTES.equals(arg))) {
            updating = true;
            for (int i = 0; i < fields.length; i++) {
                for (int j = 0; j < fields[i].length; j++) {
                    int idx = i * fields[i].length + j;
                    final char[] symbols = new char[2];
                    String.format("%02X", (int) binaryData.getByte(idx)).getChars(0, 2, symbols, 0);
                    fields[i][j].setText(new String(symbols));
                }
            }
            updating = false;
        }
        if ((o == binaryData) && (BinaryData.OP.SELECTION.equals(arg))) {
            for (int i = 0; i < fields.length; i++) {
                for (int j = 0; j < fields[i].length; j++) {
                    int idx = i * fields[i].length + j;
                    if ((idx >= binaryData.getSelOffset()) && (idx < binaryData.getSelOffset() + binaryData.getSelLength())) {
                        fields[i][j].getStyleClass().remove(SELECTED_CLASS);
                        fields[i][j].getStyleClass().add(SELECTED_CLASS);
                    } else {
                        fields[i][j].getStyleClass().remove(SELECTED_CLASS);
                    }
                }
            }
        }
    }
    */
}
