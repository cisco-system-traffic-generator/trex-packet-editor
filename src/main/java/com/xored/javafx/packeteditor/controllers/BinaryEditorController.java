package com.xored.javafx.packeteditor.controllers;

import com.xored.javafx.packeteditor.data.BinaryData;
import com.xored.javafx.packeteditor.data.FieldEditorModel;
import com.xored.javafx.packeteditor.data.IBinaryData;
import com.xored.javafx.packeteditor.scapy.ScapyUtils;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;

public class BinaryEditorController implements Initializable, Observer {
    private Logger logger = LoggerFactory.getLogger(BinaryEditorController.class);

    @FXML private Group beGroup;
    @FXML private ScrollPane beGroupScrollPane;
    @Inject private IBinaryData binaryData;

    @Inject
    FieldEditorModel model;

    boolean updating = false;

    private Text[][] texts;
    private Text[] lineNums;
    private Text[] lineHex;
    private Rectangle[] selRect = new Rectangle[3];
    private Rectangle[] selRectHex = new Rectangle[3];
    private Rectangle editingRect = new Rectangle();

    final int maxByteColumns = 16;
    final int maxByteRows = 64; // higher values may require display optimizations

    final double yPadding = 0;
    final double xPadding = 5;      // Global padding
    final double xBytePadding = 50; // Padding for bytes column
    final double xHexPadding = 55;  // Padding for hex column (latest column)
    final double xOffset = 0;
    final double yOffset = 20;
    final double numLineLength = 36.12; // Length of bytes number column
    final double byteLength = 15;
    final double byteLengthHex = 7.224375;  // 9.6325 for font-size=16; 7.224375 for font-size=12
          double byteLengthHexScale = -1;
    final double byteGap = 5;      // Gap between bytes inside series
    final double byteWordGap = 25; // Gap between 4-bytes series

    int idxEditing = -1;
    int editingStep = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        reloadAll();
        binaryData.getObservable().addObserver(this);

