package com.restaurant.pos;

import com.restaurant.pos.db.Database;
import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import com.restaurant.pos.ui.*;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Initialize DB schema and seed data
        Database.initialize();

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        Tab orderTab = new Tab("🛒 Order", new OrderTab());
        Tab inventoryTab = new Tab("📦 Inventory", new InventoryTab());
        Tab ordersTab = new Tab("📋 Orders", new OrdersTab());
        Tab analyticsTab = new Tab("📊 Analytics", new AnalyticsTab());
        Tab auditTab = new Tab("🔍 Audit", new AuditTab());
        
        tabs.getTabs().addAll(orderTab, inventoryTab, ordersTab, analyticsTab, auditTab);

        BorderPane root = new BorderPane(tabs);
        Scene scene = new Scene(root, 1400, 900);
        
        // Apply MaterialFX theme
        UserAgentBuilder.builder()
            .themes(JavaFXThemes.MODENA)
            .themes(MaterialFXStylesheets.forAssemble(true))
            .setDeploy(true)
            .setResolveAssets(true)
            .build()
            .setGlobal();
        
        primaryStage.setTitle("Restaurant POS System");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
