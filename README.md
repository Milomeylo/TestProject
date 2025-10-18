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
- Java 21+
- Maven 3.9+

## Run
```bash
mvn -q -DskipTests package
mvn -q javafx:run -Dexec.mainClass=com.restaurant.pos.MainApp
```

If the JavaFX plugin has trouble on your platform, you can run:
```bash
java --module-path ~/.m2/repository/org/openjfx/javafx-controls/21.0.5 --add-modules javafx.controls,javafx.fxml \
     -cp target/pos-1.0-SNAPSHOT.jar:$(dependency:list -DincludeScope=runtime -DoutputAbsoluteArtifactFilename=true -DincludeTypes=jar -DexcludeTransitive=false -DappendOutput=false | grep ".jar" | tr '\n' ':') \
     com.restaurant.pos.MainApp
```

## Notes
- Database file is created at first run with seed data.
- Receipts are stored in `order_receipt` and can be reprinted from the Orders tab.
- Forecasting uses a simple moving average of last N months.
- Promos can be auto-generated from the Analytics tab (7 days window, 20%).
