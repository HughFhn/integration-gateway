package com.example.gateway.controller;

import com.example.gateway.converter.Hl7ToFhirConverter;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.fhir.context.FhirContext;

@RestController
@RequestMapping("/fhir")
public class FhirController {

    private final FhirContext fhirContext = FhirContext.forR4();

    @PostMapping("/convert")
    public ResponseEntity<String> convertHl7ToFhir(@RequestBody String hl7) throws Exception {
        System.out.println("Received HL7:\n" + hl7); // Debug print

        // Parse HL7 but disable validation to allow any type not just HAPI 2.6
        ca.uhn.hl7v2.parser.PipeParser parser = new ca.uhn.hl7v2.parser.PipeParser();
        parser.getParserConfiguration().setValidating(false);  // disable validation
        Message message = parser.parse(hl7);


        // Convert using centralized converter
        String fhirJson = Hl7ToFhirConverter.convert(message, fhirContext, message.getName());

        return ResponseEntity.ok(fhirJson);
    }
}
