package com.restaurant.pos.ui;

import com.restaurant.pos.service.ForecastService;
import com.restaurant.pos.service.PromoService;
import javafx.geometry.Insets;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class AnalyticsTab extends BorderPane {
    private final ForecastService forecastService = new ForecastService();
    private final PromoService promoService = new PromoService();

    private final CategoryAxis xAxis = new CategoryAxis();
    private final NumberAxis yAxis = new NumberAxis();
    private final BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);

    public AnalyticsTab() {
        setPadding(new Insets(10));
        xAxis.setLabel("Menu Item");
        yAxis.setLabel("Forecast Qty (next month)");
        chart.setTitle("Sales Forecast");

        Spinner<Integer> months = new Spinner<>();
        months.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 12, 3));
        Button refresh = new Button("Refresh Forecast");
        refresh.setOnAction(e -> refreshForecast(months.getValue()));

        Button genPromos = new Button("Generate Expiry Promos (7d, 20%)");
        genPromos.setOnAction(e -> { promoService.generateExpiryBasedPromos(7, 20); refreshForecast(months.getValue()); });

        HBox top = new HBox(10, months, refresh, genPromos);
        setTop(top);
        setCenter(chart);
        refreshForecast(months.getValue());
    }

    private void refreshForecast(int monthsWindow) {
        chart.getData().clear();
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName("Forecast");
        for (var f : forecastService.forecastNextMonthSales(monthsWindow)) {
            s.getData().add(new XYChart.Data<>(f.name(), f.forecastQty()));
        }
        chart.getData().add(s);
    }
}
