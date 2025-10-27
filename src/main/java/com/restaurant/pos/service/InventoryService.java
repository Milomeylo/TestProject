package com.restaurant.pos.service;

import com.restaurant.pos.db.Database;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class InventoryService {
    public static record StockLevel(int menuItemId, String name, int totalQuantity) {}
    
    public static record BatchInfo(int batchId, int menuItemId, String menuItemName, 
                                    int quantity, int unitCostCents, LocalDate expiryDate, 
                                    LocalDate createdAt, ExpiryWarningLevel warningLevel) {}
    
    public enum ExpiryWarningLevel {
        NONE,      // > 7 days or no expiry
        WARNING,   // 3-7 days
        CRITICAL   // < 3 days
    }
    
    public static record ExpiryWarning(int menuItemId, String name, LocalDate expiryDate, 
                                        int daysUntilExpiry, int quantity, ExpiryWarningLevel level) {}

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

    public List<BatchInfo> getAllBatches() {
        String sql = "SELECT ib.id, ib.menu_item_id, mi.name, ib.quantity, ib.unit_cost_cents, " +
                     "ib.expiry_date, ib.created_at " +
                     "FROM inventory_batch ib " +
                     "JOIN menu_item mi ON mi.id = ib.menu_item_id " +
                     "WHERE ib.quantity > 0 " +
                     "ORDER BY ib.expiry_date NULLS LAST, ib.id";
        List<BatchInfo> batches = new ArrayList<>();
        try (Connection conn = Database.getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql); 
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String expiryStr = rs.getString("expiry_date");
                LocalDate expiry = expiryStr != null ? LocalDate.parse(expiryStr) : null;
                ExpiryWarningLevel level = calculateWarningLevel(expiry);
                
                batches.add(new BatchInfo(
                    rs.getInt("id"),
                    rs.getInt("menu_item_id"),
                    rs.getString("name"),
                    rs.getInt("quantity"),
                    rs.getInt("unit_cost_cents"),
                    expiry,
                    LocalDate.parse(rs.getString("created_at")),
                    level
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return batches;
    }

    public List<ExpiryWarning> getExpiryWarnings() {
        String sql = "SELECT ib.menu_item_id, mi.name, ib.expiry_date, SUM(ib.quantity) AS qty " +
                     "FROM inventory_batch ib " +
                     "JOIN menu_item mi ON mi.id = ib.menu_item_id " +
                     "WHERE ib.expiry_date IS NOT NULL AND ib.quantity > 0 " +
                     "AND DATE(ib.expiry_date) <= DATE('now', '+7 days') " +
                     "GROUP BY ib.menu_item_id, mi.name, ib.expiry_date " +
                     "ORDER BY ib.expiry_date";
        List<ExpiryWarning> warnings = new ArrayList<>();
        try (Connection conn = Database.getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql); 
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                LocalDate expiry = LocalDate.parse(rs.getString("expiry_date"));
                long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), expiry);
                ExpiryWarningLevel level = calculateWarningLevel(expiry);
                
                warnings.add(new ExpiryWarning(
                    rs.getInt("menu_item_id"),
                    rs.getString("name"),
                    expiry,
                    (int) daysUntil,
                    rs.getInt("qty"),
                    level
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return warnings;
    }

    private ExpiryWarningLevel calculateWarningLevel(LocalDate expiryDate) {
        if (expiryDate == null) {
            return ExpiryWarningLevel.NONE;
        }
        long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
        if (daysUntil < 3) {
            return ExpiryWarningLevel.CRITICAL;
        } else if (daysUntil < 7) {
            return ExpiryWarningLevel.WARNING;
        }
        return ExpiryWarningLevel.NONE;
    }

    public int calculateCOGS(int menuItemId, int quantity, Connection conn) throws SQLException {
        String select = "SELECT unit_cost_cents, quantity FROM inventory_batch " +
                       "WHERE menu_item_id = ? AND quantity > 0 " +
                       "ORDER BY expiry_date NULLS LAST, id";
        int totalCost = 0;
        int remaining = quantity;
        
        try (PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setInt(1, menuItemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (remaining > 0 && rs.next()) {
                    int batchQty = rs.getInt("quantity");
                    int unitCost = rs.getInt("unit_cost_cents");
                    int consume = Math.min(batchQty, remaining);
                    totalCost += consume * unitCost;
                    remaining -= consume;
                }
            }
        }
        return totalCost;
    }
}
