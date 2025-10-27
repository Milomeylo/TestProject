package com.restaurant.pos.ui;

import com.restaurant.pos.service.InventoryService;
import com.restaurant.pos.service.MenuService;
import io.github.palexdev.materialfx.controls.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.time.LocalDate;

public class InventoryTab extends BorderPane {
    private final InventoryService inventoryService = new InventoryService();
    private final MenuService menuService = new MenuService();

    private final TableView<InventoryService.StockLevel> stockTable = new TableView<>();
    private final TableView<InventoryService.BatchInfo> batchTable = new TableView<>();
    private final VBox warningsBox = new VBox(5);

    public InventoryTab() {
        setPadding(new Insets(10));
        setupStockTable();
        setupBatchTable();
        refresh();

        MFXButton addStock = new MFXButton("Add Stock Batch");
        addStock.setOnAction(e -> openAddStockDialog());

        MFXButton refreshBtn = new MFXButton("Refresh");
        refreshBtn.setOnAction(e -> refresh());

        HBox buttons = new HBox(10, addStock, refreshBtn);
        buttons.setAlignment(Pos.CENTER_LEFT);
        buttons.setPadding(new Insets(10, 0, 10, 0));

        Label stockLabel = new Label("Stock Levels");
        stockLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        Label batchLabel = new Label("Inventory Batches (FIFO Order)");
        batchLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        Label warningLabel = new Label("⚠ Expiry Warnings");
        warningLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #ff6b6b;");

        VBox stockBox = new VBox(10, stockLabel, stockTable);
        VBox batchBox = new VBox(10, batchLabel, batchTable);
        VBox warningContainer = new VBox(10, warningLabel, warningsBox);
        warningContainer.setPadding(new Insets(10));
        warningContainer.setStyle("-fx-background-color: #fff3cd; -fx-border-color: #ffc107; -fx-border-width: 2;");

        VBox.setVgrow(stockTable, Priority.ALWAYS);
        VBox.setVgrow(batchTable, Priority.ALWAYS);

        VBox leftPanel = new VBox(10, stockBox, buttons);
        VBox rightPanel = new VBox(10, batchBox, warningContainer);
        
        HBox mainContent = new HBox(10, leftPanel, rightPanel);
        HBox.setHgrow(leftPanel, Priority.ALWAYS);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);
        
        setCenter(mainContent);
    }

    private void setupStockTable() {
        TableColumn<InventoryService.StockLevel, String> name = new TableColumn<>("Item");
        name.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().name()));
        TableColumn<InventoryService.StockLevel, String> qty = new TableColumn<>("Total Qty");
        qty.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(String.valueOf(c.getValue().totalQuantity())));
        stockTable.getColumns().setAll(name, qty);
        stockTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
    }

    private void setupBatchTable() {
        TableColumn<InventoryService.BatchInfo, String> name = new TableColumn<>("Item");
        name.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().menuItemName()));
        
        TableColumn<InventoryService.BatchInfo, String> qty = new TableColumn<>("Qty");
        qty.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(String.valueOf(c.getValue().quantity())));
        
        TableColumn<InventoryService.BatchInfo, String> cost = new TableColumn<>("Unit Cost");
        cost.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(
            String.format("$%.2f", c.getValue().unitCostCents() / 100.0)));
        
        TableColumn<InventoryService.BatchInfo, String> expiry = new TableColumn<>("Expiry");
        expiry.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(
            c.getValue().expiryDate() != null ? c.getValue().expiryDate().toString() : "N/A"));
        
        TableColumn<InventoryService.BatchInfo, String> status = new TableColumn<>("Status");
        status.setCellValueFactory(c -> {
            InventoryService.ExpiryWarningLevel level = c.getValue().warningLevel();
            String text = switch (level) {
                case CRITICAL -> "⚠ CRITICAL";
                case WARNING -> "⚠ Warning";
                case NONE -> "OK";
            };
            return new javafx.beans.property.ReadOnlyStringWrapper(text);
        });
        
        // Color code rows based on expiry
        batchTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(InventoryService.BatchInfo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    switch (item.warningLevel()) {
                        case CRITICAL -> setStyle("-fx-background-color: #ffcccc;");
                        case WARNING -> setStyle("-fx-background-color: #fff3cd;");
                        default -> setStyle("");
                    }
                }
            }
        });
        
        batchTable.getColumns().setAll(name, qty, cost, expiry, status);
        batchTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
    }

    private void refresh() {
        stockTable.getItems().setAll(inventoryService.getStockLevels());
        batchTable.getItems().setAll(inventoryService.getAllBatches());
        refreshWarnings();
    }

    private void refreshWarnings() {
        warningsBox.getChildren().clear();
        var warnings = inventoryService.getExpiryWarnings();
        
        if (warnings.isEmpty()) {
            Label noWarnings = new Label("No items expiring soon");
            noWarnings.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            warningsBox.getChildren().add(noWarnings);
        } else {
            for (var warning : warnings) {
                String color = warning.level() == InventoryService.ExpiryWarningLevel.CRITICAL ? 
                    "#dc3545" : "#ffc107";
                String icon = warning.level() == InventoryService.ExpiryWarningLevel.CRITICAL ? 
                    "🔴" : "🟡";
                
                Label warnLabel = new Label(String.format("%s %s - %d units expire in %d days (%s)", 
                    icon, warning.name(), warning.quantity(), warning.daysUntilExpiry(), warning.expiryDate()));
                warnLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + color + "; -fx-font-weight: bold;");
                warningsBox.getChildren().add(warnLabel);
            }
        }
    }

    private void openAddStockDialog() {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Add Stock Batch");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        MFXComboBox<MenuService.MenuItem> itemBox = new MFXComboBox<>();
        itemBox.getItems().setAll(menuService.listActiveMenuItems());
        itemBox.setPromptText("Select item");

        MFXTextField qtyField = new MFXTextField();
        qtyField.setFloatingText("Quantity");
        qtyField.setPromptText("Enter quantity");
        
        MFXTextField costField = new MFXTextField();
        costField.setFloatingText("Unit Cost (cents)");
        costField.setPromptText("Enter cost in cents");
        
        DatePicker expiryPicker = new DatePicker();
        expiryPicker.setPromptText("Optional expiry date");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(15); grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Item:"), itemBox);
        grid.addRow(1, new Label("Quantity:"), qtyField);
        grid.addRow(2, new Label("Unit Cost:"), costField);
        grid.addRow(3, new Label("Expiry Date:"), expiryPicker);
        dlg.getDialogPane().setContent(grid);

        dlg.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                MenuService.MenuItem mi = itemBox.getSelectedItem();
                int qty = parseInt(qtyField.getText(), 0);
                int cost = parseInt(costField.getText(), 0);
                LocalDate exp = expiryPicker.getValue();
                if (mi != null && qty > 0 && cost > 0) {
                    inventoryService.addStockBatch(mi.id(), qty, cost, exp);
                    refresh();
                    new Alert(Alert.AlertType.INFORMATION, 
                        String.format("Added %d units of %s", qty, mi.name())).showAndWait();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Please fill all required fields correctly").showAndWait();
                }
            }
            return null;
        });

        dlg.showAndWait();
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }
}
