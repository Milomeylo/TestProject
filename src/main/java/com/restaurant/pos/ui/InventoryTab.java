package com.restaurant.pos.ui;

import com.restaurant.pos.service.InventoryService;
import com.restaurant.pos.service.MenuService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

public class InventoryTab extends BorderPane {
    private final InventoryService inventoryService = new InventoryService();
    private final MenuService menuService = new MenuService();

    private final TableView<InventoryService.StockLevel> table = new TableView<>();

    public InventoryTab() {
        setPadding(new Insets(10));
        setupTable();
        refresh();

        Button addStock = new Button("Add Stock Batch");
        addStock.setOnAction(e -> openAddStockDialog());

        VBox box = new VBox(10, table, addStock);
        setCenter(box);
    }

    private void setupTable() {
        TableColumn<InventoryService.StockLevel, String> name = new TableColumn<>("Item");
        name.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().name()));
        TableColumn<InventoryService.StockLevel, String> qty = new TableColumn<>("Total Qty");
        qty.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(String.valueOf(c.getValue().totalQuantity())));
        table.getColumns().setAll(name, qty);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
    }

    private void refresh() {
        table.getItems().setAll(inventoryService.getStockLevels());
    }

    private void openAddStockDialog() {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Add Stock Batch");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<MenuService.MenuItem> itemBox = new ComboBox<>();
        itemBox.getItems().setAll(menuService.listActiveMenuItems());
        itemBox.setCellFactory(cb -> new ListCell<>() {
            @Override protected void updateItem(MenuService.MenuItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        });
        itemBox.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(MenuService.MenuItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        });

        TextField qtyField = new TextField();
        TextField costField = new TextField();
        DatePicker expiryPicker = new DatePicker();

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(10));
        grid.addRow(0, new Label("Item"), itemBox);
        grid.addRow(1, new Label("Quantity"), qtyField);
        grid.addRow(2, new Label("Unit Cost (cents)"), costField);
        grid.addRow(3, new Label("Expiry (optional)"), expiryPicker);
        dlg.getDialogPane().setContent(grid);

        dlg.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                MenuService.MenuItem mi = itemBox.getValue();
                int qty = parseInt(qtyField.getText(), 0);
                int cost = parseInt(costField.getText(), 0);
                LocalDate exp = expiryPicker.getValue();
                if (mi != null && qty > 0 && cost > 0) {
                    inventoryService.addStockBatch(mi.id(), qty, cost, exp);
                    refresh();
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
