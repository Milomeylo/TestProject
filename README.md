# 🍽️ Restaurant POS System (JavaFX + MaterialFX)

A comprehensive JavaFX-based Point of Sale system for restaurants with advanced inventory management, analytics, and audit capabilities.

## ✨ Features

### 🛒 **Order Management**
- **Menu Display**: Browse all available menu items with real-time stock availability
- **Smart Cart**: Add items to cart with automatic stock validation
- **Discount System**: Automatic 20% discount for Senior Citizens and PWD customers
- **Stock Validation**: Prevents ordering items with insufficient inventory
- **Receipt Generation**: Digital receipts with detailed breakdown (subtotal, discount, tax, total)
- **Print Support**: Print physical receipts directly from the system

### 📦 **Inventory Management**
- **FIFO Deduction**: First-In-First-Out inventory tracking
- **Batch Management**: Track multiple inventory batches with individual expiry dates
- **Expiry Warnings**: 
  - 🔴 **Critical**: Items expiring in < 3 days (red highlight)
  - 🟡 **Warning**: Items expiring in 3-7 days (yellow highlight)
- **Stock Levels**: Real-time view of total quantities per item
- **Add Stock**: Easy interface to add new inventory batches with cost and expiry tracking
- **Auto-disable Menu**: Items with zero stock are automatically hidden from ordering

### 💸 **Discounts & Accessibility**
- **Senior Citizen Discount**: Automatic 20% discount
- **PWD Discount**: Automatic 20% discount
- **Discount Tracking**: All discounts logged in order history and audit trail

### 🧂 **Smart Promotions**
- **Auto-Promo Generation**: Automatically create discounts for items nearing expiration
- **Configurable**: Set days before expiry and discount percentage
- **Expiry-Based**: Helps reduce waste by promoting soon-to-expire items

### 📊 **Analytics & Reporting**
- **Profit & Loss Dashboard**: 
  - Total Revenue
  - Cost of Goods Sold (COGS)
  - Gross Profit
  - Profit Margin %
- **Sales Trends**: Line chart showing daily sales over time
- **Top Selling Items**: Bar chart of best-performing menu items
- **Category Analysis**: Pie chart showing sales distribution by category
- **Sales Forecasting**: Predict next month's demand based on historical data
- **Customizable Period**: Analyze data for 7-365 days

### 🧾 **Receipts & Audit Trail**
- **Digital Receipts**: Stored in database for reprinting
- **Detailed Breakdown**: Shows all items, quantities, discounts, tax, and totals
- **Reprint Capability**: Access and reprint any historical receipt
- **Audit Logging**: 
  - Order creation and edits
  - Payment confirmations
  - Inventory updates
  - Promo generation
  - User role tracking (STAFF/ADMIN/SYSTEM)
- **Color-Coded Logs**: Visual distinction between action types
- **Filterable**: Filter audit logs by entity type (ORDER, PAYMENT, INVENTORY, PROMO)

### 📈 **Transaction History**
- **Order History**: Complete list of all orders with details
- **Discount Type Tracking**: See which orders used senior/PWD discounts
- **Status Monitoring**: Track order status (PAID, PENDING, etc.)
- **Receipt Access**: Quick access to reprint any receipt

## 🎨 User Interface

Built with **MaterialFX** for a modern, polished look:
- Material Design components (MFXButton, MFXTextField, MFXComboBox)
- Color-coded tables for quick visual identification
- Responsive layouts
- Intuitive navigation with tabbed interface
- Real-time updates and warnings

## 🗄️ Database Schema

**SQLite** database with the following tables:
- `menu_item`: Menu items with pricing and SKU
- `inventory_batch`: Inventory batches with FIFO tracking and expiry dates
- `promo`: Promotional discounts (manual and auto-generated)
- `orders`: Order records with discount type and COGS
- `order_item`: Line items for each order
- `payment`: Payment records
- `order_receipt`: Stored receipts for reprinting
- `inventory_ledger`: Complete inventory movement history
- `audit_log`: Comprehensive audit trail

## 🚀 Prerequisites

