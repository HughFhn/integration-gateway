package com.example.gateway.routes;

import com.example.gateway.REDCap.Config;
import org.apache.camel.builder.RouteBuilder;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Component
public class REDCapRoute extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(REDCapRoute.class);
    private Config config;

    // Grab the API Token and URL
    @Autowired
    public void REDCapAPIService(Config config) {
        this.config = config;
    }

    @Override
    public void configure() throws Exception {
        from("file:input/redcap?include=.*\\.json$")
                .routeId("redcap-import-route")
                .process(exchange -> {
                    String jsonData = exchange.getIn().getBody(String.class);
                    log.info("Processing REDCap import for file: {}", exchange.getIn().getHeader("CamelFileName"));

                    String response = callREDCapAPI(jsonData);
                    exchange.getMessage().setBody(response);
                })
                .to("file:output/redcap?fileName=${file:name.noext}-response.json")
                .log("REDCap import completed: ${file:name.noext}-response.json");
    }

    private String callREDCapAPI(String jsonData) {
        try {
            // Set the header
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("token", config.getApiToken()));
            params.add(new BasicNameValuePair("content", "record"));
            params.add(new BasicNameValuePair("format", "json"));
            params.add(new BasicNameValuePair("type", "flat"));
            params.add(new BasicNameValuePair("data", jsonData));

            var client = HttpClientBuilder.create().build();
            var post = new HttpPost(config.getApiUrl());
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

            // Display response to POST
            log.info("REDCap API response code: {}", response.getStatusLine().getStatusCode());
            return result.toString();

        } catch (Exception e) {
            log.error("Error calling REDCap API: {}", e.getMessage(), e);
            return "{\"error\":\"Failed to communicate with REDCap API\"}";
        }
    }

}
