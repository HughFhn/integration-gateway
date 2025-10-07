package com.example.gateway.token;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class RequestToken {

    private static final String LOGIN_URL = "http://localhost:8081/auth/login";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "password";
    private static final String TOKEN_FILE = "output/token.txt";
    private static final Logger log = LogManager.getLogger(RequestToken.class);

    private static String cachedToken = null;

    public static String getToken() throws IOException {

        if (cachedToken != null) {
            return cachedToken;
        }

        HttpURLConnection con = getHttpURLConnection();

        try(BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }

            String token = response.toString();

            // crude extraction (assuming JSON shape above)
            // Just writes token
            if (token.contains(":")) {
                token = token.substring(token.indexOf(":") + 20, token.length() - 2);
            }

            // cache in memory
            cachedToken = token;

            // save to file
            File file = new File(TOKEN_FILE);
            file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(token);
            }
            return token;
        }
    }

    // Method to send POST request
    private static HttpURLConnection getHttpURLConnection() throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(LOGIN_URL).openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        String jsonInputString = "{\"username\": \"" + USERNAME + "\", \"password\": \"" + PASSWORD + "\"}";
        try(OutputStream os = con.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        return con;
    }
}
