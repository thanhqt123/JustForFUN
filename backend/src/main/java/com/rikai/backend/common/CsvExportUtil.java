package com.rikai.backend.common;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class CsvExportUtil {
    public static final String BYTE_ORDER_MARK = "\uFEFF";
    public static void writeLine(Writer writer, List<String> values) throws IOException {
        boolean first = true;
        for (String value : values) {
            if (!first) {
                writer.append(',');
            }
            writer.append(escapeAndSanitize(value));
            first = false;
        }
        writer.append("\n");
    }
    private static String escapeAndSanitize(String value) {
        if (value == null) {
            return "";
        }
        String sanitized = value.trim();

        // Chống CSV Injection
        if (sanitized.startsWith("=") || sanitized.startsWith("+") ||
                sanitized.startsWith("-") || sanitized.startsWith("@")) {
            sanitized = "'" + sanitized;
        }

        // Escape nháy kép và các ký tự đặc biệt
        if (sanitized.contains("\"")) {
            sanitized = sanitized.replace("\"", "\"\"");
        }
        if (sanitized.contains(",") || sanitized.contains("\n") || sanitized.contains("\"")) {
            sanitized = "\"" + sanitized + "\"";
        }

        return sanitized;
    }

}
