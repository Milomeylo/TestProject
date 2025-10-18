package com.restaurant.pos;

import com.restaurant.pos.db.Database;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import com.restaurant.pos.ui.OrderTab;
import com.restaurant.pos.ui.InventoryTab;
import com.restaurant.pos.ui.OrdersTab;
import com.restaurant.pos.ui.AnalyticsTab;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Initialize DB schema and seed data
        Database.initialize();

        TabPane tabs = new TabPane();
        tabs.getTabs().add(new Tab("Order", new OrderTab()));
        tabs.getTabs().add(new Tab("Inventory", new InventoryTab()));
        tabs.getTabs().add(new Tab("Orders", new OrdersTab()));
        tabs.getTabs().add(new Tab("Analytics", new AnalyticsTab()));

        BorderPane root = new BorderPane(tabs);
        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setTitle("Restaurant POS");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Tab createPlaceholderTab(String title, String message) {
        Tab tab = new Tab(title);
        tab.setClosable(false);
        tab.setContent(new BorderPane(new Label(message)));
        return tab;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
