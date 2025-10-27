package com.restaurant.pos.ui;

import com.restaurant.pos.service.AuditService;
import io.github.palexdev.materialfx.controls.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class AuditTab extends BorderPane {
    private final AuditService auditService = new AuditService();
    private final TableView<AuditService.AuditLog> table = new TableView<>();
    private MFXComboBox<String> filterCombo;

    public AuditTab() {
        setPadding(new Insets(15));
        setupTable();
        
        Label title = new Label("🔍 Audit Trail");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        Label subtitle = new Label("Admin/Staff Only - All critical actions are logged here");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
        
        filterCombo = new MFXComboBox<>();
        filterCombo.getItems().addAll("ALL", "ORDER", "PAYMENT", "INVENTORY", "PROMO");
        filterCombo.selectFirst();
        filterCombo.setPromptText("Filter by type");
        
        MFXButton refreshBtn = new MFXButton("Refresh");
        refreshBtn.setOnAction(e -> refresh());
        
        MFXButton filterBtn = new MFXButton("Apply Filter");
        filterBtn.setOnAction(e -> applyFilter());
        
        HBox controls = new HBox(15, new Label("Filter:"), filterCombo, filterBtn, refreshBtn);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(10, 0, 10, 0));
        
        VBox header = new VBox(5, title, subtitle, controls);
        header.setPadding(new Insets(10));
        header.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ddd; -fx-border-width: 0 0 2 0;");
        
        setTop(header);
        setCenter(table);
        
        refresh();
    }

    private void setupTable() {
        TableColumn<AuditService.AuditLog, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(
            String.valueOf(c.getValue().id())));
        idCol.setPrefWidth(60);
        
        TableColumn<AuditService.AuditLog, String> actionCol = new TableColumn<>("Action");
        actionCol.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(
            c.getValue().actionType()));
        actionCol.setPrefWidth(100);
        
        TableColumn<AuditService.AuditLog, String> entityCol = new TableColumn<>("Entity");
        entityCol.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(
            c.getValue().entityType() + (c.getValue().entityId() != null ? " #" + c.getValue().entityId() : "")));
        entityCol.setPrefWidth(120);
        
        TableColumn<AuditService.AuditLog, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(
            c.getValue().description()));
        descCol.setPrefWidth(400);
        
        TableColumn<AuditService.AuditLog, String> userCol = new TableColumn<>("User");
        userCol.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(
            c.getValue().userRole()));
        userCol.setPrefWidth(80);
        
        TableColumn<AuditService.AuditLog, String> timeCol = new TableColumn<>("Timestamp");
        timeCol.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(
            c.getValue().createdAt()));
        timeCol.setPrefWidth(180);
        
        table.getColumns().setAll(idCol, actionCol, entityCol, descCol, userCol, timeCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        
        // Color code rows by action type
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(AuditService.AuditLog item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    switch (item.actionType()) {
                        case "CREATE" -> setStyle("-fx-background-color: #e8f5e9;");
                        case "PAYMENT" -> setStyle("-fx-background-color: #e3f2fd;");
                        case "INVENTORY" -> setStyle("-fx-background-color: #fff3e0;");
                        default -> setStyle("");
                    }
                }
            }
        });
    }

    private void refresh() {
        table.getItems().setAll(auditService.getRecentLogs(200));
    }

    private void applyFilter() {
        String filter = filterCombo.getSelectedItem();
        if (filter == null || filter.equals("ALL")) {
            refresh();
        } else {
            table.getItems().setAll(auditService.getLogsByEntityType(filter, 200));
        }
    }
}
