package com.zenwealth.util;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class StockDatabase {
    private static StockDatabase instance;
    private final List<StockEntry> entries = new ArrayList<>();
    private final Map<String, StockEntry> byCode = new LinkedHashMap<>();

    private StockDatabase() {
        String[] candidates = {
            "GPLIST.xls",
            System.getProperty("user.dir") + "/GPLIST.xls",
            "D:/WHR/homework/2/java/ks3/GPLIST.xls"
        };
        boolean loaded = false;
        for (String path : candidates) {
            if (Files.exists(Path.of(path))) {
                load(path);
                loaded = true;
                break;
            }
        }
        if (!loaded) {
            System.err.println("[StockDatabase] Could not find GPLIST.xls in: " + String.join(", ", candidates));
        }
    }

    public static synchronized StockDatabase getInstance() {
        if (instance == null) {
            instance = new StockDatabase();
        }
        return instance;
    }

    private void load(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath);
             HSSFWorkbook workbook = new HSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                System.err.println("[StockDatabase] No header row found");
                return;
            }

            int codeCol = -1, nameCol = -1;
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell == null) continue;
                String h = cell.toString().trim();
                if ((h.contains("代码") || h.contains("ts_code") || h.contains("symbol"))
                        && !h.contains("B股") && !h.contains("Bɹ")) {
                    codeCol = i;
                } else if (h.contains("证券简称") || h.contains("简称")
                        || h.contains("name") || h.contains("名称")) {
                    nameCol = i;
                }
            }

            if (codeCol == -1) codeCol = 0;
            if (nameCol == -1) nameCol = 1;

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Cell codeCell = row.getCell(codeCol);
                Cell nameCell = row.getCell(nameCol);
                if (codeCell == null || nameCell == null) continue;

                String code = codeCell.toString().trim();
                String name = nameCell.toString().trim();
                if (code.isEmpty() || name.isEmpty()) continue;

                String tsCode = code;
                if (!code.contains(".") && code.length() == 6) {
                    tsCode = code + (code.startsWith("6") ? ".SH" : ".SZ");
                }

                StockEntry entry = new StockEntry(tsCode, name);
                entries.add(entry);
                byCode.put(tsCode, entry);
            }

            System.out.println("[StockDatabase] Loaded " + entries.size() + " stocks from " + filePath);

        } catch (Exception e) {
            System.err.println("[StockDatabase] Failed to load: " + e.getMessage());
        }
    }

    public List<StockEntry> search(String keyword) {
        if (keyword == null || keyword.isBlank()) return entries;
        String kw = keyword.trim().toLowerCase();
        return entries.stream()
            .filter(e -> e.code.toLowerCase().contains(kw)
                      || e.name.toLowerCase().contains(kw))
            .limit(20)
            .collect(Collectors.toList());
    }

    public StockEntry findByCode(String tsCode) {
        return byCode.get(tsCode);
    }

    public boolean isValidCode(String tsCode) {
        return byCode.containsKey(tsCode);
    }

    public record StockEntry(String code, String name) {
        @Override
        public String toString() {
            return code + "  " + name;
        }
    }
}
