package com.example.gateway.REDCap;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class REDCapAPI {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();
        final Logger log = LoggerFactory.getLogger(REDCapAPI.class);


        String apiUrl = dotenv.get("API_URL");
        String apiKey = dotenv.get("API_KEY");

        log.info("API URL = {}", apiUrl);
        log.info("API KEY = {}", apiKey);
    }
}
