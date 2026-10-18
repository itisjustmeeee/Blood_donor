package ru.mirea.project.ui;

import javafx.scene.control.TabPane;

/** Donor workspace view. The application owns shared repositories and refresh state. */
public final class DonorView {
    private final JavaFxApp application;

    public DonorView(JavaFxApp application) {
        this.application = application;
    }

    public TabPane create() {
        return application.buildDonorTabs();
    }
}
