package com.example.gateway.controller;

import com.example.gateway.converter.FhirToHl7Converter;
import com.example.gateway.converter.Hl7ToFhirConverter;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.fhir.context.FhirContext;

import java.io.File;
import java.io.FileWriter;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fhir")
public class FhirController {

    private final FhirContext fhirContext = FhirContext.forR4();
    private Map<String, Patient> patientDatabase = new HashMap<>(); // In-memory storage for patients

    // Retrieve a specific patient by ID
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

        // Write audit log
        try (FileWriter auditWrite = new FileWriter(auditPath, true)) {
            auditWrite.write("\nConversion performed at " + dateNow + "\n");
        }

        // Print to show conversion is done
        System.out.println("\nConversion complete!");
        System.out.println("Converted HL7 to Fhir: "+fhirJson);

        return ResponseEntity.ok(fhirJson);
    }

    // Convert Fhir Json -> Hl7
    @PostMapping("/convert/fhir-to-hl7")
    public ResponseEntity<String> convertFhirToHl7(@RequestBody String fhirJson) throws Exception {
        System.out.println("\nReceived Fhir Json:\n" + fhirJson); // Debug print

        var resource = fhirContext.newJsonParser().parseResource(fhirJson);
        // Confirm that its just Patient and admission so far
        if (!(resource instanceof Patient)) {
            throw new IllegalArgumentException("Only Patient resource supported for now.\nMust implement others if needed");
        }
        // Try conversion, if not print stack trace
        String hl7Message = null;
        try {
            hl7Message = FhirToHl7Converter.convertFhirToPatient((Patient) resource);
        } catch (Exception e) {
            System.out.println("FhirToHl7Converter failed");
            e.printStackTrace();
            return ResponseEntity.status(500).body("Conversion failed: " + e.getMessage());
        }

        String outputPath = "output/test-hl7.hl7";
        // Date object for audit log
        Date dateNow = new Date();

        // Write conversion output
        try (FileWriter fw = new FileWriter(outputPath)) {
            fw.write(hl7Message);
        }

        // Write audit log (append mode so you don’t overwrite)
        try (FileWriter auditWrite = new FileWriter(auditPath, true)) {
            auditWrite.write("\nConversion performed at " + dateNow + "\n");
        }

        // Print to show conversion is done
        System.out.println("\nConversion complete!");
        System.out.println("Converted Fhir to Hl7: "+hl7Message);

        return ResponseEntity.ok(hl7Message);
    }
}
