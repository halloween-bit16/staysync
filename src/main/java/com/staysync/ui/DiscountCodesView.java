package com.staysync.ui;

import com.staysync.data.DataStore;
import com.staysync.data.DatabaseManager;
import com.staysync.model.DiscountCode;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class DiscountCodesView {

    public static VBox create() {
        VBox view = new VBox(24);
        view.setPadding(new Insets(32));
        view.setStyle("-fx-background-color: #EEF4F4;");

        Label title    = new Label("Discount Codes");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        Label subtitle = new Label("Create and manage promotional discount codes");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        VBox formCard = new VBox(16);
        formCard.setPadding(new Insets(24));
        formCard.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 12, 0, 0, 3);"
        );

        Label formTitle = new Label("Create New Discount Code");
        formTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        GridPane form = new GridPane();
        form.setHgap(16); form.setVgap(12);

        TextField codeField = field("e.g. SUMMER20");
        TextField descField = field("e.g. Summer promotion");
        TextField pctField  = field("e.g. 10  (for 10%)");

        form.add(lbl("Code:"),        0, 0); form.add(codeField, 1, 0);
        form.add(lbl("Description:"), 2, 0); form.add(descField, 3, 0);
        form.add(lbl("Discount %:"),  0, 1); form.add(pctField,  1, 1);

        ColumnConstraints labelCC = new ColumnConstraints(110);
        labelCC.setHgrow(Priority.NEVER);
        ColumnConstraints fieldCC = new ColumnConstraints();
        fieldCC.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(labelCC, fieldCC, labelCC, fieldCC);

        Label feedbackLbl = new Label();
        feedbackLbl.setStyle("-fx-font-size: 13px;");

        Button createBtn = new Button("✚  Create Code");
        createBtn.setStyle(
            "-fx-background-color: #6CB2B2; -fx-text-fill: white;" +
            "-fx-background-radius: 8; -fx-cursor: hand;" +
            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 8 20;");

        createBtn.setOnAction(e -> {
            String code = codeField.getText().trim().toUpperCase();
            String desc = descField.getText().trim();
            String pctTxt = pctField.getText().trim();

            if (code.isEmpty() || desc.isEmpty() || pctTxt.isEmpty()) {
                feedbackLbl.setText("⚠  Please fill in all fields.");
                feedbackLbl.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 13px;");
                return;
            }
            if (DataStore.getDiscountCodes().stream().anyMatch(d -> d.getCode().equals(code))) {
                feedbackLbl.setText("⚠  Code \"" + code + "\" already exists.");
                feedbackLbl.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 13px;");
                return;
            }
            double pct;
            try {
                pct = Double.parseDouble(pctTxt);
                if (pct <= 0 || pct > 100) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                feedbackLbl.setText("⚠  Discount % must be between 1 and 100.");
                feedbackLbl.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 13px;");
                return;
            }

            DiscountCode dc = new DiscountCode(code, desc, pct, true);
            DataStore.getDiscountCodes().add(dc);
            DatabaseManager.saveDiscountCode(dc);
            DataStore.addAuditEntry("Created discount code: " + code + " (" + pct + "% off)");

            codeField.clear(); descField.clear(); pctField.clear();
            feedbackLbl.setText("✔  Code \"" + code + "\" created successfully.");
            feedbackLbl.setStyle("-fx-text-fill: #27AE60; -fx-font-size: 13px;");
        });

        formCard.getChildren().addAll(formTitle, form, new HBox(12, createBtn, feedbackLbl));

        Label tableTitle = new Label("All Discount Codes");
        tableTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        TableView<DiscountCode> table = new TableView<>(DataStore.getDiscountCodes());
        table.setPlaceholder(new Label("No discount codes created yet."));
        table.getStyleClass().add("styled-table");
        table.setStyle("-fx-font-size: 13px;");

        TableColumn<DiscountCode, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getCode()));
        codeCol.setPrefWidth(130);

        TableColumn<DiscountCode, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getDescription()));
        descCol.setPrefWidth(240);

        TableColumn<DiscountCode, String> pctCol = new TableColumn<>("Discount");
        pctCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                String.format("%.0f%%", cd.getValue().getDiscountPercent())));
        pctCol.setPrefWidth(90);

        TableColumn<DiscountCode, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                cd.getValue().isActive() ? "✅ Active" : "❌ Inactive"));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(item.startsWith("✅")
                        ? "-fx-text-fill: #27AE60; -fx-font-weight: bold;"
                        : "-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            }
        });
        statusCol.setPrefWidth(110);

        TableColumn<DiscountCode, Void> actionCol = new TableColumn<>("Action");
        actionCol.setPrefWidth(130);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button actionBtn = new Button();
            {
                actionBtn.setOnAction(e -> {
                    DiscountCode dc = getTableView().getItems().get(getIndex());
                    if (dc.isActive()) {
                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                        confirm.setTitle("Deactivate Code");
                        confirm.setHeaderText(null);
                        confirm.setContentText("Deactivate code \"" + dc.getCode() + "\"? It can no longer be used at checkout.");
                        confirm.showAndWait().ifPresent(btn -> {
                            if (btn == ButtonType.OK) {
                                dc.setActive(false);
                                DatabaseManager.deactivateDiscountCode(dc.getCode());
                                DataStore.addAuditEntry("Deactivated discount code: " + dc.getCode());
                                getTableView().refresh();
                            }
                        });
                    } else {
                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                        confirm.setTitle("Reactivate Code");
                        confirm.setHeaderText(null);
                        confirm.setContentText("Reactivate code \"" + dc.getCode() + "\"? It will be usable at checkout again.");
                        confirm.showAndWait().ifPresent(btn -> {
                            if (btn == ButtonType.OK) {
                                dc.setActive(true);
                                DatabaseManager.reactivateDiscountCode(dc.getCode());
                                DataStore.addAuditEntry("Reactivated discount code: " + dc.getCode());
                                getTableView().refresh();
                            }
                        });
                    }
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                DiscountCode dc = getTableView().getItems().get(getIndex());
                if (dc.isActive()) {
                    actionBtn.setText("Deactivate");
                    actionBtn.setStyle(
                        "-fx-background-color: #e74c3c; -fx-text-fill: white;" +
                        "-fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 4 10;");
                } else {
                    actionBtn.setText("Reactivate");
                    actionBtn.setStyle(
                        "-fx-background-color: #27AE60; -fx-text-fill: white;" +
                        "-fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 4 10;");
                }
                setGraphic(actionBtn);
            }
        });

        table.getColumns().addAll(codeCol, descCol, pctCol, statusCol, actionCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        view.getChildren().addAll(
            new VBox(4, title, subtitle),
            formCard,
            tableTitle,
            table
        );
        return view;
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
}
