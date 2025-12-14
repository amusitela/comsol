/*
 * QwenClient.java - AI API 调用封装
 * 支持 Qwen (通义千问) 优先，DeepSeek 备用
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class QwenClient {

    private static final String ENV_FILE = ".env";

    // Qwen (阿里通义千问)
    private static final String QWEN_API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private static final String QWEN_MODEL = "qwen-turbo";

    // DeepSeek (备用)
    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String DEEPSEEK_MODEL = "deepseek-chat";

    private String qwenApiKey;
    private String deepseekApiKey;
    private String activeProvider = null;

    public QwenClient() {
        // 加载 Qwen API Key
        qwenApiKey = System.getenv("QWEN_API_KEY");
        if (qwenApiKey == null || qwenApiKey.isEmpty()) {
            qwenApiKey = loadFromEnvFile("QWEN_API_KEY");
        }
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

        if (qwenApiKey != null && !qwenApiKey.isEmpty()) {
            System.out.println("[OK] Qwen API Key 已加载");
        }
        if (deepseekApiKey != null && !deepseekApiKey.isEmpty()) {
            System.out.println("[OK] DeepSeek API Key 已加载 (备用)");
        }
        if (!isConfigured()) {
            System.err.println("[警告] 未找到 QWEN_API_KEY 或 DEEPSEEK_API_KEY");
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

    public QwenClient(String apiKey) {
        this.qwenApiKey = apiKey;
    }

    public boolean isConfigured() {
        return (qwenApiKey != null && !qwenApiKey.isEmpty()) ||
                (deepseekApiKey != null && !deepseekApiKey.isEmpty());
    }

    public String chat(String userMessage, String systemPrompt) throws Exception {
        if (!isConfigured()) {
            throw new Exception("API Key 未配置。请在 .env 文件中设置 QWEN_API_KEY 或 DEEPSEEK_API_KEY");
        }

        Exception lastError = null;

        // 优先 Qwen
        if (qwenApiKey != null && !qwenApiKey.isEmpty()) {
            try {
                String result = callApi(QWEN_API_URL, QWEN_MODEL, qwenApiKey, userMessage, systemPrompt);
                if (activeProvider == null) {
                    activeProvider = "Qwen";
                    System.out.println("使用 AI: Qwen (通义千问)");
                }
                return result;
            } catch (Exception e) {
                lastError = e;
                System.out.println("Qwen 失败: " + e.getMessage());
            }
        }

        // 备用 DeepSeek
        if (deepseekApiKey != null && !deepseekApiKey.isEmpty()) {
            try {
                String result = callApi(DEEPSEEK_API_URL, DEEPSEEK_MODEL, deepseekApiKey, userMessage, systemPrompt);
                if (activeProvider == null || !activeProvider.equals("DeepSeek")) {
                    activeProvider = "DeepSeek";
                    System.out.println("使用 AI: DeepSeek (备用)");
                }
                return result;
            } catch (Exception e) {
                lastError = e;
            }
        }

        throw lastError != null ? lastError : new Exception("所有 AI 服务均不可用");
    }

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
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                    StandardCharsets.UTF_8));

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
            reader.close();

            if (code >= 200 && code < 300) {
                return extractContent(response.toString());
            } else {
                throw new Exception("HTTP " + code + ": " + response.toString());
            }
        } finally {
            conn.disconnect();
        }
    }

    private String buildRequestBody(String model, String userMessage, String systemPrompt) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":\"").append(model).append("\",\"messages\":[");
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            sb.append("{\"role\":\"system\",\"content\":").append(escapeJson(systemPrompt)).append("},");
        }
        sb.append("{\"role\":\"user\",\"content\":").append(escapeJson(userMessage)).append("}");
        sb.append("],\"temperature\":0.3,\"max_tokens\":2048}");
        return sb.toString();
    }

    private String extractContent(String json) {
        int i = json.indexOf("\"content\":");
        if (i == -1)
            return "无法解析响应";
        i = json.indexOf("\"", i + 10);
        if (i == -1)
            return "无法解析响应";
        int j = findEndQuote(json, i + 1);
        if (j == -1)
            return "无法解析响应";
        return unescapeJson(json.substring(i + 1, j));
    }

    private int findEndQuote(String s, int start) {
        boolean esc = false;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (esc) {
                esc = false;
                continue;
            }
            if (c == '\\') {
                esc = true;
                continue;
            }
            if (c == '"')
                return i;
        }
        return -1;
    }

    private String escapeJson(String s) {
        if (s == null)
            return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
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
                    sb.append(c < ' ' ? String.format("\\u%04x", (int) c) : c);
            }
        }
        return sb.append("\"").toString();
    }

    private String unescapeJson(String s) {
        if (s == null)
            return "";
        StringBuilder sb = new StringBuilder();
        boolean esc = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (esc) {
                switch (c) {
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
                        if (i + 4 < s.length()) {
                            try {
                                sb.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                                i += 4;
                            } catch (Exception e) {
                                sb.append("\\u");
                            }
                        }
                        break;
                    default:
                        sb.append(c);
                }
                esc = false;
            } else if (c == '\\')
                esc = true;
            else
                sb.append(c);
        }
        return sb.toString();
    }
}
