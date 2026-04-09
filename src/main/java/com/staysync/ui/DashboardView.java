package com.staysync.ui;

import com.staysync.data.DataStore;
import com.staysync.model.Booking;
import com.staysync.model.RoomStatus;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class DashboardView {

    public static VBox create(BorderPane owner) {
        VBox view = new VBox(24);
        view.setPadding(new Insets(32));
        view.setStyle("-fx-background-color: #EEF4F4;");

        Label title    = new Label("Dashboard");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        Label subtitle = new Label("Hotel Operations Overview");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        Label availableVal = new Label();
        availableVal.textProperty().bind(Bindings.createStringBinding(
                () -> String.valueOf(countByStatus(RoomStatus.AVAILABLE)),
                DataStore.getRooms()));

        Label occupiedVal = new Label();
        occupiedVal.textProperty().bind(Bindings.createStringBinding(
                () -> String.valueOf(countByStatus(RoomStatus.BOOKED)),
                DataStore.getRooms()));

        Label cleaningVal = new Label();
        cleaningVal.textProperty().bind(Bindings.createStringBinding(
                () -> String.valueOf(countByStatus(RoomStatus.CLEANING)),
                DataStore.getRooms()));

        Label maintenanceVal = new Label();
        maintenanceVal.textProperty().bind(Bindings.createStringBinding(
                () -> String.valueOf(countByStatus(RoomStatus.MAINTENANCE)),
                DataStore.getRooms()));

        Label revenueVal = new Label();
        revenueVal.textProperty().bind(Bindings.createStringBinding(
                () -> String.format("Rs. %.2f", DataStore.getCompletedBookings().stream()
                        .mapToDouble(cb -> cb.getAmountPaid())
                        .sum()),
                DataStore.getCompletedBookings()));

        FlowPane cards = new FlowPane(20, 20);
        cards.getChildren().addAll(
            card("\ud83c\udfe0 Available",   availableVal,   "#6CB2B2", "Ready to book"),
            card("\ud83d\udccb Occupied",    occupiedVal,    "#FB9119", "Currently occupied"),
            card("\u2728 Cleaning",          cleaningVal,    "#f39c12", "Awaiting housekeeping"),
            card("\u26a0 Maintenance",       maintenanceVal, "#e74c3c", "Under repair"),
            card("\ud83d\udcb0 Revenue",     revenueVal,     "#27AE60", "Completed checkouts")
        );

        Label tableTitle = new Label("Recent Bookings");
        tableTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        TableView<Booking> table = new TableView<>(DataStore.getBookings());
        table.setPlaceholder(new Label("No bookings yet."));
        table.getStyleClass().add("styled-table");

        TableColumn<Booking, String> custCol  = new TableColumn<>("Customer");
        custCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getCustomer().getName()));
        TableColumn<Booking, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getCustomer().getPhone()));
        TableColumn<Booking, String> roomCol  = new TableColumn<>("Room");
        roomCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getRoom().getRoomNo()));
        TableColumn<Booking, String> nightsCol= new TableColumn<>("Nights");
        nightsCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(String.valueOf(cd.getValue().getNights())));
        TableColumn<Booking, String> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                String.format("Rs. %.2f", cd.getValue().getTotalPrice())));

        table.getColumns().add(custCol);
        table.getColumns().add(phoneCol);
        table.getColumns().add(roomCol);
        table.getColumns().add(nightsCol);
        table.getColumns().add(totalCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        view.getChildren().addAll(new VBox(4, title, subtitle),
                cards, tableTitle, table);
        return view;
    }

    private static VBox card(String title, Label valueLabel, String color, String desc) {
        VBox c = new VBox(8);
        c.setPadding(new Insets(22));
        c.setAlignment(Pos.CENTER_LEFT);
        c.setStyle("-fx-background-color: white; -fx-background-radius: 12;" +
                   "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 12, 0, 0, 3);");
        HBox.setHgrow(c, Priority.ALWAYS);
        Label tl = new Label(title); tl.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");
        valueLabel.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label dl = new Label(desc);  dl.setStyle("-fx-font-size: 11px; -fx-text-fill: #bdc3c7;");
        c.getChildren().addAll(tl, valueLabel, dl);
        return c;
    }

    private static long countByStatus(RoomStatus status) {
        return DataStore.getRooms().stream().filter(r -> r.getStatus() == status).count();
    }
}
