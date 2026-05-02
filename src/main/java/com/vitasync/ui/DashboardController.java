package com.vitasync.ui;

import com.vitasync.exceptions.DatabaseSyncException;
import com.vitasync.exceptions.ResourceAllocationException;
import com.vitasync.management.BedManager;
import com.vitasync.management.DischargeSummaryService;
import com.vitasync.management.DoctorManager;
import com.vitasync.management.EmergencyRequestManager;
import com.vitasync.management.InventoryManager;
import com.vitasync.management.PatientManager;
import com.vitasync.model.Doctor;
import com.vitasync.model.EmergencyRequest;
import com.vitasync.model.InventoryItem;
import com.vitasync.model.PatientPriority;
import com.vitasync.model.PatientRecord;
import com.vitasync.records.RecordManager;
import com.vitasync.simulator.VitalSignListener;
import com.vitasync.triage.TriageEngine;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Integrated Dashboard Controller — VITA-SYNC + VITAL-CONNECT
 * Tabs: Live Monitor | Emergency Triage | Bed Manager | Admissions | Doctors | Inventory | Alert History
 */
public class DashboardController {

    // --- COLOR PALETTE (DRACULA/GITHUB DARK THEME) ---
    private static final String BG_DARK = "#0d1117";
    private static final String BG_CARD = "#161b22";
    private static final String BORDER  = "#30363d";
    private static final String BLUE    = "#58a6ff";
    private static final String GREEN   = "#3fb950";
    private static final String YELLOW  = "#e3b341";
    private static final String RED     = "#f85149";
    private static final String MUTED   = "#8b949e";
    private static final String TEXT    = "#e6edf3";
    private static final String PURPLE  = "#bc8cff";
    private static final String ORANGE  = "#f0883e";

    private final RecordManager            recordManager;
    private final TriageEngine             triageEngine;
    private final AlertStore               alertStore;
    private final BedManager              bedManager;
    private final PatientManager          patientManager;
    private final DoctorManager           doctorManager;
    private final InventoryManager        inventoryManager;
    private final EmergencyRequestManager erManager;
    private final DischargeSummaryService summaryService;
    private final VitalSignListener       listener;
    private final Doctor                  loggedInDoctor;

    private final ObservableList<PatientRow>       patientRows   = FXCollections.observableArrayList();
    private final ObservableList<TriageRow>        triageRows    = FXCollections.observableArrayList();
    private final ObservableList<BedRow>           bedRows       = FXCollections.observableArrayList();
    private final ObservableList<DoctorRow>        doctorRows    = FXCollections.observableArrayList();
    private final ObservableList<InventoryRow>     inventoryRows = FXCollections.observableArrayList();
    private final ObservableList<EmergencyRequest> erRows        = FXCollections.observableArrayList();

    private Label totalPatientsLbl, criticalCountLbl, freeBedLbl, loggedInLbl, pendingErLbl;

    public DashboardController(RecordManager recordManager, TriageEngine triageEngine,
                                AlertStore alertStore, BedManager bedManager,
                                PatientManager patientManager, DoctorManager doctorManager,
                                InventoryManager inventoryManager,
                                EmergencyRequestManager erManager,
                                DischargeSummaryService summaryService,
                                VitalSignListener listener,
                                Doctor loggedInDoctor) {
        this.recordManager    = recordManager;
        this.triageEngine     = triageEngine;
        this.alertStore       = alertStore;
        this.bedManager       = bedManager;
        this.patientManager   = patientManager;
        this.doctorManager    = doctorManager;
        this.inventoryManager = inventoryManager;
        this.erManager        = erManager;
        this.summaryService   = summaryService;
        this.listener         = listener;
        this.loggedInDoctor   = loggedInDoctor;
    }

    public void show(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG_DARK + ";");
        root.setTop(buildHeader());

        TabPane tabs = new TabPane();
        tabs.setStyle("-fx-background-color: " + BG_DARK + ";");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        tabs.getTabs().addAll(
            buildTab(" 📋 Live Monitor",      buildVitalsTab()),
            buildTab(" 🚨 Emergency Triage",  buildTriageTab()),
            buildTab(" 🛏 Bed Manager",       buildBedsTab()),
            buildTab(" 👤 Admissions",        buildManagementTab()),
            buildTab(" 🩺 Doctors",           buildDoctorsTab()),
            buildTab(" 💊 Inventory",         buildInventoryTab()),
            buildTab(" 🚑 ER Requests",       buildErTab()),
            buildTab(" 📊 Analytics",         buildAnalyticsTab()),
            buildTab(" ⚠ Alert History",      buildAlertsTab())
        );

