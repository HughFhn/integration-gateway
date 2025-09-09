package com.example.gateway.routes;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.hl7.HL7DataFormat;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.hl7v2.model.Message;

@Component
public class Hl7ToFhirRoute extends RouteBuilder {

    @Override
    public void configure() {

        FhirContext fhirContext = FhirContext.forR4();

        // Optional: HL7 v2 data format for Camel
        HL7DataFormat hl7 = new HL7DataFormat();

        from("file:input?noop=true&include=.*\\.hl7$") // read HL7 files
                .routeId("hl7-to-fhir")
                .unmarshal(hl7) // parse HL7 v2
                .process(exchange -> {
                    Message hl7Message = exchange.getIn().getBody(Message.class);

                    // Minimal example: convert HL7 → FHIR Patient
                    Patient patient = new Patient();
                    patient.addName().setFamily("Doe").addGiven("John"); // map fields as needed

                    // Convert FHIR Patient to JSON
                    String fhirJson = fhirContext.newJsonParser().encodeResourceToString(patient);

                    // Put JSON into the exchange body
                    exchange.getMessage().setBody(fhirJson);
                })
                .to("file:output?fileName=${file:name.noext}-fhir.json") // save FHIR JSON
                .log("HL7 -> FHIR conversion completed: ${file:name.noext}-fhir.json");
    }
}
