package eecs3311.studyagent;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** OpenAI-compatible chat-completions adapter; secrets remain in local .env or process env. */
public final class HttpLlmClient implements LlmClient {
    private final String key;
    private final URI endpoint;
    private final String model;

    public HttpLlmClient(Path projectRoot) throws IOException {
        Properties configuration = new Properties();
        Path env = projectRoot.resolve(".env");
        if (Files.isRegularFile(env)) {
            for (String line : Files.readAllLines(env)) {
                String trimmed = line.trim();
                int equal = trimmed.indexOf('=');
                if (!trimmed.isBlank() && !trimmed.startsWith("#") && equal > 0)
                    configuration.setProperty(trimmed.substring(0, equal).trim(), trimmed.substring(equal + 1).trim());
            }
        }
        key = setting(configuration, "LLM_API_KEY", "");
        endpoint = URI.create(setting(configuration, "LLM_BASE_URL", "https://api.openai.com/v1/chat/completions"));
        model = setting(configuration, "LLM_MODEL", "gpt-6-luna");
        if (!"https".equalsIgnoreCase(endpoint.getScheme())) throw new IllegalArgumentException("LLM endpoint must use HTTPS");
    }

    private static String setting(Properties p, String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? p.getProperty(name, fallback) : value;
    }

    @Override public String complete(String systemPrompt, String userPrompt) throws IOException {
        return request(systemPrompt, userPrompt);
    }

    @Override public boolean supportsImages() { return true; }

    @Override public String completeWithImages(String systemPrompt, String userPrompt, List<String> dataUrls) throws IOException {
        List<Map<String, Object>> content = new java.util.ArrayList<>();
        content.add(Map.of("type", "text", "text", userPrompt));
        for (String url : dataUrls) content.add(Map.of("type", "image_url", "image_url", Map.of("url", url, "detail", "high")));
        return request(systemPrompt, content);
    }

    private String request(String systemPrompt, Object userContent) throws IOException {
        if (key.isBlank()) throw new IllegalStateException("LLM_API_KEY is blank. Copy .env.example to .env and add your key.");
        String body = Json.write(Map.of("model", model,
                "reasoning_effort", "none", "response_format", Map.of("type", "json_object"),
                "messages", List.of(Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userContent))));
        HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(90))
                .header("Authorization", "Bearer " + key).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        try {
            HttpResponse<String> response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
                    .build().send(request, HttpResponse.BodyHandlers.ofString());
            return responseContent(response.statusCode(), response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("LLM request interrupted", ex);
        }
    }

    static String responseContent(int status, String body) throws IOException {
            if (status / 100 != 2) throw new IOException(switch (status) {
                case 401, 403 -> "OpenAI rejected the API credentials (HTTP " + status + "). Check the local .env key and model access.";
                case 429 -> "OpenAI quota or rate limit reached (HTTP 429). Check API billing/credits, or wait and retry.";
                default -> "AI service request failed (HTTP " + status + "). Retry when the service is available.";
            });
        try {
            Map<String, Object> root = Json.object(Json.parse(body));
            Map<String, Object> choice = Json.object(Json.array(root.get("choices")).get(0));
            if ("length".equals(choice.get("finish_reason")))
                throw new IOException("LLM response was truncated; retry with less source text.");
            String content = Json.string(Json.object(choice.get("message")), "content");
            if (content.isBlank()) throw new IOException("LLM returned an empty JSON response.");
            return content;
        } catch (RuntimeException ex) {
            throw new IOException("The AI service returned an invalid response. Nothing was saved. Retry the action.", ex);
        }
    }
}
