package com.restaurant.pos.ui;

import com.restaurant.pos.service.*;
import io.github.palexdev.materialfx.controls.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.print.PrinterJob;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderTab extends BorderPane {
    private final MenuService menuService = new MenuService();
    private final OrderService orderService = new OrderService();
    private final MenuAvailabilityService availabilityService = new MenuAvailabilityService();

    private final ObservableList<MenuService.MenuItem> menuItems = FXCollections.observableArrayList();
    private final ObservableList<OrderService.CartLine> cartLines = FXCollections.observableArrayList();

    private final TableView<MenuService.MenuItem> menuTable = new TableView<>();
    private final TableView<OrderService.CartLine> cartTable = new TableView<>();

    private final Label totalsLabel = new Label("Subtotal: $0.00    Discount: $0.00    Tax: $0.00    Total: $0.00");
    private MFXComboBox<DiscountService.DiscountType> discountTypeCombo;
    private Label availabilityWarningLabel;

    public OrderTab() {
        setPadding(new Insets(10));
        setupMenuTable();
        setupCartTable();

        refreshMenuItems();
        menuTable.setItems(menuItems);
        cartTable.setItems(cartLines);

        // Discount type selector
        discountTypeCombo = new MFXComboBox<>();
        discountTypeCombo.getItems().addAll(DiscountService.DiscountType.values());
        discountTypeCombo.selectFirst();
        discountTypeCombo.setOnAction(e -> updateTotals());

        MFXTextField qtyField = new MFXTextField();
        qtyField.setText("1");
        qtyField.setPrefWidth(80);
        qtyField.setFloatingText("Quantity");
        
        MFXButton addBtn = new MFXButton("Add to Cart");
        addBtn.setOnAction(e -> addSelectedToCart(parseQty(qtyField.getText())));

        availabilityWarningLabel = new Label();
        availabilityWarningLabel.setTextFill(Color.RED);
        availabilityWarningLabel.setVisible(false);

        HBox addBox = new HBox(10, new Label("Qty:"), qtyField, addBtn);
        addBox.setPadding(new Insets(10, 0, 10, 0));
        addBox.setAlignment(Pos.CENTER_LEFT);

        MFXButton removeBtn = new MFXButton("Remove Selected");
        removeBtn.setOnAction(e -> removeSelectedFromCart());

        MFXButton checkoutBtn = new MFXButton("Checkout");
        checkoutBtn.setOnAction(e -> doCheckout());

        HBox discountBox = new HBox(10, new Label("Customer Type:"), discountTypeCombo);
        discountBox.setAlignment(Pos.CENTER_LEFT);
        discountBox.setPadding(new Insets(5, 0, 5, 0));

        HBox actions = new HBox(10, removeBtn, spacer(), checkoutBtn);

        Label menuLabel = new Label("Menu");
        menuLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label cartLabel = new Label("Cart");
        cartLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        VBox left = new VBox(10, menuLabel, menuTable, addBox, availabilityWarningLabel);
        VBox right = new VBox(10, cartLabel, cartTable, discountBox, totalsLabel, actions);
        VBox.setVgrow(menuTable, Priority.ALWAYS);
        VBox.setVgrow(cartTable, Priority.ALWAYS);
        left.setPadding(new Insets(10));
        right.setPadding(new Insets(10));
        
        setLeft(left);
        setCenter(right);

        // Listen to menu selection to show availability
        menuTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                checkItemAvailability(newVal);
            }
        });

        updateTotals();
    }

    private void refreshMenuItems() {
        List<MenuService.MenuItem> allItems = menuService.listActiveMenuItems();
        menuItems.clear();
        
        // Filter out items with no stock
        for (MenuService.MenuItem item : allItems) {
            if (availabilityService.isMenuItemAvailable(item.id())) {
                menuItems.add(item);
            }
        }
    }

    private void checkItemAvailability(MenuService.MenuItem item) {
        MenuAvailabilityService.AvailabilityStatus status = availabilityService.checkAvailability(item.id(), 1);
        if (status.available()) {
            availabilityWarningLabel.setText("Available: " + status.availableQuantity() + " in stock");
            availabilityWarningLabel.setTextFill(Color.GREEN);
            availabilityWarningLabel.setVisible(true);
        } else {
            availabilityWarningLabel.setText("⚠ " + status.reason());
            availabilityWarningLabel.setTextFill(Color.RED);
            availabilityWarningLabel.setVisible(true);
        }
    }

    private void setupMenuTable() {
        TableColumn<MenuService.MenuItem, String> name = new TableColumn<>("Name");
        name.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().name()));
        TableColumn<MenuService.MenuItem, String> category = new TableColumn<>("Category");
        category.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().category()));
        TableColumn<MenuService.MenuItem, String> price = new TableColumn<>("Price");
        price.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.format("$%.2f", c.getValue().priceCents()/100.0)));
        menuTable.getColumns().setAll(name, category, price);
        menuTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
    }

    private void setupCartTable() {
        TableColumn<OrderService.CartLine, String> name = new TableColumn<>("Name");
        name.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().name()));
        TableColumn<OrderService.CartLine, String> qty = new TableColumn<>("Qty");
        qty.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.valueOf(c.getValue().quantity())));
        TableColumn<OrderService.CartLine, String> total = new TableColumn<>("Line Total");
        total.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.format("$%.2f", (c.getValue().unitPriceCents()*c.getValue().quantity())/100.0)));
        cartTable.getColumns().setAll(name, qty, total);
        cartTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
    }

    private void addSelectedToCart(int quantity) {
        MenuService.MenuItem selected = menuTable.getSelectionModel().getSelectedItem();
        if (selected == null || quantity <= 0) return;
        
        // Check availability before adding
        MenuAvailabilityService.AvailabilityStatus status = availabilityService.checkAvailability(selected.id(), quantity);
        if (!status.available()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, 
                "Cannot add to cart: " + status.reason(), 
                ButtonType.OK);
            alert.setTitle("Insufficient Stock");
            alert.showAndWait();
            return;
        }
        
        cartLines.add(new OrderService.CartLine(selected.id(), selected.name(), quantity, selected.priceCents()));
        updateTotals();
    }

    private void removeSelectedFromCart() {
        OrderService.CartLine selected = cartTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            cartLines.remove(selected);
            updateTotals();
        }
    }

    private void doCheckout() {
        if (cartLines.isEmpty()) return;
        
        // Validate all items are still available
        Map<Integer, Integer> itemQuantities = new HashMap<>();
        for (OrderService.CartLine line : cartLines) {
            itemQuantities.merge(line.menuItemId(), line.quantity(), Integer::sum);
        }
        
        Map<Integer, MenuAvailabilityService.AvailabilityStatus> availabilityMap = 
            availabilityService.checkMultipleAvailability(itemQuantities);
        
        for (Map.Entry<Integer, MenuAvailabilityService.AvailabilityStatus> entry : availabilityMap.entrySet()) {
            if (!entry.getValue().available()) {
                Alert alert = new Alert(Alert.AlertType.ERROR, 
                    "Cannot complete order: " + entry.getValue().reason(), 
                    ButtonType.OK);
                alert.setTitle("Insufficient Stock");
                alert.showAndWait();
                return;
            }
        }
        
        List<OrderService.CartLine> list = new ArrayList<>(cartLines);
        DiscountService.DiscountType discountType = discountTypeCombo.getSelectedItem();
        if (discountType == null) discountType = DiscountService.DiscountType.REGULAR;
        
        OrderService.OrderResult result = orderService.placeOrder(list, "CASH", discountType);
        
        String message = String.format("Order #%d\nSubtotal: $%.2f\nDiscount: $%.2f\nTax: $%.2f\nTotal: $%.2f", 
            result.orderId, 
            result.subtotalCents/100.0,
            result.discountCents/100.0,
            result.taxCents/100.0,
            result.totalCents/100.0);
        new Alert(Alert.AlertType.INFORMATION, message).showAndWait();
        
        // Receipt display
        TextArea ta = new TextArea(result.receipt);
        ta.setEditable(false);
        ta.setPrefColumnCount(40);
        ta.setPrefRowCount(20);
        ta.setStyle("-fx-font-family: 'Courier New', monospace;");
        
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Receipt - Order #" + result.orderId);
        dlg.getDialogPane().setContent(ta);
        ButtonType printType = new ButtonType("Print", ButtonBar.ButtonData.LEFT);
        dlg.getDialogPane().getButtonTypes().addAll(printType, ButtonType.OK);
        dlg.setResultConverter(bt -> {
            if (bt == printType) {
                try {
                    PrinterJob job = PrinterJob.createPrinterJob();
                    if (job != null) {
                        boolean success = job.printPage(ta);
                        if (success) job.endJob();
                    }
                } catch (Exception ignored) {}
            }
            return null;
        });
        dlg.showAndWait();
        
        cartLines.clear();
        refreshMenuItems(); // Refresh to update availability
        updateTotals();
    }

    private void updateTotals() {
        int subtotal = cartLines.stream().mapToInt(l -> l.unitPriceCents()*l.quantity()).sum();
        
        DiscountService.DiscountType discountType = discountTypeCombo != null ? 
            discountTypeCombo.getSelectedItem() : DiscountService.DiscountType.REGULAR;
        if (discountType == null) discountType = DiscountService.DiscountType.REGULAR;
        
        DiscountService discountService = new DiscountService();
        int discount = discountService.calculateDiscount(subtotal, discountType).discountCents();
        
        int afterDiscount = subtotal - discount;
        int tax = (int)Math.round(afterDiscount * OrderService.TAX_RATE);
        int total = afterDiscount + tax;
        
        totalsLabel.setText(String.format("Subtotal: $%.2f    Discount: $%.2f    Tax: $%.2f    Total: $%.2f", 
            subtotal/100.0, discount/100.0, tax/100.0, total/100.0));
        totalsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
    }

    private int parseQty(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 1; }
    }

    private Node spacer() {
        HBox h = new HBox();
        HBox.setHgrow(h, Priority.ALWAYS);
        return h;
    }
}
