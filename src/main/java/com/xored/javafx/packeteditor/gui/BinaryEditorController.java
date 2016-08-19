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
import javafx.scene.text.Font;
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
    private Rectangle backgroundRect = new Rectangle();
    private Rectangle selRect = new Rectangle();
    private Rectangle editingRect = new Rectangle();

    double xOffset = 10;
    double yOffset = 20;
    double numLineLength = 45;
    double byteLength = 15;
    double bytePad = 5;
    double byteWordPad = 15;

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

        backgroundRect.setWidth(600.0);
        backgroundRect.setHeight(200.0);
        backgroundRect.setFill(Color.WHITE);

        beGroup.getChildren().add(backgroundRect);
        beGroup.getChildren().add(selRect);
        beGroup.getChildren().add(editingRect);
        for (int i = 0; i < h; i++) {
            texts[i] = new Text[w];
            lineNums[i] = new Text(String.format("%04X", i * w) + ':');
            lineHex[i] = new Text(convertHexToString(binaryData.getBytes(i*w, w)));

            lineNums[i].setTranslateX(xOffset);
            lineNums[i].setTranslateY(yOffset * (i+1));
            lineNums[i].setFont(Font.font("monospace"));


            lineHex[i].setTranslateX(numLineLength + xOffset + w * byteLength + w * bytePad + (w/4 - 1) * byteWordPad + xOffset);
            lineHex[i].setTranslateY(yOffset * (i+1));
            lineHex[i].setFont(Font.font("monospace"));

            beGroup.getChildren().addAll(lineNums[i], lineHex[i]);

            for (int j = 0; j < w; j++) {
                final int f_i = i;
                final int f_j = j;
                final int idx = i * w + j;

                final Text text = new Text();
                final char[] symbols = new char[2];
                String.format("%02X", (int) binaryData.getByte(idx)).getChars(0, 2, symbols, 0);
                text.setText(new String(symbols));


                text.setTranslateX(numLineLength + xOffset + j * bytePad + (j/4) * byteWordPad + byteLength * j);
                text.setTranslateY(yOffset * (i+1));
                text.setTranslateZ(100);

                text.setFont(Font.font("monospace"));

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
                        b &= 0x0FFFF0F;
                    } else {
                        b &= 0x0FFFFF0;
                    }
                    b |= val << (1 - editingStep) * 4;
                    binaryData.setByte(idxEditing, (byte)b);

                    int i = idxEditing / texts[0].length;
                    int j = idxEditing % texts[0].length;

                    updating = true;
                    //String.format("%02X", b).getChars(0, 2, symbols, 0);
                    texts[i][j].setText( String.format("%02X", (byte)b));
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

        binaryData.getObservable().addObserver(this);
    }

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
                    texts[i][j].setText(String.format("%02X", (byte) binaryData.getByte(idx)));
                }
                lineHex[i].setText(convertHexToString(binaryData.getBytes(i*texts[i].length,  texts[i].length)));
            }
            updating = false;
        }
        if ((o == binaryData) && (BinaryData.OP.SELECTION.equals(arg))) {
            int offset = binaryData.getSelOffset();
            int length = binaryData.getSelLength();
            int l = binaryData.getSelOffset() / texts[0].length;

            double x = numLineLength + xOffset + offset * bytePad + (offset/4) * byteWordPad + byteLength * offset;

            int ie = offset + length - 1;
            double end = numLineLength + xOffset + ie * bytePad + (ie/4) * byteWordPad + byteLength * ie;

            selRect.setTranslateX(x);
            selRect.setWidth(end - x + byteLength);
            selRect.setHeight(yOffset);
            selRect.setTranslateY(l * yOffset + 5);
            selRect.setTranslateZ(0);

            selRect.setFill(Color.AQUAMARINE);
        }
    }

    private void startEditing(int idx) {
        idxEditing = idx;
        int ty = idx / texts[0].length;
        int tx = idx % texts[0].length;

        double x = numLineLength + xOffset + tx * bytePad + (tx/4) * byteWordPad + byteLength * tx - bytePad/2;

        editingRect.setTranslateX(x);
        editingRect.setWidth(byteLength + bytePad);
        editingRect.setHeight(yOffset);
        editingRect.setTranslateY(ty * yOffset + 5);
        editingRect.setTranslateZ(0);

        editingRect.setFill(Color.WHITE);
        editingRect.setStroke(Color.BLACK);
    }
}
