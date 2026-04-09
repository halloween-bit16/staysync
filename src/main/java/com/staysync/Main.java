package com.staysync;

import com.staysync.data.DatabaseManager;
import com.staysync.ui.LoginView;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        DatabaseManager.initialize();
        primaryStage.setTitle("StaySync – Login");
        primaryStage.setResizable(false);
        primaryStage.setScene(LoginView.build(primaryStage));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
