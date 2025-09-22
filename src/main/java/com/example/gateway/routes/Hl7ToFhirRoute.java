package com.example.gateway.routes;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.hl7.HL7DataFormat;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.util.Terser;

import com.example.gateway.converter.Hl7ToFhirConverter;

@Component
public class Hl7ToFhirRoute extends RouteBuilder {

    @Override
    public void configure() {

        FhirContext fhirContext = FhirContext.forR4();
        HL7DataFormat hl7 = new HL7DataFormat();

        from("file:input?include=.*\\.hl7$")
                .unmarshal(hl7)
                .routeId("hl7-to-fhir")
                .process(exchange -> {
                    // Get the HL7 message from Camel body
                    Message hl7Message = exchange.getIn().getBody(Message.class);

                    // Use Terser to extract HL7 message type
                    Terser terser = new Terser(hl7Message);
                    String messageCode = terser.get("/MSH-9-1");   // e.g. ADT
                    String triggerEvent = terser.get("/MSH-9-2"); // e.g. A01
                    String messageType = messageCode + "_" + triggerEvent;

                    // Call the converter with detected message type
                    String fhirJson = Hl7ToFhirConverter.convert(hl7Message, fhirContext, messageType);

                    // Set FHIR JSON as Camel body
                    exchange.getMessage().setBody(fhirJson);
                })
                // Write FHIR JSON to output file
                .to("file:output?fileName=${file:name.noext}-fhir.json")
                // Log completion
                .log("HL7 -> FHIR conversion completed: ${file:name.noext}-fhir.json");
    }
}
