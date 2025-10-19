package com.restaurant.pos.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.*;
import java.time.LocalDate;

public final class Database {
    private static final Logger log = LoggerFactory.getLogger(Database.class);

    private static final String DB_DIR = "data";
    private static final String DB_PATH = DB_DIR + File.separator + "pos.db";
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_PATH;

    private Database() {}

    public static void initialize() {
        try {
            File dir = new File(DB_DIR);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IllegalStateException("Failed to create data directory: " + DB_DIR);
            }
            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);
                try (Statement st = conn.createStatement()) {
                    st.execute("PRAGMA foreign_keys = ON");
                    // Schema
                    st.addBatch("CREATE TABLE IF NOT EXISTS menu_item (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "name TEXT NOT NULL UNIQUE, " +
                            "category TEXT, " +
                            "price_cents INTEGER NOT NULL, " +
                            "sku TEXT, " +
                            "active INTEGER NOT NULL DEFAULT 1" +
                            ")");

                    st.addBatch("CREATE TABLE IF NOT EXISTS inventory_batch (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "menu_item_id INTEGER NOT NULL, " +
                            "quantity INTEGER NOT NULL, " +
                            "unit_cost_cents INTEGER NOT NULL, " +
                            "expiry_date TEXT, " +
                            "created_at TEXT NOT NULL, " +
                            "FOREIGN KEY(menu_item_id) REFERENCES menu_item(id) ON DELETE CASCADE" +
                            ")");

                    st.addBatch("CREATE INDEX IF NOT EXISTS idx_inventory_item_expiry ON inventory_batch(menu_item_id, expiry_date)");

                    st.addBatch("CREATE TABLE IF NOT EXISTS promo (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "menu_item_id INTEGER NOT NULL, " +
                            "discount_percent REAL NOT NULL, " +
                            "start_date TEXT NOT NULL, " +
                            "end_date TEXT NOT NULL, " +
                            "reason TEXT, " +
                            "auto_generated INTEGER NOT NULL DEFAULT 0, " +
                            "FOREIGN KEY(menu_item_id) REFERENCES menu_item(id) ON DELETE CASCADE" +
                            ")");

                    st.addBatch("CREATE TABLE IF NOT EXISTS orders (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "created_at TEXT NOT NULL, " +
                            "subtotal_cents INTEGER NOT NULL, " +
                            "discount_cents INTEGER NOT NULL, " +
                            "tax_cents INTEGER NOT NULL, " +
                            "total_cents INTEGER NOT NULL, " +
                            "payment_method TEXT NOT NULL, " +
                            "status TEXT NOT NULL DEFAULT 'PAID'" +
                            ")");

                    st.addBatch("CREATE TABLE IF NOT EXISTS order_item (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "order_id INTEGER NOT NULL, " +
                            "menu_item_id INTEGER NOT NULL, " +
                            "quantity INTEGER NOT NULL, " +
                            "unit_price_cents INTEGER NOT NULL, " +
                            "line_total_cents INTEGER NOT NULL, " +
                            "FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE, " +
                            "FOREIGN KEY(menu_item_id) REFERENCES menu_item(id) ON DELETE CASCADE" +
                            ")");

                    st.addBatch("CREATE TABLE IF NOT EXISTS payment (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "order_id INTEGER NOT NULL, " +
                            "amount_cents INTEGER NOT NULL, " +
                            "method TEXT NOT NULL, " +
                            "created_at TEXT NOT NULL, " +
                            "FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE" +
                            ")");

                    st.addBatch("CREATE TABLE IF NOT EXISTS order_receipt (" +
                            "order_id INTEGER PRIMARY KEY, " +
                            "content TEXT NOT NULL, " +
                            "FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE" +
                            ")");

                    st.addBatch("CREATE TABLE IF NOT EXISTS inventory_ledger (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "menu_item_id INTEGER NOT NULL, " +
                            "quantity_change INTEGER NOT NULL, " +
                            "reason TEXT NOT NULL, " +
                            "ref_type TEXT, " +
                            "ref_id INTEGER, " +
                            "created_at TEXT NOT NULL, " +
                            "FOREIGN KEY(menu_item_id) REFERENCES menu_item(id) ON DELETE CASCADE" +
                            ")");

                    st.executeBatch();
                }
                seed(conn);
                conn.commit();
            }
        } catch (Exception e) {
            log.error("DB initialization failed", e);
            throw new RuntimeException(e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL);
    }

    private static void seed(Connection conn) throws SQLException {
        // Seed only if no menu items exist
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM menu_item");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next() && rs.getInt(1) == 0) {
                log.info("Seeding sample menu and inventory");
                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO menu_item(name, category, price_cents, sku) VALUES (?,?,?,?)")) {
                    insertMenu(ins, "Burger", "Food", 899, "BURG001");
                    insertMenu(ins, "Fries", "Food", 299, "FRIE001");
                    insertMenu(ins, "Cola", "Beverage", 199, "COLA001");
                }
                // Add some inventory batches
                try (PreparedStatement inv = conn.prepareStatement(
                        "INSERT INTO inventory_batch(menu_item_id, quantity, unit_cost_cents, expiry_date, created_at) " +
                                "VALUES (?,?,?,?,?)")) {
                    addBatch(inv, 1, 50, 450, LocalDate.now().plusDays(7));
                    addBatch(inv, 2, 100, 100, LocalDate.now().plusDays(5));
                    addBatch(inv, 3, 80, 80, LocalDate.now().plusDays(90));
                }
            }
        }
    }

    private static void insertMenu(PreparedStatement ps, String name, String category, int priceCents, String sku) throws SQLException {
        ps.setString(1, name);
        ps.setString(2, category);
        ps.setInt(3, priceCents);
        ps.setString(4, sku);
        ps.addBatch();
        ps.executeBatch();
    }

    private static void addBatch(PreparedStatement ps, int menuItemId, int qty, int unitCostCents, LocalDate expiry) throws SQLException {
        ps.setInt(1, menuItemId);
        ps.setInt(2, qty);
        ps.setInt(3, unitCostCents);
        ps.setString(4, expiry != null ? expiry.toString() : null);
        ps.setString(5, LocalDate.now().toString());
        ps.addBatch();
        ps.executeBatch();
    }
}
