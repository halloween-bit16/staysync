package com.staysync.ui;

import com.staysync.data.DataStore;
import com.staysync.data.DatabaseManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class LoginView {

    private static final String[][] USERS = {
        {"admin",     "admin123",  "ADMIN"},
        {"reception", "hotel123",  "RECEPTIONIST"}
    };

    public static Scene build(Stage stage) {
        VBox card = new VBox(20);
        card.setPadding(new Insets(40));
        card.setMaxWidth(400);
        card.setAlignment(Pos.CENTER);
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 20, 0, 0, 4);"
        );

        Label logo = new Label("🏨  StaySync");
        logo.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #6CB2B2;");

        Label tagline = new Label("Hotel Management Dashboard");
        tagline.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");

        Separator sep = new Separator();

        Label userLbl = new Label("Username");
        userLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");
        TextField userField = new TextField();
        userField.setPromptText("Enter username");
        userField.getStyleClass().add("form-field");
        userField.setMaxWidth(Double.MAX_VALUE);

        Label passLbl = new Label("Password");
        passLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Enter password");
        passField.getStyleClass().add("form-field");
        passField.setMaxWidth(Double.MAX_VALUE);

        Label errorLbl = new Label();
        errorLbl.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");

        Button loginBtn = new Button("Login  →");
        loginBtn.getStyleClass().add("primary-btn");
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        Runnable doLogin = () -> {
            String u = userField.getText().trim();
            String p = passField.getText().trim();
            String role = null;
            for (String[] cred : USERS) {
                if (cred[0].equals(u) && cred[1].equals(p)) { role = cred[2]; break; }
            }
            if (role == null) {
                errorLbl.setText("Invalid username or password.");
                passField.clear();
                return;
            }
            DataStore.setCurrentUserRole(role);
            DatabaseManager.initialize();
            DatabaseManager.loadAll();

            MainController ctrl = new MainController();
            Scene mainScene = new Scene(ctrl.getRoot(), 1200, 740);
            mainScene.getStylesheets().add(
                LoginView.class.getResource("/css/style.css").toExternalForm());
            DataStore.addAuditEntry("Logged in as " + role);

            stage.setScene(mainScene);
            stage.setTitle("StaySync – Hotel Management Dashboard");
            stage.setResizable(true);
            stage.setMaximized(true);
        };

        loginBtn.setOnAction(e -> doLogin.run());
        passField.setOnAction(e -> doLogin.run());

        card.getChildren().addAll(logo, tagline, sep, userLbl, userField,
                                  passLbl, passField, errorLbl, loginBtn);

        StackPane root = new StackPane(card);
        root.setStyle("-fx-background-color: #EEF4F4;");

        Scene loginScene = new Scene(root, 720, 520);
        loginScene.getStylesheets().add(
            LoginView.class.getResource("/css/style.css").toExternalForm());
        return loginScene;
    }
}
