package ru.mirea.project.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;

public final class MainController {
    @FXML private Label welcomeLabel;
    @FXML private TabPane tabs;
    private JavaFxApp application;

    public void setApplication(JavaFxApp application) {
        this.application = application;
        welcomeLabel.setText("Добро пожаловать, " + application.getCurrentUser().fullName());
        tabs.getTabs().setAll(application.buildCurrentUserTabs().getTabs());
    }

    @FXML
    private void logout() {
        application.logout();
    }
}
