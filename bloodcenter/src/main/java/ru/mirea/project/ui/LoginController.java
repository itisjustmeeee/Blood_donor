package ru.mirea.project.ui;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public final class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    private JavaFxApp application;

    public void setApplication(JavaFxApp application) {
        this.application = application;
    }

    @FXML
    private void login() {
        application.handleLogin(emailField.getText().trim(), passwordField.getText());
    }

    @FXML
    private void register() {
        application.openRegistrationDialog();
    }
}
