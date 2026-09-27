package ru.mirea.project.service;

import ru.mirea.project.model.BloodBatch;

import java.util.List;

public interface BloodBatchSorter {
    List<BloodBatch> sort(List<BloodBatch> batches);
}
