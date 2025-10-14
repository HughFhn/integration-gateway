package com.example.gateway.REDCap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

    @Value("${redcap.api.token}")
    private String apiToken;

    @Value("${redcap.api.url}")
    private String apiUrl;

    public String getApiToken() {
        return apiToken;
    }

    public String getApiUrl() {
        return apiUrl;
    }
}
