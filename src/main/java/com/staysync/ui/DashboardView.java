package com.staysync.ui;

import com.staysync.data.DataStore;
import com.staysync.model.Booking;
import com.staysync.model.Room;
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

        long available = DataStore.getRooms().stream().filter(r -> r.getStatus() == com.staysync.model.RoomStatus.AVAILABLE).count();
        long occupied  = DataStore.getRooms().stream().filter(r -> r.getStatus() == com.staysync.model.RoomStatus.BOOKED).count();
        long cleaning  = DataStore.getRooms().stream().filter(r -> r.getStatus() == com.staysync.model.RoomStatus.CLEANING).count();
        long maint     = DataStore.getRooms().stream().filter(r -> r.getStatus() == com.staysync.model.RoomStatus.MAINTENANCE).count();
        double revenue = 0.0;

        FlowPane cards = new FlowPane(20, 20);
        cards.getChildren().addAll(
            card("\ud83c\udfe0 Available",   String.valueOf(available), "#6CB2B2", "Ready to book"),
            card("\ud83d\udccb Occupied",    String.valueOf(occupied),  "#FB9119", "Currently occupied"),
            card("\u2728 Cleaning",          String.valueOf(cleaning),  "#f39c12", "Awaiting housekeeping"),
            card("\u26a0 Maintenance",       String.valueOf(maint),     "#e74c3c", "Under repair"),
            card("\ud83d\udcb0 Revenue",     String.format("Rs. %.2f", revenue), "#27AE60", "Completed checkouts")
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

    private static VBox card(String title, String value, String color, String desc) {
        VBox c = new VBox(8);
        c.setPadding(new Insets(22));
        c.setAlignment(Pos.CENTER_LEFT);
        c.setStyle("-fx-background-color: white; -fx-background-radius: 12;" +
                   "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 12, 0, 0, 3);");
        HBox.setHgrow(c, Priority.ALWAYS);
        Label tl = new Label(title); tl.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");
        Label vl = new Label(value); vl.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label dl = new Label(desc);  dl.setStyle("-fx-font-size: 11px; -fx-text-fill: #bdc3c7;");
        c.getChildren().addAll(tl, vl, dl);
        return c;
    }
}
