package com.example.gateway.controller;

import com.example.gateway.converter.Hl7ToFhirConverter;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.fhir.context.FhirContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.Date;

import static org.apache.camel.component.xslt.XsltOutput.file;

@RestController
@RequestMapping("/fhir")
public class FhirController {

    private final FhirContext fhirContext = FhirContext.forR4();

    @PostMapping("/convert")
    public ResponseEntity<String> convertHl7ToFhir(@RequestBody String hl7) throws Exception {
        System.out.println("\nReceived HL7:\n" + hl7); // Debug print

        // Parse HL7 but disable validation to allow any type not just HAPI 2.6
        ca.uhn.hl7v2.parser.PipeParser parser = new ca.uhn.hl7v2.parser.PipeParser();
        parser.getParserConfiguration().setValidating(false);  // disable validation
        Message message = parser.parse(hl7);

        // Convert using centralized converter
        String fhirJson = Hl7ToFhirConverter.convert(message, fhirContext, message.getName());

        // Create directory if missing
        new File("output").mkdirs();

        String outputPath = "output/test-fhir.json";

        // Date object for audit log
        Date dateNow = new Date();

        // Write conversion output
        try (FileWriter fw = new FileWriter(outputPath)) {
            fw.write(fhirJson);
        }

        // Write audit log (append mode so you don’t overwrite)
        try (FileWriter auditWrite = new FileWriter(outputPath, true)) {
            auditWrite.write("\nConversion performed at " + dateNow);
        }

        // Print to show conversion is done
        System.out.println("\nConversion complete!");
        System.out.println("Converted HL7: "+fhirJson);

        return ResponseEntity.ok(fhirJson);
    }
}
