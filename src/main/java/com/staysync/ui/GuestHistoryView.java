package com.staysync.ui;

import com.staysync.data.DataStore;
import com.staysync.model.CompletedBooking;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class GuestHistoryView {

    public static VBox create() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(32));
        view.setStyle("-fx-background-color: #EEF4F4;");

        Label title    = new Label("Guest History");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        Label subtitle = new Label("Search past stays by phone number");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        HBox searchBar = new HBox(12);
        TextField phoneField = new TextField();
        phoneField.setPromptText("Enter guest phone number…");
        phoneField.getStyleClass().add("form-field");
        HBox.setHgrow(phoneField, Priority.ALWAYS);

        Button searchBtn = new Button("🔍  Search");
        searchBtn.getStyleClass().add("primary-btn");

        searchBar.getChildren().addAll(phoneField, searchBtn);

        Label summaryLbl = new Label();
        summaryLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        TableView<CompletedBooking> table = new TableView<>();
        table.setPlaceholder(new Label("Search by phone number to view a guest's history."));
        table.getStyleClass().add("styled-table");

        TableColumn<CompletedBooking, String> nameCol = new TableColumn<>("Guest");
        nameCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getCustomerName()));

        TableColumn<CompletedBooking, String> roomCol = new TableColumn<>("Room");
        roomCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getRoomNo()));

        TableColumn<CompletedBooking, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getRoomType()));

        TableColumn<CompletedBooking, String> ciCol = new TableColumn<>("Check-In");
        ciCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                cd.getValue().getCheckInDate() != null ? cd.getValue().getCheckInDate().toString() : "—"));

        TableColumn<CompletedBooking, String> coCol = new TableColumn<>("Check-Out");
        coCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                cd.getValue().getCheckOutDate() != null ? cd.getValue().getCheckOutDate().toString() : "—"));

        TableColumn<CompletedBooking, String> nightsCol = new TableColumn<>("Nights");
        nightsCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                String.valueOf(cd.getValue().getNights())));

        TableColumn<CompletedBooking, String> payCol = new TableColumn<>("Payment");
        payCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getPaymentMethod()));

        TableColumn<CompletedBooking, String> paidCol = new TableColumn<>("Amount Paid");
        paidCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                String.format("Rs. %.2f", cd.getValue().getAmountPaid())));

        table.getColumns().add(nameCol);
        table.getColumns().add(roomCol);
        table.getColumns().add(typeCol);
        table.getColumns().add(ciCol);
        table.getColumns().add(coCol);
        table.getColumns().add(nightsCol);
        table.getColumns().add(payCol);
        table.getColumns().add(paidCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        Runnable doSearch = () -> {
            String phone = phoneField.getText().trim();
            if (phone.isEmpty()) {
                summaryLbl.setText("Please enter a phone number.");
                table.getItems().clear(); return;
            }
            var results = DataStore.getCompletedBookings().stream()
                    .filter(cb -> cb.getCustomerPhone().contains(phone))
                    .toList();
            table.getItems().setAll(results);
            double total = results.stream().mapToDouble(CompletedBooking::getAmountPaid).sum();
            summaryLbl.setText(results.isEmpty()
                    ? "No history found for: " + phone
                    : results.size() + " stay(s) found  |  Total Spent: Rs. " + String.format("%.2f", total));
        };

        searchBtn.setOnAction(e -> doSearch.run());
        phoneField.setOnAction(e -> doSearch.run());

        view.getChildren().addAll(new VBox(4, title, subtitle), searchBar,
                                  summaryLbl, table);
        return view;
    }
}
