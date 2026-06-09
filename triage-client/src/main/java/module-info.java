module com.example.triageclient {
    requires com.fasterxml.jackson.databind;
    requires java.net.http;
    requires jdk.httpserver;
    requires javafx.controls;
    requires javafx.fxml;

    exports com.example.triageclient;
    opens com.example.triageclient.controller to javafx.fxml;
    opens com.example.triageclient.dto to com.fasterxml.jackson.databind;
}
