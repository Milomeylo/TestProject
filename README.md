# Restaurant POS (JavaFX)

A JavaFX-based restaurant ordering and inventory management system with:
- Menu browsing and order cart
- Order processing with receipt printing
- Payments and order history
- Inventory tracking with FIFO deduction and expiries
- Automatic promos for near-expiry stock
- Analytics with simple sales forecasting
- SQLite persistence (file `data/pos.db`)

## Prerequisites (Windows only)
- Java 25+
- Maven 3.9+

## Run
```bat
mvn -q -DskipTests package
mvn -q javafx:run -Dexec.mainClass=com.restaurant.pos.MainApp
```

If the JavaFX plugin has trouble on your platform, you can run:
```bat
REM If you prefer running directly with Java (Windows)
set JFX_VER=25.0.1
set JFX_MP=%USERPROFILE%\.m2\repository\org\openjfx\javafx-controls\%JFX_VER%
java --module-path "%JFX_MP%" --add-modules javafx.controls,javafx.fxml -cp "target\pos-1.0-SNAPSHOT.jar" com.restaurant.pos.MainApp
```

## Notes
- Database file is created at first run with seed data.
- Receipts are stored in `order_receipt` and can be reprinted from the Orders tab.
- Forecasting uses a simple moving average of last N months.
- Promos can be auto-generated from the Analytics tab (7 days window, 20%).
