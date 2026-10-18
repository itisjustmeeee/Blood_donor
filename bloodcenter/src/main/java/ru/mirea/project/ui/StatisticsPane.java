package ru.mirea.project.ui;

import javafx.scene.layout.VBox;

/** Statistics section facade used by workspace views. */
public final class StatisticsPane {
    private final JavaFxApp application;
    private final Integer donorId;

    public StatisticsPane(JavaFxApp application, Integer donorId) {
        this.application = application;
        this.donorId = donorId;
    }

    public VBox create() {
        return application.createStatisticsPane(donorId);
    }
}