- **Java 25+**
- **Maven 3.9+**
- **JavaFX 25.0.1** (automatically downloaded by Maven)
- **MaterialFX 11.17.0** (automatically downloaded by Maven)

## 📦 Installation & Running

### Build the project:
```bash
mvn clean package
```

### Run the application:
```bash
mvn javafx:run
```

### Alternative (Direct Java execution):
```bash
# Windows
set JFX_VER=25.0.1
set JFX_MP=%USERPROFILE%\.m2\repository\org\openjfx\javafx-controls\%JFX_VER%
java --module-path "%JFX_MP%" --add-modules javafx.controls,javafx.fxml -cp "target\pos-1.0-SNAPSHOT.jar" com.restaurant.pos.MainApp

# Linux/Mac
JFX_VER=25.0.1
JFX_MP=~/.m2/repository/org/openjfx/javafx-controls/$JFX_VER
java --module-path "$JFX_MP" --add-modules javafx.controls,javafx.fxml -cp "target/pos-1.0-SNAPSHOT.jar" com.restaurant.pos.MainApp
```

## 📂 Project Structure

```
src/main/java/com/restaurant/pos/
├── MainApp.java                    # Application entry point
├── db/
│   └── Database.java              # Database initialization & schema
├── service/
│   ├── MenuService.java           # Menu item operations
│   ├── OrderService.java          # Order processing with discounts & audit
│   ├── InventoryService.java      # Inventory management & expiry warnings
│   ├── DiscountService.java       # Discount calculations (Senior/PWD)
│   ├── AuditService.java          # Audit trail logging
│   ├── MenuAvailabilityService.java # Stock availability checks
│   ├── AnalyticsService.java      # Sales analytics & profit/loss
│   ├── PromoService.java          # Promotional discount management
│   └── ForecastService.java       # Sales forecasting
└── ui/
    ├── OrderTab.java              # Order placement interface
    ├── InventoryTab.java          # Inventory management interface
    ├── OrdersTab.java             # Order history interface
    ├── AnalyticsTab.java          # Analytics dashboard
    └── AuditTab.java              # Audit trail viewer (Admin only)
```

## 🎯 Key Business Rules

1. **FIFO Inventory**: Items are deducted from oldest batches first
2. **Automatic Discounts**: 20% for Senior Citizens and PWD (configurable)
3. **Stock Validation**: Cannot order items with insufficient inventory
4. **Expiry Warnings**: Visual alerts for items expiring within 7 days
5. **Auto-Promos**: System can generate discounts for near-expiry items
6. **Tax Rate**: 7% tax applied after discounts
7. **COGS Tracking**: Cost of goods sold calculated using FIFO unit costs
8. **Audit Trail**: All critical actions logged with timestamps and user roles

## 💡 Usage Tips

1. **Adding Inventory**: Go to Inventory tab → Add Stock Batch → Fill details including expiry date
2. **Placing Orders**: 
   - Select customer type (Regular/Senior/PWD)
   - Add items to cart (system validates stock)
   - Checkout to complete order
3. **Generating Promos**: Analytics tab → Generate Expiry Promos button
4. **Viewing Analytics**: Analytics tab → Adjust days period → Refresh
5. **Audit Trail**: Audit tab → Filter by type → View all logged actions
6. **Reprinting Receipts**: Orders tab → Select order → Reprint Receipt

## 🔒 Security & Compliance

- **Audit Trail**: Complete logging of all critical operations
- **Discount Tracking**: All senior/PWD discounts properly recorded
- **Inventory Accountability**: Full ledger of all inventory movements
- **Receipt Storage**: All receipts stored for compliance and reprinting

## 📝 Notes

- Database file created at `data/pos.db` on first run
- Sample data seeded automatically (Burger, Fries, Cola)
- All monetary values stored in cents for precision
- Receipts formatted for 40-column thermal printers
- MaterialFX theme applied globally for consistent UI

## 🛠️ Technologies Used

- **JavaFX 25.0.1**: UI framework
- **MaterialFX 11.17.0**: Material Design components
- **SQLite 3.46.0.0**: Embedded database
- **SLF4J 2.0.13**: Logging framework
- **Maven**: Build and dependency management

## 📄 License

This is a demonstration project for a restaurant POS system.
