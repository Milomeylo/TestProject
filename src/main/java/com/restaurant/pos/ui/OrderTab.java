package com.restaurant.pos.ui;

import com.restaurant.pos.service.MenuService;
import com.restaurant.pos.service.OrderService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.print.PrinterJob;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class OrderTab extends BorderPane {
    private final MenuService menuService = new MenuService();
    private final OrderService orderService = new OrderService();

    private final ObservableList<MenuService.MenuItem> menuItems = FXCollections.observableArrayList();
    private final ObservableList<OrderService.CartLine> cartLines = FXCollections.observableArrayList();

    private final TableView<MenuService.MenuItem> menuTable = new TableView<>();
    private final TableView<OrderService.CartLine> cartTable = new TableView<>();

    private final Label totalsLabel = new Label("Subtotal: $0.00    Tax: $0.00    Total: $0.00");

    public OrderTab() {
        setPadding(new Insets(10));
        setupMenuTable();
        setupCartTable();

        menuItems.setAll(menuService.listActiveMenuItems());
        menuTable.setItems(menuItems);
        cartTable.setItems(cartLines);

        TextField qtyField = new TextField("1");
        qtyField.setPrefWidth(60);
        Button addBtn = new Button("Add to Cart");
        addBtn.setOnAction(e -> addSelectedToCart(parseQty(qtyField.getText())));

        HBox addBox = new HBox(10, new Label("Qty:"), qtyField, addBtn);
        addBox.setPadding(new Insets(10, 0, 10, 0));

        Button removeBtn = new Button("Remove Selected");
        removeBtn.setOnAction(e -> removeSelectedFromCart());

        Button checkoutBtn = new Button("Checkout");
        checkoutBtn.setOnAction(e -> doCheckout());

        HBox actions = new HBox(10, removeBtn, spacer(), checkoutBtn);

        VBox left = new VBox(new Label("Menu"), menuTable, addBox);
        VBox right = new VBox(new Label("Cart"), cartTable, totalsLabel, actions);
        VBox.setVgrow(menuTable, Priority.ALWAYS);
        VBox.setVgrow(cartTable, Priority.ALWAYS);
        setLeft(left);
        setCenter(right);

        updateTotals();
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
        List<OrderService.CartLine> list = new ArrayList<>(cartLines);
        OrderService.OrderResult result = orderService.placeOrder(list, "CASH");
        new Alert(Alert.AlertType.INFORMATION, "Order #" + result.orderId + "\nTotal: $" + String.format("%.2f", result.totalCents/100.0)).showAndWait();
        // simple receipt display for now
        TextArea ta = new TextArea(result.receipt);
        ta.setEditable(false);
        ta.setPrefColumnCount(40);
        ta.setPrefRowCount(20);
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Receipt");
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
        updateTotals();
    }

    private void updateTotals() {
        int subtotal = cartLines.stream().mapToInt(l -> l.unitPriceCents()*l.quantity()).sum();
        int tax = (int)Math.round(subtotal * OrderService.TAX_RATE);
        int total = subtotal + tax;
        totalsLabel.setText(String.format("Subtotal: $%.2f    Tax: $%.2f    Total: $%.2f", subtotal/100.0, tax/100.0, total/100.0));
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
