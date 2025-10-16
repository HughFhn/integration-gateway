package com.example.gateway.routes;

import ca.uhn.fhir.context.FhirContext;
import com.example.gateway.InputValidator;
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

@Component
public class REDCapRoute extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(REDCapRoute.class);

    @Autowired
    private REDCapAPIService redcapService;

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

                    // Convert records to fhir
                    StringBuilder FHIR = new StringBuilder();
                    for (Map<String, Object> record : records) {
                        String fhirJson = REDCapToFhirConverter.convert(record, fhirContext);
                        FHIR.append(fhirJson);
                    }

                    // Validates the converter works
                    if (String.valueOf(FHIR).contains("family")) {
                        String jsonString = "\"family\": \"Doe\",\n\"given\": [ \"John\" ]";
                        int familyIndex = jsonString.indexOf("\"family\":");
                        if (familyIndex != -1) {
                            // Find the start of the family name (after the colon and quotes)
                            int startIndex = jsonString.indexOf("\"", familyIndex + 9) + 1;
                            // Find the end of the family name (next quote)
                            int endIndex = jsonString.indexOf("\"", startIndex);

                            // Extract the family name
                            String familyName = jsonString.substring(startIndex, endIndex);

                            // Print the result
                            log.debug("Family name detected: " + familyName);  // This will print: Family name detected: Doe
                        }
                    }
                    else {
                        log.warn("Invalid FHIR Json conversion.");
                    }

                    exchange.getMessage().setBody(FHIR.toString());
                })
                // Write conversion to a file in the output folder
                .to("file:output/redcap?fileName=${file:name.noext}-response.json")
                // Log completion
                .log("REDCap import completed: ${file:name.noext}-response.json");
    }
}
