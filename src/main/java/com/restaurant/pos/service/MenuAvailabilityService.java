package com.restaurant.pos.service;

import com.restaurant.pos.db.Database;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class MenuAvailabilityService {
    public static record AvailabilityStatus(boolean available, int availableQuantity, String reason) {}

    public AvailabilityStatus checkAvailability(int menuItemId, int requestedQuantity) {
        String sql = "SELECT COALESCE(SUM(quantity), 0) AS total FROM inventory_batch WHERE menu_item_id = ? AND quantity > 0";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, menuItemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int available = rs.getInt("total");
                    if (available >= requestedQuantity) {
                        return new AvailabilityStatus(true, available, "Available");
                    } else if (available > 0) {
                        return new AvailabilityStatus(false, available, 
                            String.format("Insufficient stock (only %d available)", available));
                    } else {
                        return new AvailabilityStatus(false, 0, "Out of stock");
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return new AvailabilityStatus(false, 0, "Item not found");
    }

    public Map<Integer, AvailabilityStatus> checkMultipleAvailability(Map<Integer, Integer> itemQuantities) {
        Map<Integer, AvailabilityStatus> results = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : itemQuantities.entrySet()) {
            results.put(entry.getKey(), checkAvailability(entry.getKey(), entry.getValue()));
        }
        return results;
    }

    public boolean isMenuItemAvailable(int menuItemId) {
        return checkAvailability(menuItemId, 1).available();
    }

    public int getAvailableQuantity(int menuItemId) {
        return checkAvailability(menuItemId, 1).availableQuantity();
    }
}
