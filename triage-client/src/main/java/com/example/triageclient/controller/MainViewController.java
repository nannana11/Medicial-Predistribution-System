package com.example.triageclient.controller;

import com.example.triageclient.dto.TriageResponse;
import com.example.triageclient.service.TriageApiException;
import com.example.triageclient.service.TriageApiService;
import com.example.triageclient.util.AlertUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

public class MainViewController {
    private static final int MAX_MESSAGE_LENGTH = 1000;

    @FXML
    private TextArea symptomInput;
    @FXML
    private Button submitButton;
    @FXML
    private Button clearButton;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label statusLabel;
    @FXML
    private VBox resultPanel;
    @FXML
    private Label departmentLabel;
    @FXML
    private Label urgencyLabel;
    @FXML
    private Label emergencyLabel;
    @FXML
    private Label replyLabel;

    private final TriageApiService apiService = new TriageApiService();

    @FXML
    private void initialize() {
        resultPanel.setVisible(false);
        resultPanel.setManaged(false);
        progressIndicator.setVisible(false);
        statusLabel.setText("请尽量完整描述症状、持续时间和严重程度。");
    }

    @FXML
    private void handleSubmit() {
        String message = symptomInput.getText() == null ? "" : symptomInput.getText().trim();
        if (message.isEmpty()) {
            AlertUtil.showWarning("请输入您的症状描述后再提交。");
            symptomInput.requestFocus();
            return;
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            AlertUtil.showWarning("症状描述不能超过 " + MAX_MESSAGE_LENGTH + " 个字符，请适当精简。");
            symptomInput.requestFocus();
            return;
        }

        setLoading(true);
        apiService.submitSymptoms(message).whenComplete((response, throwable) ->
                Platform.runLater(() -> {
                    setLoading(false);
                    if (throwable != null) {
                        showRequestError(throwable);
                    } else {
                        showResult(response);
                    }
                }));
    }

    @FXML
    private void handleClear() {
        symptomInput.clear();
        resultPanel.setVisible(false);
        resultPanel.setManaged(false);
        statusLabel.setText("请尽量完整描述症状、持续时间和严重程度。");
        symptomInput.requestFocus();
    }

    private void showResult(TriageResponse response) {
        departmentLabel.setText(displayValue(response.getRecommendedDepartment(), "请参考下方系统建议"));
        urgencyLabel.setText(formatUrgency(response.getUrgencyLevel()));
        emergencyLabel.setText(response.isNeedEmergency() ? "是，请立即联系现场医护人员" : "否");
        emergencyLabel.getStyleClass().removeAll("emergency", "normal");
        emergencyLabel.getStyleClass().add(response.isNeedEmergency() ? "emergency" : "normal");
        replyLabel.setText(response.getReply());
        resultPanel.setManaged(true);
        resultPanel.setVisible(true);
        statusLabel.setText("预分诊建议已生成。");
    }

    private void showRequestError(Throwable throwable) {
        Throwable displayCause = findApiException(throwable);
        String message = displayCause.getMessage() == null || displayCause.getMessage().isBlank()
                ? "当前无法获取预分诊建议，请联系工作人员。"
                : displayCause.getMessage();
        statusLabel.setText("提交失败，请检查后重试。");
        AlertUtil.showError(message);
    }

    private Throwable findApiException(Throwable throwable) {
        Throwable current = throwable;
        Throwable deepest = throwable;
        while (current != null) {
            if (current instanceof TriageApiException) {
                return current;
            }
            deepest = current;
            current = current.getCause();
        }
        return deepest;
    }

    private void setLoading(boolean loading) {
        submitButton.setDisable(loading);
        clearButton.setDisable(loading);
        symptomInput.setDisable(loading);
        progressIndicator.setVisible(loading);
        statusLabel.setText(loading ? "正在生成预分诊建议，请稍候……"
                : "请尽量完整描述症状、持续时间和严重程度。");
    }

    private String displayValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String formatUrgency(String urgency) {
        if (urgency == null || urgency.isBlank()) {
            return "未说明";
        }
        return switch (urgency.trim().toLowerCase()) {
            case "low" -> "低";
            case "medium" -> "中";
            case "high" -> "高";
            case "unknown" -> "待补充";
            case "emergency", "critical" -> "紧急";
            default -> urgency;
        };
    }
}
