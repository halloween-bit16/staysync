package com.staysync.ui;

import com.staysync.data.DataStore;
import com.staysync.model.CompletedBooking;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ReportsView {

    public static void show(Stage owner) {
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(owner);
        stage.setTitle("📊 Revenue Reports - StaySync");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: #EEF4F4;");

        VBox root = new VBox(24);
        root.setPadding(new Insets(32));
        root.setStyle("-fx-background-color: #EEF4F4;");

        Label title = new Label("Revenue Reports");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        long totalRooms = DataStore.getRooms().size();
        long bookedRooms = DataStore.getRooms().stream().filter(r -> !r.isAvailable()).count();
        double occRate = totalRooms == 0 ? 0 : (bookedRooms * 100.0 / totalRooms);
        double totalRevenue = DataStore.getCompletedBookings().stream()
                .mapToDouble(CompletedBooking::getAmountPaid).sum();
        long totalCheckouts = DataStore.getCompletedBookings().size();

        HBox statRow = new HBox(16);
        statRow.getChildren().addAll(
            infoCard("💰 Total Revenue",    String.format("Rs. %.2f", totalRevenue), "#27AE60"),
            infoCard("📊 Occupancy Rate",   String.format("%.1f%%", occRate),     "#6CB2B2"),
            infoCard("🚪 Total Checkouts",  String.valueOf(totalCheckouts),        "#FB9119"),
            infoCard("🛏️ Booked/Total",     bookedRooms + " / " + totalRooms,     "#8e44ad")
        );

        Label typeTitle = new Label("Revenue by Room Type");
        typeTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        Map<String, Double> byType = DataStore.getCompletedBookings().stream()
                .collect(Collectors.groupingBy(CompletedBooking::getRoomType,
                         Collectors.summingDouble(CompletedBooking::getAmountPaid)));

        VBox typeChart = buildBarChart(byType, totalRevenue, "#6CB2B2");

        Label dayTitle = new Label("Revenue – Last 7 Days");
        dayTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd");
        Map<String, Double> byDay = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) byDay.put(today.minusDays(i).format(fmt), 0.0);
        DataStore.getCompletedBookings().forEach(cb -> {
            if (cb.getCheckoutTimestamp() != null) {
                String key = cb.getCheckoutTimestamp().toLocalDate().format(fmt);
                byDay.merge(key, cb.getAmountPaid(), Double::sum);
            }
        });

        double maxDay = byDay.values().stream().mapToDouble(d -> d).max().orElse(1.0);
        VBox dayChart = buildBarChart(byDay, maxDay == 0 ? 1.0 : maxDay, "#FB9119");

        Button exportBtn = new Button("📄  Export to CSV");
        exportBtn.getStyleClass().add("primary-btn");
        exportBtn.setOnAction(e -> exportCSV(stage));

        root.getChildren().addAll(title, statRow,
            section(typeTitle, typeChart),
            section(dayTitle, dayChart),
            exportBtn);
        scroll.setContent(root);

        Scene scene = new Scene(scroll, 900, 680);
        scene.getStylesheets().add(
            ReportsView.class.getResource("/css/style.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    private static VBox buildBarChart(Map<String, Double> data, double maxVal, String color) {
        VBox container = new VBox(10);
        container.setPadding(new Insets(12));
        container.setStyle("-fx-background-color: white; -fx-background-radius: 10;" +
                           "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);");

        if (data.isEmpty()) {
            container.getChildren().add(new Label("No data available."));
            return container;
        }

        data.forEach((label, value) -> {
            double pct = maxVal == 0 ? 0 : (value / maxVal);

            Label nameLbl = new Label(label);
            nameLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e;");
            nameLbl.setMinWidth(100);

            Region fill = new Region();
            fill.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 4;");
            fill.setPrefHeight(22);

            Region bg = new Region();
            bg.setStyle("-fx-background-color: #ecf0f1; -fx-background-radius: 4;");
            bg.setPrefHeight(22);
            bg.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(bg, Priority.ALWAYS);

            StackPane bar = new StackPane();
            bar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            bar.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(bar, Priority.ALWAYS);
            bar.getChildren().add(bg);

            fill.prefWidthProperty().bind(bar.widthProperty().multiply(pct));
            bar.getChildren().add(fill);

            Label valLbl = new Label(String.format("Rs. %.2f", value));
            valLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
            valLbl.setMinWidth(80);
            valLbl.setAlignment(Pos.CENTER_RIGHT);

            HBox row = new HBox(10, nameLbl, bar, valLbl);
            row.setAlignment(Pos.CENTER_LEFT);
            container.getChildren().add(row);
        });
        return container;
    }

    private static VBox section(Label title, VBox body) {
        VBox s = new VBox(10, title, body);
        return s;
    }

    private static VBox infoCard(String title, String value, String color) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(18));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10;" +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 10, 0, 0, 2);");
        HBox.setHgrow(card, Priority.ALWAYS);
        Label t = new Label(title); t.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
        Label v = new Label(value); v.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        card.getChildren().addAll(t, v);
        return card;
    }

    private static void exportCSV(Stage owner) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Report CSV");
        fc.setInitialFileName("staySync_report.csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        var file = fc.showSaveDialog(owner);
        if (file == null) return;

        try (FileWriter fw = new FileWriter(file)) {
            fw.write("Guest Name,Phone,Room,Type,Check-In,Check-Out,Nights,Total Price,Payment Method,Amount Paid,Checkout Time\n");
            for (CompletedBooking cb : DataStore.getCompletedBookings()) {
                fw.write(String.join(",",
                    q(cb.getCustomerName()), q(cb.getCustomerPhone()),
                    q(cb.getRoomNo()),       q(cb.getRoomType()),
                    q(str(cb.getCheckInDate())), q(str(cb.getCheckOutDate())),
                    String.valueOf(cb.getNights()),
                    String.format("%.2f", cb.getTotalPrice()),
                    q(cb.getPaymentMethod()),
                    String.format("%.2f", cb.getAmountPaid()),
                    q(cb.getCheckoutTimestamp() != null ? cb.getCheckoutTimestamp().toString() : "")
                ) + "\n");
            }
            new Alert(Alert.AlertType.INFORMATION,
                "Report exported to:\n" + file.getAbsolutePath()).showAndWait();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Export failed: " + ex.getMessage()).showAndWait();
        }
    }

    private static String q(String s)   { return "\"" + (s == null ? "" : s.replace("\"","\"\"")) + "\""; }
    private static String str(Object o) { return o == null ? "" : o.toString(); }
}
