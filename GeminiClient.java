/*
 * GeminiClient.java - AI API 调用封装
 * 支持多个 AI 服务商：Qwen (通义千问) 优先，DeepSeek 备用
 * 从环境变量或 .env 文件读取 API Key
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GeminiClient {

    // API 配置
    private static final String ENV_FILE = ".env";

    // Qwen (阿里通义千问) - OpenAI 兼容模式
    private static final String QWEN_API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private static final String QWEN_MODEL = "qwen-turbo"; // 也可用 qwen-plus, qwen-max

    // DeepSeek (备用)
    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String DEEPSEEK_MODEL = "deepseek-chat";

    private String qwenApiKey;
    private String deepseekApiKey;
    private String activeProvider = null; // 当前成功的提供商

    public GeminiClient() {
        // 加载 Qwen API Key
        qwenApiKey = System.getenv("QWEN_API_KEY");
        if (qwenApiKey == null || qwenApiKey.isEmpty()) {
            qwenApiKey = loadFromEnvFile("QWEN_API_KEY");
        }
        // 兼容 DASHSCOPE_API_KEY
        if (qwenApiKey == null || qwenApiKey.isEmpty()) {
            qwenApiKey = System.getenv("DASHSCOPE_API_KEY");
        }
        if (qwenApiKey == null || qwenApiKey.isEmpty()) {
            qwenApiKey = loadFromEnvFile("DASHSCOPE_API_KEY");
        }

        // 加载 DeepSeek API Key
        deepseekApiKey = System.getenv("DEEPSEEK_API_KEY");
        if (deepseekApiKey == null || deepseekApiKey.isEmpty()) {
            deepseekApiKey = loadFromEnvFile("DEEPSEEK_API_KEY");
        }

        // 状态输出
        if (qwenApiKey != null && !qwenApiKey.isEmpty()) {
            System.out.println("✓ Qwen API Key 已加载");
        }
        if (deepseekApiKey != null && !deepseekApiKey.isEmpty()) {
            System.out.println("✓ DeepSeek API Key 已加载 (备用)");
        }
        if (!isConfigured()) {
            System.err.println("警告: 未找到任何 API Key (QWEN_API_KEY 或 DEEPSEEK_API_KEY)");
        }
    }

    private String loadFromEnvFile(String keyName) {
        String[] possiblePaths = {
                ENV_FILE,
                System.getProperty("user.dir") + "/" + ENV_FILE,
                System.getProperty("user.home") + "/" + ENV_FILE
        };

        for (String path : possiblePaths) {
            File envFile = new File(path);
            if (envFile.exists() && envFile.isFile()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#"))
                            continue;

                        if (line.startsWith(keyName + "=")) {
                            String value = line.substring(keyName.length() + 1).trim();
                            if ((value.startsWith("\"") && value.endsWith("\"")) ||
                                    (value.startsWith("'") && value.endsWith("'"))) {
                                value = value.substring(1, value.length() - 1);
                            }
                            return value;
                        }
                    }
                } catch (Exception e) {
                    /* ignore */ }
            }
        }
        return null;
    }

    public GeminiClient(String apiKey) {
        this.qwenApiKey = apiKey;
    }

    public boolean isConfigured() {
        return (qwenApiKey != null && !qwenApiKey.isEmpty()) ||
                (deepseekApiKey != null && !deepseekApiKey.isEmpty());
    }

    /**
     * 发送消息到 AI API 并获取响应
     * 优先使用 Qwen，失败则降级到 DeepSeek
     */
    public String chat(String userMessage, String systemPrompt) throws Exception {
        if (!isConfigured()) {
            throw new Exception("API Key 未配置。请在 .env 文件中设置 QWEN_API_KEY 或 DEEPSEEK_API_KEY");
        }

        Exception lastError = null;

        // 1. 优先尝试 Qwen
        if (qwenApiKey != null && !qwenApiKey.isEmpty()) {
            try {
                String result = callApi(QWEN_API_URL, QWEN_MODEL, qwenApiKey, userMessage, systemPrompt);
                if (activeProvider == null) {
                    activeProvider = "Qwen";
                    System.out.println("使用 AI 服务: Qwen (通义千问)");
                }
                return result;
            } catch (Exception e) {
                lastError = e;
                System.out.println("Qwen 请求失败: " + e.getMessage());
                System.out.println("尝试使用 DeepSeek 备用...");
            }
        }

        // 2. 降级到 DeepSeek
        if (deepseekApiKey != null && !deepseekApiKey.isEmpty()) {
            try {
                String result = callApi(DEEPSEEK_API_URL, DEEPSEEK_MODEL, deepseekApiKey, userMessage, systemPrompt);
                if (activeProvider == null || !activeProvider.equals("DeepSeek")) {
                    activeProvider = "DeepSeek";
                    System.out.println("使用 AI 服务: DeepSeek (备用)");
                }
                return result;
            } catch (Exception e) {
                lastError = e;
                System.out.println("DeepSeek 请求也失败: " + e.getMessage());
            }
        }

        throw lastError != null ? lastError : new Exception("所有 AI 服务均不可用");
    }

    /**
     * 调用指定的 AI API
     */
    private String callApi(String apiUrl, String model, String apiKey,
            String userMessage, String systemPrompt) throws Exception {
        String requestBody = buildRequestBody(model, userMessage, systemPrompt);

        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(120000);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();

            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
            reader.close();

            if (responseCode >= 200 && responseCode < 300) {
                return extractContent(response.toString());
            } else {
                throw new Exception("HTTP " + responseCode + ": " + response.toString());
            }

        } finally {
            conn.disconnect();
        }
    }

    private String buildRequestBody(String model, String userMessage, String systemPrompt) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"model\": \"").append(model).append("\",\n");
        sb.append("  \"messages\": [\n");

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            sb.append("    {\"role\": \"system\", \"content\": ").append(escapeJson(systemPrompt)).append("},\n");
        }

        sb.append("    {\"role\": \"user\", \"content\": ").append(escapeJson(userMessage)).append("}\n");
        sb.append("  ],\n");
        sb.append("  \"temperature\": 0.3,\n");
        sb.append("  \"max_tokens\": 2048\n");
        sb.append("}");

        return sb.toString();
    }

    private String extractContent(String jsonResponse) {
        int contentStart = jsonResponse.indexOf("\"content\":");
        if (contentStart == -1)
            return "无法解析 API 响应";

        contentStart = jsonResponse.indexOf("\"", contentStart + 10);
        if (contentStart == -1)
            return "无法解析 API 响应";

        int contentEnd = findEndOfJsonString(jsonResponse, contentStart + 1);
        if (contentEnd == -1)
            return "无法解析 API 响应";

        return unescapeJson(jsonResponse.substring(contentStart + 1, contentEnd));
    }

    private int findEndOfJsonString(String json, int start) {
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"')
                return i;
        }
        return -1;
    }

    private String escapeJson(String text) {
        if (text == null)
            return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < ' ')
                        sb.append(String.format("\\u%04x", (int) c));
                    else
                        sb.append(c);
            }
        }
        return sb.append("\"").toString();
    }

    private String unescapeJson(String text) {
        if (text == null)
            return "";
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaped) {
                switch (c) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        if (i + 4 < text.length()) {
                            try {
                                sb.append((char) Integer.parseInt(text.substring(i + 1, i + 5), 16));
                                i += 4;
                            } catch (Exception e) {
                                sb.append("\\u");
                            }
                        }
                        break;
                    default:
                        sb.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
