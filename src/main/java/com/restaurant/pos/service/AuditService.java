package com.restaurant.pos.service;

import com.restaurant.pos.db.Database;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuditService {
    public static record AuditLog(int id, String actionType, String entityType, Integer entityId, 
                                   String description, String userRole, String createdAt) {}

    public void logAction(String actionType, String entityType, Integer entityId, String description, String userRole) {
        String sql = "INSERT INTO audit_log(action_type, entity_type, entity_id, description, user_role, created_at) VALUES (?,?,?,?,?,?)";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, actionType);
            ps.setString(2, entityType);
            if (entityId != null) {
                ps.setInt(3, entityId);
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setString(4, description);
            ps.setString(5, userRole);
            ps.setString(6, LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void logOrderCreated(int orderId, int totalCents, String discountType) {
        logAction("CREATE", "ORDER", orderId, 
            String.format("Order created - Total: $%.2f, Discount: %s", totalCents/100.0, discountType), 
            "STAFF");
    }

    public void logPaymentReceived(int orderId, int amountCents, String method) {
        logAction("PAYMENT", "ORDER", orderId, 
            String.format("Payment received - Amount: $%.2f, Method: %s", amountCents/100.0, method), 
            "STAFF");
    }

    public void logInventoryUpdate(int menuItemId, int quantityChange, String reason) {
        logAction("INVENTORY", "MENU_ITEM", menuItemId, 
            String.format("Inventory %s by %d - Reason: %s", 
                quantityChange > 0 ? "increased" : "decreased", 
                Math.abs(quantityChange), reason), 
            "STAFF");
    }

    public void logPromoGenerated(int promoId, int menuItemId, double discountPercent) {
        logAction("CREATE", "PROMO", promoId, 
            String.format("Auto-promo generated for item %d - Discount: %.0f%%", menuItemId, discountPercent), 
            "SYSTEM");
    }

    public List<AuditLog> getRecentLogs(int limit) {
        String sql = "SELECT id, action_type, entity_type, entity_id, description, user_role, created_at " +
                     "FROM audit_log ORDER BY id DESC LIMIT ?";
        List<AuditLog> logs = new ArrayList<>();
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Integer entityId = rs.getObject("entity_id") != null ? rs.getInt("entity_id") : null;
                    logs.add(new AuditLog(
                        rs.getInt("id"),
                        rs.getString("action_type"),
                        rs.getString("entity_type"),
                        entityId,
                        rs.getString("description"),
                        rs.getString("user_role"),
                        rs.getString("created_at")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return logs;
    }

    public List<AuditLog> getLogsByEntityType(String entityType, int limit) {
        String sql = "SELECT id, action_type, entity_type, entity_id, description, user_role, created_at " +
                     "FROM audit_log WHERE entity_type = ? ORDER BY id DESC LIMIT ?";
        List<AuditLog> logs = new ArrayList<>();
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entityType);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Integer entityId = rs.getObject("entity_id") != null ? rs.getInt("entity_id") : null;
                    logs.add(new AuditLog(
                        rs.getInt("id"),
                        rs.getString("action_type"),
                        rs.getString("entity_type"),
                        entityId,
                        rs.getString("description"),
                        rs.getString("user_role"),
                        rs.getString("created_at")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return logs;
    }
}
