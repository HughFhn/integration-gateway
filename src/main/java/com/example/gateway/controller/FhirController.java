package com.example.gateway.controller;

import com.example.gateway.Hl7ParserUtil;
import com.example.gateway.converter.FhirToHl7Converter;
import com.example.gateway.converter.Hl7ToFhirConverter;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.hl7v2.model.Message;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fhir")
public class FhirController {

    private static final Logger log = LoggerFactory.getLogger(FhirController.class);

    private final FhirContext fhirContext = FhirContext.forR4();
    private final Map<String, Patient> patientDatabase = new HashMap<>();

    @GetMapping("/patient/{id}")
    public ResponseEntity<Patient> getPatient(@PathVariable String id) {
        Patient patient = patientDatabase.get(id);
        if (patient == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(patient);
    }

    // Create a new patient
    @PostMapping("/patient")
    public ResponseEntity<Patient> createPatient(@RequestBody Patient patient) {
        String patientId = String.valueOf(patientDatabase.size() + 1);
        patient.setId(patientId);
        patientDatabase.put(patientId, patient);
        return ResponseEntity.status(201).body(patient);
    }
    String auditPath = "output/audit.log";

    // Convert Hl7 to Fhir Json
    @PostMapping("/convert/hl7-to-fhir")
    public ResponseEntity<String> convertHl7ToFhir(@RequestBody String hl7) {
        System.out.println("\nReceived HL7:\n" + hl7); // Debug print

        try {
            // Use Hl7 util to parse message
            Message message = Hl7ParserUtil.parseHL7(hl7);

            // now safely convert to FHIR
            String fhirJson = Hl7ToFhirConverter.convert(message, fhirContext, message.getName());

            // Create directory if missing
            new File("output").mkdirs();

            // Save converted JSON
            try (FileWriter fw = new FileWriter("output/test-fhir.json")) {
                fw.write(fhirJson);
            }

            // Append audit log
            try (FileWriter auditWriter = new FileWriter(auditPath, true)) {
                auditWriter.write("HL7→FHIR conversion performed at " + new Date() + "\n");
            }

            log.info("Conversion complete! FHIR JSON output saved.");
            return ResponseEntity.ok(fhirJson);
        }
         catch (Exception e) {
            log.error("Failed to convert HL7 to FHIR: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"error\":\"Failed to parse HL7: " + e.getMessage() + "\"}");
        }
    }

    // Convert Fhir Json -> Hl7
    @PostMapping("/convert/fhir-to-hl7")
    public ResponseEntity<String> convertFhirToHl7(@RequestBody String fhirJson) {
        System.out.println("\nReceived Fhir Json:\n" + fhirJson); // Debug print

        try {
            var resource = fhirContext.newJsonParser().parseResource(fhirJson);
            Patient patient = getPatient(resource);

            // Perform conversion
            String hl7Message = FhirToHl7Converter.convertFhirToPatient(patient);

        String outputPath = "output/test-hl7.hl7";
        // Date object for audit log
        Date dateNow = new Date();

        // Write conversion output
        try (FileWriter fw = new FileWriter(outputPath)) {
            fw.write(hl7Message);
        }

        // Write audit log (append mode so you don’t overwrite)
        try (FileWriter auditWrite = new FileWriter(auditPath, true)) {
            auditWrite.write("FHIR→HL7 conversion performed at " + dateNow + "\n");
        }

            log.info("Conversion complete! HL7 message saved.");
            return ResponseEntity.ok(hl7Message);

        } catch (Exception e) {
            log.error("Failed to convert FHIR to HL7: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"Conversion failed: " + e.getMessage() + "\"}");
        }
    }

    private static Patient getPatient(IBaseResource resource) {
        Patient patient = null;

        // Loop and if to see if patient is present either inside bundle or as a raw text
        if (resource instanceof Patient) {
            // Raw Patient resource
            patient = (Patient) resource;
        } else if (resource instanceof Bundle bundle) {
            // Bundle with Patient inside
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.getResource() instanceof Patient) {
                    patient = (Patient) entry.getResource();
                    break;
                }
            }
        }

        if (patient == null) {
            throw new IllegalArgumentException("FHIR input must be either a Patient resource or a Bundle containing a Patient");
        }
        return patient;
    }
}
