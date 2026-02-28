package com.srt.randomchat.bot;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OllamaClient {

    private final BotProperties properties;
    private final RestClient restClient;

    public OllamaClient(BotProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    public String generate(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return null;
        }

        OllamaGenerateRequest request = new OllamaGenerateRequest(
                properties.getModel(),
                prompt,
                false,
                new OllamaOptions(properties.getMaxTokens(), properties.getTemperature())
        );

        try {
            OllamaGenerateResponse response = restClient.post()
                    .uri("/api/generate")
                    .body(request)
                    .retrieve()
                    .body(OllamaGenerateResponse.class);
            if (response == null || response.response() == null || response.response().isBlank()) {
                return null;
            }
            return response.response().trim();
        } catch (RestClientException ex) {
            return null;
        }
    }
}
