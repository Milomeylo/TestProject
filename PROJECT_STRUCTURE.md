# 📁 Project Structure

## Directory Layout

```
restaurant-pos/
├── pom.xml                          # Maven configuration with dependencies
├── README.md                        # Main documentation
├── FEATURES.md                      # Complete feature list
├── QUICKSTART.md                    # Quick start guide
├── PROJECT_STRUCTURE.md             # This file
│
├── data/                            # Created at runtime
│   └── pos.db                       # SQLite database
│
├── src/main/java/com/restaurant/pos/
│   │
│   ├── MainApp.java                 # 🚀 Application Entry Point
│   │   ├── Initializes database
│   │   ├── Creates UI tabs
│   │   ├── Applies MaterialFX theme
│   │   └── Launches JavaFX application
│   │
│   ├── db/
│   │   └── Database.java            # 🗄️ Database Management
│   │       ├── Schema creation
│   │       ├── Table initialization
│   │       ├── Seed data
│   │       └── Connection management
│   │
│   ├── service/                     # 🔧 Business Logic Layer
│   │   │
│   │   ├── MenuService.java         # Menu Operations
│   │   │   └── List active menu items
│   │   │
│   │   ├── OrderService.java        # 🛒 Order Processing
│   │   │   ├── Place orders
│   │   │   ├── Apply discounts
│   │   │   ├── Calculate tax
│   │   │   ├── Generate receipts
│   │   │   ├── Record payments
│   │   │   └── Audit logging
│   │   │
│   │   ├── InventoryService.java    # 📦 Inventory Management
│   │   │   ├── FIFO deduction
│   │   │   ├── Stock level tracking
│   │   │   ├── Batch management
│   │   │   ├── Expiry warnings
│   │   │   └── COGS calculation
│   │   │
│   │   ├── DiscountService.java     # 💸 Discount Calculations
│   │   │   ├── Senior discount (20%)
│   │   │   ├── PWD discount (20%)
│   │   │   └── Discount validation
│   │   │
│   │   ├── AuditService.java        # 🔍 Audit Trail
│   │   │   ├── Log all actions
│   │   │   ├── Track user roles
│   │   │   ├── Filter logs
│   │   │   └── Retrieve history
│   │   │
│   │   ├── MenuAvailabilityService.java  # ✅ Stock Validation
│   │   │   ├── Check availability
│   │   │   ├── Validate quantities
│   │   │   └── Multi-item validation
│   │   │
│   │   ├── AnalyticsService.java    # 📊 Business Intelligence
│   │   │   ├── Sales trends
│   │   │   ├── Top selling items
│   │   │   ├── Category analysis
│   │   │   ├── Profit & Loss
│   │   │   └── Revenue calculations
│   │   │
│   │   ├── PromoService.java        # 🎁 Promotions
│   │   │   ├── Auto-generate promos
│   │   │   ├── Expiry-based discounts
│   │   │   └── List active promos
│   │   │
│   │   └── ForecastService.java     # 🔮 Sales Forecasting
│   │       ├── Moving average
│   │       ├── Historical analysis
│   │       └── Demand prediction
│   │
│   └── ui/                          # 🎨 User Interface Layer
│       │
│       ├── OrderTab.java            # 🛒 Order Placement
│       │   ├── Menu display
│       │   ├── Shopping cart
│       │   ├── Customer type selector
│       │   ├── Stock validation
│       │   ├── Checkout process
│       │   └── Receipt preview
│       │
│       ├── InventoryTab.java        # 📦 Inventory Management
│       │   ├── Stock levels table
│       │   ├── Batch details table
│       │   ├── Expiry warnings panel
│       │   ├── Add stock dialog
│       │   └── Color-coded alerts
│       │
│       ├── OrdersTab.java           # 📋 Order History
│       │   ├── Order list
│       │   ├── Discount tracking
│       │   ├── Status display
│       │   └── Receipt reprinting
│       │
│       ├── AnalyticsTab.java        # 📊 Analytics Dashboard
│       │   ├── P&L summary
│       │   ├── Sales trend chart
│       │   ├── Top items chart
│       │   ├── Category pie chart
│       │   ├── Forecast chart
│       │   └── Promo generation
│       │
│       └── AuditTab.java            # 🔍 Audit Trail Viewer
│           ├── Audit log table
│           ├── Action filtering
│           ├── Color-coded rows
│           └── Admin access
│
└── target/                          # Build output (created by Maven)
    ├── classes/                     # Compiled .class files
    ├── pos-1.0-SNAPSHOT.jar        # Executable JAR
    └── maven-archiver/              # Maven metadata
```

## Database Schema

