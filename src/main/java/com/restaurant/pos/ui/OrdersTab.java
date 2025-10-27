package com.restaurant.pos.ui;

import com.restaurant.pos.db.Database;
import io.github.palexdev.materialfx.controls.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class OrdersTab extends BorderPane {
    private final TableView<OrderRow> table = new TableView<>();

    public static class OrderRow {
        public final int id;
        public final String createdAt;
        public final double total;
        public final String discountType;
        public final String status;
        
        public OrderRow(int id, String createdAt, int totalCents, String discountType, String status) {
            this.id = id; 
            this.createdAt = createdAt; 
            this.total = totalCents/100.0;
            this.discountType = discountType != null ? discountType : "REGULAR";
            this.status = status != null ? status : "PAID";
        }
    }

    public OrdersTab() {
        setPadding(new Insets(15));
        setupTable();
        refresh();
        
        Label title = new Label("📋 Order History");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        MFXButton printBtn = new MFXButton("Reprint Receipt");
        printBtn.setOnAction(e -> reprint());
        
        MFXButton refreshBtn = new MFXButton("Refresh");
        refreshBtn.setOnAction(e -> refresh());
        
        HBox buttons = new HBox(10, printBtn, refreshBtn);
        buttons.setAlignment(Pos.CENTER_LEFT);
        buttons.setPadding(new Insets(10, 0, 10, 0));
        
        VBox header = new VBox(10, title, buttons);
        header.setPadding(new Insets(10));
        
        setTop(header);
        setCenter(table);
    }

    private void setupTable() {
        TableColumn<OrderRow, String> id = new TableColumn<>("Order #");
        id.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(String.valueOf(c.getValue().id)));
        id.setPrefWidth(80);
        
        TableColumn<OrderRow, String> dt = new TableColumn<>("Date/Time");
        dt.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().createdAt));
        dt.setPrefWidth(180);
        
        TableColumn<OrderRow, String> total = new TableColumn<>("Total");
        total.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(String.format("$%.2f", c.getValue().total)));
        total.setPrefWidth(100);
        
        TableColumn<OrderRow, String> discount = new TableColumn<>("Discount Type");
        discount.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().discountType));
        discount.setPrefWidth(120);
        
        TableColumn<OrderRow, String> status = new TableColumn<>("Status");
        status.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().status));
        status.setPrefWidth(80);
        
        table.getColumns().setAll(id, dt, total, discount, status);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        
        // Color code rows by discount type
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(OrderRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    if (item.discountType.equals("SENIOR") || item.discountType.equals("PWD")) {
                        setStyle("-fx-background-color: #fff3cd;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    private void refresh() {
        table.getItems().clear();
        String sql = "SELECT id, created_at, total_cents, discount_type, status FROM orders ORDER BY id DESC LIMIT 200";
        try (Connection conn = Database.getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql); 
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                table.getItems().add(new OrderRow(
                    rs.getInt("id"), 
                    rs.getString("created_at"), 
                    rs.getInt("total_cents"),
                    rs.getString("discount_type"),
                    rs.getString("status")
                ));
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    private void reprint() {
        OrderRow row = table.getSelectionModel().getSelectedItem();
        if (row == null) {
            new Alert(Alert.AlertType.WARNING, "Please select an order first").showAndWait();
            return;
        }
        
        try (Connection conn = Database.getConnection(); 
             PreparedStatement ps = conn.prepareStatement("SELECT content FROM order_receipt WHERE order_id = ?")) {
            ps.setInt(1, row.id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String content = rs.getString(1);
                    TextArea ta = new TextArea(content);
                    ta.setEditable(false);
                    ta.setPrefColumnCount(45);
                    ta.setPrefRowCount(25);
                    ta.setStyle("-fx-font-family: 'Courier New', monospace;");
                    
                    Dialog<Void> dlg = new Dialog<>();
                    dlg.setTitle("Receipt - Order #" + row.id);
                    dlg.getDialogPane().getButtonTypes().add(ButtonType.OK);
                    dlg.getDialogPane().setContent(ta);
                    dlg.showAndWait();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Receipt not found for order #" + row.id).showAndWait();
                }
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error loading receipt: " + e.getMessage()).show();
        }
    }
}
