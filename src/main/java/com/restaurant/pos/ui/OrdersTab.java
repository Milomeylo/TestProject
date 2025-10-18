package com.restaurant.pos.ui;

import com.restaurant.pos.db.Database;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

public class OrdersTab extends BorderPane {
    private final TableView<OrderRow> table = new TableView<>();

    public static class OrderRow {
        public final int id;
        public final String createdAt;
        public final double total;
        public OrderRow(int id, String createdAt, int totalCents) {
            this.id = id; this.createdAt = createdAt; this.total = totalCents/100.0;
        }
    }

    public OrdersTab() {
        setPadding(new Insets(10));
        setupTable();
        refresh();
        Button print = new Button("Reprint Receipt");
        print.setOnAction(e -> reprint());
        setBottom(print);
    }

    private void setupTable() {
        TableColumn<OrderRow, String> id = new TableColumn<>("Order #");
        id.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(String.valueOf(c.getValue().id)));
        TableColumn<OrderRow, String> dt = new TableColumn<>("Date");
        dt.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().createdAt));
        TableColumn<OrderRow, String> total = new TableColumn<>("Total");
        total.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(String.format("$%.2f", c.getValue().total)));
        table.getColumns().setAll(id, dt, total);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        setCenter(table);
    }

    private void refresh() {
        table.getItems().clear();
        String sql = "SELECT id, created_at, total_cents FROM orders ORDER BY id DESC LIMIT 200";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                table.getItems().add(new OrderRow(rs.getInt(1), rs.getString(2), rs.getInt(3)));
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    private void reprint() {
        OrderRow row = table.getSelectionModel().getSelectedItem();
        if (row == null) return;
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT content FROM order_receipt WHERE order_id = ?")) {
            ps.setInt(1, row.id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String content = rs.getString(1);
                    TextArea ta = new TextArea(content);
                    ta.setEditable(false);
                    Dialog<Void> dlg = new Dialog<>();
                    dlg.setTitle("Receipt #" + row.id);
                    dlg.getDialogPane().getButtonTypes().add(ButtonType.OK);
                    dlg.getDialogPane().setContent(ta);
                    dlg.showAndWait();
                }
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }
}
