package com.example.gateway.REDCap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class REDCapAPIService {

    private static final Logger log = LoggerFactory.getLogger(REDCapAPIService.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final Config config;

    @Autowired
    public REDCapAPIService(Config config) {
        this.config = config;
    }


    // Retrieve records from REDCap
    public String getRecords(List<String> fields, String filterLogic) {
        try {
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("token", config.getApiToken()));
            params.add(new BasicNameValuePair("content", "record"));
            params.add(new BasicNameValuePair("format", "json"));
            params.add(new BasicNameValuePair("type", "flat"));

            if (fields != null && !fields.isEmpty()) {
                params.add(new BasicNameValuePair("fields", String.join(",", fields)));
            }
            if (filterLogic != null && !filterLogic.isEmpty()) {
                params.add(new BasicNameValuePair("filterLogic", filterLogic));
            }

            HttpClient client = HttpClientBuilder.create().build();
            HttpPost post = new HttpPost(config.getApiUrl());
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
            return result.toString();

        } catch (Exception e) {
            log.error("Error retrieving records", e);
            return null;
        }
    }

}
