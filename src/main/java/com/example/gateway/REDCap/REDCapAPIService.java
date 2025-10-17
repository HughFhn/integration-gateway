package com.example.gateway.REDCap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

@Service
public class REDCapAPIService {

    private static final Logger log = LoggerFactory.getLogger(REDCapAPIService.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final REDCapConfig REDCapConfig;

    @Autowired
    public REDCapAPIService(REDCapConfig REDCapConfig) {
        this.REDCapConfig = REDCapConfig;
    }


    // Retrieve records from REDCap
    public String getRecords(List<String> fields, String filterLogic) {
        try {
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("token", REDCapConfig.getApiToken()));
            params.add(new BasicNameValuePair("content", "record"));
            params.add(new BasicNameValuePair("format", "json"));
            params.add(new BasicNameValuePair("type", "flat"));

            if (fields != null && !fields.isEmpty()) {
                params.add(new BasicNameValuePair("fields", String.join(",", fields)));
            }
            if (filterLogic != null && !filterLogic.isEmpty()) {
                params.add(new BasicNameValuePair("filterLogic", filterLogic));
            }

            String apiUrl = REDCapConfig.getApiUrl();
            log.info("REDCap API URL: {}", apiUrl);
            if (apiUrl == null || apiUrl.isEmpty()) {
                log.error("REDCap API URL is not configured. Please check application properties.");
                return null;
            }

            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            String trustStorePath = "src/main/resources/ssl/redcap-truststore.jks";
            log.info("Truststore path: {}", trustStorePath);

            try (FileInputStream fis = new FileInputStream(trustStorePath)) {
                trustStore.load(fis, "changeit".toCharArray());
                log.info("Truststore loaded successfully.");
            } catch (FileNotFoundException e) {
                log.error("Truststore file not found at {}. Please ensure the file exists.", trustStorePath, e);
                return null;
            } catch (IOException e) {
                log.error("Error loading truststore from {}. Please check the file and password.", trustStorePath, e);
                return null;
            }

            SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                    .loadTrustMaterial(trustStore, null)
                    .build();

            SSLConnectionSocketFactory sslFactory = new SSLConnectionSocketFactory(sslContext);

            CloseableHttpClient client = HttpClientBuilder.create()
                    .setSSLSocketFactory(sslFactory)
                    .build();

            HttpPost post = new HttpPost(REDCapConfig.getApiUrl());
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");
            post.setEntity(new UrlEncodedFormEntity(params));

            HttpResponse response = client.execute(post);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent())
            );

            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }

            log.info("Retrieved Records from REDCap");
            System.out.println(result);
            return result.toString();

        } catch (Exception e) {
            log.error("Error retrieving records", e);
            return null;
        }
    }

}
