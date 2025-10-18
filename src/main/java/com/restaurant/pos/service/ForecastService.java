package com.restaurant.pos.service;

import com.restaurant.pos.db.Database;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class ForecastService {
    public static record Forecast(int menuItemId, String name, int forecastQty) {}

    // Simple forecasting: moving average of last N months order quantities per item
    public List<Forecast> forecastNextMonthSales(int monthsWindow) {
        Map<Integer, String> idToName = new HashMap<>();
        Map<Integer, List<Integer>> itemToMonthlyQty = new HashMap<>();

        String sql = "SELECT mi.id, mi.name, STRFTIME('%Y-%m', o.created_at) AS ym, SUM(oi.quantity) AS qty " +
                "FROM orders o JOIN order_item oi ON o.id = oi.order_id JOIN menu_item mi ON mi.id = oi.menu_item_id " +
                "WHERE DATE(o.created_at) >= DATE('now', ?) " +
                "GROUP BY mi.id, mi.name, ym";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            YearMonth startYm = YearMonth.now().minusMonths(monthsWindow);
            ps.setString(1, String.format("-%d months", monthsWindow));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt(1);
                    idToName.put(id, rs.getString(2));
                    int qty = rs.getInt(4);
                    itemToMonthlyQty.computeIfAbsent(id, k -> new ArrayList<>()).add(qty);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        List<Forecast> forecasts = new ArrayList<>();
        for (Map.Entry<Integer, List<Integer>> e : itemToMonthlyQty.entrySet()) {
            int id = e.getKey();
            List<Integer> q = e.getValue();
            int sum = q.stream().mapToInt(Integer::intValue).sum();
            int avg = (int) Math.round(sum / Math.max(1.0, q.size()));
            forecasts.add(new Forecast(id, idToName.getOrDefault(id, "Item " + id), avg));
        }
        forecasts.sort(Comparator.comparingInt((Forecast f) -> -f.forecastQty));
        return forecasts;
    }
}