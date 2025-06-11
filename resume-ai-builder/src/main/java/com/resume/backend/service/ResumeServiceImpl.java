package com.resume.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResumeServiceImpl implements ResumeService {

    private static final Logger logger = LoggerFactory.getLogger(ResumeServiceImpl.class);

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqApiUrl;

    @Value("${groq.model:llama3-70b-8192}")
    private String groqModel;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    public ResumeServiceImpl(RestTemplate restTemplate, ObjectMapper objectMapper, ResourceLoader resourceLoader) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() {
        logger.info("=== GROQ CONFIGURATION DEBUG ===");
        logger.info("Groq API Key: {}", groqApiKey != null ?
                (groqApiKey.startsWith("gsk_") ? "✅ Valid format (length: " + groqApiKey.length() + ")" : "❌ Invalid format") : "❌ NULL");
        logger.info("Groq Model: {}", groqModel);
        logger.info("Groq URL: {}", groqApiUrl);
        logger.info("===============================");
    }

    @Override
    public Map<String, Object> generateResumeResponse(String userResumeDescription) throws IOException {
        logger.info("🚀 Starting resume generation for user description");

        String promptString = this.loadPromptFromFile("resume_prompt.txt");
        String promptContent = this.putValuesToTemplate(promptString, Map.of(
                "userDescription", userResumeDescription));

        String response = callGroqAPI(promptContent);
        Map<String, Object> stringObjectMap = parseMultipleResponses(response);

        logger.info("✅ Resume generation completed successfully");
        return stringObjectMap;
    }

    private String callGroqAPI(String prompt) throws IOException {
        logger.info("🔍 DEBUG: Starting API call");
        logger.info("🔍 API Key present: {}", groqApiKey != null && !groqApiKey.trim().isEmpty());
        logger.info("🔍 API Key length: {}", groqApiKey != null ? groqApiKey.length() : "NULL");
        logger.info("🔍 Using model: {}", groqModel);
        logger.info("🔍 Using URL: {}", groqApiUrl);

        // Create request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        logger.info("🔍 Headers created: {}", headers.keySet());
        logger.info("🔍 Authorization header: {}", headers.getFirst("Authorization") != null ?
                "Bearer " + (headers.getFirst("Authorization").startsWith("Bearer ") ? "✅ Present" : "❌ Wrong format") : "❌ Missing");

        // Create request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", groqModel);
        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)));
        requestBody.put("max_tokens", 4000);
        requestBody.put("temperature", 0.7);

        logger.info("🔍 Request body model: {}", requestBody.get("model"));
        logger.info("🔍 Request body keys: {}", requestBody.keySet());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            logger.info("📤 Making API call to: {}", groqApiUrl);

            ResponseEntity<String> response = restTemplate.exchange(
                    groqApiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class);

            logger.info("📥 API Response Status: {}", response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("✅ API call successful");

                // Parse the response to extract the content
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                String content = jsonResponse.path("choices").get(0).path("message").path("content").asText();

                logger.info("✅ Content extracted, length: {}", content.length());
                return content;
            } else {
                logger.error("❌ API call failed with status: {}", response.getStatusCode());
                logger.error("❌ Response body: {}", response.getBody());
                throw new RuntimeException("Groq API call failed with status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("💥 Error calling Groq API: {}", e.getMessage());
            logger.error("💥 Exception type: {}", e.getClass().getSimpleName());

            // Log more details about HTTP client errors
            if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                org.springframework.web.client.HttpClientErrorException httpError =
                        (org.springframework.web.client.HttpClientErrorException) e;
                logger.error("💥 HTTP Status: {}", httpError.getStatusCode());
                logger.error("💥 Response Body: {}", httpError.getResponseBodyAsString());
                logger.error("💥 Status Text: {}", httpError.getStatusText());
            }

            throw new IOException("Error calling Groq API: " + e.getMessage(), e);
        }
    }

    String loadPromptFromFile(String filename) throws IOException {
        logger.debug("📁 Loading prompt from file: {}", filename);

        Resource resource = resourceLoader.getResource("classpath:" + filename);
        try (InputStream inputStream = resource.getInputStream()) {
            String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            logger.debug("📁 Prompt loaded, length: {}", content.length());
            return content;
        }
    }

    String putValuesToTemplate(String template, Map<String, String> values) {
        logger.debug("🔧 Replacing template values: {}", values.keySet());

        for (Map.Entry<String, String> entry : values.entrySet()) {
            template = template.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return template;
    }

    public static Map<String, Object> parseMultipleResponses(String response) {
        Map<String, Object> jsonResponse = new HashMap<>();

        // Extract content inside <think> tags
        int thinkStart = response.indexOf("<think>") + 7;
        int thinkEnd = response.indexOf("</think>");
        if (thinkStart != -1 && thinkEnd != -1) {
            String thinkContent = response.substring(thinkStart, thinkEnd).trim();
            jsonResponse.put("think", thinkContent);
        } else {
            jsonResponse.put("think", null); // Handle missing <think> tags
        }

        // Extract content that is in JSON format
        int jsonStart = response.indexOf("```json") + 7; // Start after ```json
        int jsonEnd = response.lastIndexOf("```"); // End before ```
        if (jsonStart != -1 && jsonEnd != -1 && jsonStart < jsonEnd) {
            String jsonContent = response.substring(jsonStart, jsonEnd).trim();
            try {
                // Convert JSON string to Map using Jackson ObjectMapper
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> dataContent = objectMapper.readValue(jsonContent, Map.class);
                jsonResponse.put("data", dataContent);
            } catch (Exception e) {
                jsonResponse.put("data", null); // Handle invalid JSON
                System.err.println("Invalid JSON format in the response: " + e.getMessage());
            }
        } else {
            jsonResponse.put("data", null); // Handle missing JSON
        }

        return jsonResponse;
    }
}