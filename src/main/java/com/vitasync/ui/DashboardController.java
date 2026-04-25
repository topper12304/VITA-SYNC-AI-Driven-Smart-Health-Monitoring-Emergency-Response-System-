package com.vitasync.ui;

import com.vitasync.exceptions.DatabaseSyncException;
import com.vitasync.exceptions.ResourceAllocationException;
import com.vitasync.management.BedManager;
import com.vitasync.management.PatientManager;
import com.vitasync.model.Bed;
import com.vitasync.model.PatientPriority;
import com.vitasync.model.PatientRecord;
import com.vitasync.records.RecordManager;
import com.vitasync.simulator.VitalSignListener;
import com.vitasync.triage.TriageEngine;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DashboardController {

    private static final String BG_DARK = "#0d1117";
    private static final String BG_CARD = "#161b22";
    private static final String BORDER  = "#30363d";
    private static final String BLUE    = "#58a6ff";
    private static final String GREEN   = "#3fb950";
    private static final String YELLOW  = "#e3b341";
    private static final String RED     = "#f85149";
    private static final String MUTED   = "#8b949e";
    private static final String TEXT    = "#e6edf3";

    private final RecordManager   recordManager;
    private final TriageEngine    triageEngine;
    private final AlertStore      alertStore;
    private final BedManager      bedManager;
    private final PatientManager  patientManager;
    private final VitalSignListener listener;

    private final ObservableList<PatientRow> patientRows = FXCollections.observableArrayList();
    private final ObservableList<TriageRow>  triageRows  = FXCollections.observableArrayList();
    private final ObservableList<BedRow>     bedRows     = FXCollections.observableArrayList();

    private Label totalPatientsLbl, criticalCountLbl, freeBedLbl;

    public DashboardController(RecordManager recordManager, TriageEngine triageEngine,
                                AlertStore alertStore, BedManager bedManager,
                                PatientManager patientManager, VitalSignListener listener) {
        this.recordManager  = recordManager;
        this.triageEngine   = triageEngine;
        this.alertStore     = alertStore;
        this.bedManager     = bedManager;
        this.patientManager = patientManager;
        this.listener       = listener;
    }

    public void show(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG_DARK + ";");
        root.setTop(buildHeader());

        TabPane tabs = new TabPane();
        tabs.setStyle("-fx-background-color: " + BG_DARK + ";");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        tabs.getTabs().addAll(
            buildTab(" Patient Vitals",   buildVitalsTab()),
            buildTab("  Triage Queue",     buildTriageTab()),
            buildTab(" Bed Management",   buildBedsTab()),
            buildTab("  Patient Management", buildManagementTab()),
            buildTab("  Critical Alerts",  buildAlertsTab())
        );

        root.setCenter(tabs);

        Scene scene = new Scene(root, 1200, 780);
        stage.setTitle("VITA-SYNC — Phase 3 (AI Analytics & Response)");
        stage.setScene(scene);
        stage.show();

        startRefreshTimer();
    }

    private HBox buildHeader() {
        Label title = label("⚡ VITA-SYNC", 22, FontWeight.BOLD, BLUE);
        Label sub   = label("Smart Health Monitor | Phase 3", 13, FontWeight.NORMAL, MUTED);
        totalPatientsLbl = label("Patients: 0", 12, FontWeight.NORMAL, TEXT);
        criticalCountLbl = label("Critical: 0", 12, FontWeight.BOLD, RED);
        freeBedLbl       = label("Free Beds: 0", 12, FontWeight.NORMAL, GREEN);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(16, title, sub, spacer, totalPatientsLbl, criticalCountLbl, freeBedLbl);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 24, 12, 24));
        header.setStyle("-fx-background-color:" + BG_CARD + ";-fx-border-color:" + BORDER + ";-fx-border-width:0 0 1 0;");
        return header;
    }

    private VBox buildVitalsTab() {
        TableView<PatientRow> table = styledTable();
        table.setItems(patientRows);
        
        // --- PHASE 3: LIVE MONITORING COLUMNS ---
        TableColumn<PatientRow, String> idCol = col("Patient ID", 100, r -> r.patientId);
        TableColumn<PatientRow, String> nameCol = col("Name", 140, r -> r.name);
        TableColumn<PatientRow, String> bedCol = col("Bed", 80, r -> r.bed);
        TableColumn<PatientRow, String> hrCol = colorCol("HR (bpm)", 90, r -> String.valueOf(r.heartRate), v -> Integer.parseInt(v) < 40 || Integer.parseInt(v) > 140 ? RED : TEXT);
        TableColumn<PatientRow, String> spo2Col = colorCol("SpO2 (%)", 90, r -> String.valueOf(r.spO2), v -> Integer.parseInt(v) < 90 ? RED : TEXT);
        TableColumn<PatientRow, String> statusCol = colorCol("Status", 100, r -> r.status, v -> v.equals("CRITICAL") ? RED : v.equals("WARNING") ? YELLOW : GREEN);

        // --- PHASE 3: NEW ANALYTICS COLUMN WITH BUTTON ---
        TableColumn<PatientRow, Void> actionCol = new TableColumn<>("Analytics");
        actionCol.setPrefWidth(120);
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("View Graph");
            {
                btn.setStyle("-fx-background-color: #21262d; -fx-text-fill: #58a6ff; -fx-border-color: #30363d; -fx-border-radius: 4; -fx-cursor: hand;");
                btn.setOnAction(event -> {
                    PatientRow rowData = getTableView().getItems().get(getIndex());
                    PatientRecord record = recordManager.getPatientRecord(rowData.patientId);
                    if (record != null) {
                        VitalsChartWindow.show(record);
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(btn);
            }
        });

        table.getColumns().addAll(List.of(idCol, nameCol, bedCol, hrCol, spo2Col, statusCol, actionCol));
        return card("📋 Live Monitor", table);
    }

    private VBox buildTriageTab() {
        TableView<TriageRow> table = styledTable();
        table.setItems(triageRows);
        table.getColumns().addAll(List.of(
            col("Rank", 60, r -> "#" + r.rank),
            col("Patient ID", 110, r -> r.patientId),
            colorCol("AI Risk Score", 120, r -> String.format("%.4f", r.score), v -> Double.parseDouble(v) < 0.4 ? RED : GREEN),
            colorCol("Risk Assessment (NEWS2)", 200, r -> triageEngine.predictRiskStatus(r.hr, r.spo2), v -> v.contains("HIGH") ? RED : v.contains("MODERATE") ? YELLOW : GREEN)
        ));
        return card("🚨 AI Emergency Prioritization", table);
    }

    private VBox buildBedsTab() {
        TableView<BedRow> table = styledTable();
        table.setItems(bedRows);
        table.getColumns().addAll(List.of(
            col("Bed ID", 100, r -> r.bedId),
            colorCol("Status", 110, r -> r.status, v -> v.equals("FREE") ? GREEN : RED),
            col("Patient", 120, r -> r.patientId)
        ));
        return card("🛏 Bed Status", table);
    }

    private VBox buildManagementTab() {
        TextField idF = styledField("Patient ID"); TextField nameF = styledField("Patient Name"); TextField ageF = styledField("Age");
        Button btn = styledButton("Admit Patient", GREEN);
        btn.setOnAction(e -> {
            try {
                patientManager.admitPatient(idF.getText(), nameF.getText(), Integer.parseInt(ageF.getText()));
                idF.clear(); nameF.clear(); ageF.clear();
            } catch (Exception ex) { System.out.println("Error: " + ex.getMessage()); }
        });
        VBox form = new VBox(10, new Label("Admit New Patient"), idF, nameF, ageF, btn);
        return card("Management", form);
    }

    private VBox buildAlertsTab() {
        TableView<AlertStore.AlertEntry> table = styledTable();
        table.setItems(alertStore.getAlerts());
        table.getColumns().addAll(List.of(
            col("Time", 100, a -> a.time),
            col("Patient", 100, a -> a.patientId),
            col("Vital", 120, a -> a.vitalType),
            col("Value", 80, a -> a.value)
        ));
        return card("⚠ Alerts History", table);
    }

    private void startRefreshTimer() {
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(1500), e -> refreshData()));
        tl.setCycleCount(Timeline.INDEFINITE);
        tl.play();
    }

    private void refreshData() {
        List<PatientRecord> records = recordManager.getAllActiveRecords();
        patientRows.setAll(records.stream().map(r -> new PatientRow(r, bedManager.getBedForPatient(r.getPatientId()))).toList());
        
        List<PatientPriority> queue = triageEngine.getAllPrioritizedPatients();
        triageRows.setAll(java.util.stream.IntStream.range(0, queue.size()).mapToObj(i -> {
            PatientPriority p = queue.get(i);
            PatientRecord pr = recordManager.getPatientRecord(p.getPatientId());
            return new TriageRow(i + 1, p, pr != null ? pr.getCurrentHeartRate() : 0, pr != null ? pr.getCurrentSpO2() : 0);
        }).toList());

        bedRows.setAll(bedManager.getAllBeds().stream().map(b -> new BedRow(b.getBedId(), b.getStatus().name(), b.getAssignedPatientId())).toList());
        
        totalPatientsLbl.setText("Patients: " + records.size());
        freeBedLbl.setText("Free Beds: " + bedManager.getFreeBedCount());
    }

    private Tab buildTab(String t, javafx.scene.Node c) { 
        Tab tab = new Tab(t, c); 
        return tab; 
    }

    private VBox card(String t, javafx.scene.Node c) { 
        VBox v = new VBox(5, label(t, 13, FontWeight.BOLD, BLUE), c);
        v.setPadding(new Insets(10));
        v.setStyle("-fx-background-color:" + BG_CARD + ";-fx-border-color:" + BORDER + ";-fx-border-radius:8;");
        VBox.setVgrow(c, Priority.ALWAYS);
        return v; 
    }

    private <T> TableView<T> styledTable() { 
        TableView<T> t = new TableView<>(); 
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); 
        t.setStyle("-fx-background-color: transparent;");
        return t; 
    }

    private Label label(String t, double s, FontWeight w, String c) { 
        Label l = new Label(t); 
        l.setFont(Font.font("System", w, s)); 
        l.setTextFill(Color.web(c)); 
        return l; 
    }

    private TextField styledField(String p) { 
        TextField t = new TextField(); 
        t.setPromptText(p); 
        t.setStyle("-fx-background-color: #21262d; -fx-text-fill: white; -fx-border-color: #30363d;");
        return t; 
    }

    private Button styledButton(String t, String c) { 
        Button b = new Button(t); 
        b.setStyle("-fx-background-color:" + c + "; -fx-text-fill: #0d1117; -fx-font-weight: bold;"); 
        return b; 
    }
    
    private <R> TableColumn<R, String> col(String t, double w, RowMapper<R> m) {
        TableColumn<R, String> c = new TableColumn<>(t);
        c.setPrefWidth(w);
        c.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(m.map(data.getValue())));
        return c;
    }

    private <R> TableColumn<R, String> colorCol(String t, double w, RowMapper<R> m, ColorMapper cm) {
        TableColumn<R, String> c = col(t, w, m);
        c.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else { setText(item); setStyle("-fx-text-fill:" + cm.color(item)); }
            }
        });
        return c;
    }

    @FunctionalInterface interface RowMapper<R> { String map(R r); }
    @FunctionalInterface interface ColorMapper { String color(String val); }

    static class PatientRow {
        final String patientId, name, bed, status; final int heartRate, spO2;
        PatientRow(PatientRecord r, String b) {
            patientId = r.getPatientId(); name = r.getName(); heartRate = r.getCurrentHeartRate(); spO2 = r.getCurrentSpO2(); bed = b;
            status = heartRate < 40 || heartRate > 140 || spO2 < 90 ? "CRITICAL" : heartRate < 55 || heartRate > 110 || spO2 < 94 ? "WARNING" : "NORMAL";
        }
    }
    static class TriageRow {
        final int rank, hr, spo2; final String patientId; final double score;
        TriageRow(int r, PatientPriority p, int h, int s) { rank = r; patientId = p.getPatientId(); score = p.getHealthStabilityScore(); hr = h; spo2 = s; }
    }
    static class BedRow {
        final String bedId, status, patientId;
        BedRow(String i, String s, String p) { bedId = i; status = s; patientId = p != null ? p : "—"; }
    }
}