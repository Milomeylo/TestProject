package com.restaurant.pos.ui;

import com.restaurant.pos.service.*;
import io.github.palexdev.materialfx.controls.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class AnalyticsTab extends BorderPane {
    private final ForecastService forecastService = new ForecastService();
    private final PromoService promoService = new PromoService();
    private final AnalyticsService analyticsService = new AnalyticsService();

    private final VBox chartsContainer = new VBox(15);
    private final VBox profitLossBox = new VBox(10);

    public AnalyticsTab() {
        setPadding(new Insets(15));

        // Control panel
        Spinner<Integer> daysSpinner = new Spinner<>();
        daysSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(7, 365, 30));
        daysSpinner.setPrefWidth(80);

        MFXButton refreshBtn = new MFXButton("Refresh Analytics");
        refreshBtn.setOnAction(e -> refreshAllAnalytics(daysSpinner.getValue()));

        MFXButton genPromosBtn = new MFXButton("Generate Expiry Promos");
        genPromosBtn.setOnAction(e -> {
            promoService.generateExpiryBasedPromos(7, 20);
            new Alert(Alert.AlertType.INFORMATION, "Expiry-based promos generated successfully!").showAndWait();
            refreshAllAnalytics(daysSpinner.getValue());
        });

        HBox controls = new HBox(15, new Label("Analysis Period (days):"), daysSpinner, refreshBtn, genPromosBtn);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(10));

        // Profit/Loss Dashboard
        profitLossBox.setPadding(new Insets(15));
        profitLossBox.setStyle("-fx-background-color: #e3f2fd; -fx-border-color: #2196f3; -fx-border-width: 2;");

        ScrollPane scrollPane = new ScrollPane(chartsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: white;");

        VBox mainContent = new VBox(10, profitLossBox, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        setTop(controls);
        setCenter(mainContent);

        refreshAllAnalytics(30);
    }

    private void refreshAllAnalytics(int days) {
        chartsContainer.getChildren().clear();
        
        // Profit/Loss Summary
        refreshProfitLoss(days);
        
        // Sales Trend Chart
        chartsContainer.getChildren().add(createSalesTrendChart(days));
        
        // Top Selling Items Chart
        chartsContainer.getChildren().add(createTopSellingChart(days));
        
        // Category Sales Pie Chart
        chartsContainer.getChildren().add(createCategorySalesChart(days));
        
        // Forecast Chart
        chartsContainer.getChildren().add(createForecastChart());
    }

    private void refreshProfitLoss(int days) {
        profitLossBox.getChildren().clear();
        
        Label title = new Label("💰 Profit & Loss Summary");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        AnalyticsService.ProfitLoss pl = analyticsService.calculateProfitLoss(days);
        
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 0, 0, 0));
        
        addPLRow(grid, 0, "Total Revenue:", String.format("$%.2f", pl.totalRevenueCents() / 100.0), "#4caf50");
        addPLRow(grid, 1, "Cost of Goods Sold:", String.format("$%.2f", pl.totalCogsCents() / 100.0), "#ff9800");
        addPLRow(grid, 2, "Gross Profit:", String.format("$%.2f", pl.profitCents() / 100.0), 
            pl.profitCents() >= 0 ? "#2196f3" : "#f44336");
        addPLRow(grid, 3, "Profit Margin:", String.format("%.2f%%", pl.profitMargin()), 
            pl.profitMargin() >= 0 ? "#2196f3" : "#f44336");
        
        profitLossBox.getChildren().addAll(title, grid);
    }

    private void addPLRow(GridPane grid, int row, String label, String value, String color) {
        Label lblLabel = new Label(label);
        lblLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        
        grid.add(lblLabel, 0, row);
        grid.add(lblValue, 1, row);
    }

    private LineChart<String, Number> createSalesTrendChart(int days) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Sales Trend (Last " + days + " Days)");
        chart.setPrefHeight(300);
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Daily Sales");
        
        for (var trend : analyticsService.getDailySalesTrend(days)) {
            series.getData().add(new XYChart.Data<>(trend.period(), trend.totalSalesCents() / 100.0));
        }
        
        chart.getData().add(series);
        return chart;
    }

    private BarChart<String, Number> createTopSellingChart(int days) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Top 10 Selling Items (Last " + days + " Days)");
        chart.setPrefHeight(300);
        xAxis.setLabel("Item");
        yAxis.setLabel("Quantity Sold");
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Quantity");
        
        for (var item : analyticsService.getTopSellingItems(10, days)) {
            series.getData().add(new XYChart.Data<>(item.name(), item.totalQuantity()));
        }
        
        chart.getData().add(series);
        return chart;
    }

    private PieChart createCategorySalesChart(int days) {
        PieChart chart = new PieChart();
        chart.setTitle("Sales by Category (Last " + days + " Days)");
        chart.setPrefHeight(300);
        
        for (var cat : analyticsService.getCategorySales(days)) {
            chart.getData().add(new PieChart.Data(
                cat.category() + " (" + cat.percentage() + "%)", 
                cat.totalSalesCents() / 100.0
            ));
        }
        
        return chart;
    }

    private BarChart<String, Number> createForecastChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Sales Forecast (Next Month)");
        chart.setPrefHeight(300);
        xAxis.setLabel("Menu Item");
        yAxis.setLabel("Forecast Quantity");
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Forecast");
        
        for (var f : forecastService.forecastNextMonthSales(3)) {
            series.getData().add(new XYChart.Data<>(f.name(), f.forecastQty()));
        }
        
        chart.getData().add(series);
        return chart;
    }
}
