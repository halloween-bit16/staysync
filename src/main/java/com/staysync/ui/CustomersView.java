package com.staysync.ui;

import com.staysync.data.DataStore;
import com.staysync.model.Customer;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class CustomersView {

    public static VBox create() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(32));
        view.setStyle("-fx-background-color: #EEF4F4;");

        Label title    = new Label("Customers");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        Label subtitle = new Label("Current hotel guests — search by name or phone");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        TextField searchField = new TextField();
        searchField.setPromptText("Search by name or phone…");
        searchField.getStyleClass().add("form-field");
        searchField.setMaxWidth(320);

        FilteredList<Customer> filtered = new FilteredList<>(DataStore.getCustomers(), c -> true);
        searchField.textProperty().addListener((obs, ov, nv) -> {
            String q = nv.trim().toLowerCase();
            filtered.setPredicate(c -> q.isEmpty()
                    || c.getName().toLowerCase().contains(q)
                    || c.getPhone().toLowerCase().contains(q));
        });

        TableView<Customer> table = new TableView<>(filtered);
        table.setPlaceholder(new Label("No guests found."));
        table.getStyleClass().add("styled-table");

        TableColumn<Customer, String> nameCol = new TableColumn<>("Customer Name");
        nameCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getName()));

        TableColumn<Customer, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getPhone()));

        TableColumn<Customer, String> roomCol = new TableColumn<>("Room");
        roomCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper("Room " + cd.getValue().getRoomNo()));
        roomCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else { setText(item); setStyle("-fx-font-weight: bold; -fx-text-fill: #2C3E50;"); }
            }
        });

        TableColumn<Customer, String> checkInCol = new TableColumn<>("Check-In");
        checkInCol.setCellValueFactory(cd -> {
            var booking = DataStore.getBookings().stream()
                    .filter(b -> b.getRoom().getRoomNo().equals(cd.getValue().getRoomNo()))
                    .findFirst();
            return new ReadOnlyStringWrapper(
                    booking.map(b -> b.getCheckInDate() != null ? b.getCheckInDate().toString() : "—")
                           .orElse("—"));
        });

        TableColumn<Customer, String> nightsCol = new TableColumn<>("Nights");
        nightsCol.setCellValueFactory(cd -> {
            var booking = DataStore.getBookings().stream()
                    .filter(b -> b.getRoom().getRoomNo().equals(cd.getValue().getRoomNo()))
                    .findFirst();
            return new ReadOnlyStringWrapper(
                    booking.map(b -> String.valueOf(b.getNights())).orElse("—"));
        });

        table.getColumns().add(nameCol);
        table.getColumns().add(phoneCol);
        table.getColumns().add(roomCol);
        table.getColumns().add(checkInCol);
        table.getColumns().add(nightsCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        view.getChildren().addAll(new VBox(4, title, subtitle), searchField, table);
        return view;
    }
}
