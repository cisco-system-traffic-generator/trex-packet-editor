package com.xored.packeteditor;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import org.junit.Ignore;
import org.junit.Test;
import org.testfx.service.query.EmptyNodeQueryException;

import static javafx.scene.input.KeyCode.*;
import static org.junit.Assume.assumeFalse;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.util.NodeQueryUtils.hasText;

//@FixMethodOrder(MethodSorters.NAME_ASCENDING) // execute tests in order
public class TestPacketEditorUI extends TestPacketEditorUIBase {

    @Test
    public void shoud_has_as_raw_title_postfix() {
        newDocument();
        addLayerForce("TCP");
        addLayerForce("IP");
        verifyThat("#Ether-TCP-pane", (TitledPane pane) -> pane.getText().contains("as Raw"));
    }
    
    @Test
    public void should_preserve_collapse_state() {
        newDocument();
        verifyUserModelFieldDefault("#Ether-src");

        verifyThat("#Ether-pane", (TitledPane pane) -> pane.isExpanded() == true );
        verifyThat("#append-protocol-pane", (TitledPane pane) -> pane.isExpanded() == true );

        clickOn("#Ether-pane .title .arrow-button"); // collapse Ether pane

        verifyThat("#Ether-pane", (TitledPane pane) -> pane.isExpanded() == false );

        addLayerIPv4();
        // Ether should remain collapsed
        verifyThat("#Ether-pane", (TitledPane pane) -> pane.isExpanded() == false );
        // But newly added layer should be expanded
        verifyThat("#Ether-IP-pane", (TitledPane pane) -> pane.isExpanded() == true );
    }

    @Test
    public void should_save_textfield_values() {
        verifyUserModelFieldDefault("#Ether-src");
        clickOn("#Ether-src");
        push(ESCAPE);
        verifyUserModelFieldDefault("#Ether-src");
        clickOn("#Ether-src");
        push(ENTER);
        verifyUserModelFieldSet("#Ether-src");
    }

    @Ignore
    @Test
    public void should_save_enumfield_values() {
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
        clickOn("#Ether-src");
        write("00:11:22:33:44:55");
        push(ENTER);
        verifyUserModelFieldSet("#Ether-src");
        verifyThat("#Ether-src", hasText("00:11:22:33:44:55"));
    }

    @Test
    public void should_not_set_incorrect_field_value() {
        clickOn("#Ether-src");
        write("00:11:22:33:44:55:66"); // too long
        push(ENTER); // try to commit -> fails
        push(ESCAPE); // back to field view
        verifyUserModelFieldUnset("#Ether-src");
    }

    @Test
    public void should_select_all_field_content_on_field_click() {
        final int mac_text_length = 17;
        clickOn("#Ether-src");
        verifyThat("#Ether-src", (TextField tf) -> tf.getSelectedText().length() == mac_text_length);
    }

    @Test
    public void should_change_ether_type_and_back() {
        addLayerIPv4();
        expandAllProtocols();

        scrollFieldsUp();
        setFieldText("#Ether-type", "LOOP");

        scrollFieldsDown();
        setFieldText("#Ether-IP-src", "127.0.1.2");

        verifyThat("#Ether-type", (Label t) -> t.getText().contains("LOOP")); // default value for Ether
        verifyUserModelFieldSet("#Ether-type");
        verifyUserModelFieldSet("#Ether-IP-src");

        scrollFieldsUp();
        setFieldText("#Ether-type", "IPv4");
        verifyUserModelFieldSet("#Ether-type");
    }

    @Test
    public void should_create_nested_ether() {
        // Ether()/Ether() is converted to Dot3()/Ether()
        addLayerForce("Ether");
        verifyThat("#Ether-type", (Label t) -> t.getText().contains("0x1")); // type for Dot3 is interpreted as 0x1 by scapy
        verifyThat("#Ether-Ether-type", (Label t) -> t.getText().contains("LOOP")); // default value for Ether
    }

    @Test
    public void should_append_and_edit_abstract_protocols() {
        addLayer("Raw");
        addLayerForce("Ether");
        addLayerForce("IP");
        expandAllProtocols();

        setFieldText("#Ether-type", "LOOP");
        verifyThat("#Ether-type", (Label t) -> t.getText().contains("LOOP")); // default value for Ether

        setFieldText("#Ether-type", "LOOP");
        verifyThat("#Ether-Raw-Ether-type", (Label t) -> t.getText().contains("IPv4"));
        setFieldText("#Ether-Raw-Ether-type", "IPv6");
        verifyThat("#Ether-Raw-Ether-type", (Label t) -> t.getText().contains("IPv6"));
        verifyUserModelFieldSet("#Ether-type");
        verifyUserModelFieldSet("#Ether-Raw-Ether-type");

    }

