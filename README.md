# Restaurant POS (JavaFX)

A JavaFX-based restaurant ordering and inventory management system with:
- Menu browsing and order cart
- Order processing with receipt printing
- Payments and order history
- Inventory tracking with FIFO deduction and expiries
- Automatic promos for near-expiry stock
- Analytics with simple sales forecasting
- SQLite persistence (file `data/pos.db`)

## Prerequisites
- Java 25+
- Maven 3.9+ (or use the `java` command shown below)

## Run
```bash
mvn -q -DskipTests package
mvn -q javafx:run -Dexec.mainClass=com.restaurant.pos.MainApp
```

If the JavaFX plugin has trouble on your platform, you can run:
```bash
# If Maven isn't installed, run directly with Java 25
JFX_VER=25.0.1
JFX_MP=~/.m2/repository/org/openjfx/javafx-controls/${JFX_VER}
java \
  --enable-preview \
  --module-path "${JFX_MP}" \
  --add-modules javafx.controls,javafx.fxml \
  -cp "target/pos-1.0-SNAPSHOT.jar:$(jdeps -q --print-classpath target/pos-1.0-SNAPSHOT.jar 2>/dev/null || echo ~/.m2/repository/*)" \
  com.restaurant.pos.MainApp
```

Note: When running with plain `java`, ensure JavaFX platform-specific classifiers are present (e.g., `linux`, `win`, `mac`, `mac-aarch64`).

## Notes
- Database file is created at first run with seed data.
- Receipts are stored in `order_receipt` and can be reprinted from the Orders tab.
- Forecasting uses a simple moving average of last N months.
- Promos can be auto-generated from the Analytics tab (7 days window, 20%).
