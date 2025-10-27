package com.restaurant.pos.service;

import com.restaurant.pos.db.Database;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class AnalyticsService {
    public static record SalesTrend(String period, int totalSalesCents, int orderCount) {}
    public static record TopSellingItem(int menuItemId, String name, int totalQuantity, int totalRevenueCents) {}
    public static record ProfitLoss(int totalRevenueCents, int totalCogsCents, int profitCents, double profitMargin) {}
    public static record CategorySales(String category, int totalSalesCents, int percentage) {}

    public List<SalesTrend> getDailySalesTrend(int days) {
        String sql = "SELECT DATE(created_at) AS day, SUM(total_cents) AS total, COUNT(*) AS cnt " +
                     "FROM orders WHERE DATE(created_at) >= DATE('now', ?) " +
                     "GROUP BY day ORDER BY day";
        List<SalesTrend> trends = new ArrayList<>();
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, String.format("-%d days", days));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    trends.add(new SalesTrend(
                        rs.getString("day"),
                        rs.getInt("total"),
                        rs.getInt("cnt")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return trends;
    }

    public List<SalesTrend> getMonthlySalesTrend(int months) {
        String sql = "SELECT STRFTIME('%Y-%m', created_at) AS month, SUM(total_cents) AS total, COUNT(*) AS cnt " +
                     "FROM orders WHERE DATE(created_at) >= DATE('now', ?) " +
                     "GROUP BY month ORDER BY month";
        List<SalesTrend> trends = new ArrayList<>();
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, String.format("-%d months", months));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    trends.add(new SalesTrend(
                        rs.getString("month"),
                        rs.getInt("total"),
                        rs.getInt("cnt")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return trends;
    }

    public List<TopSellingItem> getTopSellingItems(int limit, int days) {
        String sql = "SELECT mi.id, mi.name, SUM(oi.quantity) AS qty, SUM(oi.line_total_cents) AS revenue " +
                     "FROM order_item oi " +
                     "JOIN menu_item mi ON mi.id = oi.menu_item_id " +
                     "JOIN orders o ON o.id = oi.order_id " +
                     "WHERE DATE(o.created_at) >= DATE('now', ?) " +
                     "GROUP BY mi.id, mi.name " +
                     "ORDER BY qty DESC LIMIT ?";
        List<TopSellingItem> items = new ArrayList<>();
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, String.format("-%d days", days));
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new TopSellingItem(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("qty"),
                        rs.getInt("revenue")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return items;
    }

    public ProfitLoss calculateProfitLoss(int days) {
        String sql = "SELECT SUM(total_cents) AS revenue, SUM(cogs_cents) AS cogs FROM orders " +
                     "WHERE DATE(created_at) >= DATE('now', ?)";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, String.format("-%d days", days));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int revenue = rs.getInt("revenue");
                    int cogs = rs.getInt("cogs");
                    int profit = revenue - cogs;
                    double margin = revenue > 0 ? (profit * 100.0 / revenue) : 0.0;
                    return new ProfitLoss(revenue, cogs, profit, margin);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return new ProfitLoss(0, 0, 0, 0.0);
    }

    public List<CategorySales> getCategorySales(int days) {
        String sql = "SELECT mi.category, SUM(oi.line_total_cents) AS total " +
                     "FROM order_item oi " +
                     "JOIN menu_item mi ON mi.id = oi.menu_item_id " +
                     "JOIN orders o ON o.id = oi.order_id " +
                     "WHERE DATE(o.created_at) >= DATE('now', ?) " +
                     "GROUP BY mi.category";
        List<CategorySales> sales = new ArrayList<>();
        int grandTotal = 0;
        
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, String.format("-%d days", days));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int total = rs.getInt("total");
                    grandTotal += total;
                    sales.add(new CategorySales(rs.getString("category"), total, 0));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Calculate percentages
        final int finalGrandTotal = grandTotal;
        if (finalGrandTotal > 0) {
            sales = sales.stream()
                .map(s -> new CategorySales(s.category(), s.totalSalesCents(), 
                    (int) Math.round(s.totalSalesCents() * 100.0 / finalGrandTotal)))
                .toList();
        }
        
        return sales;
    }
}
