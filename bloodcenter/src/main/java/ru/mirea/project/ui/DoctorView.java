package ru.mirea.project.ui;

import javafx.scene.control.TabPane;

/** Doctor workspace view. */
public final class DoctorView {
    private final JavaFxApp application;

    public DoctorView(JavaFxApp application) {
        this.application = application;
    }

    public TabPane create() {
        return application.buildDoctorTabs();
    }
}
