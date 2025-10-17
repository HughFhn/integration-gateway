package com.example.gateway.routes;

import ca.uhn.fhir.context.FhirContext;
import com.example.gateway.REDCap.REDCapAPIService;
import com.example.gateway.converter.REDCapToFhirConverter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import org.apache.camel.ProducerTemplate;

@Component
public class REDCapRoute extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(REDCapRoute.class);

    @Autowired
    private REDCapAPIService redcapService;

    @Autowired
    private ProducerTemplate template;

    private final FhirContext fhirContext = FhirContext.forR4();
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void configure() {

        from("timer://fetchRedcap?period=60000") // every 60 seconds it pulls
                .routeId("redcap-fetch-and-convert")
                .process(exchange -> {
                    log.info("Fetching records from REDCap...");

                    // Get REDCap JSON from API
                    String redcapJson = redcapService.getRecords(null, null); // Nulls as it then wont filter on certain fields
                    if (redcapJson == null && redcapJson.isEmpty()) {
                        log.warn("REDCap Records Not Found!!");
                        return;
                    }

                    // Parse into Json records
                    List<Map<String, Object>> records = mapper.readValue(redcapJson, new TypeReference<>() {});

                    // Convert records to fhir and write each to a separate file
                    for (Map<String, Object> record : records) {
                        String fhirJson = REDCapToFhirConverter.convert(record, fhirContext);
                        // Use a unique file name for each patient, e.g., based on record ID or a generated UUID
                        String fileName = "redcap-patient-" + record.get("study_id") + ".json";
                        template.sendBodyAndHeader("file:output/redcap", fhirJson, "CamelFileName", fileName);
                    }

                    exchange.getMessage().setBody("REDCap records processed and saved as individual FHIR JSON files.");
                })
                // Log completion
                .log("REDCap import completed: Individual FHIR JSON files created.");
    }
}
