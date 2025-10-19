package com.restaurant.pos.service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.restaurant.pos.db.Database;

public class MenuService {
    public static record MenuItem(int id, String name, String category, int priceCents, String sku, boolean active) {}

    public List<MenuItem> listActiveMenuItems() {
        List<MenuItem> items = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, name, category, price_cents, sku, active FROM menu_item WHERE active = 1 ORDER BY category, name")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new MenuItem(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("category"),
                            rs.getInt("price_cents"),
                            rs.getString("sku"),
                            rs.getInt("active") == 1
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return items;
    }
}
