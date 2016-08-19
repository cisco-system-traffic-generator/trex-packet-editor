package com.xored.javafx.packeteditor.gui;

import java.net.URL;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;

import com.xored.javafx.packeteditor.data.BinaryData;
import com.xored.javafx.packeteditor.data.IBinaryData;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import javax.inject.Inject;

public class BinaryEditorController implements Initializable, Observer {
    private final String STYLE = "-fx-text-box-border: transparent; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;";
    private final String SEL_STYLE = "-fx-text-box-border: transparent; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;-fx-background-color:#33adff";


    @FXML private GridPane binaryEditorPane;
    @Inject private IBinaryData binaryData;

    boolean updating = false;

    private TextField[][] fields;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        binaryEditorPane.setStyle("-fx-background-color: #FFFFFF");
        int len = binaryData.getLength();
        final int w = 16;
        final int h = len/w;
        fields = new TextField[h][];
        for (int i = 0; i < h; i++) {
            fields[i] = new TextField[w];
            for (int j = 0; j < w; j++) {
                final int f_i = i;
                final int f_j = j;
                final int idx = i * len/16 + j;
                final TextField textField = new TextField();
                fields[i][j] = textField;

                textField.setStyle(STYLE);
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
        }
    }

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

    public void setBinaryEditorPane(GridPane binaryEditorPane) {
        this.binaryEditorPane = binaryEditorPane;
    }

    public void setBinaryData(IBinaryData binaryData) {
        this.binaryData = binaryData;
    }

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
                        fields[i][j].setStyle(SEL_STYLE);
                    } else {
                        fields[i][j].setStyle(STYLE);
                    }
                }
            }
        }
    }
}