        root.setCenter(tabs);

        Scene scene = new Scene(root, 1380, 820);
        stage.setTitle("VITA-SYNC + VITAL-CONNECT | Unified Hospital Command Center");
        stage.setScene(scene);
        stage.show();

        startRefreshTimer();
    }

    private HBox buildHeader() {
        Label title = label("⚡ VITA-SYNC", 22, FontWeight.BOLD, BLUE);
        Label sub   = label("Integrated Smart Monitoring", 13, FontWeight.NORMAL, MUTED);

        totalPatientsLbl = label("Patients: 0",        12, FontWeight.NORMAL, TEXT);
        criticalCountLbl = label("Critical Alerts: 0", 12, FontWeight.BOLD,   RED);
        freeBedLbl       = label("Free Beds: 0",       12, FontWeight.NORMAL, GREEN);
        pendingErLbl     = label("ER Pending: 0",      12, FontWeight.BOLD,   ORANGE);
        loggedInLbl      = label("👨‍⚕️ Dr. " + (loggedInDoctor != null ? loggedInDoctor.getName() : "Guest")
                                 + "  [" + (loggedInDoctor != null ? loggedInDoctor.getSpecialization() : "") + "]",
                                 12, FontWeight.BOLD, PURPLE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(20, title, sub, spacer,
                totalPatientsLbl, criticalCountLbl, freeBedLbl, pendingErLbl, loggedInLbl);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 25, 15, 25));
        header.setStyle("-fx-background-color:" + BG_CARD + ";-fx-border-color:" + BORDER + ";-fx-border-width:0 0 1 0;");
        return header;
    }

    private VBox buildVitalsTab() {
        // ---- Search & Filter bar ----
        TextField searchField = styledField("🔍 Search by Patient ID or Name...");
        searchField.setPrefWidth(280);
        ComboBox<String> filterCombo = new ComboBox<>();
        filterCombo.getItems().addAll("All", "CRITICAL", "WARNING", "STABLE");
        filterCombo.setValue("All");
        filterCombo.setPrefHeight(35);
        filterCombo.setStyle("-fx-background-color: #0d1117; -fx-text-fill: white; -fx-border-color: #30363d;");

        FilteredList<PatientRow> filteredRows = new FilteredList<>(patientRows, p -> true);

        Runnable applyFilter = () -> {
            String query  = searchField.getText().toLowerCase().trim();
            String status = filterCombo.getValue();
            filteredRows.setPredicate(row -> {
                boolean matchesSearch = query.isEmpty()
                        || row.patientId.toLowerCase().contains(query)
                        || row.name.toLowerCase().contains(query);
                boolean matchesStatus = status.equals("All") || row.status.equals(status);
                return matchesSearch && matchesStatus;
            });
        };
        searchField.textProperty().addListener((obs, o, n) -> applyFilter.run());
        filterCombo.valueProperty().addListener((obs, o, n) -> applyFilter.run());

        HBox filterBar = new HBox(10, searchField, filterCombo);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        TableView<PatientRow> table = styledTable();
        table.setItems(filteredRows);

        TableColumn<PatientRow, String> idCol     = col("Patient ID",    100, r -> r.patientId);
        TableColumn<PatientRow, String> nameCol   = col("Name",          140, r -> r.name);
        TableColumn<PatientRow, String> bedCol    = col("Bed",            80, r -> r.bed);
        TableColumn<PatientRow, String> hrCol     = colorCol("HR (bpm)",  90, r -> String.valueOf(r.heartRate),
                v -> Integer.parseInt(v) < 40 || Integer.parseInt(v) > 140 ? RED : TEXT);
        TableColumn<PatientRow, String> spo2Col   = colorCol("SpO2 (%)",  90, r -> String.valueOf(r.spO2),
                v -> Integer.parseInt(v) < 90 ? RED : TEXT);
        TableColumn<PatientRow, String> statusCol = colorCol("Status",   110, r -> r.status,
                v -> v.equals("CRITICAL") ? RED : v.equals("WARNING") ? YELLOW : GREEN);

        TableColumn<PatientRow, Void> actionCol = new TableColumn<>("Operations");
        actionCol.setPrefWidth(240);
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button graphBtn = new Button("📈 Graph");
            private final Button reqBtn   = new Button("🚑 Request ER");
            private final HBox   box      = new HBox(8, graphBtn, reqBtn);
            {
                graphBtn.setStyle("-fx-background-color:#21262d;-fx-text-fill:#58a6ff;-fx-border-color:#30363d;-fx-cursor:hand;");
                reqBtn.setStyle("-fx-background-color:#21262d;-fx-text-fill:#f85149;-fx-border-color:#f85149;-fx-cursor:hand;");

                graphBtn.setOnAction(e -> {
                    PatientRow row = getTableView().getItems().get(getIndex());
                    PatientRecord rec = recordManager.getPatientRecord(row.patientId);
                    if (rec != null) VitalsChartWindow.show(rec);
                });

                reqBtn.setOnAction(e -> {
                    PatientRow row = getTableView().getItems().get(getIndex());
                    String docName = loggedInDoctor != null ? "Dr. " + loggedInDoctor.getName() : "Awaiting Assignment";
                    erManager.createRequest(
                            row.patientId,
                            EmergencyRequest.Priority.CRITICAL,
                            "Manual ER request — Status: " + row.status
                                    + " | HR=" + row.heartRate + " | SpO2=" + row.spO2,
                            docName);
                    reqBtn.setText("✔ Sent");
                    reqBtn.setDisable(true);
                    reqBtn.setStyle("-fx-background-color:#21262d;-fx-text-fill:#3fb950;-fx-border-color:#3fb950;");
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                PatientRow row = getTableView().getItems().get(getIndex());
                reqBtn.setVisible(!row.status.equals("STABLE"));
                setGraphic(box);
            }
        });

        table.getColumns().addAll(List.of(idCol, nameCol, bedCol, hrCol, spo2Col, statusCol, actionCol));

        VBox content = new VBox(10, filterBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return card("🏥 Live Patient Monitoring System", content);
    }

    private VBox buildTriageTab() {
        TableView<TriageRow> table = styledTable();
        table.setItems(triageRows);
        table.getColumns().addAll(List.of(
            col("Priority", 70, r -> "#" + r.rank),
            col("Patient ID", 120, r -> r.patientId),
            colorCol("Health Score", 130, r -> String.format("%.4f", r.score), v -> Double.parseDouble(v) < 0.4 ? RED : GREEN),
            colorCol("Clinical Risk", 180, r -> triageEngine.predictRiskStatus(r.hr, r.spo2), v -> v.contains("HIGH") ? RED : v.contains("MODERATE") ? YELLOW : GREEN),
            colorCol("Action Status", 150, r -> r.score < 0.38 ? "EMERGENCY REQ" : "MONITORING", v -> v.equals("EMERGENCY REQ") ? RED : MUTED)
        ));
        return card("🚨 AI-Powered Emergency Triage Queue", table);
    }

    private VBox buildBedsTab() {
        TableView<BedRow> table = styledTable();
        table.setItems(bedRows);
        table.getColumns().addAll(List.of(
            col("Bed Number", 120, r -> r.bedId),
            colorCol("Availability", 130, r -> r.status, v -> v.equals("FREE") ? GREEN : RED),
            col("Occupied By", 150, r -> r.patientId)
        ));
        return card("🛏 Real-time Bed Occupancy", table);
    }

    private VBox buildManagementTab() {
        Label title = label("Admit New Patient", 16, FontWeight.BOLD, TEXT);
        TextField idF   = styledField("Patient ID (e.g., P-101)");
        TextField nameF = styledField("Full Name");
        TextField ageF  = styledField("Age");

        // Doctor assignment dropdown
        Label docLbl = label("Assign Doctor (optional)", 12, FontWeight.NORMAL, MUTED);
        ComboBox<String> docCombo = new ComboBox<>();
        docCombo.setPromptText("Select Doctor");
        docCombo.setPrefWidth(320);
        docCombo.setPrefHeight(35);
        docCombo.setStyle("-fx-background-color: #0d1117; -fx-text-fill: white; -fx-border-color: #30363d;");
        refreshDoctorCombo(docCombo);

        Label statusLbl = label("", 12, FontWeight.NORMAL, GREEN);

        Button btn = styledButton("✔ Confirm Admission", GREEN);
        btn.setPrefWidth(200);
        btn.setOnAction(e -> {
            try {
                if (idF.getText().isEmpty() || nameF.getText().isEmpty()) {
                    statusLbl.setTextFill(Color.web(RED));
                    statusLbl.setText("Patient ID and Name are required.");
                    return;
                }
                int age = 0;
                try { age = Integer.parseInt(ageF.getText()); }
                catch (NumberFormatException ex) {
                    statusLbl.setTextFill(Color.web(RED));
                    statusLbl.setText("Age must be a number.");
                    return;
                }
                patientManager.admitPatient(idF.getText().trim(), nameF.getText().trim(), age);

                // Auto-assign selected doctor if chosen
                String selectedDoc = docCombo.getValue();
                if (selectedDoc != null && !selectedDoc.isEmpty()) {
                    String docUsername = selectedDoc.split(" \\| ")[0].trim();
                    doctorManager.findByUsername(docUsername).ifPresent(doc -> {
                        doc.assignToPatient(idF.getText().trim());
                    });
                }

                statusLbl.setTextFill(Color.web(GREEN));
                statusLbl.setText("✔ Patient " + idF.getText() + " admitted successfully.");
                idF.clear(); nameF.clear(); ageF.clear(); docCombo.setValue(null);
                refreshDoctorCombo(docCombo);
            } catch (Exception ex) {
                statusLbl.setTextFill(Color.web(RED));
                statusLbl.setText("Error: " + ex.getMessage());
            }
        });

        // Discharge section
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: " + BORDER + ";");
        Label disTitle = label("Discharge Patient", 16, FontWeight.BOLD, TEXT);
        TextField disIdF = styledField("Patient ID to discharge");
        Label disStatus  = label("", 12, FontWeight.NORMAL, GREEN);
        Button disBtn = styledButton("✖ Discharge Patient", RED);
        disBtn.setPrefWidth(200);
        disBtn.setOnAction(e -> {
            String pid = disIdF.getText().trim();
            if (pid.isEmpty()) return;
            // Generate summary BEFORE discharging (while record still exists)
            PatientRecord rec = recordManager.getPatientRecord(pid);
            String bedId = bedManager.getBedForPatient(pid);
            String docName = loggedInDoctor != null ? "Dr. " + loggedInDoctor.getName() : null;
            String summary = (rec != null)
                    ? summaryService.generateSummary(rec, docName, bedId)
                    : "Patient record not found.";
            patientManager.dischargePatient(pid);
            disStatus.setTextFill(Color.web(GREEN));
            disStatus.setText("✔ Patient " + pid + " discharged.");
            disIdF.clear();
            // Show summary popup
            showSummaryPopup(pid, summary);
        });

        VBox form = new VBox(12, title, idF, nameF, ageF, docLbl, docCombo, statusLbl, btn,
                             sep, disTitle, disIdF, disStatus, disBtn);
        form.setPadding(new Insets(20));
        form.setAlignment(Pos.TOP_LEFT);
        return card("👤 Patient Registration & Discharge", form);
    }

    private void refreshDoctorCombo(ComboBox<String> combo) {
        combo.getItems().setAll(
            doctorManager.getAvailableDoctors().stream()
                .map(d -> d.getUsername() + " | Dr. " + d.getName() + " (" + d.getSpecialization() + ")")
                .toList()
        );
    }

    // ---- DOCTORS TAB ----
    private VBox buildDoctorsTab() {
        TableView<DoctorRow> table = styledTable();
        table.setItems(doctorRows);
        table.getColumns().addAll(List.of(
            col("ID",             60,  r -> String.valueOf(r.id)),
            col("Name",           160, r -> r.name),
            col("Specialization", 180, r -> r.specialization),
            col("Username",       110, r -> r.username),
            colorCol("Status",    100, r -> r.status,
                     v -> v.equals("Available") ? GREEN : v.equals("Busy") ? YELLOW : MUTED),
            col("Assigned To",    120, r -> r.assignedPatient)
        ));

        // Add Doctor Form
        Label formTitle = label("Add New Doctor", 14, FontWeight.BOLD, TEXT);
        TextField dIdF    = styledField("Doctor ID (number)");
        TextField dNameF  = styledField("Full Name");
        TextField dSpecF  = styledField("Specialization");
        TextField dUserF  = styledField("Username");
        PasswordField dPassF = new PasswordField();
        dPassF.setPromptText("Password");
        dPassF.setPrefHeight(35);
        dPassF.setStyle("-fx-background-color: #0d1117; -fx-text-fill: white; -fx-border-color: #30363d; -fx-border-radius: 5;");
        Label dStatus = label("", 12, FontWeight.NORMAL, GREEN);

        Button addDocBtn = styledButton("➕ Add Doctor", PURPLE);
        addDocBtn.setOnAction(e -> {
            try {
                int did = Integer.parseInt(dIdF.getText().trim());
                doctorManager.addDoctor(did, dNameF.getText().trim(), dSpecF.getText().trim(),
                                        dUserF.getText().trim(), dPassF.getText());
                dStatus.setTextFill(Color.web(GREEN));
                dStatus.setText("✔ Doctor added: " + dNameF.getText());
                dIdF.clear(); dNameF.clear(); dSpecF.clear(); dUserF.clear(); dPassF.clear();
            } catch (Exception ex) {
                dStatus.setTextFill(Color.web(RED));
                dStatus.setText("Error: " + ex.getMessage());
            }
        });

        HBox form = new HBox(10, dIdF, dNameF, dSpecF, dUserF, dPassF, addDocBtn);
        form.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(12, table, formTitle, form, dStatus);
        VBox.setVgrow(table, Priority.ALWAYS);
        content.setPadding(new Insets(5));
        return card("🩺 Doctor Management (Hash Table)", content);
    }

    // ---- INVENTORY TAB ----
    private VBox buildInventoryTab() {
        TableView<InventoryRow> table = styledTable();
        table.setItems(inventoryRows);
        table.getColumns().addAll(List.of(
            col("Item ID",  70,  r -> String.valueOf(r.itemId)),
            col("Name",     200, r -> r.name),
            col("Category", 120, r -> r.category),
            colorCol("Quantity", 90, r -> String.valueOf(r.quantity),
                     v -> Integer.parseInt(v) <= 5 ? RED : Integer.parseInt(v) <= 20 ? YELLOW : GREEN)
        ));

        // Add / Restock form
        Label formTitle = label("Add / Restock Item", 14, FontWeight.BOLD, TEXT);
        TextField iIdF   = styledField("Item ID");
        TextField iNameF = styledField("Item Name");
        TextField iQtyF  = styledField("Quantity");
        ComboBox<String> catCombo = new ComboBox<>();
        catCombo.getItems().addAll("Medicine", "Equipment", "Consumable", "General");
        catCombo.setValue("Medicine");
        catCombo.setPrefHeight(35);
        catCombo.setStyle("-fx-background-color: #0d1117; -fx-text-fill: white; -fx-border-color: #30363d;");
        Label iStatus = label("", 12, FontWeight.NORMAL, GREEN);

        Button addItemBtn = styledButton("➕ Add / Restock", ORANGE);
        addItemBtn.setOnAction(e -> {
            try {
                int id  = Integer.parseInt(iIdF.getText().trim());
                int qty = Integer.parseInt(iQtyF.getText().trim());
                inventoryManager.addOrUpdateItem(id, iNameF.getText().trim(), qty, catCombo.getValue());
                iStatus.setTextFill(Color.web(GREEN));
                iStatus.setText("✔ Item updated: " + iNameF.getText());
                iIdF.clear(); iNameF.clear(); iQtyF.clear();
            } catch (Exception ex) {
                iStatus.setTextFill(Color.web(RED));
                iStatus.setText("Error: " + ex.getMessage());
            }
        });

        // Use item form
        Label useTitle = label("Use Item (Dispense)", 14, FontWeight.BOLD, TEXT);
        TextField useIdF  = styledField("Item ID");
        TextField useQtyF = styledField("Quantity to use");
        Label useStatus   = label("", 12, FontWeight.NORMAL, GREEN);
        Button useBtn = styledButton("➖ Dispense", YELLOW);
        useBtn.setOnAction(e -> {
            try {
                int id  = Integer.parseInt(useIdF.getText().trim());
                int qty = Integer.parseInt(useQtyF.getText().trim());
                boolean ok = inventoryManager.useItem(id, qty);
                if (ok) {
                    useStatus.setTextFill(Color.web(GREEN));
                    useStatus.setText("✔ Dispensed " + qty + " units of item #" + id);
                } else {
                    useStatus.setTextFill(Color.web(RED));
                    useStatus.setText("✖ Insufficient stock or item not found.");
                }
                useIdF.clear(); useQtyF.clear();
            } catch (Exception ex) {
                useStatus.setTextFill(Color.web(RED));
                useStatus.setText("Error: " + ex.getMessage());
            }
        });

        HBox addForm = new HBox(10, iIdF, iNameF, iQtyF, catCombo, addItemBtn);
        addForm.setAlignment(Pos.CENTER_LEFT);
        HBox useForm = new HBox(10, useIdF, useQtyF, useBtn);
        useForm.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(12, table, formTitle, addForm, iStatus, useTitle, useForm, useStatus);
        VBox.setVgrow(table, Priority.ALWAYS);
        content.setPadding(new Insets(5));
        return card("💊 Inventory Management (BST / TreeMap)", content);
    }

    // ---- DISCHARGE SUMMARY POPUP ----
    private void showSummaryPopup(String patientId, String summary) {
        javafx.application.Platform.runLater(() -> {
            Stage popup = new Stage();
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.setTitle("Discharge Summary — " + patientId);

            TextArea area = new TextArea(summary);
            area.setEditable(false);
            area.setWrapText(false);
            area.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 13; " +
                          "-fx-background-color: #0d1117; -fx-text-fill: #e6edf3; " +
                          "-fx-control-inner-background: #0d1117;");
            area.setPrefSize(560, 420);

            Button closeBtn = styledButton("Close", GREEN);
            closeBtn.setOnAction(e -> popup.close());

            VBox root = new VBox(12, area, closeBtn);
            root.setPadding(new Insets(20));
            root.setStyle("-fx-background-color: #161b22;");
            root.setAlignment(Pos.CENTER);

            popup.setScene(new Scene(root));
            popup.show();
        });
    }

    // ---- EMERGENCY REQUESTS TAB ----
    private VBox buildErTab() {
        TableView<EmergencyRequest> table = styledTable();
        table.setItems(erRows);

        table.getColumns().addAll(List.of(
            col("ER #",         60,  r -> String.valueOf(r.getRequestId())),
            col("Patient",      100, r -> r.getPatientId()),
            colorCol("Priority",90,  r -> r.getPriority().name(),
                     v -> v.equals("CRITICAL") ? RED : v.equals("HIGH") ? ORANGE : v.equals("MODERATE") ? YELLOW : GREEN),
            col("Description",  260, r -> r.getDescription()),
            colorCol("Status",  100, r -> r.getStatus().name(),
                     v -> v.equals("PENDING") ? ORANGE : v.equals("DISPATCHED") ? YELLOW : GREEN),
            col("Doctor",       150, r -> r.getAssignedDoctor()),
            col("Time",          90, r -> r.getCreatedAtStr())
        ));

        // Action column — update status
        TableColumn<EmergencyRequest, Void> actionCol = new TableColumn<>("Action");
        actionCol.setPrefWidth(200);
        actionCol.setCellFactory(p -> new TableCell<>() {
            private final Button dispatchBtn = new Button("Dispatch");
            private final Button resolveBtn  = new Button("Resolve");
            private final HBox   box         = new HBox(8, dispatchBtn, resolveBtn);
            {
                dispatchBtn.setStyle("-fx-background-color:#21262d;-fx-text-fill:#e3b341;-fx-border-color:#e3b341;-fx-cursor:hand;");
                resolveBtn.setStyle("-fx-background-color:#21262d;-fx-text-fill:#3fb950;-fx-border-color:#3fb950;-fx-cursor:hand;");
                dispatchBtn.setOnAction(e -> {
                    EmergencyRequest req = getTableView().getItems().get(getIndex());
                    String doc = loggedInDoctor != null ? "Dr. " + loggedInDoctor.getName() : req.getAssignedDoctor();
                    erManager.updateStatus(req.getRequestId(), EmergencyRequest.Status.DISPATCHED, doc);
                });
                resolveBtn.setOnAction(e -> {
                    EmergencyRequest req = getTableView().getItems().get(getIndex());
                    erManager.updateStatus(req.getRequestId(), EmergencyRequest.Status.RESOLVED,
                            req.getAssignedDoctor());
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                EmergencyRequest req = getTableView().getItems().get(getIndex());
                dispatchBtn.setDisable(req.getStatus() != EmergencyRequest.Status.PENDING);
                resolveBtn.setDisable(req.getStatus() == EmergencyRequest.Status.RESOLVED);
                setGraphic(box);
            }
        });
        table.getColumns().add(actionCol);

        return card("🚑 Emergency Request Management (VITAL-CONNECT Bridge)", table);
    }

    // ---- ANALYTICS TAB ----
    private VBox buildAnalyticsTab() {
        // Stat cards row
        Label statTitle = label("📊 Hospital Analytics Dashboard", 16, FontWeight.BOLD, TEXT);

        // We use Labels that get refreshed by the timer
        Label totalPatientsCard = statCard("Total Patients",    "0", BLUE);
        Label criticalCard      = statCard("Critical Now",      "0", RED);
        Label stableCard        = statCard("Stable",            "0", GREEN);
        Label warningCard       = statCard("Warning",           "0", YELLOW);
        Label freeBedCard       = statCard("Free Beds",         "0", GREEN);
        Label erPendingCard     = statCard("ER Pending",        "0", ORANGE);
        Label lowStockCard      = statCard("Low Stock Items",   "0", RED);
        Label doctorsAvailCard  = statCard("Doctors Available", "0", PURPLE);

        HBox statsRow1 = new HBox(15, totalPatientsCard, criticalCard, stableCard, warningCard);
        HBox statsRow2 = new HBox(15, freeBedCard, erPendingCard, lowStockCard, doctorsAvailCard);
        statsRow1.setAlignment(Pos.CENTER_LEFT);
        statsRow2.setAlignment(Pos.CENTER_LEFT);

        // Top critical patients list
        Label topTitle = label("Most Critical Patients (by Triage Score)", 13, FontWeight.BOLD, MUTED);
        ListView<String> topList = new ListView<>();
        topList.setPrefHeight(160);
        topList.setStyle("-fx-background-color: #0d1117; -fx-text-fill: white; -fx-border-color: #30363d;");

        // Low stock warnings
        Label lowTitle = label("⚠ Low Stock Alerts (≤ 5 units)", 13, FontWeight.BOLD, RED);
        ListView<String> lowList = new ListView<>();
        lowList.setPrefHeight(120);
        lowList.setStyle("-fx-background-color: #0d1117; -fx-text-fill: white; -fx-border-color: #30363d;");

        // Wire refresh — update every cycle via a separate timeline
        Timeline analyticsTimer = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            List<PatientRecord> records = recordManager.getAllActiveRecords();
            long critical = records.stream().filter(r -> r.getCurrentSpO2() < 90 || r.getCurrentHeartRate() > 140 || r.getCurrentHeartRate() < 40).count();
            long warning  = records.stream().filter(r -> {
                int hr = r.getCurrentHeartRate(); int sp = r.getCurrentSpO2();
                return !(hr < 40 || hr > 140 || sp < 90) && (hr < 55 || hr > 110 || sp < 94);
            }).count();
            long stable   = records.size() - critical - warning;

            updateStatCard(totalPatientsCard, String.valueOf(records.size()));
            updateStatCard(criticalCard,      String.valueOf(critical));
            updateStatCard(stableCard,        String.valueOf(stable));
            updateStatCard(warningCard,       String.valueOf(warning));
            updateStatCard(freeBedCard,       String.valueOf(bedManager.getFreeBedCount()));
            updateStatCard(erPendingCard,     String.valueOf(erManager.getPendingCount()));
            updateStatCard(lowStockCard,      String.valueOf(inventoryManager.getLowStockItems().size()));
            updateStatCard(doctorsAvailCard,  String.valueOf(doctorManager.getAvailableDoctors().size()));

            // Top 5 critical patients
            List<String> topItems = triageEngine.getAllPrioritizedPatients().stream()
                    .limit(5)
                    .map(p -> {
                        PatientRecord r = recordManager.getPatientRecord(p.getPatientId());
                        return String.format("%-8s  Score: %.4f  HR: %d  SpO2: %d%%",
                                p.getPatientId(),
                                p.getHealthStabilityScore(),
                                r != null ? r.getCurrentHeartRate() : 0,
                                r != null ? r.getCurrentSpO2() : 0);
                    }).toList();
            topList.getItems().setAll(topItems);

            // Low stock items
            List<String> lowItems = inventoryManager.getLowStockItems().stream()
                    .map(i -> String.format("ID %-4d  %-22s  Qty: %d", i.getItemId(), i.getName(), i.getQuantity()))
                    .toList();
            lowList.getItems().setAll(lowItems.isEmpty() ? List.of("✔ All items sufficiently stocked") : lowItems);
        }));
        analyticsTimer.setCycleCount(Timeline.INDEFINITE);
        analyticsTimer.play();

        VBox content = new VBox(14,
                statTitle, statsRow1, statsRow2,
                topTitle, topList,
                lowTitle, lowList);
        content.setPadding(new Insets(10));
        return card("📊 Hospital Analytics & Insights", content);
    }

    /** Creates a styled stat card label. */
    private Label statCard(String title, String value, String color) {
        Label l = new Label(title + "\n" + value);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        l.setTextFill(Color.web(color));
        l.setPrefWidth(160);
        l.setPrefHeight(60);
        l.setAlignment(Pos.CENTER);
        l.setStyle("-fx-background-color: #161b22; -fx-border-color: " + color +
                   "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8;");
        return l;
    }

    private void updateStatCard(Label card, String newValue) {
        String[] lines = card.getText().split("\n");
        if (lines.length >= 1) card.setText(lines[0] + "\n" + newValue);
    }

    private VBox buildAlertsTab() {
        TableView<AlertStore.AlertEntry> table = styledTable();
        table.setItems(alertStore.getAlerts());
        table.getColumns().addAll(List.of(
            col("Timestamp", 120, a -> a.time),
            col("Subject", 100, a -> a.patientId),
            col("Anomaly Type", 140, a -> a.vitalType),
            colorCol("Value", 90, a -> a.value, v -> RED)
        ));
        return card("⚠ Historical Critical Incident Log", table);
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

        bedRows.setAll(bedManager.getAllBeds().stream()
                .map(b -> new BedRow(b.getBedId(), b.getStatus().name(), b.getAssignedPatientId())).toList());

        doctorRows.setAll(doctorManager.getAllDoctors().stream()
                .map(DoctorRow::new).toList());

        inventoryRows.setAll(inventoryManager.getAllItemsSorted().stream()
                .map(InventoryRow::new).toList());

        erRows.setAll(erManager.getAllRequests());

        totalPatientsLbl.setText("Patients: " + records.size());
        criticalCountLbl.setText("Critical Alerts: " + records.stream()
                .filter(r -> r.getCurrentSpO2() < 90 || r.getCurrentHeartRate() > 140).count());
        freeBedLbl.setText("Free Beds: " + bedManager.getFreeBedCount());
        pendingErLbl.setText("ER Pending: " + erManager.getPendingCount());
    }

    // --- HELPER UI METHODS ---

    private Tab buildTab(String t, javafx.scene.Node c) { 
        Tab tab = new Tab(t, c); 
        return tab; 
    }

    private VBox card(String t, javafx.scene.Node c) { 
        VBox v = new VBox(8, label(t, 14, FontWeight.BOLD, BLUE), c);
        v.setPadding(new Insets(15));
        v.setStyle("-fx-background-color:" + BG_CARD + ";-fx-border-color:" + BORDER + ";-fx-border-radius:10;");
        VBox.setVgrow(c, Priority.ALWAYS);
        return v; 
    }

    private <T> TableView<T> styledTable() { 
        TableView<T> t = new TableView<>(); 
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); 
        t.setStyle("-fx-background-color: transparent; -fx-table-cell-border-color: " + BORDER + ";");
        return t; 
    }

    private Label label(String t, double s, FontWeight w, String c) { 
        Label l = new Label(t); 
        l.setFont(Font.font("Segoe UI", w, s)); 
        l.setTextFill(Color.web(c)); 
        return l; 
    }

    private TextField styledField(String p) { 
        TextField t = new TextField(); 
        t.setPromptText(p); 
        t.setPrefHeight(35);
        t.setStyle("-fx-background-color: #0d1117; -fx-text-fill: white; -fx-border-color: #30363d; -fx-border-radius: 5;");
        return t; 
    }

    private Button styledButton(String t, String c) { 
        Button b = new Button(t); 
        b.setPrefHeight(35);
        b.setStyle("-fx-background-color:" + c + "; -fx-text-fill: #0d1117; -fx-font-weight: bold; -fx-cursor: hand; -fx-border-radius: 5;"); 
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
                else { 
                    setText(item); 
                    setTextFill(Color.web(cm.color(item)));
                    setStyle("-fx-alignment: CENTER-LEFT;");
                }
            }
        });
        return c;
    }

    @FunctionalInterface interface RowMapper<R> { String map(R r); }
    @FunctionalInterface interface ColorMapper { String color(String val); }

    // --- DATA WRAPPER CLASSES ---

    static class PatientRow {
        final String patientId, name, bed, status; final int heartRate, spO2;
        PatientRow(PatientRecord r, String b) {
            patientId = r.getPatientId(); name = r.getName(); heartRate = r.getCurrentHeartRate(); spO2 = r.getCurrentSpO2(); bed = b != null ? b : "Waiting...";
            status = (heartRate < 40 || heartRate > 140 || spO2 < 90) ? "CRITICAL" : 
                     (heartRate < 55 || heartRate > 110 || spO2 < 94) ? "WARNING" : "STABLE";
        }
    }

    static class TriageRow {
        final int rank, hr, spo2; final String patientId; final double score;
        TriageRow(int r, PatientPriority p, int h, int s) { 
            rank = r; patientId = p.getPatientId(); score = p.getHealthStabilityScore(); hr = h; spo2 = s; 
        }
    }

    static class BedRow {
        final String bedId, status, patientId;
        BedRow(String i, String s, String p) { bedId = i; status = s; patientId = p != null ? p : "—"; }
    }

    static class DoctorRow {
        final int id; final String name, specialization, username, status, assignedPatient;
        DoctorRow(Doctor d) {
            id = d.getId(); name = d.getName(); specialization = d.getSpecialization();
            username = d.getUsername();
            status = d.isAvailable() ? "Available" : "Busy";
            assignedPatient = d.getAssignedPatientId() != null ? d.getAssignedPatientId() : "—";
        }
    }

    static class InventoryRow {
        final int itemId, quantity; final String name, category;
        InventoryRow(InventoryItem item) {
            itemId = item.getItemId(); name = item.getName();
            quantity = item.getQuantity(); category = item.getCategory();
        }
    }
}