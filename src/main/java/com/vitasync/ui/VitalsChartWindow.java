package com.vitasync.ui;

import com.vitasync.model.PatientRecord;
import com.vitasync.model.VitalSignReading;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;

public class VitalsChartWindow {

    public static void show(PatientRecord record) {
        // Platform.runLater ensures UI changes happen on the correct thread
        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                stage.setTitle("Analytics: " + record.getName());

                NumberAxis xAxis = new NumberAxis();
                xAxis.setLabel("Recent Readings");
                xAxis.setForceZeroInRange(false);

                NumberAxis yAxis = new NumberAxis(30, 160, 10);
                yAxis.setLabel("Value");

                LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
                chart.setTitle("Live Health Trends");
                chart.setAnimated(false);
                chart.setCreateSymbols(true);

                XYChart.Series<Number, Number> hrSeries = new XYChart.Series<>();
                hrSeries.setName("HR (bpm)");
                XYChart.Series<Number, Number> spo2Series = new XYChart.Series<>();
                spo2Series.setName("SpO2 (%)");

                chart.getData().addAll(hrSeries, spo2Series);

                // Update Logic
                Timeline timeline = new Timeline(new KeyFrame(Duration.millis(1000), e -> {
                    // CRITICAL: Create a snapshot copy of the history to avoid crash
                    List<VitalSignReading> historySnapshot;
                    synchronized (record) {
                        historySnapshot = new ArrayList<>(record.getHistory());
                    }

                    if (!historySnapshot.isEmpty()) {
                        hrSeries.getData().clear();
                        spo2Series.getData().clear();
                        
                        int start = Math.max(0, historySnapshot.size() - 15);
                        for (int i = start; i < historySnapshot.size(); i++) {
                            int x = i - start;
                            hrSeries.getData().add(new XYChart.Data<>(x, historySnapshot.get(i).getHeartRate()));
                            spo2Series.getData().add(new XYChart.Data<>(x, historySnapshot.get(i).getSpO2()));
                        }
                    }
                }));
                
                timeline.setCycleCount(Timeline.INDEFINITE);
                timeline.play();

                Scene scene = new Scene(chart, 650, 400);
                stage.setScene(scene);
                stage.setOnCloseRequest(ev -> timeline.stop());
                stage.show();

            } catch (Exception ex) {
                System.err.println("Failed to open graph window: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }
}