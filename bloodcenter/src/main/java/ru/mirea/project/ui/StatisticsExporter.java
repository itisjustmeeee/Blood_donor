package ru.mirea.project.ui;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

public final class StatisticsExporter {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private StatisticsExporter() {
    }

    public static Path export(List<List<String>> rows, String name, String format) throws IOException {
        String extension = "1".equals(format) ? "csv" : "xlsx";
        String fileName = name + "_" + LocalDateTime.now().format(FILE_TIME) + "." + extension;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Выберите место для сохранения статистики");
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setSelectedFile(new java.io.File(fileName));
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Файл " + extension.toUpperCase(), extension));

        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return null;

        Path path = chooser.getSelectedFile().toPath();
        String pathText = path.toString();
        if (!pathText.toLowerCase().endsWith("." + extension)) {
            path = Path.of(pathText + "." + extension);
        }
        if (Files.exists(path)) {
            int answer = JOptionPane.showConfirmDialog(null,
                    "Файл уже существует. Перезаписать его?",
                    "Подтверждение перезаписи",
                    JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) return null;
        }
        if ("1".equals(format)) return exportCsv(rows, path);
        return exportExcel(rows, path);
    }

    private static Path exportCsv(List<List<String>> rows, Path path) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write('\uFEFF');
            for (List<String> row : rows) {
                for (int i = 0; i < row.size(); i++) {
                    if (i > 0) writer.write(';');
                    writer.write(csvValue(row.get(i)));
                }
                writer.newLine();
            }
        }
        return path;
    }

    private static Path exportExcel(List<List<String>> rows, Path path) throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Статистика");
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex);
                for (int cellIndex = 0; cellIndex < rows.get(rowIndex).size(); cellIndex++) {
                    Cell cell = row.createCell(cellIndex);
                    cell.setCellValue(rows.get(rowIndex).get(cellIndex));
                }
            }
            for (int i = 0; i < rows.stream().mapToInt(List::size).max().orElse(0); i++) {
                sheet.autoSizeColumn(i);
            }
            try (var output = Files.newOutputStream(path)) {
                workbook.write(output);
            }
        }
        return path;
    }

    private static String csvValue(String value) {
        String safeValue = value == null ? "" : value;
        if (safeValue.contains(";") || safeValue.contains("\"")
                || safeValue.contains("\n") || safeValue.contains("\r")) {
            return "\"" + safeValue.replace("\"", "\"\"") + "\"";
        }
        return safeValue;
    }
}
