package com.docmind.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();
    private String uploadDir = "./data/uploads";
    private String vectorStorePath = "./data/vector-store.json";

    @Data
    public static class Jwt {
        private String secret;
        private long expirationMs = 604800000L;
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:5173");
    }
}
