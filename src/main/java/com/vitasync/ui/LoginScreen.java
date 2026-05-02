package com.vitasync.ui;

import com.vitasync.management.DoctorManager;
import com.vitasync.model.Doctor;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Doctor Login Screen — shown before the main dashboard.
 * Authenticates against the DoctorManager (SHA-256 hashed passwords).
 */
public class LoginScreen {

    private static final String BG_DARK  = "#0d1117";
    private static final String BG_CARD  = "#161b22";
    private static final String BORDER   = "#30363d";
    private static final String BLUE     = "#58a6ff";
    private static final String GREEN    = "#3fb950";
    private static final String RED      = "#f85149";
    private static final String MUTED    = "#8b949e";
    private static final String TEXT     = "#e6edf3";

    private final DoctorManager  doctorManager;
    private final Consumer<Doctor> onLoginSuccess;

    /**
     * @param doctorManager  the manager used to authenticate
     * @param onLoginSuccess callback invoked with the logged-in Doctor on success
     */
    public LoginScreen(DoctorManager doctorManager, Consumer<Doctor> onLoginSuccess) {
        this.doctorManager  = doctorManager;
        this.onLoginSuccess = onLoginSuccess;
    }

    public void show(Stage stage) {
        // ---- Root ----
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG_DARK + ";");

        // ---- Card ----
        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40, 50, 40, 50));
        card.setMaxWidth(420);
        card.setStyle(
            "-fx-background-color: " + BG_CARD + ";" +
            "-fx-border-color: " + BORDER + ";" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;"
        );

        // ---- Logo / Title ----
        Label logo  = lbl("⚡ VITA-SYNC", 28, FontWeight.BOLD, BLUE);
        Label sub   = lbl("Integrated Smart Hospital System", 13, FontWeight.NORMAL, MUTED);
        Label title = lbl("Doctor Login", 18, FontWeight.BOLD, TEXT);
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: " + BORDER + ";");

        // ---- Form Fields ----
        Label userLbl = lbl("Username", 12, FontWeight.NORMAL, MUTED);
        TextField userField = styledField("Enter your username");

        Label passLbl = lbl("Password", 12, FontWeight.NORMAL, MUTED);
        PasswordField passField = styledPass("Enter your password");

        // ---- Error Label ----
        Label errorLbl = lbl("", 12, FontWeight.NORMAL, RED);
        errorLbl.setVisible(false);

        // ---- Login Button ----
        Button loginBtn = new Button("Login →");
        loginBtn.setPrefWidth(320);
        loginBtn.setPrefHeight(42);
        loginBtn.setStyle(
            "-fx-background-color: " + BLUE + ";" +
            "-fx-text-fill: #0d1117;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 14;" +
            "-fx-cursor: hand;" +
            "-fx-background-radius: 8;"
        );

        // ---- Default credentials hint ----
        Label hint = lbl("Default: pramod / pramod123  |  aman / aman123", 11, FontWeight.NORMAL, MUTED);

        // ---- Login Action ----
        Runnable doLogin = () -> {
            String username = userField.getText().trim();
            String password = passField.getText();

            if (username.isEmpty() || password.isEmpty()) {
                showError(errorLbl, "Please enter both username and password.");
                return;
            }

            Optional<Doctor> result = doctorManager.authenticate(username, password);
            if (result.isPresent()) {
                onLoginSuccess.accept(result.get());
            } else {
                showError(errorLbl, "Invalid username or password. Please try again.");
                passField.clear();
            }
        };

        loginBtn.setOnAction(e -> doLogin.run());
        passField.setOnAction(e -> doLogin.run()); // Enter key in password field

        card.getChildren().addAll(logo, sub, sep, title, userLbl, userField, passLbl, passField, errorLbl, loginBtn, hint);

        BorderPane.setAlignment(card, Pos.CENTER);
        root.setCenter(card);

        Scene scene = new Scene(root, 900, 600);
        stage.setTitle("VITA-SYNC — Doctor Login");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    // ---- Helpers ----

    private void showError(Label lbl, String msg) {
        lbl.setText("⚠ " + msg);
        lbl.setVisible(true);
    }

    private Label lbl(String text, double size, FontWeight weight, String color) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", weight, size));
        l.setTextFill(Color.web(color));
        return l;
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefHeight(38);
        tf.setPrefWidth(320);
        tf.setStyle(
            "-fx-background-color: #0d1117;" +
            "-fx-text-fill: white;" +
            "-fx-border-color: " + BORDER + ";" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-font-size: 13;"
        );
        return tf;
    }

    private PasswordField styledPass(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.setPrefHeight(38);
        pf.setPrefWidth(320);
        pf.setStyle(
            "-fx-background-color: #0d1117;" +
            "-fx-text-fill: white;" +
            "-fx-border-color: " + BORDER + ";" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-font-size: 13;"
        );
        return pf;
    }
}
