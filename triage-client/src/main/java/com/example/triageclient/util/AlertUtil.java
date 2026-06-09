package com.example.triageclient.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public final class AlertUtil {
    private AlertUtil() {
    }

    public static void showWarning(String message) {
        show(Alert.AlertType.WARNING, "输入提示", message);
    }

    public static void showError(String message) {
        show(Alert.AlertType.ERROR, "系统提示", message);
    }

    private static void show(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
