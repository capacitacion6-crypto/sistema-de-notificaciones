package com.example.ticketero.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConfigurationProperties(prefix = "telegram")
public class TelegramConfig {

    private String botToken;
    private String apiUrl = "https://api.telegram.org/bot";

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public String getBotToken() {
        return botToken;
    }

    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getFullApiUrl() {
        return apiUrl + botToken;
    }
}