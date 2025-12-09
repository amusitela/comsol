/*
 * ConfigManager.java - JSON 配置文件读写工具
 * 简单的 JSON 序列化/反序列化 (无外部依赖)
 */

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigManager {

    private static final String DEFAULT_CONFIG_FILE = "config.json";

    /**
     * 保存配置到 JSON 文件
     */
    public static void saveConfig(SimulationConfig config, String path) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{\n");

        Field[] fields = SimulationConfig.class.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            try {
                Object value = field.get(config);
                String name = field.getName();

                json.append("  \"").append(name).append("\": ");

                if (value instanceof String) {
                    json.append("\"").append(escapeJson((String) value)).append("\"");
                } else if (value instanceof Boolean) {
                    json.append(value.toString());
                } else {
                    json.append(value.toString());
                }

                if (i < fields.length - 1) {
                    json.append(",");
                }
                json.append("\n");
            } catch (IllegalAccessException e) {
                // Skip inaccessible fields
            }
        }

        json.append("}");

        Files.write(Paths.get(path), json.toString().getBytes("UTF-8"));
        System.out.println("Config saved to: " + path);
    }

    /**
     * 从 JSON 文件加载配置
     */
    public static SimulationConfig loadConfig(String path) throws IOException {
        SimulationConfig config = new SimulationConfig();

        if (!Files.exists(Paths.get(path))) {
            System.out.println("Config file not found, using defaults: " + path);
            return config;
        }

        String content = new String(Files.readAllBytes(Paths.get(path)), "UTF-8");

        // Simple JSON parsing
        Field[] fields = SimulationConfig.class.getDeclaredFields();
        for (Field field : fields) {
            String name = field.getName();
            String pattern = "\"" + name + "\":";
            int idx = content.indexOf(pattern);
            if (idx < 0)
                continue;

            int valueStart = idx + pattern.length();
            // Skip whitespace
            while (valueStart < content.length() &&
                    Character.isWhitespace(content.charAt(valueStart))) {
                valueStart++;
            }

            try {
                Class<?> type = field.getType();

                if (type == String.class) {
                    int start = content.indexOf('"', valueStart) + 1;
                    int end = content.indexOf('"', start);
                    String value = unescapeJson(content.substring(start, end));
                    field.set(config, value);
                } else if (type == double.class) {
                    int end = findValueEnd(content, valueStart);
                    double value = Double.parseDouble(content.substring(valueStart, end).trim());
                    field.setDouble(config, value);
                } else if (type == int.class) {
                    int end = findValueEnd(content, valueStart);
                    int value = Integer.parseInt(content.substring(valueStart, end).trim());
                    field.setInt(config, value);
                } else if (type == boolean.class) {
                    int end = findValueEnd(content, valueStart);
                    boolean value = Boolean.parseBoolean(content.substring(valueStart, end).trim());
                    field.setBoolean(config, value);
                }
            } catch (Exception e) {
                System.out.println("Warning: Failed to parse field " + name + ": " + e.getMessage());
            }
        }

        System.out.println("Config loaded from: " + path);
        return config;
    }

    /**
     * 保存到默认位置
     */
    public static void saveConfig(SimulationConfig config) throws IOException {
        saveConfig(config, DEFAULT_CONFIG_FILE);
    }

    /**
     * 从默认位置加载
     */
    public static SimulationConfig loadConfig() throws IOException {
        return loadConfig(DEFAULT_CONFIG_FILE);
    }

    // ============================================
    // 辅助方法
    // ============================================

    private static int findValueEnd(String content, int start) {
        int i = start;
        while (i < content.length()) {
            char c = content.charAt(i);
            if (c == ',' || c == '\n' || c == '}') {
                return i;
            }
            i++;
        }
        return i;
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String unescapeJson(String s) {
        return s.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) {
        try {
            // 测试保存
            SimulationConfig config = SimulationConfig.getDefault();
            config.inletVelocity = 0.05; // 修改一个值
            saveConfig(config, "test_config.json");

            // 测试加载
            SimulationConfig loaded = loadConfig("test_config.json");
            System.out.println("Loaded config:");
            System.out.println(loaded);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
