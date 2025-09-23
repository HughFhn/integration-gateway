package com.example.gateway.routes;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.hl7.HL7DataFormat;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.FhirContext;
import com.example.gateway.converter.FhirToHl7Converter;

@Component
public class FhirToHl7Route extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        FhirContext fhirContext = FhirContext.forR4();
        HL7DataFormat hl7 = new HL7DataFormat();

        from("file:input?include=.*\\.json$")       // Input FHIR JSON files
                .routeId("fhir-to-hl7")
                .process(exchange -> {
                    String fhirJson = exchange.getIn().getBody(String.class);

                    // Parse JSON to FHIR Patient resource
                    org.hl7.fhir.r4.model.Patient patient =
                            fhirContext.newJsonParser().parseResource(org.hl7.fhir.r4.model.Patient.class, fhirJson);

                    // Convert FHIR -> HL7
                    String hl7Message = FhirToHl7Converter.convertFhirToPatient(patient);

                    // Set HL7 message as body
                    exchange.getMessage().setBody(hl7Message);
                })
                // Marshal to HL7 pipe format
                .marshal(hl7)
                // Write HL7 message to output folder
                .to("file:output?fileName=${file:name.noext}-hl7.hl7")
                // Log completion
                .log("FHIR -> HL7 conversion completed: ${file:name.noext}-hl7.hl7");
    }
}