    @Test
    public void should_build_tcpip_stack() {
        addLayerIPv4();
        expandPane("#Ether-pane");
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
    public void should_have_row_ctx_menu() {
        assumeFalse("headless not supported", isHeadless());

        showCtxMenu("#Ether-src-label");
        clickOn("Generate");
        verifyThat("#Ether-src", this::fieldLabelIsSet);

        showCtxMenu("#Ether-src-label");
        clickOn("Set to default");
        verifyThat("#Ether-src", this::fieldLabelIsUnSet);
    }

    @Test
    public void should_have_protocol_ctx_menu() {
        assumeFalse("headless not supported", isHeadless());
        // creates Ether/TCP/IP and tests protocol ctx menu(move, delete)
        addLayerForce("TCP");
        with("#Ether-pane", (TitledPane pane) -> pane.setExpanded(false));
        with("#Ether-TCP-pane", (TitledPane pane) -> pane.setExpanded(false));
        addLayerIPv4();
        with("#Ether-TCP-IP-pane", (TitledPane pane) -> pane.setExpanded(false));

        showCtxMenu("#Ether-TCP-pane");
        clickOn("Move Layer Down");
        showCtxMenu("#Ether-IP-TCP-pane");
        clickOn("Move Layer Up");

        verifyThat("#Ether-TCP-pane", (TitledPane pane) -> pane.isExpanded() == false );
        verifyThat("#Ether-TCP-IP-pane", (TitledPane pane) -> pane.isExpanded() == false);

        showCtxMenu("#Ether-TCP-pane");
        clickOn("Delete layer");
        showCtxMenu("#Ether-IP-pane");
        clickOn("Delete layer");
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
    public void load_pcap_file_collapse() {
        loadPcapFile("http.pcap");
        interrupt();
        // when file loaded from pcap we collapse all protcols
        verifyThat("#Ether-pane", (TitledPane pane) -> pane.isExpanded() == false );
        verifyThat("#Ether-IP-pane", (TitledPane pane) -> pane.isExpanded() == false );
        verifyThat("#Ether-IP-TCP-pane", (TitledPane pane) -> pane.isExpanded() == false);
        verifyThat("#Ether-IP-version", hasText("4"));
        verifyThat("#Ether-IP-TCP-seq", hasText("951057939"));
    }

    @Test
    public void template_tcp_syn() {
        clickOn("File");
        interrupt(200);
        clickOn("Templates");
        interrupt(200);
        moveTo("IPv4"); // can't clickOn directly, since it will hide while mouse is moving diagonal
        clickOn("TCP-SYN");
        interrupt(200);

        // Verify loaded values
        clickOn("#collapseAllBtn");
        interrupt(200);
        clickOn("#Ether-IP-TCP-pane");
        //with("#Ether-IP-TCP-pane", (TitledPane pane) -> pane.setExpanded(true));
        interrupt(200);
        verifyThat("#Ether-IP-TCP-flags", hasText("S"));
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
    public void should_support_large_payload() {
        loadPcapFile("payload_64k.pcap");
        verifyThat("#Ether-IP-version", hasText("4"));
        verifyThat("#Ether-IP-TCP-Raw-load", t -> t != null);
    }

    @Test
    public void load_and_save_pcap_file_neg() {
        loadPcapFile("http.pcap");
        savePcapFileEx("/...abracadabraZZZ/http-2.pcap", true);
    }

    @Test
    public void should_build_payload() {
        addLayer("Raw");
        interrupt();
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
        interrupt();
    }

    @Test
    public void should_build_payload_random_ascii() {
        should_build_payload();
        clickOn("#Ether-Raw-load");
        clickOn("#payloadChoiceType");
        clickOn("Random ascii");
        clickOn("#patternSize");
        interrupt();
        push(DIGIT3,DIGIT2);
        interrupt();
        clickOn("#payloadButtonSave");
        interrupt();
        verifyThat("#Ether-Raw-load", hasText("G n<B2U/+IU0TA{(|GO.d2@\"@{|f yri"));
    }

    @Test
    public void should_build_payload_random_non_ascii() {
        should_build_payload();
        clickOn("#Ether-Raw-load");
        clickOn("#payloadChoiceType");
        clickOn("Random non-ascii");
        clickOn("#patternSize");
        push(DIGIT3,DIGIT2);
        clickOn("#payloadButtonSave");
        interrupt();
        verifyThat("#Ether-Raw-load", hasText("<binary>"));
    }

    @Test
    public void should_build_payload_code() {
        should_build_payload();
        clickOn("#Ether-Raw-load");
        clickOn("#payloadChoiceType");
        clickOn("Code pattern");
        clickOn("#patternSize");
        push(DIGIT3,DIGIT2);
        clickOn("#codePatternText");
        push(DIGIT3,DIGIT2);
        clickOn("#payloadButtonSave");
        interrupt();
        verifyThat("#Ether-Raw-load", hasText("22222222222222222222222222222222"));
    }

    @Test
    public void should_build_payload_code_no_size() {
        should_build_payload();
        clickOn("#Ether-Raw-load");
        clickOn("#payloadChoiceType");
        clickOn("Code pattern");
        clickOn("#codePatternText");
        push(DIGIT3,DIGIT2);
        push(DIGIT3,DIGIT2);
        push(DIGIT3,DIGIT2);
        push(DIGIT3,DIGIT2);
        push(DIGIT3,DIGIT2);
        clickOn("#payloadButtonSave");
        interrupt();
        verifyThat("#Ether-Raw-load", hasText("22222"));
    }

    @Test
    public void should_save_template() {
        should_build_payload();
        clickOn("#Ether-Raw-load");
        clickOn("#payloadChoiceType");
        clickOn("Code pattern");
        clickOn("#codePatternText");
        push(DIGIT3,DIGIT2);
        push(DIGIT3,DIGIT2);
        push(DIGIT3,DIGIT2);
        push(DIGIT3,DIGIT2);
        push(DIGIT3,DIGIT2);
        clickOn("#payloadButtonSave");
        interrupt();
        verifyThat("#Ether-Raw-load", hasText("22222"));

        // Save as template
        clickOn("#saveMenuButton");
        clickOn("Save as template...");
        clickOn("OK");
        try {
            Node btn = (Node) lookup("OK").query();
            if (btn != null) {
                clickOn("OK");
            }
        }catch (EmptyNodeQueryException ex) {

        }
        interrupt();

        // Load saved template
        clickOn("#newTemplateMenuButton");
        moveTo("IPv4"); // can't clickOn directly, since it will hide while mouse is moving diagonal
        clickOn("NewTemplate");
        interrupt();

        // Verify loaded values
        clickOn("#expandAllBtn");
        interrupt();
        verifyThat("#Ether-Raw-load", hasText("22222"));
    }


    @Test
    public void should_save_template2() {
        should_build_payload();
        clickOn("#Ether-Raw-load");
        clickOn("#payloadChoiceType");
        clickOn("Code pattern");
        clickOn("#codePatternText");
        push(DIGIT3,DIGIT2);
        push(DIGIT3,DIGIT2);
        push(DIGIT3,DIGIT2);
        push(DIGIT3,DIGIT2);
        push(DIGIT3,DIGIT2);
        clickOn("#payloadButtonSave");
        interrupt();
        verifyThat("#Ether-Raw-load", hasText("22222"));

        // Save as template
        clickOn("#saveMenuButton");
        clickOn("Save as template...");
        push(DIGIT1,DIGIT2);
        push(SLASH);
        push(DIGIT3,DIGIT4);
        push(SLASH);
        push(DIGIT5,DIGIT6);
        push(SLASH);
        push(DIGIT7,DIGIT8);
        interrupt(200);
        clickOn("OK");
        try {
            Node btn = (Node) lookup("OK").query();
            if (btn != null) {
                clickOn("OK");
            }
        } catch (EmptyNodeQueryException ex) {

        }
        interrupt();

        // Load saved template
        clickOn("#newTemplateMenuButton");
        moveTo("IPv4"); // can't clickOn directly, since it will hide while mouse is moving diagonal
        moveTo("12");
        moveTo("34");
        moveTo("56");
        moveTo("78");
        clickOn("78");
        interrupt();

        // Verify loaded values
        clickOn("#expandAllBtn");
        interrupt();
        verifyThat("#Ether-Raw-load", hasText("22222"));
        interrupt();
    }

    @Test
    public void should_load_template() {
        clickOn("#newTemplateMenuButton");
        moveTo("IPv4"); // can't clickOn directly, since it will hide while mouse is moving diagonal
        moveTo("IPv6");
        moveTo("ICMP");
        moveTo("UDP");
        clickOn("UDP");
        interrupt();

        // Verify loaded values
        clickOn("#collapseAllBtn");
        interrupt();
        with("#Ether-pane", (TitledPane pane) -> pane.setExpanded(true));
        interrupt();
        verifyThat("#Ether-IPv6-UDP-dport", hasText("53"));
    }

}
