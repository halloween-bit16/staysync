package com.staysync.ui;

import com.staysync.data.DataStore;
import com.staysync.data.DatabaseManager;
import com.staysync.model.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class BookingView {

    public static VBox create() {
        VBox view = new VBox();
        view.setStyle("-fx-background-color: #EEF4F4;");

        VBox header = new VBox(2);
        header.setPadding(new Insets(24, 32, 4, 32));
        Label title    = new Label("Room Booking");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        Label subtitle = new Label("Book an available room for a customer");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");
        header.getChildren().addAll(title, subtitle);

        VBox topPane = new VBox(14);
        topPane.setPadding(new Insets(16, 32, 16, 32));
        topPane.setStyle("-fx-background-color: #EEF4F4;");

        VBox formCard = new VBox(14);
        formCard.setPadding(new Insets(20));
        formCard.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 12, 0, 0, 3);"
        );

        Label formTitle = new Label("New Booking");
        formTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        GridPane form = new GridPane();
        form.setHgap(14); form.setVgap(10);

        TextField nameField  = field("Full name");
        TextField phoneField = field("Phone number");
        ComboBox<Room> roomCombo = new ComboBox<>();
        roomCombo.setPromptText("Select available room");
        roomCombo.setMaxWidth(Double.MAX_VALUE);
        roomCombo.getStyleClass().add("form-field");
        roomCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Room r) {
                return r == null ? "" : "Room " + r.getRoomNo() + " – " + r.getType()
                        + "  ($" + String.format("%.2f", r.getPrice()) + "/night)";
            }
            @Override public Room fromString(String s) { return null; }
        });
        refreshRoomCombo(roomCombo);

        DatePicker arrivalPicker   = new DatePicker(LocalDate.now());
        DatePicker departurePicker = new DatePicker(LocalDate.now().plusDays(1));
        arrivalPicker.setMaxWidth(Double.MAX_VALUE);
        departurePicker.setMaxWidth(Double.MAX_VALUE);

        Label summaryLbl = new Label("Select room and dates");
        summaryLbl.setStyle(
            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #6CB2B2;" +
            "-fx-background-color: #f0f9f9; -fx-background-radius: 6; -fx-padding: 6 10;");

        form.add(lbl("Guest Name:"),    0, 0); form.add(nameField,       1, 0);
        form.add(lbl("Phone:"),         2, 0); form.add(phoneField,      3, 0);
        form.add(lbl("Room:"),          0, 1); form.add(roomCombo,       1, 1);
        form.add(lbl("Arrival:"),       2, 1); form.add(arrivalPicker,   3, 1);
        form.add(lbl("Departure:"),     0, 2); form.add(departurePicker, 1, 2);
        form.add(summaryLbl,            2, 2, 2, 1);

        ColumnConstraints labelCC = new ColumnConstraints(100);
        labelCC.setHgrow(Priority.NEVER);
        ColumnConstraints fieldCC = new ColumnConstraints();
        fieldCC.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(labelCC, fieldCC, labelCC, fieldCC);

        Runnable computeSummary = () -> {
            LocalDate arr = arrivalPicker.getValue();
            LocalDate dep = departurePicker.getValue();
            Room r = roomCombo.getValue();
            if (arr != null && dep != null && dep.isAfter(arr) && r != null) {
                long n = ChronoUnit.DAYS.between(arr, dep);
                summaryLbl.setText(n + " night(s)  |  Total: $"
                        + String.format("%.2f", n * r.getPrice()));
            } else if (arr != null && dep != null && !dep.isAfter(arr)) {
                summaryLbl.setText("⚠ Departure must be after arrival");
                summaryLbl.setStyle(summaryLbl.getStyle().replace("#6CB2B2","#e74c3c"));
            } else {
                summaryLbl.setText("Select room and dates to see total");
            }
        };
        arrivalPicker.valueProperty().addListener((o,ov,nv)  -> computeSummary.run());
        departurePicker.valueProperty().addListener((o,ov,nv)-> computeSummary.run());
        roomCombo.valueProperty().addListener((o,ov,nv)      -> computeSummary.run());

        Button bookBtn = new Button("✔  Book Room");
        bookBtn.getStyleClass().add("accent-btn");

        bookBtn.setOnAction(e -> {
            String name  = nameField.getText().trim();
            String phone = phoneField.getText().trim();
            Room   room  = roomCombo.getValue();
            LocalDate arr = arrivalPicker.getValue();
            LocalDate dep = departurePicker.getValue();

            if (name.isEmpty() || phone.isEmpty() || room == null || arr == null || dep == null) {
                alert(Alert.AlertType.ERROR, "Missing Fields", "Please fill in all fields."); return;
            }
            if (phone.replaceAll("\\D", "").length() != 10) {
                alert(Alert.AlertType.ERROR, "Invalid Phone", "Phone number must be exactly 10 digits."); return;
            }
            if (!dep.isAfter(arr)) {
                alert(Alert.AlertType.ERROR, "Invalid Dates", "Departure must be after arrival."); return;
            }
            if (!room.isAvailable()) {
                alert(Alert.AlertType.ERROR, "Room Unavailable",
                        "Room " + room.getRoomNo() + " is no longer available.");
                refreshRoomCombo(roomCombo); return;
            }
            int nights    = (int) ChronoUnit.DAYS.between(arr, dep);
            double total  = nights * room.getPrice();

            Customer customer = new Customer(name, phone, room.getRoomNo());
            Booking  booking  = new Booking(customer, room, arr, dep, nights, total);

            room.setStatus(RoomStatus.BOOKED);
            DatabaseManager.updateRoomStatus(room.getRoomNo(), RoomStatus.BOOKED);
            DataStore.getCustomers().add(customer);
            DataStore.getBookings().add(booking);
            DatabaseManager.saveBooking(booking);
            DataStore.addAuditEntry("Booked Room " + room.getRoomNo() + " for " + name
                    + " (" + nights + " nights, $" + String.format("%.2f", total) + ")");

            nameField.clear(); phoneField.clear();
            roomCombo.setValue(null); refreshRoomCombo(roomCombo);
            arrivalPicker.setValue(LocalDate.now());
            departurePicker.setValue(LocalDate.now().plusDays(1));
            alert(Alert.AlertType.INFORMATION, "Booking Confirmed",
                    "Room " + room.getRoomNo() + " booked for " + name
                    + "\n" + nights + " nights  >>  Total: $" + String.format("%.2f", total));
        });

        formCard.getChildren().addAll(formTitle, form, bookBtn);
        topPane.getChildren().add(formCard);

        VBox bottomPane = new VBox(10);
        bottomPane.setPadding(new Insets(10, 32, 24, 32));
        bottomPane.setStyle("-fx-background-color: #EEF4F4;");

        Label tableTitle = new Label("Active Bookings");
        tableTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        Button exportBtn = new Button("📄  Export to CSV");
        exportBtn.setStyle("-fx-background-color: #6CB2B2; -fx-text-fill: white;" +
                          "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 5 14;");
        exportBtn.setOnAction(e -> {
            Stage stage = (Stage) exportBtn.getScene().getWindow();
            exportBookingsCSV(stage);
        });

        HBox tableHeader = new HBox(12, tableTitle, exportBtn);
        tableHeader.setAlignment(Pos.CENTER_LEFT);

        TableView<Booking> table = new TableView<>(DataStore.getBookings());
        table.setPlaceholder(new Label("No active bookings. Use the form above to create one."));
        table.getStyleClass().add("styled-table");
        table.setStyle("-fx-font-size: 13px;");

        TableColumn<Booking, String> custCol   = col("Customer",  400, cd ->
                new ReadOnlyStringWrapper(cd.getValue().getCustomer().getName()));
        TableColumn<Booking, String> phoneCol  = col("Phone",     130, cd ->
                new ReadOnlyStringWrapper(cd.getValue().getCustomer().getPhone()));
        TableColumn<Booking, String> roomCol   = col("Room",       80, cd ->
                new ReadOnlyStringWrapper(cd.getValue().getRoom().getRoomNo()));
        TableColumn<Booking, String> typeCol   = col("Type",       90, cd ->
                new ReadOnlyStringWrapper(cd.getValue().getRoom().getType()));
        TableColumn<Booking, String> ciCol     = col("Check-In",  110, cd ->
                new ReadOnlyStringWrapper(cd.getValue().getCheckInDate() != null
                        ? cd.getValue().getCheckInDate().toString() : "—"));
        TableColumn<Booking, String> coCol     = col("Check-Out", 110, cd ->
                new ReadOnlyStringWrapper(cd.getValue().getCheckOutDate() != null
                        ? cd.getValue().getCheckOutDate().toString() : "—"));
        TableColumn<Booking, String> nightsCol = col("Nights",     70, cd ->
                new ReadOnlyStringWrapper(String.valueOf(cd.getValue().getNights())));
        TableColumn<Booking, String> totalCol  = col("Total",      90, cd ->
                new ReadOnlyStringWrapper(String.format("$%.2f", cd.getValue().getTotalPrice())));

        TableColumn<Booking, Void> editCol = new TableColumn<>("Edit");
        editCol.setPrefWidth(90); editCol.setMinWidth(90);
        editCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("✎ Edit");
            { btn.setStyle("-fx-background-color: #6CB2B2; -fx-text-fill: white;" +
                           "-fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 4 8;");
              btn.setOnAction(e -> {
                  Booking b = getTableView().getItems().get(getIndex());
                  new EditBookingDialog(b, getTableView()).showAndWait();
                  getTableView().refresh();
              });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty); setGraphic(empty ? null : btn);
            }
        });

        TableColumn<Booking, Void> cancelCol = new TableColumn<>("Cancel");
        cancelCol.setPrefWidth(100); cancelCol.setMinWidth(100);
        cancelCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("✘ Cancel");
            { btn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;" +
                           "-fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 4 8;");
              btn.setOnAction(e -> {
                  Booking b = getTableView().getItems().get(getIndex());
                  Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                  confirm.setTitle("Cancel Booking");
                  confirm.setHeaderText(null);
                  confirm.setContentText("Cancel booking for " + b.getCustomer().getName()
                          + " in Room " + b.getRoom().getRoomNo() + "?");
                  confirm.showAndWait().ifPresent(r -> {
                      if (r == ButtonType.OK) cancelBooking(b, getTableView());
                  });
              });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty); setGraphic(empty ? null : btn);
            }
        });

        table.getColumns().add(custCol);  table.getColumns().add(phoneCol);
        table.getColumns().add(roomCol);  table.getColumns().add(typeCol);
        table.getColumns().add(ciCol);    table.getColumns().add(coCol);
        table.getColumns().add(nightsCol);table.getColumns().add(totalCol);
        table.getColumns().add(editCol);  table.getColumns().add(cancelCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        bottomPane.getChildren().addAll(tableHeader, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(bottomPane, Priority.ALWAYS);

        SplitPane split = new SplitPane(topPane, bottomPane);
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(0.38);
        VBox.setVgrow(split, Priority.ALWAYS);

        view.getChildren().addAll(header, split);
        VBox.setVgrow(split, Priority.ALWAYS);
        return view;
    }

    private static void cancelBooking(Booking b, TableView<Booking> table) {
        b.getRoom().setStatus(RoomStatus.CLEANING);
        DatabaseManager.updateRoomStatus(b.getRoom().getRoomNo(), RoomStatus.CLEANING);
        DataStore.getBookings().remove(b);
        DataStore.getCustomers().removeIf(c -> c.getRoomNo().equals(b.getRoom().getRoomNo()));
        DatabaseManager.deleteBooking(b.getId());
        DataStore.addAuditEntry("CANCELLED booking #" + b.getId()
                + " – " + b.getCustomer().getName() + ", Room " + b.getRoom().getRoomNo());
        table.refresh();
        new Alert(Alert.AlertType.INFORMATION,
                "Booking cancelled. Room " + b.getRoom().getRoomNo() + " set to Cleaning.").showAndWait();
    }

    private static void refreshRoomCombo(ComboBox<Room> combo) {
        List<Room> avail = DataStore.getRooms().stream().filter(Room::isAvailable).toList();
        combo.getItems().setAll(avail);
    }

    @SuppressWarnings("unchecked")
    private static <T> TableColumn<T, String> col(
            String title, double prefWidth,
            javafx.util.Callback<TableColumn.CellDataFeatures<T, String>,
                                  javafx.beans.value.ObservableValue<String>> factory) {
        TableColumn<T, String> col = new TableColumn<>(title);
        col.setCellValueFactory(factory);
        col.setPrefWidth(prefWidth);
        return col;
    }

    private static TextField field(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.getStyleClass().add("form-field");
        return f;
    }

    private static Label lbl(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e; -fx-font-weight: bold;");
        return l;
    }

    private static void alert(Alert.AlertType t, String title, String msg) {
        Alert a = new Alert(t); a.setTitle(title); a.setHeaderText(null);
        a.setContentText(msg); a.showAndWait();
    }

    private static void exportBookingsCSV(Stage owner) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Active Bookings CSV");
        fc.setInitialFileName("active_bookings.csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        var file = fc.showSaveDialog(owner);
        if (file == null) return;
        try (FileWriter fw = new FileWriter(file)) {
            fw.write("Guest Name,Phone,Room,Type,Check-In,Check-Out,Nights,Total\n");
            for (Booking b : DataStore.getBookings()) {
                fw.write(String.join(",",
                    q(b.getCustomer().getName()), q(b.getCustomer().getPhone()),
                    q(b.getRoom().getRoomNo()), q(b.getRoom().getType()),
                    q(b.getCheckInDate()  != null ? b.getCheckInDate().toString()  : ""),
                    q(b.getCheckOutDate() != null ? b.getCheckOutDate().toString() : ""),
                    String.valueOf(b.getNights()),
                    String.format("%.2f", b.getTotalPrice())
                ) + "\n");
            }
            new Alert(Alert.AlertType.INFORMATION,
                "Exported to:\n" + file.getAbsolutePath()).showAndWait();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Export failed: " + ex.getMessage()).showAndWait();
        }
    }

    private static String q(String s) {
        return "\"" + (s == null ? "" : s.replace("\"", "\"\"")) + "\"";
    }
}
