/*
 * AIConfigParser.java - 解析 AI 响应并映射到配置变更
 * 将自然语言描述转换为结构化的配置修改
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AIConfigParser {

    /**
     * 配置变更项
     */
    public static class ConfigChange {
        public String fieldName; // 配置字段名
        public String fieldLabel; // 字段中文名
        public String oldValue; // 旧值
        public String newValue; // 新值
        public String fieldType; // 字段类型: double, int, string, boolean

        public ConfigChange(String fieldName, String fieldLabel, String oldValue, String newValue, String fieldType) {
            this.fieldName = fieldName;
            this.fieldLabel = fieldLabel;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.fieldType = fieldType;
        }

        @Override
        public String toString() {
            return fieldLabel + " (" + fieldName + "): " + oldValue + " → " + newValue;
        }
    }

    /**
     * AI 解析结果
     */
    public static class ParseResult {
        public boolean success;
        public String message; // AI 的说明/回复
        public List<ConfigChange> changes; // 配置变更列表
        public String error; // 错误信息

        public ParseResult() {
            this.changes = new ArrayList<>();
        }
    }

    // 字段信息映射
    private static final Map<String, String[]> FIELD_INFO = new HashMap<>();

    static {
        // 格式: fieldName -> [中文名, 类型, 单位]
        FIELD_INFO.put("domainWidth", new String[] { "域宽度", "double", "m" });
        FIELD_INFO.put("domainHeight", new String[] { "域高度", "double", "m" });
        FIELD_INFO.put("cylinderRadius", new String[] { "圆柱半径", "double", "m" });
        FIELD_INFO.put("cylinderX", new String[] { "圆柱X坐标", "double", "m" });
        FIELD_INFO.put("cylinderY", new String[] { "圆柱Y坐标", "double", "m" });
        FIELD_INFO.put("inletType", new String[] { "入口类型", "string", "" });
        FIELD_INFO.put("inletVelocity", new String[] { "入口速度", "double", "m/s" });
        FIELD_INFO.put("inletPressure", new String[] { "入口压力", "double", "Pa" });
        FIELD_INFO.put("outletType", new String[] { "出口类型", "string", "" });
        FIELD_INFO.put("outletPressure", new String[] { "出口压力", "double", "Pa" });
        FIELD_INFO.put("outletVelocity", new String[] { "出口速度", "double", "m/s" });
        FIELD_INFO.put("topBoundaryType", new String[] { "上边界类型", "string", "" });
        FIELD_INFO.put("bottomBoundaryType", new String[] { "下边界类型", "string", "" });
        FIELD_INFO.put("cylinderWallType", new String[] { "圆柱壁面类型", "string", "" });
        FIELD_INFO.put("cylinderWallCondition", new String[] { "圆柱壁面条件", "string", "" });
        FIELD_INFO.put("flowType", new String[] { "流动类型", "string", "" });
        FIELD_INFO.put("equationForm", new String[] { "方程形式", "string", "" });
        FIELD_INFO.put("fluidName", new String[] { "流体名称", "string", "" });
        FIELD_INFO.put("density", new String[] { "密度", "double", "kg/m³" });
        FIELD_INFO.put("dynamicViscosity", new String[] { "动力粘度", "double", "Pa·s" });
        FIELD_INFO.put("meshSizeLevel", new String[] { "网格精度等级", "int", "" });
        FIELD_INFO.put("meshMaxSize", new String[] { "最大网格尺寸", "double", "m" });
        FIELD_INFO.put("meshMinSize", new String[] { "最小网格尺寸", "double", "m" });
        FIELD_INFO.put("cylinderMeshMaxSize", new String[] { "圆柱网格尺寸", "double", "m" });
        FIELD_INFO.put("startTime", new String[] { "开始时间", "double", "s" });
        FIELD_INFO.put("endTime", new String[] { "结束时间", "double", "s" });
        FIELD_INFO.put("timeStep", new String[] { "时间步长", "double", "s" });
        FIELD_INFO.put("outputDir", new String[] { "输出目录", "string", "" });
        FIELD_INFO.put("modelFileName", new String[] { "模型文件名", "string", "" });
        FIELD_INFO.put("exportVelocity", new String[] { "导出速度云图", "boolean", "" });
        FIELD_INFO.put("exportVorticity", new String[] { "导出涡量云图", "boolean", "" });
        FIELD_INFO.put("exportPressure", new String[] { "导出压力云图", "boolean", "" });
        FIELD_INFO.put("exportAnimation", new String[] { "导出动画", "boolean", "" });
        FIELD_INFO.put("animationFps", new String[] { "动画帧率", "int", "fps" });
        FIELD_INFO.put("animationMaxFrames", new String[] { "最大帧数", "int", "" });
    }

    /**
     * 生成系统提示词（告诉 AI 如何解析和响应）
     */
    public static String generateSystemPrompt(SimulationConfig currentConfig) {
        StringBuilder sb = new StringBuilder();

        sb.append("你是一个 COMSOL 圆柱绕流（卡门涡街）仿真配置助手。用户会用自然语言描述想要修改的仿真参数，你需要理解用户意图并输出结构化的配置修改。\n\n");

        sb.append("## 当前配置\n");
        sb.append("```json\n");
        sb.append(configToJson(currentConfig));
        sb.append("\n```\n\n");

        sb.append("## 可用配置字段\n");
        sb.append("| 字段名 | 中文名 | 类型 | 单位 |\n");
        sb.append("|--------|--------|------|------|\n");
        for (Map.Entry<String, String[]> entry : FIELD_INFO.entrySet()) {
            String[] info = entry.getValue();
            sb.append("| ").append(entry.getKey()).append(" | ").append(info[0])
                    .append(" | ").append(info[1]).append(" | ").append(info[2]).append(" |\n");
        }
        sb.append("\n");

        sb.append("## 常用预设\n");
        sb.append("- 空气 (Air): density=1.225 kg/m³, dynamicViscosity=1.7894e-5 Pa·s\n");
        sb.append("- 水 (Water): density=998.0 kg/m³, dynamicViscosity=1.002e-3 Pa·s\n\n");

        sb.append("## 响应格式要求\n");
        sb.append("请严格按以下 JSON 格式响应，不要添加任何其他文字：\n");
        sb.append("```json\n");
        sb.append("{\n");
        sb.append("  \"message\": \"对用户的友好回复，解释将要进行的修改\",\n");
        sb.append("  \"changes\": [\n");
        sb.append("    {\"field\": \"字段名\", \"value\": \"新值\"},\n");
        sb.append("    {\"field\": \"字段名\", \"value\": \"新值\"}\n");
        sb.append("  ]\n");
        sb.append("}\n");
        sb.append("```\n\n");

        sb.append("## 注意事项\n");
        sb.append("1. 只输出 JSON，不要有其他文字\n");
        sb.append("2. 数值字段用数字，不要带单位\n");
        sb.append("3. 字符串字段用双引号\n");
        sb.append("4. 布尔字段用 true/false\n");
        sb.append("5. 如果用户的请求不涉及配置修改（如普通问候或问题），返回 changes 为空数组，只在 message 中回复\n");
        sb.append("6. 如果用户要切换流体（如\"使用水\"），同时更新 fluidName、density、dynamicViscosity\n");

        return sb.toString();
    }

    /**
     * 将当前配置转为 JSON 字符串
     */
    private static String configToJson(SimulationConfig cfg) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"domainWidth\": ").append(cfg.domainWidth).append(",\n");
        sb.append("  \"domainHeight\": ").append(cfg.domainHeight).append(",\n");
        sb.append("  \"cylinderRadius\": ").append(cfg.cylinderRadius).append(",\n");
        sb.append("  \"cylinderX\": ").append(cfg.cylinderX).append(",\n");
        sb.append("  \"cylinderY\": ").append(cfg.cylinderY).append(",\n");
        sb.append("  \"inletType\": \"").append(cfg.inletType).append("\",\n");
        sb.append("  \"inletVelocity\": ").append(cfg.inletVelocity).append(",\n");
        sb.append("  \"inletPressure\": ").append(cfg.inletPressure).append(",\n");
        sb.append("  \"outletType\": \"").append(cfg.outletType).append("\",\n");
        sb.append("  \"outletPressure\": ").append(cfg.outletPressure).append(",\n");
        sb.append("  \"outletVelocity\": ").append(cfg.outletVelocity).append(",\n");
        sb.append("  \"topBoundaryType\": \"").append(cfg.topBoundaryType).append("\",\n");
        sb.append("  \"bottomBoundaryType\": \"").append(cfg.bottomBoundaryType).append("\",\n");
        sb.append("  \"cylinderWallType\": \"").append(cfg.cylinderWallType).append("\",\n");
        sb.append("  \"cylinderWallCondition\": \"").append(cfg.cylinderWallCondition).append("\",\n");
        sb.append("  \"flowType\": \"").append(cfg.flowType).append("\",\n");
        sb.append("  \"equationForm\": \"").append(cfg.equationForm).append("\",\n");
        sb.append("  \"fluidName\": \"").append(cfg.fluidName).append("\",\n");
        sb.append("  \"density\": ").append(cfg.density).append(",\n");
        sb.append("  \"dynamicViscosity\": ").append(cfg.dynamicViscosity).append(",\n");
        sb.append("  \"meshSizeLevel\": ").append(cfg.meshSizeLevel).append(",\n");
        sb.append("  \"meshMaxSize\": ").append(cfg.meshMaxSize).append(",\n");
        sb.append("  \"meshMinSize\": ").append(cfg.meshMinSize).append(",\n");
        sb.append("  \"cylinderMeshMaxSize\": ").append(cfg.cylinderMeshMaxSize).append(",\n");
        sb.append("  \"startTime\": ").append(cfg.startTime).append(",\n");
        sb.append("  \"endTime\": ").append(cfg.endTime).append(",\n");
        sb.append("  \"timeStep\": ").append(cfg.timeStep).append(",\n");
        sb.append("  \"outputDir\": \"").append(cfg.outputDir).append("\",\n");
        sb.append("  \"modelFileName\": \"").append(cfg.modelFileName).append("\",\n");
        sb.append("  \"exportVelocity\": ").append(cfg.exportVelocity).append(",\n");
        sb.append("  \"exportVorticity\": ").append(cfg.exportVorticity).append(",\n");
        sb.append("  \"exportAnimation\": ").append(cfg.exportAnimation).append(",\n");
        sb.append("  \"animationFps\": ").append(cfg.animationFps).append(",\n");
        sb.append("  \"animationMaxFrames\": ").append(cfg.animationMaxFrames).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * 解析 AI 响应，提取配置变更
     */
    public static ParseResult parseAIResponse(String aiResponse, SimulationConfig currentConfig) {
        ParseResult result = new ParseResult();

        try {
            // 尝试提取 JSON 部分（AI 可能会包含 ```json ... ``` 代码块）
            String json = extractJson(aiResponse);
            if (json == null) {
                result.success = false;
                result.error = "无法从 AI 响应中提取 JSON";
                result.message = aiResponse; // 显示原始响应
                return result;
            }

            // 解析 message
            result.message = extractStringField(json, "message");
            if (result.message == null) {
                result.message = "AI 未提供说明";
            }

            // 解析 changes 数组
            String changesArray = extractArrayField(json, "changes");
            if (changesArray != null && !changesArray.isEmpty()) {
                parseChanges(changesArray, currentConfig, result);
            }

            result.success = true;

        } catch (Exception e) {
            result.success = false;
            result.error = "解析错误: " + e.getMessage();
            result.message = aiResponse;
        }

        return result;
    }

    /**
     * 从响应中提取 JSON
     */
    private static String extractJson(String response) {
        // 尝试查找 ```json ... ``` 代码块
        int start = response.indexOf("```json");
        if (start != -1) {
            start = response.indexOf("\n", start) + 1;
            int end = response.indexOf("```", start);
            if (end != -1) {
                return response.substring(start, end).trim();
            }
        }

        // 尝试查找 ``` ... ``` 代码块
        start = response.indexOf("```");
        if (start != -1) {
            start = response.indexOf("\n", start) + 1;
            int end = response.indexOf("```", start);
            if (end != -1) {
                String content = response.substring(start, end).trim();
                if (content.startsWith("{")) {
                    return content;
                }
            }
        }

        // 直接查找 { ... }
        start = response.indexOf("{");
        if (start != -1) {
            int end = response.lastIndexOf("}");
            if (end > start) {
                return response.substring(start, end + 1);
            }
        }

        return null;
    }

    /**
     * 提取 JSON 中的字符串字段
     */
    private static String extractStringField(String json, String fieldName) {
        String pattern = "\"" + fieldName + "\"";
        int start = json.indexOf(pattern);
        if (start == -1)
            return null;

        start = json.indexOf(":", start) + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start)))
            start++;

        if (json.charAt(start) != '"')
            return null;
        start++;

        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'n':
                        sb.append('\n');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    default:
                        sb.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * 提取 JSON 中的数组字段
     */
    private static String extractArrayField(String json, String fieldName) {
        String pattern = "\"" + fieldName + "\"";
        int start = json.indexOf(pattern);
        if (start == -1)
            return null;

        start = json.indexOf("[", start);
        if (start == -1)
            return null;

        int depth = 0;
        int end = start;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[')
                depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) {
                    end = i;
                    break;
                }
            }
        }

        return json.substring(start, end + 1);
    }

    /**
     * 解析 changes 数组
     */
    private static void parseChanges(String changesArray, SimulationConfig cfg, ParseResult result) {
        // 简单解析数组中的每个对象
        int pos = 1; // 跳过 [

        while (pos < changesArray.length()) {
            int objStart = changesArray.indexOf("{", pos);
            if (objStart == -1)
                break;

            int objEnd = changesArray.indexOf("}", objStart);
            if (objEnd == -1)
                break;

            String obj = changesArray.substring(objStart, objEnd + 1);

            String field = extractStringField(obj, "field");
            String value = extractValue(obj, "value");

            if (field != null && value != null && FIELD_INFO.containsKey(field)) {
                String[] info = FIELD_INFO.get(field);
                String oldValue = getFieldValue(cfg, field);

                ConfigChange change = new ConfigChange(field, info[0], oldValue, value, info[1]);
                result.changes.add(change);
            }

            pos = objEnd + 1;
        }
    }

    /**
     * 提取值（可以是字符串、数字或布尔）
     */
    private static String extractValue(String json, String fieldName) {
        String pattern = "\"" + fieldName + "\"";
        int start = json.indexOf(pattern);
        if (start == -1)
            return null;

        start = json.indexOf(":", start) + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start)))
            start++;

        char firstChar = json.charAt(start);

        if (firstChar == '"') {
            // 字符串值
            return extractStringField(json, fieldName);
        } else {
            // 数字或布尔值
            int end = start;
            while (end < json.length()) {
                char c = json.charAt(end);
                if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c))
                    break;
                end++;
            }
            return json.substring(start, end).trim();
        }
    }

    /**
     * 获取配置字段的当前值
     */
    private static String getFieldValue(SimulationConfig cfg, String field) {
        switch (field) {
            case "domainWidth":
                return String.valueOf(cfg.domainWidth);
            case "domainHeight":
                return String.valueOf(cfg.domainHeight);
            case "cylinderRadius":
                return String.valueOf(cfg.cylinderRadius);
            case "cylinderX":
                return String.valueOf(cfg.cylinderX);
            case "cylinderY":
                return String.valueOf(cfg.cylinderY);
            case "inletType":
                return cfg.inletType;
            case "inletVelocity":
                return String.valueOf(cfg.inletVelocity);
            case "inletPressure":
                return String.valueOf(cfg.inletPressure);
            case "outletType":
                return cfg.outletType;
            case "outletPressure":
                return String.valueOf(cfg.outletPressure);
            case "outletVelocity":
                return String.valueOf(cfg.outletVelocity);
            case "topBoundaryType":
                return cfg.topBoundaryType;
            case "bottomBoundaryType":
                return cfg.bottomBoundaryType;
            case "cylinderWallType":
                return cfg.cylinderWallType;
            case "cylinderWallCondition":
                return cfg.cylinderWallCondition;
            case "flowType":
                return cfg.flowType;
            case "equationForm":
                return cfg.equationForm;
            case "fluidName":
                return cfg.fluidName;
            case "density":
                return String.valueOf(cfg.density);
            case "dynamicViscosity":
                return String.valueOf(cfg.dynamicViscosity);
            case "meshSizeLevel":
                return String.valueOf(cfg.meshSizeLevel);
            case "meshMaxSize":
                return String.valueOf(cfg.meshMaxSize);
            case "meshMinSize":
                return String.valueOf(cfg.meshMinSize);
            case "cylinderMeshMaxSize":
                return String.valueOf(cfg.cylinderMeshMaxSize);
            case "startTime":
                return String.valueOf(cfg.startTime);
            case "endTime":
                return String.valueOf(cfg.endTime);
            case "timeStep":
                return String.valueOf(cfg.timeStep);
            case "outputDir":
                return cfg.outputDir;
            case "modelFileName":
                return cfg.modelFileName;
            case "exportVelocity":
                return String.valueOf(cfg.exportVelocity);
            case "exportVorticity":
                return String.valueOf(cfg.exportVorticity);
            case "exportAnimation":
                return String.valueOf(cfg.exportAnimation);
            case "animationFps":
                return String.valueOf(cfg.animationFps);
            case "animationMaxFrames":
                return String.valueOf(cfg.animationMaxFrames);
            default:
                return "";
        }
    }

    /**
     * 将变更应用到配置对象
     */
    public static void applyChanges(SimulationConfig cfg, List<ConfigChange> changes) {
        for (ConfigChange change : changes) {
            setFieldValue(cfg, change.fieldName, change.newValue);
        }
    }

    /**
     * 设置配置字段值
     */
    private static void setFieldValue(SimulationConfig cfg, String field, String value) {
        try {
            switch (field) {
                case "domainWidth":
                    cfg.domainWidth = Double.parseDouble(value);
                    break;
                case "domainHeight":
                    cfg.domainHeight = Double.parseDouble(value);
                    break;
                case "cylinderRadius":
                    cfg.cylinderRadius = Double.parseDouble(value);
                    break;
                case "cylinderX":
                    cfg.cylinderX = Double.parseDouble(value);
                    break;
                case "cylinderY":
                    cfg.cylinderY = Double.parseDouble(value);
                    break;
                case "inletType":
                    cfg.inletType = value;
                    break;
                case "inletVelocity":
                    cfg.inletVelocity = Double.parseDouble(value);
                    break;
                case "inletPressure":
                    cfg.inletPressure = Double.parseDouble(value);
                    break;
                case "outletType":
                    cfg.outletType = value;
                    break;
                case "outletPressure":
                    cfg.outletPressure = Double.parseDouble(value);
                    break;
                case "outletVelocity":
                    cfg.outletVelocity = Double.parseDouble(value);
                    break;
                case "topBoundaryType":
                    cfg.topBoundaryType = value;
                    break;
                case "bottomBoundaryType":
                    cfg.bottomBoundaryType = value;
                    break;
                case "cylinderWallType":
                    cfg.cylinderWallType = value;
                    break;
                case "cylinderWallCondition":
                    cfg.cylinderWallCondition = value;
                    break;
                case "flowType":
                    cfg.flowType = value;
                    break;
                case "equationForm":
                    cfg.equationForm = value;
                    break;
                case "fluidName":
                    cfg.fluidName = value;
                    break;
                case "density":
                    cfg.density = Double.parseDouble(value);
                    break;
                case "dynamicViscosity":
                    cfg.dynamicViscosity = Double.parseDouble(value);
                    break;
                case "meshSizeLevel":
                    cfg.meshSizeLevel = Integer.parseInt(value);
                    break;
                case "meshMaxSize":
                    cfg.meshMaxSize = Double.parseDouble(value);
                    break;
                case "meshMinSize":
                    cfg.meshMinSize = Double.parseDouble(value);
                    break;
                case "cylinderMeshMaxSize":
                    cfg.cylinderMeshMaxSize = Double.parseDouble(value);
                    break;
                case "startTime":
                    cfg.startTime = Double.parseDouble(value);
                    break;
                case "endTime":
                    cfg.endTime = Double.parseDouble(value);
                    break;
                case "timeStep":
                    cfg.timeStep = Double.parseDouble(value);
                    break;
                case "outputDir":
                    cfg.outputDir = value;
                    break;
                case "modelFileName":
                    cfg.modelFileName = value;
                    break;
                case "exportVelocity":
                    cfg.exportVelocity = Boolean.parseBoolean(value);
                    break;
                case "exportVorticity":
                    cfg.exportVorticity = Boolean.parseBoolean(value);
                    break;
                case "exportAnimation":
                    cfg.exportAnimation = Boolean.parseBoolean(value);
                    break;
                case "animationFps":
                    cfg.animationFps = Integer.parseInt(value);
                    break;
                case "animationMaxFrames":
                    cfg.animationMaxFrames = Integer.parseInt(value);
                    break;
            }
        } catch (NumberFormatException e) {
            System.err.println("无法解析字段 " + field + " 的值: " + value);
        }
    }
}
