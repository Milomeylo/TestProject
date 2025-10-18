package com.restaurant.pos.service;

import com.restaurant.pos.db.Database;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InventoryService {
    public static record StockLevel(int menuItemId, String name, int totalQuantity) {}

    public List<StockLevel> getStockLevels() {
        String sql = "SELECT mi.id, mi.name, COALESCE(SUM(ib.quantity),0) AS qty " +
                "FROM menu_item mi LEFT JOIN inventory_batch ib ON mi.id = ib.menu_item_id " +
                "GROUP BY mi.id, mi.name ORDER BY mi.name";
        List<StockLevel> levels = new ArrayList<>();
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                levels.add(new StockLevel(rs.getInt(1), rs.getString(2), rs.getInt(3)));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return levels;
    }

    public void deductInventoryFIFO(int menuItemId, int quantity, Connection conn) throws SQLException {
        String select = "SELECT id, quantity FROM inventory_batch WHERE menu_item_id = ? AND quantity > 0 ORDER BY expiry_date NULLS LAST, id";
        try (PreparedStatement ps = conn.prepareStatement(select, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)) {
            ps.setInt(1, menuItemId);
            try (ResultSet rs = ps.executeQuery()) {
                int remaining = quantity;
                while (remaining > 0 && rs.next()) {
                    int batchQty = rs.getInt("quantity");
                    int consume = Math.min(batchQty, remaining);
                    rs.updateInt("quantity", batchQty - consume);
                    rs.updateRow();
                    remaining -= consume;
                }
                if (remaining > 0) {
                    throw new SQLException("Insufficient stock for menu_item_id=" + menuItemId);
                }
            }
        }
    }

    public void addStockBatch(int menuItemId, int quantity, int unitCostCents, LocalDate expiryDate) {
        String sql = "INSERT INTO inventory_batch(menu_item_id, quantity, unit_cost_cents, expiry_date, created_at) VALUES (?,?,?,?,?)";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, menuItemId);
            ps.setInt(2, quantity);
            ps.setInt(3, unitCostCents);
            ps.setString(4, expiryDate != null ? expiryDate.toString() : null);
            ps.setString(5, LocalDate.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
