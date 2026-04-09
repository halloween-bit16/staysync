package com.staysync.ui;

import com.staysync.data.DataStore;
import com.staysync.data.DatabaseManager;
import com.staysync.model.Room;
import com.staysync.model.RoomStatus;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

public class RoomsView {

    public static VBox create() {
        VBox view = new VBox(24);
        view.setPadding(new Insets(32));
        view.setStyle("-fx-background-color: #EEF4F4;");

        Label title    = new Label("Room Management");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        Label subtitle = new Label("Manage room inventory and housekeeping status");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        if (DataStore.isAdmin()) {
            VBox formCard = buildAddForm();
            view.getChildren().addAll(new VBox(4, title, subtitle), formCard);
        } else {
            view.getChildren().add(new VBox(4, title, subtitle));
        }

        TextField searchField = new TextField();
        searchField.setPromptText("Search room number…");
        searchField.getStyleClass().add("form-field");
        searchField.setMaxWidth(200);

        ComboBox<String> typeFilter = new ComboBox<>(
            FXCollections.observableArrayList("All Types", "Single", "Double", "Suite", "Deluxe"));
        typeFilter.setValue("All Types");

        ComboBox<String> statusFilter = new ComboBox<>(
            FXCollections.observableArrayList("All Statuses", "AVAILABLE", "BOOKED", "CLEANING", "MAINTENANCE"));
        statusFilter.setValue("All Statuses");

        HBox filterBar = new HBox(12, searchField, typeFilter, statusFilter);

        Label tableTitle = new Label("All Rooms");
        tableTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        FilteredList<Room> filtered = new FilteredList<>(DataStore.getRooms(), r -> true);

        Runnable applyFilter = () -> filtered.setPredicate(r -> {
            String q  = searchField.getText().trim().toLowerCase();
            String ty = typeFilter.getValue();
            String st = statusFilter.getValue();
            boolean matchNo   = q.isEmpty() || r.getRoomNo().toLowerCase().contains(q);
            boolean matchType = "All Types".equals(ty) || r.getType().equals(ty);
            boolean matchSt   = "All Statuses".equals(st) || r.getStatus().name().equals(st);
            return matchNo && matchType && matchSt;
        });
        searchField.textProperty().addListener((o,ov,nv) -> applyFilter.run());
        typeFilter.valueProperty().addListener((o,ov,nv)  -> applyFilter.run());
        statusFilter.valueProperty().addListener((o,ov,nv)-> applyFilter.run());

        TableView<Room> table = new TableView<>(filtered);
        table.setPlaceholder(new Label("No rooms match the filter."));
        table.getStyleClass().add("styled-table");

        TableColumn<Room, String> roomNoCol = new TableColumn<>("Room No");
        roomNoCol.setCellValueFactory(new PropertyValueFactory<>("roomNo"));

        TableColumn<Room, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));

        TableColumn<Room, Double> priceCol = new TableColumn<>("Price / Night");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : String.format("Rs. %.2f", item));
            }
        });

        TableColumn<Room, RoomStatus> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> cd.getValue().statusProperty());
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(RoomStatus st, boolean empty) {
                super.updateItem(st, empty);
                if (empty || st == null) { setText(null); setStyle(""); return; }
                setText(st.getDisplay());
                setStyle("-fx-text-fill: " + st.getColor() + "; -fx-font-weight: bold;");
            }
        });

        TableColumn<Room, Void> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button cleanBtn = new Button("✔ Mark Clean");
            private final Button maintBtn = new Button("⚒ Maintenance");
            private final HBox   box      = new HBox(6, cleanBtn, maintBtn);
            {
                cleanBtn.setStyle("-fx-background-color: #27AE60; -fx-text-fill: white;" +
                                  "-fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 11px;");
                maintBtn.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white;" +
                                  "-fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 11px;");
                cleanBtn.setOnAction(e -> updateStatus(RoomStatus.AVAILABLE));
                maintBtn.setOnAction(e -> updateStatus(RoomStatus.MAINTENANCE));
            }
            private void updateStatus(RoomStatus st) {
                Room r = getTableView().getItems().get(getIndex());
                if (r.getStatus() == RoomStatus.BOOKED) {
                    new Alert(Alert.AlertType.ERROR, "Cannot change status of a booked room.").showAndWait();
                    return;
                }
                r.setStatus(st);
                DatabaseManager.updateRoomStatus(r.getRoomNo(), st);
                DataStore.addAuditEntry("Room " + r.getRoomNo() + " marked as " + st.name());
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });

        table.getColumns().add(roomNoCol);
        table.getColumns().add(typeCol);
        table.getColumns().add(priceCol);
        table.getColumns().add(statusCol);
        table.getColumns().add(actionCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        view.getChildren().addAll(filterBar, tableTitle, table);
        return view;
    }

    private static VBox buildAddForm() {
        VBox formCard = new VBox(16);
        formCard.setPadding(new Insets(24));
        formCard.setStyle("-fx-background-color: white; -fx-background-radius: 12;" +
                          "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 12, 0, 0, 3);");

        Label formTitle = new Label("Add New Room");
        formTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        GridPane form = new GridPane();
        form.setHgap(16); form.setVgap(12);

        TextField roomNoField = new TextField(); roomNoField.setPromptText("e.g. 101");
        roomNoField.getStyleClass().add("form-field");
        ComboBox<String> typeCombo = new ComboBox<>(
            FXCollections.observableArrayList("Single", "Double", "Suite", "Deluxe"));
        typeCombo.setPromptText("Room type");
        typeCombo.setMaxWidth(Double.MAX_VALUE);
        typeCombo.getStyleClass().add("form-field");
        TextField priceField = new TextField(); priceField.setPromptText("e.g. 150.00");
        priceField.getStyleClass().add("form-field");

        form.add(lbl("Room Number:"), 0, 0); form.add(roomNoField, 1, 0);
        form.add(lbl("Type:"),        2, 0); form.add(typeCombo,   3, 0);
        form.add(lbl("Price/Night:"), 0, 1); form.add(priceField,  1, 1);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(i % 2 == 1 ? Priority.ALWAYS : Priority.NEVER);
            form.getColumnConstraints().add(cc);
        }

        Button addBtn = new Button("✚  Add Room");
        addBtn.getStyleClass().add("primary-btn");
        addBtn.setOnAction(e -> {
            String no    = roomNoField.getText().trim();
            String ty    = typeCombo.getValue();
            String prTxt = priceField.getText().trim();
            if (no.isEmpty() || ty == null || prTxt.isEmpty()) {
                alert(Alert.AlertType.ERROR, "Missing Fields", "Fill all fields.");
                return;
            }
            if (DataStore.getRooms().stream().anyMatch(r -> r.getRoomNo().equalsIgnoreCase(no))) {
                alert(Alert.AlertType.ERROR, "Duplicate", "Room " + no + " already exists.");
                return;
            }
            try {
                double price = Double.parseDouble(prTxt);
                if (price <= 0) throw new NumberFormatException();
                Room room = new Room(no, ty, price, RoomStatus.AVAILABLE);
                DataStore.getRooms().add(room);
                DatabaseManager.saveRoom(room);
                DataStore.addAuditEntry("Added room " + no + " (" + ty + ")");
                roomNoField.clear(); typeCombo.setValue(null); priceField.clear();
                alert(Alert.AlertType.INFORMATION, "Room Added", "Room " + no + " added successfully!");
            } catch (NumberFormatException ex) {
                alert(Alert.AlertType.ERROR, "Invalid Price", "Enter a positive number.");
            }
        });

        formCard.getChildren().addAll(formTitle, form, addBtn);
        return formCard;
    }

    private static Label lbl(String t) {
        Label l = new Label(t); l.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e;");
        return l;
    }

    private static void alert(Alert.AlertType t, String title, String msg) {
        Alert a = new Alert(t); a.setTitle(title); a.setHeaderText(null);
        a.setContentText(msg); a.showAndWait();
    }
}
