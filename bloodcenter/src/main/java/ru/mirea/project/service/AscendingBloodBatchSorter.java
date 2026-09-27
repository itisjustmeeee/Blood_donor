package ru.mirea.project.service;

import ru.mirea.project.model.BloodBatch;

import java.util.Comparator;
import java.util.List;

public class AscendingBloodBatchSorter implements BloodBatchSorter {
    @Override
    public List<BloodBatch> sort(List<BloodBatch> batches) {
        return batches.stream()
                .sorted(Comparator.comparingInt(BloodBatch::totalVolume))
                .toList();
    }
}
