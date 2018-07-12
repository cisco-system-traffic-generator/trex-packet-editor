package com.xored.javafx.packeteditor.data;

import com.google.gson.*;
import com.google.inject.Inject;
import com.xored.javafx.packeteditor.data.user.Document;
import com.xored.javafx.packeteditor.metatdata.FEInstructionParameterMeta;
import com.xored.javafx.packeteditor.metatdata.FeParameterMeta;
import com.xored.javafx.packeteditor.metatdata.InstructionExpressionMeta;
import com.xored.javafx.packeteditor.service.IMetadataService;
import javafx.scene.control.Alert;
import javafx.scene.layout.HBox;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.Text;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HighLevelVmImporter {
    @Inject
    IMetadataService metadataService;

    private static final String INSTRUCTIONS = "instructions";
    private static final String GLOBAL_PARAMETERS = "global_parameters";
    private static final int WRAPPING_WIDTH = 350;

    private Gson gson = new Gson();

    public void importToUserModel(Document userModel, String hlvm) {
        List<String> issues = new ArrayList<>();
        JsonObject vm = gson.fromJson(hlvm, JsonElement.class).getAsJsonObject();

        if (vm.has(INSTRUCTIONS)) {
            parseInstructions(userModel, vm.get(INSTRUCTIONS).getAsJsonArray(), issues);
        }

        if (vm.has(GLOBAL_PARAMETERS)) {
            parseGlobalParameters(userModel, vm.get(GLOBAL_PARAMETERS).getAsJsonObject(), issues);
        }

        if (!issues.isEmpty()) {
            showAlert(issues);
        }
    }

    private void parseInstructions(Document userModel, JsonArray instructions, List<String> issues) {
        for (int i = 0; i < instructions.size(); i++) {
            JsonObject instr = instructions.get(i).getAsJsonObject();
            String id = instr.get("id").getAsString();

            if (!metadataService.getFeInstructions().containsKey(id)) {
                issues.add(MessageFormat.format("Command with id \"{0}\" doesn't exist",
                        id));
                continue;
            }

            InstructionExpressionMeta instructionMeta = metadataService.getFeInstructions().get(id);
            JsonObject instr_params = instr.get("parameters").getAsJsonObject();
            List<String> unvisited_params = instr_params.entrySet().stream().map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            List<FEInstructionParameter2> parameters = instructionMeta.getParameterMetas().stream().map(meta -> {
                JsonElement resultValue = new JsonPrimitive(meta.getDefaultValue());
                if (instr_params.has(meta.getId())) {
                    JsonElement receivedValue = instr_params.get(meta.getId());
                    if (!receivedValue.isJsonNull()) {
                        if (meta.getType().equals(FEInstructionParameterMeta.Type.EXPRESSION)) {
                            String expression = new Gson().toJson(receivedValue);
                            resultValue = FEInstructionParameter2.createExpressionValue(expression);
                        } else {
                            resultValue = new JsonPrimitive(receivedValue.getAsString());
                        }
                    }
                    unvisited_params.remove(meta.getId());
                }
                return new FEInstructionParameter2(meta, resultValue);
            })
            .collect(Collectors.toList());

            unvisited_params.forEach((param) ->
                issues.add(MessageFormat.format("Parameter \"{0}\", in instruction \"{1}\" was not found.",
                        param,
                        id))
            );

            InstructionExpression instruction = new InstructionExpression(instructionMeta, parameters);
            userModel.addInstruction(instruction);
        }
    }

    private void parseGlobalParameters(Document userModel, JsonObject global_parameters, List<String> issues) {
        global_parameters.entrySet().forEach(entry -> {
            if (metadataService.getFeParameters().containsKey(entry.getKey())) {
                FeParameterMeta instructionMeta = metadataService.getFeParameters().get(entry.getKey());
                userModel.createFePrarameter(instructionMeta, entry.getValue().getAsString());
            } else {
                issues.add(MessageFormat.format("Global parameter \"{0}\", was not found.",
                        entry.getKey()));
            }
        });
    }

    private void showAlert(List<String> issues) {
        Alert alert = new Alert(Alert.AlertType.WARNING);

        String content = String.join("\n", issues);
        Text text = new Text(content);
        text.setWrappingWidth(WRAPPING_WIDTH);
        text.setFontSmoothingType(FontSmoothingType.LCD);

        HBox container = new HBox();
        container.getChildren().add(text);
        alert.getDialogPane().setContent(container);

        alert.setHeaderText("There was errors while importing current stream");
        alert.showAndWait();
    }
}
