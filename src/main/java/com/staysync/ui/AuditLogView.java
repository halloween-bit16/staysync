package com.staysync.ui;

import com.staysync.data.DataStore;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class AuditLogView {

    public static VBox create() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(32));
        view.setStyle("-fx-background-color: #EEF4F4;");

        Label title    = new Label("Audit Log");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        Label subtitle = new Label("Admin-only: full record of all actions");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        ListView<String> logList = new ListView<>(DataStore.getAuditLog());
        logList.getStyleClass().add("styled-table");
        logList.setStyle("-fx-font-size: 13px; -fx-font-family: 'Consolas', monospace;");
        VBox.setVgrow(logList, Priority.ALWAYS);

        Button clearBtn = new Button("Clear View");
        clearBtn.setStyle(
            "-fx-background-color: #e74c3c; -fx-text-fill: white;" +
            "-fx-background-radius: 7; -fx-cursor: hand; -fx-padding: 7 16;");
        clearBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Clear Audit Log View");
            confirm.setHeaderText(null);
            confirm.setContentText("This clears the on-screen view only. DB records are preserved. Continue?");
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.OK) DataStore.getAuditLog().clear();
            });
        });

        view.getChildren().addAll(new VBox(4, title, subtitle), logList, clearBtn);
        return view;
    }
}
