package com.staysync.ui;

import com.staysync.data.BillGenerator;
import com.staysync.data.DataStore;
import com.staysync.data.DatabaseManager;
import com.staysync.model.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

public class CheckoutView {

    public static VBox create() {
        VBox view = new VBox(24);
        view.setPadding(new Insets(32));
        view.setStyle("-fx-background-color: #EEF4F4;");

        Label title    = new Label("Checkout");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        Label subtitle = new Label("Process room checkout with payment recording");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        VBox lookupCard = new VBox(14);
        lookupCard.setPadding(new Insets(24));
        lookupCard.setMaxWidth(680);
        lookupCard.setStyle("-fx-background-color: white; -fx-background-radius: 12;" +
                           "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 12, 0, 0, 3);");

        Label formTitle = new Label("Process Checkout");
        formTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        HBox lookupRow = new HBox(12);
        TextField roomField = new TextField();
        roomField.setPromptText("Room number (e.g. 101)");
        roomField.getStyleClass().add("form-field");
        HBox.setHgrow(roomField, Priority.ALWAYS);
        Button findBtn = new Button("⌕  Find");
        findBtn.getStyleClass().add("primary-btn");
        lookupRow.getChildren().addAll(roomField, findBtn);

        Label guestLbl  = new Label();
        guestLbl.setStyle("-fx-text-fill: #2C3E50; -fx-font-size: 13px;");
        Label datesLbl  = new Label();
        datesLbl.setStyle("-fx-text-fill: #2C3E50; -fx-font-size: 13px;");
        Label nightsLbl = new Label();
        nightsLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #6CB2B2;");
        Label originalTotalLbl = new Label();
        originalTotalLbl.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");
        Label discountLineLbl  = new Label();
        discountLineLbl.setStyle("-fx-text-fill: #27AE60; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label totalLbl = new Label();
        totalLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #27AE60; -fx-font-size: 16px;");

        VBox summaryBox = new VBox(5, guestLbl, datesLbl, nightsLbl,
                new Separator(), originalTotalLbl, discountLineLbl, totalLbl);
        summaryBox.setVisible(false);

        Label discountSectionLbl = new Label("Discount Code (optional)");
        discountSectionLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #34495e;");

        HBox discountRow = new HBox(10);
        ComboBox<String> discountCombo = new ComboBox<>();
        discountCombo.setPromptText("Select discount code…");
        discountCombo.getStyleClass().add("form-field");
        discountCombo.setMaxWidth(220);
        
        Runnable refreshDiscounts = () -> {
            discountCombo.getItems().clear();
            DataStore.getDiscountCodes().stream()
                .filter(DiscountCode::isActive)
                .forEach(dc -> discountCombo.getItems().add(dc.getCode()));
        };
        refreshDiscounts.run();
        Button applyBtn = new Button("Apply");
        applyBtn.setStyle(
            "-fx-background-color: #6CB2B2; -fx-text-fill: white;" +
            "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 14;");
        Button removeBtn = new Button("✕ Remove");
        removeBtn.setStyle(
            "-fx-background-color: #e74c3c; -fx-text-fill: white;" +
            "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 14;");
        removeBtn.setVisible(false);
        Label discountMsgLbl = new Label();
        discountMsgLbl.setStyle("-fx-font-size: 12px;");
        discountRow.getChildren().addAll(discountCombo, applyBtn, removeBtn, discountMsgLbl);

        VBox discountBox = new VBox(8, discountSectionLbl, discountRow);
        discountBox.setVisible(false);

        Label payLbl = new Label("Payment Method:");
        payLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e;");
        ComboBox<String> payCombo = new ComboBox<>(
            FXCollections.observableArrayList("Cash", "Card", "Deposit"));
        payCombo.setValue("Cash");
        payCombo.setMaxWidth(200);
        payCombo.getStyleClass().add("form-field");

        Label amtLbl = new Label("Amount Received (Rs.):");
        amtLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e;");
        TextField amtField = new TextField();
        amtField.getStyleClass().add("form-field");
        amtField.setMaxWidth(200);

        VBox paymentBox = new VBox(10,
            new HBox(12, payLbl, payCombo),
            new HBox(12, amtLbl, amtField));
        paymentBox.setVisible(false);

        Button checkoutBtn = new Button("✔  Confirm Checkout");
        checkoutBtn.getStyleClass().add("primary-btn");
        checkoutBtn.setVisible(false);

        final Booking[] foundBooking    = {null};
        final double[]  discountedTotal = {0.0};
        final double[]  originalTotal   = {0.0};

        Runnable resetDiscount = () -> {
            discountCombo.setValue(null);
            discountMsgLbl.setText("");
            removeBtn.setVisible(false);
            discountLineLbl.setVisible(false);
            discountLineLbl.setText("");
            originalTotalLbl.setVisible(false);
            if (foundBooking[0] != null) {
                discountedTotal[0] = originalTotal[0];
                totalLbl.setText("Total Due: Rs. " + String.format("%.2f", originalTotal[0]));
                amtField.setText(String.format("%.2f", originalTotal[0]));
            }
        };

        applyBtn.setOnAction(e -> {
            if (foundBooking[0] == null) return;
            String code = discountCombo.getValue();
            if (code == null || code.trim().isEmpty()) {
                discountMsgLbl.setText("Enter a code first.");
                discountMsgLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #e74c3c;");
                return;
            }
            Optional<DiscountCode> match = DataStore.getDiscountCodes().stream()
                    .filter(d -> d.getCode().equals(code) && d.isActive())
                    .findFirst();
            if (match.isEmpty()) {
                discountMsgLbl.setText("Invalid or expired code.");
                discountMsgLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #e74c3c;");
                return;
            }
            DiscountCode dc = match.get();
            double savings = originalTotal[0] * dc.getDiscountPercent() / 100.0;
            discountedTotal[0] = originalTotal[0] - savings;

            originalTotalLbl.setText("Original: Rs. " + String.format("%.2f", originalTotal[0]));
            originalTotalLbl.setVisible(true);
            discountLineLbl.setText("Discount (" + dc.getCode() + " – " +
                    String.format("%.0f", dc.getDiscountPercent()) + "% off): -Rs. " +
                    String.format("%.2f", savings));
            discountLineLbl.setVisible(true);
            totalLbl.setText("Total Due: Rs. " + String.format("%.2f", discountedTotal[0]));
            amtField.setText(String.format("%.2f", discountedTotal[0]));
            discountMsgLbl.setText("✔ Applied!");
            discountMsgLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #27AE60;");
            removeBtn.setVisible(true);
        });

        removeBtn.setOnAction(e -> resetDiscount.run());

        findBtn.setOnAction(e -> {
            String roomNo = roomField.getText().trim();
            if (roomNo.isEmpty()) { alert(Alert.AlertType.ERROR, "Enter a room number."); return; }

            Optional<Booking> opt = DataStore.getBookings().stream()
                    .filter(b -> b.getRoom().getRoomNo().equalsIgnoreCase(roomNo))
                    .findFirst();
            if (opt.isEmpty()) {
                alert(Alert.AlertType.ERROR, "No active booking for room \"" + roomNo + "\".");
                summaryBox.setVisible(false);
                discountBox.setVisible(false);
                paymentBox.setVisible(false);
                checkoutBtn.setVisible(false);
                foundBooking[0] = null;
                return;
            }
            Booking b = opt.get();
            foundBooking[0] = b;
            originalTotal[0]   = b.getTotalPrice();
            discountedTotal[0] = b.getTotalPrice();

            guestLbl.setText("Guest: " + b.getCustomer().getName()
                    + "  |  Phone: " + b.getCustomer().getPhone());
            datesLbl.setText("Check-In: " + (b.getCheckInDate() != null ? b.getCheckInDate() : "-")
                    + "  >>  " + (b.getCheckOutDate() != null ? b.getCheckOutDate() : "-"));
            nightsLbl.setText("Nights: " + b.getNights());
            originalTotalLbl.setVisible(false);
            discountLineLbl.setVisible(false);
            discountLineLbl.setText("");
            totalLbl.setText("Total Due: Rs. " + String.format("%.2f", b.getTotalPrice()));
            amtField.setText(String.format("%.2f", b.getTotalPrice()));
            refreshDiscounts.run();
            discountCombo.setValue(null);
            discountMsgLbl.setText("");
            removeBtn.setVisible(false);

            summaryBox.setVisible(true);
            discountBox.setVisible(true);
            paymentBox.setVisible(true);
            checkoutBtn.setVisible(true);
        });

        checkoutBtn.setOnAction(e -> {
            Booking b = foundBooking[0];
            if (b == null) return;

            String payMethod = payCombo.getValue();
            double amtPaid;
            try { amtPaid = Double.parseDouble(amtField.getText().trim()); }
            catch (NumberFormatException ex) {
                alert(Alert.AlertType.ERROR, "Invalid amount."); return;
            }

            String appliedVal = discountCombo.getValue();
            String appliedCode = appliedVal != null ? appliedVal.trim().toUpperCase() : "";
            boolean hasDiscount = !appliedCode.isEmpty() &&
                    DataStore.getDiscountCodes().stream()
                             .anyMatch(d -> d.getCode().equals(appliedCode) && d.isActive());

            CompletedBooking cb = new CompletedBooking(
                0, b.getCustomer().getName(), b.getCustomer().getPhone(),
                b.getRoom().getRoomNo(), b.getRoom().getType(),
                b.getCheckInDate(), b.getCheckOutDate(),
                b.getNights(), b.getTotalPrice(),
                payMethod, amtPaid, LocalDateTime.now());

            DataStore.getCompletedBookings().add(0, cb);
            DatabaseManager.saveCompletedBooking(cb);

            b.getRoom().setStatus(RoomStatus.CLEANING);
            DatabaseManager.updateRoomStatus(b.getRoom().getRoomNo(), RoomStatus.CLEANING);

            DataStore.getBookings().remove(b);
            DataStore.getCustomers().removeIf(c -> c.getRoomNo().equals(b.getRoom().getRoomNo()));
            DatabaseManager.deleteBooking(b.getId());

            String auditMsg = "Checked out Room " + b.getRoom().getRoomNo()
                    + " - " + b.getCustomer().getName()
                    + " | Paid: Rs. " + String.format("%.2f", amtPaid) + " via " + payMethod;
            if (hasDiscount) auditMsg += " | Discount: " + appliedCode;
            DataStore.addAuditEntry(auditMsg);

            File billFile = BillGenerator.generate(cb);

            roomField.clear();
            summaryBox.setVisible(false);
            discountBox.setVisible(false);
            paymentBox.setVisible(false);
            checkoutBtn.setVisible(false);
            foundBooking[0] = null;

            showCheckoutSuccess(b, amtPaid, payMethod, billFile,
                    hasDiscount ? appliedCode : null,
                    hasDiscount ? (b.getTotalPrice() - amtPaid) : 0);
        });

        lookupCard.getChildren().addAll(formTitle, lookupRow, summaryBox, discountBox, paymentBox, checkoutBtn);

        Label refTitle = new Label("Active Bookings (Reference)");
        refTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        TableView<Booking> refTable = new TableView<>(DataStore.getBookings());
        refTable.setPlaceholder(new Label("No active bookings."));
        refTable.getStyleClass().add("styled-table");

        TableColumn<Booking, String> custCol  = new TableColumn<>("Customer");
        custCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getCustomer().getName()));
        TableColumn<Booking, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getCustomer().getPhone()));
        TableColumn<Booking, String> roomCol  = new TableColumn<>("Room");
        roomCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getRoom().getRoomNo()));
        TableColumn<Booking, String> nightsCol = new TableColumn<>("Nights");
        nightsCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(String.valueOf(cd.getValue().getNights())));
        TableColumn<Booking, String> totalCol = new TableColumn<>("Total Due");
        totalCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                String.format("Rs. %.2f", cd.getValue().getTotalPrice())));

        refTable.getColumns().add(custCol);
        refTable.getColumns().add(phoneCol);
        refTable.getColumns().add(roomCol);
        refTable.getColumns().add(nightsCol);
        refTable.getColumns().add(totalCol);
        refTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(refTable, Priority.ALWAYS);

        view.getChildren().addAll(new VBox(4, title, subtitle), lookupCard, refTitle, refTable);
        return view;
    }

    private static void showCheckoutSuccess(Booking b, double amtPaid, String payMethod,
                                            File billFile, String discountCode, double savings) {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Checkout Complete");
        dialog.setHeaderText("Checkout successful!");

        StringBuilder content = new StringBuilder();
        content.append("Guest: ").append(b.getCustomer().getName()).append("\n");
        content.append("Room: ").append(b.getRoom().getRoomNo()).append("\n");
        if (discountCode != null && savings > 0) {
            content.append("Discount (").append(discountCode).append("): -Rs. ")
                   .append(String.format("%.2f", savings)).append("\n");
        }
        content.append("Paid: Rs. ").append(String.format("%.2f", amtPaid))
               .append(" via ").append(payMethod).append("\n");
        content.append("Room is now marked for Cleaning.\n\n");
        content.append(billFile != null
                ? "Bill saved to: " + billFile.getPath()
                : "Bill generation failed.");
        dialog.setContentText(content.toString());

        if (billFile != null) {
            ButtonType viewBillType = new ButtonType("View Bill", ButtonBar.ButtonData.LEFT);
            dialog.getButtonTypes().add(0, viewBillType);
            dialog.showAndWait().ifPresent(result -> {
                if (result == viewBillType) openBill(billFile);
            });
        } else {
            dialog.showAndWait();
        }
    }

    private static void openBill(File billFile) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(billFile);
            } else {
                alert(Alert.AlertType.INFORMATION, "Bill saved at:\n" + billFile.getAbsolutePath());
            }
        } catch (IOException ex) {
            alert(Alert.AlertType.ERROR, "Could not open bill: " + ex.getMessage());
        }
    }

    private static void alert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