```
pos.db (SQLite)
│
├── menu_item                        # Menu Items
│   ├── id (PK)
│   ├── name
│   ├── category
│   ├── price_cents
│   ├── sku
│   └── active
│
├── inventory_batch                  # Inventory Batches (FIFO)
│   ├── id (PK)
│   ├── menu_item_id (FK)
│   ├── quantity
│   ├── unit_cost_cents
│   ├── expiry_date
│   └── created_at
│
├── promo                            # Promotional Discounts
│   ├── id (PK)
│   ├── menu_item_id (FK)
│   ├── discount_percent
│   ├── start_date
│   ├── end_date
│   ├── reason
│   └── auto_generated
│
├── orders                           # Orders
│   ├── id (PK)
│   ├── created_at
│   ├── subtotal_cents
│   ├── discount_cents
│   ├── tax_cents
│   ├── total_cents
│   ├── payment_method
│   ├── status
│   ├── discount_type              # NEW: REGULAR/SENIOR/PWD
│   └── cogs_cents                 # NEW: Cost of goods sold
│
├── order_item                       # Order Line Items
│   ├── id (PK)
│   ├── order_id (FK)
│   ├── menu_item_id (FK)
│   ├── quantity
│   ├── unit_price_cents
│   └── line_total_cents
│
├── payment                          # Payment Records
│   ├── id (PK)
│   ├── order_id (FK)
│   ├── amount_cents
│   ├── method
│   └── created_at
│
├── order_receipt                    # Stored Receipts
│   ├── order_id (PK, FK)
│   └── content
│
├── inventory_ledger                 # Inventory Movement Log
│   ├── id (PK)
│   ├── menu_item_id (FK)
│   ├── quantity_change
│   ├── reason
│   ├── ref_type
│   ├── ref_id
│   └── created_at
│
└── audit_log                        # NEW: Audit Trail
    ├── id (PK)
    ├── action_type
    ├── entity_type
    ├── entity_id
    ├── description
    ├── user_role
    └── created_at
```

## Data Flow

### Order Placement Flow
```
User (OrderTab)
    ↓
1. Select items → MenuAvailabilityService (check stock)
    ↓
2. Add to cart → Validate availability
    ↓
3. Select discount type → DiscountService (calculate)
    ↓
4. Checkout → OrderService
    ↓
5. Create order → Database (orders table)
    ↓
6. Deduct inventory → InventoryService (FIFO)
    ↓
7. Calculate COGS → InventoryService
    ↓
8. Record payment → Database (payment table)
    ↓
9. Generate receipt → OrderService
    ↓
10. Log audit → AuditService
    ↓
11. Display receipt → User
```

### Inventory Management Flow
```
User (InventoryTab)
    ↓
1. Add stock batch → InventoryService
    ↓
2. Save to database → Database (inventory_batch)
    ↓
3. Check expiry → InventoryService (calculate warnings)
    ↓
4. Display warnings → InventoryTab (color-coded)
    ↓
5. Auto-generate promos → PromoService (if expiring soon)
    ↓
6. Log action → AuditService
```

### Analytics Flow
```
User (AnalyticsTab)
    ↓
1. Select period → AnalyticsService
    ↓
2. Query database → Aggregate data
    ↓
3. Calculate P&L → Revenue - COGS
    ↓
4. Generate trends → Daily/monthly aggregation
    ↓
5. Top items → Sort by quantity/revenue
    ↓
6. Category sales → Group by category
    ↓
7. Forecast → ForecastService (moving average)
    ↓
8. Display charts → JavaFX Charts
```

## Component Dependencies

```
MainApp
  ├── Database (initialization)
  └── UI Tabs
      ├── OrderTab
      │   ├── MenuService
      │   ├── OrderService
      │   ├── MenuAvailabilityService
      │   └── DiscountService
      │
      ├── InventoryTab
      │   ├── InventoryService
      │   └── MenuService
      │
      ├── OrdersTab
      │   └── Database (direct queries)
      │
      ├── AnalyticsTab
      │   ├── AnalyticsService
      │   ├── ForecastService
      │   └── PromoService
      │
      └── AuditTab
          └── AuditService
```

## Technology Stack

```
Presentation Layer
├── JavaFX 25.0.1 (UI Framework)
└── MaterialFX 11.17.0 (Material Design Components)

Business Logic Layer
├── Java 25 (Core Language)
└── Custom Services (11 service classes)

Data Layer
├── SQLite 3.46.0.0 (Embedded Database)
└── JDBC (Database Connectivity)

Build & Dependencies
├── Maven 3.9+ (Build Tool)
└── SLF4J 2.0.13 (Logging)
```

## Key Design Patterns

1. **Service Layer Pattern**: Business logic separated from UI
2. **Repository Pattern**: Database access through service layer
3. **MVC Pattern**: Model (services/db), View (UI), Controller (event handlers)
4. **Factory Pattern**: Database connection management
5. **Observer Pattern**: JavaFX property bindings and listeners
6. **Strategy Pattern**: Different discount types (Regular/Senior/PWD)

## File Count Summary

- **Java Files**: 16
- **Service Classes**: 11
- **UI Classes**: 5
- **Database Tables**: 9
- **Documentation Files**: 4

## Lines of Code (Approximate)

- **Service Layer**: ~1,500 lines
- **UI Layer**: ~1,800 lines
- **Database**: ~200 lines
- **Total**: ~3,500 lines

## Build Artifacts

After `mvn package`:
- `pos-1.0-SNAPSHOT.jar` (~50KB)
- Dependencies downloaded to `~/.m2/repository/`
- Compiled classes in `target/classes/`
