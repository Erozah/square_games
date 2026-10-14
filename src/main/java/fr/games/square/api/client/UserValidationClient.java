package fr.games.square.api.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
public class UserValidationClient {

    private static final Logger log = LoggerFactory.getLogger(UserValidationClient.class);
    private final RestClient restClient;

    public UserValidationClient(@Value("${users.services.url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public boolean isValidUser(UUID userId) {
        try {
            Boolean isValid = restClient.get()
                    .uri("/users/{id}/valid", userId)
                    .retrieve()
                    .body(Boolean.class);
            return Boolean.TRUE.equals(isValid);
        } catch (Exception e) {
            log.warn("Failed to validate user with id {}: {}", userId, e.getMessage());
            return false;
        }
    }

    public String login(String username, String password) {
        try {
            Map<String, String> body = Map.of("username", username, "password", password);
            Map<?, ?> response = restClient.post()
                    .uri("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (response != null) {
                Object token = response.get("token");
                if (token == null) {
                    token = response.get("Token");
                }
                return token != null ? token.toString() : null;
            }
        } catch (Exception e) {
            log.warn("Authentication failed for user {}: {}", username, e.getMessage());
            return null;
        }
        return null;
    }

    public Map<String, Object> register(String username, String email, String password) {
        try {
            Map<String, String> body = Map.of(
                    "username", username,
                    "email", email,
                    "password", password
            );
            @SuppressWarnings("unchecked")
            Map<String, Object> result = restClient.post()
                    .uri("/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            return result;
        } catch (Exception e) {
            log.warn("Failed to register user {} ({}): {}", username, email, e.getMessage());
            return null;
        }
    }

    public record UserInfo(UUID id, String username, String email) {}

    public UserInfo findUser(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }
        String trimmed = identifier.trim();
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/users/search")
                            .queryParam("q", trimmed)
                            .build())
                    .retrieve()
                    .body(UserInfo.class);
        } catch (Exception e) {
            log.warn("Failed to find user with query '{}': {}", trimmed, e.getMessage());
            return null;
        }
    }
}
