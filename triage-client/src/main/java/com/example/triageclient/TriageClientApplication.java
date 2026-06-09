package com.example.triageclient;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class TriageClientApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                TriageClientApplication.class.getResource("/fxml/main-view.fxml"));
        Scene scene = new Scene(loader.load(), 920, 680);
        scene.getStylesheets().add(Objects.requireNonNull(
                TriageClientApplication.class.getResource("/css/style.css")).toExternalForm());

        stage.setTitle("医院智能预分诊辅助系统");
        stage.setMinWidth(560);
        stage.setMinHeight(480);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
