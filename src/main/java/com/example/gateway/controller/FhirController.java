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
        // Parse HL7
        Message message = new PipeParser().parse(hl7);

        // Convert using centralized converter
        String fhirJson = Hl7ToFhirConverter.convert(message, fhirContext, message.getName());

        return ResponseEntity.ok(fhirJson);
    }
}
