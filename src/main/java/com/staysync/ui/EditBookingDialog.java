package com.staysync.ui;

import com.staysync.data.DataStore;
import com.staysync.model.*;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class EditBookingDialog extends Dialog<Void> {

    private final Booking   booking;
    private final TableView<?> ownerTable;

    public EditBookingDialog(Booking booking, TableView<?> ownerTable) {
        this.booking    = booking;
        this.ownerTable = ownerTable;
        buildUI();
    }

    private void buildUI() {
        setTitle("Edit Booking – Room " + booking.getRoom().getRoomNo());
        setHeaderText(null);
        getDialogPane().setStyle("-fx-background-color: white;");

        GridPane form = new GridPane();
        form.setHgap(14);
        form.setVgap(12);
        form.setPadding(new Insets(20));

        TextField nameField  = new TextField(booking.getCustomer().getName());
        nameField.getStyleClass().add("form-field");

        TextField phoneField = new TextField(booking.getCustomer().getPhone());
        phoneField.getStyleClass().add("form-field");

        ComboBox<Room> roomCombo = new ComboBox<>();
        roomCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Room r) {
                return r == null ? "" : "Room " + r.getRoomNo() + " – " + r.getType()
                        + "  ($" + String.format("%.2f", r.getPrice()) + "/night)";
            }
            @Override public Room fromString(String s) { return null; }
        });
        var available = DataStore.getRooms().stream()
                .filter(r -> r.isAvailable() || r.getRoomNo()
                        .equals(booking.getRoom().getRoomNo()))
                .toList();
        roomCombo.getItems().setAll(available);
        roomCombo.setValue(booking.getRoom());
        roomCombo.setMaxWidth(Double.MAX_VALUE);

        DatePicker ciPicker = new DatePicker(booking.getCheckInDate());
        DatePicker coPicker = new DatePicker(booking.getCheckOutDate());

        Label nightsLbl = new Label(computeNightsText(booking.getCheckInDate(), booking.getCheckOutDate()));
        nightsLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #6CB2B2;");

        ciPicker.valueProperty().addListener((o, ov, nv) ->
                nightsLbl.setText(computeNightsText(nv, coPicker.getValue())));
        coPicker.valueProperty().addListener((o, ov, nv) ->
                nightsLbl.setText(computeNightsText(ciPicker.getValue(), nv)));

        Label lbl = label("Guest Name:");
        form.add(lbl,                      0, 0); form.add(nameField,  1, 0);
        form.add(label("Phone:"),          0, 1); form.add(phoneField, 1, 1);
        form.add(label("Room:"),           0, 2); form.add(roomCombo,  1, 2);
        form.add(label("Check-In:"),       0, 3); form.add(ciPicker,   1, 3);
        form.add(label("Check-Out:"),      0, 4); form.add(coPicker,   1, 4);
        form.add(label("Stay:"),           0, 5); form.add(nightsLbl,  1, 5);

        ColumnConstraints labelCol = new ColumnConstraints(110);
        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(labelCol, fieldCol);

        getDialogPane().setContent(form);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;

            String name     = nameField.getText().trim();
            String phone    = phoneField.getText().trim();
            Room   newRoom  = roomCombo.getValue();
            LocalDate ci    = ciPicker.getValue();
            LocalDate co    = coPicker.getValue();

            if (name.isEmpty() || phone.isEmpty() || newRoom == null || ci == null || co == null) {
                new Alert(Alert.AlertType.ERROR, "All fields are required.").showAndWait();
                return null;
            }
            if (phone.replaceAll("\\D", "").length() != 10) {
                new Alert(Alert.AlertType.ERROR, "Phone number must be exactly 10 digits.").showAndWait();
                return null;
            }
            if (!co.isAfter(ci)) {
                new Alert(Alert.AlertType.ERROR, "Check-out must be after check-in.").showAndWait();
                return null;
            }

            int    nights = (int) ChronoUnit.DAYS.between(ci, co);
            double total  = nights * newRoom.getPrice();

            Room oldRoom = booking.getRoom();
            if (!newRoom.getRoomNo().equals(oldRoom.getRoomNo())) {
                oldRoom.setStatus(RoomStatus.CLEANING);
                newRoom.setStatus(RoomStatus.BOOKED);
                com.staysync.data.DatabaseManager.updateRoomStatus(oldRoom.getRoomNo(), RoomStatus.CLEANING);
                com.staysync.data.DatabaseManager.updateRoomStatus(newRoom.getRoomNo(), RoomStatus.BOOKED);
            }

            booking.getCustomer().setName(name);
            booking.getCustomer().setPhone(phone);
            booking.getCustomer().setRoomNo(newRoom.getRoomNo());
            booking.setRoom(newRoom);
            booking.setCheckInDate(ci);
            booking.setCheckOutDate(co);
            booking.setNights(nights);
            booking.setTotalPrice(total);

            com.staysync.data.DatabaseManager.saveBooking(booking);
            DataStore.addAuditEntry("Edited booking #" + booking.getId()
                    + " – Guest: " + name + ", Room: " + newRoom.getRoomNo());

            if (ownerTable != null) ownerTable.refresh();
            return null;
        });
    }

    private static String computeNightsText(LocalDate ci, LocalDate co) {
        if (ci == null || co == null || !co.isAfter(ci)) return "—";
        long n = ChronoUnit.DAYS.between(ci, co);
        return n + " night(s)";
    }

    private static Label label(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e;");
        return l;
    }
}
