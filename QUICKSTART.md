# 🚀 Quick Start Guide

## Prerequisites Check

Before running the application, ensure you have:

```bash
# Check Java version (need 25+)
java -version

# Check Maven version (need 3.9+)
mvn -version
```

## Installation Steps

### 1. Clone/Download the Project
```bash
cd /path/to/restaurant-pos
```

### 2. Build the Project
```bash
mvn clean package
```

This will:
- Download all dependencies (JavaFX, MaterialFX, SQLite)
- Compile all Java source files
- Create executable JAR in `target/` directory

### 3. Run the Application
```bash
mvn javafx:run
```

## First Run

On first launch, the application will:
1. Create `data/` directory
2. Initialize SQLite database at `data/pos.db`
3. Create all necessary tables
4. Seed sample data:
   - Burger ($8.99)
   - Fries ($2.99)
   - Cola ($1.99)
5. Add sample inventory with expiry dates

## Using the Application

### 🛒 Order Tab (Place Orders)

1. **Select Customer Type**
   - Regular (no discount)
   - Senior Citizen (20% off)
   - PWD (20% off)

2. **Add Items to Cart**
   - Select item from menu table
   - Enter quantity
   - Click "Add to Cart"
   - System validates stock availability

3. **Checkout**
   - Review cart items
   - Check totals (subtotal, discount, tax, total)
   - Click "Checkout"
   - View/print receipt

### 📦 Inventory Tab (Manage Stock)

1. **View Stock Levels**
   - See total quantity per item
   - View all batches with expiry dates
   - Color-coded warnings:
     - 🔴 Red: Expires in < 3 days
     - 🟡 Yellow: Expires in 3-7 days

2. **Add Stock**
   - Click "Add Stock Batch"
   - Select item
   - Enter quantity and unit cost
   - Set expiry date (optional)
   - Click OK

3. **Monitor Expiry Warnings**
   - Check warnings panel
   - See items expiring soon
   - Generate promos from Analytics tab

### 📋 Orders Tab (View History)

1. **Browse Orders**
   - See all past orders
   - Orders with discounts highlighted in yellow

2. **Reprint Receipt**
   - Select an order
   - Click "Reprint Receipt"
   - View or print

### 📊 Analytics Tab (Business Intelligence)

1. **View Profit & Loss**
   - See revenue, COGS, profit, margin
   - Adjust time period (7-365 days)

2. **Analyze Trends**
   - Daily sales line chart
   - Top selling items bar chart
   - Category sales pie chart
   - Sales forecast

3. **Generate Promos**
   - Click "Generate Expiry Promos"
   - Creates 20% discount for items expiring in 7 days

### 🔍 Audit Tab (Admin View)

1. **View Audit Trail**
   - See all logged actions
   - Filter by type (ORDER, PAYMENT, INVENTORY, PROMO)
   - Color-coded by action type

2. **Monitor Activity**
   - Track order creation
   - Monitor inventory changes
   - Review payment confirmations

## Common Tasks

### Adding New Menu Items

Currently requires direct database access. Use SQLite client:

```sql
INSERT INTO menu_item (name, category, price_cents, sku, active) 
VALUES ('Pizza', 'Food', 1299, 'PIZZ001', 1);
```

### Restocking Items

1. Go to Inventory tab
2. Click "Add Stock Batch"
3. Select item
4. Enter quantity and cost
5. Set expiry date if applicable

### Handling Expiring Items

1. Check Inventory tab warnings panel
2. Go to Analytics tab
3. Click "Generate Expiry Promos"
4. Promos automatically created for items expiring within 7 days

### Viewing Sales Performance

1. Go to Analytics tab
2. Adjust days spinner (e.g., 30 days)
3. Click "Refresh Analytics"
4. Review all charts and P&L dashboard

## Troubleshooting

### Database Issues

If database gets corrupted:
```bash
rm -rf data/
# Restart application - will recreate database
```

### JavaFX Not Found

Ensure JavaFX is in Maven repository:
```bash
mvn dependency:resolve
```

### MaterialFX Issues

Clear Maven cache and rebuild:
```bash
mvn clean
rm -rf ~/.m2/repository/io/github/palexdev/materialfx
mvn package
```

## Configuration

### Tax Rate
Edit `OrderService.java`:
```java
public static final double TAX_RATE = 0.07; // Change to your rate
```

### Discount Percentages
Edit `DiscountService.java`:
```java
SENIOR(0.20),  // Change to desired rate
PWD(0.20)      // Change to desired rate
```

### Expiry Warning Thresholds
Edit `InventoryService.java`:
```java
if (daysUntil < 3) {
    return ExpiryWarningLevel.CRITICAL;  // Change threshold
} else if (daysUntil < 7) {
    return ExpiryWarningLevel.WARNING;   // Change threshold
}
```

## Data Location

- **Database**: `data/pos.db`
- **Receipts**: Stored in database `order_receipt` table
- **Audit Logs**: Stored in database `audit_log` table

## Backup

To backup your data:
```bash
cp data/pos.db data/pos.db.backup
```

To restore:
```bash
cp data/pos.db.backup data/pos.db
```

## Support

For issues or questions:
1. Check `README.md` for detailed documentation
2. Review `FEATURES.md` for feature list
3. Examine source code in `src/main/java/`

## Next Steps

1. ✅ Add more menu items
2. ✅ Stock inventory with realistic quantities
3. ✅ Test order flow with different customer types
4. ✅ Monitor expiry warnings
5. ✅ Generate and review analytics
6. ✅ Check audit trail for compliance

Enjoy using the Restaurant POS System! 🎉
