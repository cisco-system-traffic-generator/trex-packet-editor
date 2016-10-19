package com.xored.packeteditor;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.junit.Test;

import static javafx.scene.input.KeyCode.*;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.util.NodeQueryUtils.hasText;

public class TestPacketEditorUI extends TestPacketEditorUIBase {

    @Test
    public void should_create_new_document() {
        newDocument();
    }

    @Test
    public void should_save_textfield_values() {
        newDocument();
        verifyUserModelFieldDefault("#Ether-src");
        clickOn("#Ether-src");
        push(ESCAPE);
        verifyUserModelFieldDefault("#Ether-src");
        clickOn("#Ether-src");
        push(ENTER);
        verifyUserModelFieldSet("#Ether-src");
    }

    @Test
    public void should_save_enumfield_values() {
        newDocument();
        verifyUserModelFieldDefault("#Ether-type");
        clickOn("#Ether-type");
        clickOn("#Ether-type");
        push(ESCAPE);
        verifyUserModelFieldDefault("#Ether-type");
        clickOn("#Ether-type");
        doubleClickOn("#Ether-type");
        push(SHIFT,L);
        push(SHIFT,O);
        push(SHIFT,O);
        push(SHIFT,P);
        push(ENTER);
        verifyUserModelFieldSet("#Ether-type");
    }

    @Test
    public void should_build_payload() {
        newDocument();
        addLayer("Raw");
        addLayer("Raw");
        verifyThat("#Ether-Raw-load", hasText("dummy"));
        clickOn("#Ether-Raw-load");
        clickOn("#payloadButtonCancel");
        verifyThat("#Ether-Raw-load", hasText("dummy"));
        clickOn("#Ether-Raw-load");
        verifyThat("#textText", hasText("dummy"));
        clickOn("#textText");
        push(SPACE,A,B,C);
        clickOn("#payloadButtonSave");
        interrupt();
        verifyThat("#Ether-Raw-load", hasText("dummy abc"));
    }

    @Test
    public void should_do_undo_redo() {
        setFieldText("#Ether-src", "00:11:22:33:44:55");
        verifyUserModelFieldSet("#Ether-src");

        setFieldText("#Ether-src", "00:11:22:33:44:99");
        verifyThat("#Ether-src", hasText("00:11:22:33:44:99"));

        undo();
        verifyUserModelFieldSet("#Ether-src");
        verifyThat("#Ether-src", hasText("00:11:22:33:44:55"));

        undo(); // undo all changes
        verifyUserModelFieldUnset("#Ether-src");

        redo();
        verifyUserModelFieldSet("#Ether-src");
        verifyThat("#Ether-src", hasText("00:11:22:33:44:55"));

        redo();
        verifyUserModelFieldSet("#Ether-src");
        verifyThat("#Ether-src", hasText("00:11:22:33:44:99"));

        redo(); // That's the extra redo. it should not do anything and should not crash view
        verifyUserModelFieldSet("#Ether-src");
        verifyThat("#Ether-src", hasText("00:11:22:33:44:99"));

        undo();
        undo(); // back to the initial state
        undo(); // extra undo. does nothing
        verifyUserModelFieldUnset("#Ether-src");
    }

    @Test
    public void should_cancel_field_editing_with_ESC() {
        newDocument();
        clickOn("#Ether-src");
        push(ESCAPE);
        verifyUserModelFieldUnset("#Ether-src");

        clickOn("#Ether-src");
        write("00:11:22:33:44:55:66");
        push(ESCAPE);
        verifyUserModelFieldUnset("#Ether-src");
    }

    @Test
    public void should_set_field_text_with_enter() {
        newDocument();
        clickOn("#Ether-src");
        write("00:11:22:33:44:55");
        push(ENTER);
        verifyUserModelFieldSet("#Ether-src");
        verifyThat("#Ether-src", hasText("00:11:22:33:44:55"));
    }

    @Test
    public void should_not_set_incorrect_field_value() {
        newDocument();
        clickOn("#Ether-src");
        write("00:11:22:33:44:55:66"); // too long
        push(ENTER); // try to commit -> fails
        push(ESCAPE); // back to field view
        verifyUserModelFieldUnset("#Ether-src");
    }

    @Test
    public void should_select_all_field_content_on_field_click() {
        final int mac_text_length = 17;
        newDocument();
        clickOn("#Ether-src");
        verifyThat("#Ether-src", (TextField tf) -> tf.getSelectedText().length() == mac_text_length);
    }

    @Test
    public void should_change_ether_type_and_back() {
        newDocument();
        addLayer("Internet Protocol Version 4");
        setFieldText("#Ether-type", "LOOP");
        setFieldText("#Ether-IP-src", "127.0.1.2");
        verifyThat("#Ether-type", (Label t) -> t.getText().contains("LOOP")); // default value for Ether
        verifyUserModelFieldSet("#Ether-type");
        verifyUserModelFieldSet("#Ether-IP-src");
        setFieldText("#Ether-type", "IPv4");
        verifyUserModelFieldSet("#Ether-type");
    }

    @Test
    public void should_build_tcpip_stack() {
        addLayer("Internet Protocol Version 4");
        verifyThat("#Ether-IP-version", hasText("4"));
        verifyThat("#Ether-type", (Label t) -> t.getText().contains("IPv4"));
        selectProtoType("IPv4");
        push(ENTER); // TODO: remove me once extra enter is not required
        verifyThat("#Ether-type", (Label t) -> t.getStyleClass().contains("field-value-set"));
    }

    @Test
    public void should_build_tcpip_stack_with_enter_or_btn() {
        setComboBoxText("#append-protocol-combobox", "IP");
        clickOn("#append-protocol-combobox .text-input");
        push(ENTER);

        scrollFieldsDown();

        setComboBoxText("#append-protocol-combobox", "TCP");
        clickOn("#append-protocol-button");


        clickOn("#append-protocol-combobox .arrow");
        clickOn("Raw");
    }

    @Test
    public void should_set_enum_value_as_text() {
        setFieldText("#Ether-type", "0x800"); // IPv4
        verifyThat("#Ether-type", (Label t) -> t.getText().contains("IPv4"));
        verifyThat("#Ether-type", this::fieldLabelIsSet);

        setFieldText("#Ether-type", ""); // clear and set default
        verifyThat("#Ether-type", (Label t) -> t.getText().contains("LOOP")); // default value for Ether
        verifyThat("#Ether-type", (Label t) -> !fieldLabelIsSet(t));

        setFieldText("#Ether-type", "0x86DD"); // IPv6
        verifyThat("#Ether-type", (Label t) -> t.getText().contains("IPv6"));
        verifyThat("#Ether-type", this::fieldLabelIsSet);
    }

    @Test
    public void load_pcap_file() {
        loadPcapFile("http.pcap");
        verifyThat("#Ether-IP-version", hasText("4"));
        verifyThat("#Ether-IP-TCP-seq", hasText("951057939"));
    }

    @Test
    public void load_pcap_file_neg() {
        loadPcapFileEx("http.bad-pcap", true);
    }

    @Test
    public void load_and_save_pcap_file() {
        loadPcapFile("http.pcap");
        savePcapFile("http-2.pcap");
        newDocument();
        loadPcapFile("http-2.pcap");
        verifyThat("#Ether-IP-version", hasText("4"));
        verifyThat("#Ether-IP-TCP-seq", hasText("951057939"));
    }

    @Test
    public void load_and_save_pcap_file_neg() {
        loadPcapFile("http.pcap");
        savePcapFileEx("/http-2.pcap", true);
    }

}