        ChangeListener<Number> sizeListener = new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov,
                                Number old_val, Number new_val) {
                Rectangle clip = new Rectangle(beGroupScrollPane.getLayoutBounds().getWidth() - 1
                        , beGroupScrollPane.getLayoutBounds().getHeight() - 1);
                clip.setLayoutX(0);
                clip.setLayoutY(0);
                beGroupScrollPane.setClip(clip);
            }
        };

        beGroupScrollPane.widthProperty().addListener(sizeListener);
        beGroupScrollPane.heightProperty().addListener(sizeListener);
    }

    private int getDisplayedBytesLength() {
        int maxBytesToDisplay =  maxByteRows * maxByteColumns;
        return Math.min(binaryData.getLength(), maxBytesToDisplay);
    }

    private void reloadAll() {
        int displayedBytesLen = getDisplayedBytesLength();

        final int w = maxByteColumns;
        final int h = displayedBytesLen /w + ((0 < displayedBytesLen % w) ? 1 : 0);

        texts = new Text[h][];
        lineNums = new Text[h];
        lineHex = new Text[h];

        beGroup.getChildren().clear();
        for (int i = 0; i < selRect.length; i++) {
            selRect[i] = new Rectangle();
            selRect[i].setFill(Color.SKYBLUE);
            selRect[i].setTranslateZ(0);

            beGroup.getChildren().add(selRect[i]);
        }
        for (int i = 0; i < selRectHex.length; i++) {
            selRectHex[i] = new Rectangle();
            selRectHex[i].setFill(Color.SKYBLUE);
            selRectHex[i].setTranslateZ(0);

            beGroup.getChildren().add(selRectHex[i]);
        }

        Rectangle rect4lineNums = new Rectangle(0, 0);
        rect4lineNums.setFill(Color.WHITESMOKE);
        rect4lineNums.setTranslateZ(0);
        beGroup.getChildren().add(rect4lineNums);

        beGroup.getChildren().add(editingRect);
        for (int i = 0; i < h; i++) {
            texts[i] = new Text[w];
            lineNums[i] = new Text(String.format("%04x", i * w));
            lineHex[i] = new Text(convertHexToString(binaryData.getBytes(i * w, Math.min(w, displayedBytesLen - i * w))));

            lineNums[i].setTranslateX(xOffset + xPadding);
            lineNums[i].setTranslateY(yOffset * (i+1) + yPadding);
            lineNums[i].setFont(Font.font("monospace"));
            lineNums[i].setFill(Color.GREY);
            lineNums[i].setTranslateZ(100);

            Bounds b = lineNums[i].getBoundsInParent();
            rect4lineNums.setTranslateX(Math.min(b.getMinX(), rect4lineNums.getX()));
            rect4lineNums.setTranslateY(Math.min(b.getMinY(), rect4lineNums.getY()));
            rect4lineNums.setWidth(Math.max(b.getMaxX() - rect4lineNums.getX() + xPadding, rect4lineNums.getWidth()));
            rect4lineNums.setHeight(Math.max(b.getMaxY() - rect4lineNums.getY(), rect4lineNums.getHeight()));
            rect4lineNums.setHeight(Math.max(beGroupScrollPane.getLayoutBounds().getHeight(), rect4lineNums.getHeight()));


            lineHex[i].setTranslateX(numLineLength + xBytePadding + xOffset + w * byteLength + w * byteGap + (w/4 - 1) * byteWordGap + xOffset + xPadding + xHexPadding);
            lineHex[i].setTranslateY(yOffset * (i+1) + yPadding);
            lineHex[i].getStyleClass().add("begrouptext");

            beGroup.getChildren().addAll(lineNums[i], lineHex[i]);

            for (int j = 0; j < w && (i * w + j < displayedBytesLen); j++) {
                final int f_i = i;
                final int f_j = j;
                final int idx = i * w + j;

                final Text text = new Text();
                byte currentByte = binaryData.getByte(idx);
                String hexByte = String.format("%02X", currentByte);
                text.setText(hexByte);


                text.setTranslateX(numLineLength + xBytePadding + xOffset + j * byteGap + (j/4) * byteWordGap + byteLength * j + xPadding);
                text.setTranslateY(yOffset * (i+1) + yPadding);
                text.setTranslateZ(100);

                text.getStyleClass().add("begrouptext");

                texts[i][j] = text;

                if (isEditingAllowed()) {
                    text.setOnMouseClicked( (MouseEvent mouseEvent) -> {
                        if(mouseEvent.getButton().equals(MouseButton.PRIMARY)){
                            if(mouseEvent.getClickCount() == 2){
                                startEditing(idx);
                                beGroup.requestFocus();
                            }
                        }
                    });
                }

                beGroup.getChildren().add(text);
                beGroup.setFocusTraversable(true);
            }
        }

        if (displayedBytesLen < binaryData.getLength()) {
            logger.info("payload is too large to display");
        }

        if (isEditingAllowed()) {
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
                        binaryData.setByte(idxEditing, (byte) b);
                        byte[] newBytes = binaryData.getBytes(0, binaryData.getLength());
                        model.editPacketBytes(newBytes);

                        int i = idxEditing / texts[0].length;
                        int j = idxEditing % texts[0].length;

                        updating = true;
                        //String.format("%02X", b).getChars(0, 2, symbols, 0);
                        texts[i][j].setText(String.format("%02X", (byte) b));
                        lineHex[i].setText(convertHexToString(binaryData.getBytes(i * texts[i].length, texts[i].length)));
                        updating = false;


                        editingStep++;
                        if (editingStep == 2) {
                            editingStep = 0;
                            idxEditing = -1;
                            editingRect.setWidth(0);
                            editingRect.setHeight(0);
                        }
                    } catch (Exception e) {
                        logger.error("binary editor error", e);
                    }
                }
            });
        }
    }

    private String convertHexToString(byte[] hex) {
        StringBuilder sb = new StringBuilder();
        for (byte decimal : hex) {
            if (ScapyUtils.isPrintableChar(decimal)) {
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
                    if (idx >= binaryData.getLength()) {
                        break;
                    }
                    texts[i][j].setText(String.format("%02X", (byte) binaryData.getByte(idx)));
                }
                lineHex[i].setText(convertHexToString(binaryData.getBytes(i*texts[i].length,  texts[i].length)));
            }
            updating = false;
        }
        if ((o == binaryData) && (BinaryData.OP.SELECTION.equals(arg))) {
            updateSelection();
        }
        if ((o == binaryData) && (BinaryData.OP.RELOAD.equals(arg))) {
            reloadAll();
        }
    }

    private void updateSelection() {
        int startByteIdx = binaryData.getSelOffset();
        int endByteIdx = startByteIdx + Math.max(0, binaryData.getSelLength() - 1);
        int lastDispByte = Math.max(0, getDisplayedBytesLength() - 1);

        int startIdx = Math.min(startByteIdx, lastDispByte);
        int endIdx = Math.min(endByteIdx, lastDispByte);

        int startRow = getByteCellRow(startIdx);
        int endRow = getByteCellRow(endIdx);

        int startColumn = getByteCellColumn(startIdx);
        int endColumn = getByteCellColumn(endIdx);
        double startX = getByteCellX(startColumn);
        double endX = getByteCellX(endColumn);

        double lineStartX = getByteCellX(0);
        double lineEndX = getByteCellX(texts[0].length - 1);

        if (startRow == endRow) {
            selRect[0].setTranslateX(startX);
            selRect[0].setWidth(endX - startX + byteLength);
            selRect[0].setHeight(yOffset);
            selRect[0].setTranslateY(startRow * yOffset + 5 + yPadding);

            for (int i = 1; i < 3; i++) {
                selRect[i].setWidth(0);
                selRect[i].setHeight(0);
            }

            selRectHex[0].setTranslateX(lineHex[startRow].getTranslateX() + startColumn * byteLengthHex * getWidthScale());
            selRectHex[0].setWidth((endColumn - startColumn + 1) * byteLengthHex * getWidthScale());
            selRectHex[0].setHeight(yOffset);
            selRectHex[0].setTranslateY(startRow * yOffset + 5 + yPadding);

            for (int i = 1; i < 3; i++) {
                selRectHex[i].setWidth(0);
                selRectHex[i].setHeight(0);
            }

        }
        else if (endRow == startRow + 1) {
            selRect[0].setTranslateX(startX);
            selRect[0].setWidth(lineEndX - startX + byteLength);
            selRect[0].setHeight(yOffset);
            selRect[0].setTranslateY(startRow * yOffset + 5 + yPadding);

            selRect[1].setTranslateX(lineStartX);
            selRect[1].setWidth(endX - lineStartX + byteLength);
            selRect[1].setHeight(yOffset);
            selRect[1].setTranslateY(endRow * yOffset + 5 + yPadding);

            selRect[2].setWidth(0);
            selRect[2].setHeight(0);

            selRectHex[0].setTranslateX(lineHex[startRow].getTranslateX() + startColumn * byteLengthHex * getWidthScale());
            selRectHex[0].setWidth((16 - startColumn) * byteLengthHex * getWidthScale());
            selRectHex[0].setHeight(yOffset);
            selRectHex[0].setTranslateY(startRow * yOffset + 5 + yPadding);

            selRectHex[1].setTranslateX(lineHex[endRow].getTranslateX());
            selRectHex[1].setWidth((endColumn + 1) * byteLengthHex * getWidthScale());
            selRectHex[1].setHeight(yOffset);
            selRectHex[1].setTranslateY(endRow * yOffset + 5 + yPadding);

            selRectHex[2].setWidth(0);
            selRectHex[2].setHeight(0);
        }
        else {
            selRect[0].setTranslateX(startX);
            selRect[0].setWidth(lineEndX - startX + byteLength);
            selRect[0].setHeight(yOffset);
            selRect[0].setTranslateY(startRow * yOffset + 5 + yPadding);

            selRect[1].setTranslateX(lineStartX);
            selRect[1].setWidth(endX - lineStartX + byteLength);
            selRect[1].setHeight(yOffset);
            selRect[1].setTranslateY(endRow * yOffset + 5 + yPadding);

            selRect[2].setTranslateX(lineStartX);
            selRect[2].setWidth(lineEndX - lineStartX + byteLength);
            selRect[2].setHeight(yOffset * (endRow - startRow - 1));
            selRect[2].setTranslateY((startRow + 1) * yOffset + 5 + yPadding);

            selRectHex[0].setTranslateX(lineHex[startRow].getTranslateX() + startColumn * byteLengthHex * getWidthScale());
            selRectHex[0].setWidth((16 - startColumn) * byteLengthHex * getWidthScale());
            selRectHex[0].setHeight(yOffset);
            selRectHex[0].setTranslateY(startRow * yOffset + 5 + yPadding);

            selRectHex[1].setTranslateX(lineHex[endRow].getTranslateX());
            selRectHex[1].setWidth((endColumn + 1) * byteLengthHex * getWidthScale());
            selRectHex[1].setHeight(yOffset);
            selRectHex[1].setTranslateY(endRow * yOffset + 5 + yPadding);

            selRectHex[2].setTranslateX(lineHex[startRow + 1].getTranslateX());
            selRectHex[2].setWidth(16 * byteLengthHex * getWidthScale());
            selRectHex[2].setHeight(yOffset * (endRow - startRow - 1));
            selRectHex[2].setTranslateY((startRow + 1) * yOffset + 5 + yPadding);
        }
    }

    private void startEditing(int idx) {
        idxEditing = idx;
        int ty = getByteCellRow(idx);
        int tx = getByteCellColumn(idx);

        double x = getByteCellX(tx) - byteGap /2;

        editingRect.setTranslateX(x);
        editingRect.setWidth(byteLength + byteGap);
        editingRect.setHeight(yOffset);
        editingRect.setTranslateY(ty * yOffset + 5 + yPadding);
        editingRect.setTranslateZ(0);

        editingRect.setFill(Color.WHITE);
        editingRect.setStroke(Color.BLACK);
    }

    private int getByteCellRow(int idx) {
        return idx / texts[0].length;
    }

    private int getByteCellColumn(int idx) {
        return idx % texts[0].length;
    }

    private double getByteCellX(int idx) {
        int xi = getByteCellColumn(idx);
        return numLineLength + xBytePadding + xOffset + xi * byteGap + (xi/4) * byteWordGap + byteLength * xi + xPadding;
    }

    private double getWidthScale() {
        if (texts != null &&
                texts.length > 0 &&
                texts[0] != null &&
                texts[0].length > 0 &&
                texts[0][0] != null)
        {
            if (byteLengthHexScale == -1 ) {
                byteLengthHexScale = (texts[0][0].getLayoutBounds().getWidth() / texts[0][0].getText().length()) / byteLengthHex;
            }
        }
        else {
            byteLengthHexScale = 1.0;
        }
        return byteLengthHexScale;
    }

    private boolean isEditingAllowed() {
        return model.isBinaryMode();
    }
}
