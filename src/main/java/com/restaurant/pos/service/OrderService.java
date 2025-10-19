package com.restaurant.pos.service;

import com.restaurant.pos.db.Database;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

public class OrderService {
    public static record CartLine(int menuItemId, String name, int quantity, int unitPriceCents) {}

    public static final double TAX_RATE = 0.07; // 7%

    public static class OrderResult {
        public final int orderId;
        public final int subtotalCents;
        public final int discountCents;
        public final int taxCents;
        public final int totalCents;
        public final String receipt;

        public OrderResult(int orderId, int subtotalCents, int discountCents, int taxCents, int totalCents, String receipt) {
            this.orderId = orderId;
            this.subtotalCents = subtotalCents;
            this.discountCents = discountCents;
            this.taxCents = taxCents;
            this.totalCents = totalCents;
            this.receipt = receipt;
        }
    }

    public OrderResult placeOrder(List<CartLine> cart, String paymentMethod) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            int subtotal = cart.stream().mapToInt(l -> l.unitPriceCents * l.quantity).sum();
            int discount = 0; // promo will adjust later
            int tax = (int) Math.round(subtotal * TAX_RATE);
            int total = subtotal - discount + tax;

            int orderId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO orders(created_at, subtotal_cents, discount_cents, tax_cents, total_cents, payment_method, status) VALUES (?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, LocalDateTime.now().toString());
                ps.setInt(2, subtotal);
                ps.setInt(3, discount);
                ps.setInt(4, tax);
                ps.setInt(5, total);
                ps.setString(6, paymentMethod);
                ps.setString(7, "PAID");
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No order id generated");
                    orderId = keys.getInt(1);
                }
            }

            // Insert items and deduct inventory FIFO
            InventoryService inventoryService = new InventoryService();
            for (CartLine line : cart) {
                try (PreparedStatement item = conn.prepareStatement(
                        "INSERT INTO order_item(order_id, menu_item_id, quantity, unit_price_cents, line_total_cents) VALUES (?,?,?,?,?)")) {
                    item.setInt(1, orderId);
                    item.setInt(2, line.menuItemId());
                    item.setInt(3, line.quantity());
                    item.setInt(4, line.unitPriceCents());
                    item.setInt(5, line.unitPriceCents() * line.quantity());
                    item.executeUpdate();
                }
                inventoryService.deductInventoryFIFO(line.menuItemId(), line.quantity(), conn);
                try (PreparedStatement led = conn.prepareStatement(
                        "INSERT INTO inventory_ledger(menu_item_id, quantity_change, reason, ref_type, ref_id, created_at) VALUES (?,?,?,?,?,?)")) {
                    led.setInt(1, line.menuItemId());
                    led.setInt(2, -line.quantity());
                    led.setString(3, "sale");
                    led.setString(4, "order");
                    led.setInt(5, orderId);
                    led.setString(6, LocalDateTime.now().toString());
                    led.executeUpdate();
                }
            }

            try (PreparedStatement pay = conn.prepareStatement(
                    "INSERT INTO payment(order_id, amount_cents, method, created_at) VALUES (?,?,?,?)")) {
                pay.setInt(1, orderId);
                pay.setInt(2, total);
                pay.setString(3, paymentMethod);
                pay.setString(4, LocalDateTime.now().toString());
                pay.executeUpdate();
            }

            String receipt = generateReceipt(conn, orderId);
            try (PreparedStatement rec = conn.prepareStatement(
                    "INSERT INTO order_receipt(order_id, content) VALUES (?,?)")) {
                rec.setInt(1, orderId);
                rec.setString(2, receipt);
                rec.executeUpdate();
            }

            conn.commit();
            return new OrderResult(orderId, subtotal, discount, tax, total, receipt);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String generateReceipt(Connection conn, int orderId) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Restaurant Receipt ===\n");
        sb.append("Order #").append(orderId).append("\n");
        sb.append("Date: ").append(LocalDateTime.now()).append("\n\n");
        sb.append(String.format("%-20s %5s %8s\n", "Item", "Qty", "Total"));
        sb.append("----------------------------------------\n");

        String sql = "SELECT mi.name, oi.quantity, oi.line_total_cents FROM order_item oi JOIN menu_item mi ON mi.id = oi.menu_item_id WHERE oi.order_id = ?";
        int subtotal = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString(1);
                    int qty = rs.getInt(2);
                    int lineTotal = rs.getInt(3);
                    subtotal += lineTotal;
                    sb.append(String.format("%-20s %5d %8.2f\n", name, qty, lineTotal / 100.0));
                }
            }
        }
        int tax = (int) Math.round(subtotal * TAX_RATE);
        int total = subtotal + tax;
        sb.append("----------------------------------------\n");
        sb.append(String.format("%-20s %13.2f\n", "Subtotal:", subtotal / 100.0));
        sb.append(String.format("%-20s %13.2f\n", "Tax:", tax / 100.0));
        sb.append(String.format("%-20s %13.2f\n", "Total:", total / 100.0));
        sb.append("\nThank you!\n");
        return sb.toString();
    }
}
