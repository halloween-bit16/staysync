package com.staysync.ui;

import com.staysync.data.DataStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;

public class MainController {

    private final BorderPane root;
    private final StackPane  contentArea;
    private Button           activeButton = null;

    public MainController() {
        root = new BorderPane();
        root.setStyle("-fx-background-color: #EEF4F4;");
        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #EEF4F4;");
        root.setLeft(buildSidebar());
        root.setCenter(contentArea);
        showDashboard();
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: #1E2D3D; -fx-padding: 0;");

        VBox brand = new VBox(4);
        brand.setPadding(new Insets(28, 20, 24, 20));
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setStyle("-fx-background-color: #172330;");
        Label logo    = new Label("🏨  StaySync");
        logo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #6CB2B2;");
        Label tagline = new Label("Hotel Management");
        tagline.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

        Label roleTag = new Label("Role: " + DataStore.getCurrentUserRole());
        roleTag.setStyle("-fx-font-size: 10px; -fx-text-fill: #6CB2B2; -fx-padding: 4 0 0 0;");

        brand.getChildren().addAll(logo, tagline, roleTag);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #2c3e50;");

        VBox navArea = new VBox(4);
        navArea.setPadding(new Insets(16, 12, 16, 12));

        Button dashBtn     = navButton("📊", "Dashboard");
        Button roomsBtn    = navButton("🏠", "Rooms");
        Button bookingBtn  = navButton("📝", "Booking");
        Button custBtn     = navButton("👥", "Customers");
        Button checkoutBtn = navButton("🚪", "Checkout");
        Button auditBtn    = navButton("📋", "Audit Log");
        Button discountBtn = navButton("🏷", "Discounts");

        dashBtn.setOnAction(e     -> { setActive(dashBtn);     showDashboard(); });
        roomsBtn.setOnAction(e    -> { setActive(roomsBtn);    showRooms(); });
        bookingBtn.setOnAction(e  -> { setActive(bookingBtn);  showBooking(); });
        custBtn.setOnAction(e     -> { setActive(custBtn);     showCustomers(); });
        checkoutBtn.setOnAction(e -> { setActive(checkoutBtn); showCheckout(); });
        auditBtn.setOnAction(e    -> { setActive(auditBtn);    showAuditLog(); });
        discountBtn.setOnAction(e -> { setActive(discountBtn); showDiscountCodes(); });

        navArea.getChildren().addAll(dashBtn, roomsBtn, bookingBtn, custBtn, checkoutBtn);

        if (DataStore.isAdmin()) {
            navArea.getChildren().add(auditBtn);
            navArea.getChildren().add(discountBtn);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button logoutBtn = new Button("🔓  Logout");
        logoutBtn.setPrefWidth(196);
        logoutBtn.setPrefHeight(40);
        logoutBtn.setAlignment(Pos.CENTER_LEFT);
        logoutBtn.setPadding(new Insets(0, 0, 0, 14));
        logoutBtn.setStyle(
            "-fx-background-color: #c0392b;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;"
        );
        logoutBtn.setOnMouseEntered(e -> logoutBtn.setStyle(
            "-fx-background-color: #e74c3c; -fx-text-fill: white;" +
            "-fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8; -fx-cursor: hand;"));
        logoutBtn.setOnMouseExited(e -> logoutBtn.setStyle(
            "-fx-background-color: #c0392b; -fx-text-fill: white;" +
            "-fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8; -fx-cursor: hand;"));

        logoutBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Logout");
            confirm.setHeaderText(null);
            confirm.setContentText("Are you sure you want to logout?");
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.OK) {
                    DataStore.setCurrentUserRole("RECEPTIONIST");
                    DataStore.addAuditEntry("Logged out");

                    javafx.stage.Stage stage =
                        (javafx.stage.Stage) root.getScene().getWindow();
                    stage.setMaximized(false);
                    stage.setResizable(false);
                    stage.setScene(LoginView.build(stage));
                    stage.setTitle("StaySync – Login");
                    stage.centerOnScreen();
                }
            });
        });

        Separator logoutSep = new Separator();
        logoutSep.setStyle("-fx-background-color: #2c3e50; -fx-padding: 0 12;");

        VBox logoutArea = new VBox(8, logoutSep, logoutBtn);
        logoutArea.setPadding(new Insets(0, 12, 8, 12));

        Label version = new Label("v2.0  –  StaySync");
        version.setStyle("-fx-font-size: 10px; -fx-text-fill: #4a6572; -fx-padding: 4 0 12 20;");

        sidebar.getChildren().addAll(brand, sep, navArea, spacer, logoutArea, version);
        setActive(dashBtn);
        return sidebar;
    }

    private Button navButton(String icon, String label) {
        Button btn = new Button(icon + "  " + label);
        btn.setPrefWidth(196); btn.setPrefHeight(44);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(0, 0, 0, 14));
        btn.setStyle(navStyle(false));
        btn.setOnMouseEntered(e -> { if (btn != activeButton) btn.setStyle(navStyle(true)); });
        btn.setOnMouseExited (e -> { if (btn != activeButton) btn.setStyle(navStyle(false)); });
        return btn;
    }

    private void setActive(Button btn) {
        if (activeButton != null) activeButton.setStyle(navStyle(false));
        activeButton = btn;
        btn.setStyle(navActiveStyle());
    }

    private String navStyle(boolean hovered) {
        return "-fx-background-color: " + (hovered ? "#263d52" : "transparent") + ";" +
               "-fx-text-fill: #cfd8dc; -fx-font-size: 14px;" +
               "-fx-background-radius: 8; -fx-cursor: hand;";
    }

    private String navActiveStyle() {
        return "-fx-background-color: #6CB2B2; -fx-text-fill: white;" +
               "-fx-font-size: 14px; -fx-font-weight: bold;" +
               "-fx-background-radius: 8; -fx-cursor: hand;";
    }

    private void showDashboard()     { contentArea.getChildren().setAll(DashboardView.create(root)); }
    private void showRooms()         { contentArea.getChildren().setAll(RoomsView.create()); }
    private void showBooking()       { contentArea.getChildren().setAll(BookingView.create()); }
    private void showCustomers()     { contentArea.getChildren().setAll(CustomersView.create()); }
    private void showCheckout()      { contentArea.getChildren().setAll(CheckoutView.create()); }
    private void showAuditLog()      { contentArea.getChildren().setAll(AuditLogView.create()); }
    private void showDiscountCodes() { contentArea.getChildren().setAll(DiscountCodesView.create()); }

    public BorderPane getRoot() { return root; }
}
