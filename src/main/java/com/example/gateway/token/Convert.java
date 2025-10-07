package com.example.gateway.token;

import com.example.gateway.utils.SslUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Convert {

    // URLs for conversion
    private static final String FhirToHl7 = "http://localhost:8081/fhir/convert/fhir-to-hl7";
    private static final String Hl7ToFhir = "http://localhost:8081/fhir/convert/hl7-to-fhir";

    // Files containing input for formats
    private static String FhirInput;
    static {
        try {
            FhirInput = Files.readString(Path.of("input/.camel/test.json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String Hl7Input;
    static {
        try {
            Hl7Input = Files.readString(Path.of("input/.camel/test.hl7"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Only requires link and input
    public static HttpURLConnection config(String url, String inputFormat) throws IOException {

        // Retrieves content type depending on Url
        String contentType;
        if (url.equals(FhirToHl7)) {
            contentType = "application/json";
        } else if (url.equals(Hl7ToFhir)) {
            contentType = "text/plain";
        } else {
            throw new IllegalArgumentException("Unknown URL for content type mapping");
        }
        // Retrieve token from Request token class
        String token = RequestToken.getToken();

        // POST to convert
        return getConnection(url, inputFormat, contentType, token);
    }

    // Called from configFunc
    private static HttpURLConnection getConnection(String url, String inputFormat, String contentType, String token) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", contentType);
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("Authorization", "Bearer " + token);
        con.setDoOutput(true);

        try(OutputStream os = con.getOutputStream()) {
            byte[] input = inputFormat.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        return con;
    }

    // Method to print response
    private static void printResponse(HttpURLConnection con) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line).append("\n");
            }
            // print response
            System.out.println(response.toString());
        } catch (IOException e) {
            System.err.println("Error reading response: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {

        //SslUtil.disableCertificateValidation(); // Trusts any no matter the cert/address
        // Make links https and update application.properties to be secure // *ONLY FOR TESTING REMOVE FOR REAL DEPLOY*

        // Get token then use to post HL7 request
        System.out.println("=".repeat(20));
        System.out.println("HL7 To Fhir");
        System.out.println("=".repeat(20));
        // Store response
        HttpURLConnection hl7Response = (config(Hl7ToFhir, Hl7Input));
        printResponse(hl7Response);

        // Same with Fhir to HL7
        System.out.println("=".repeat(20));
        System.out.println("Fhir To HL7");
        System.out.println("=".repeat(20));
        // Store response
        HttpURLConnection fhirResponse = (config(FhirToHl7, FhirInput));
        printResponse(fhirResponse);
    }
}
