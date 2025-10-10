package com.example.gateway.controller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import ca.uhn.fhir.validation.ValidationResult;
import com.example.gateway.InputValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.gateway.converter.FhirToHl7Converter;
import com.example.gateway.converter.Hl7ToFhirConverter;
import com.example.gateway.utils.Hl7ParserUtil;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.hl7v2.model.Message;

import static org.apache.jena.vocabulary.SchemaDO.event;

@RestController
@RequestMapping("/fhir")
@CrossOrigin(origins = "http://localhost:3000") // allow React dev server
public class FhirController {

    @Autowired
    private InputValidator validator;

    private static final Logger log = LoggerFactory.getLogger(FhirController.class);
    private final List<SseEmitter>  emitters = new CopyOnWriteArrayList<>();

    private final FhirContext fhirContext = FhirContext.forR4();
    private final Map<String, Patient> patientDatabase = new HashMap<>();

    private int totalConversions = 0;
    private int successCount = 0;
    private int HL7Count = 0;
    private int FHIRCount = 0;

    public FhirController() {
    }

    @GetMapping("/patient/{id}")
    public ResponseEntity<Patient> getPatient(@PathVariable String id) {
        Patient patient = patientDatabase.get(id);
        if (patient == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(patient);
    }

    @GetMapping("audit/stream")
    public SseEmitter getAuditStream() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);

        emitter.onTimeout(() -> {
                log.info("SSE emitter timed out");
                emitters.remove(emitter);
        });
        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.info("SSE emitter completed");
        });
        emitter.onError(e -> log.warn("SSE error: {}", e.getMessage()));

        return emitter;
    }

    public void broadcastAudit(Date date, String type, String status, String user, long latencyTime) {
        Map<String, String> broadcastMap = Map.of(
                "Date", String.valueOf(date),
                "Type", type,
                "Status", status,
                "User", user,
                "Latency", String.valueOf(latencyTime)
        );

        List<SseEmitter> deadEmitters = new ArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("audit")
                        .data(broadcastMap)); // include actual data in the event
            } catch (IOException e) {
                log.warn("Client disconnected while sending SSE event: {}", e.getMessage());
                emitter.complete();
                deadEmitters.add(emitter); // mark for cleanup
            } catch (Exception e) {
                log.error("Unexpected SSE error: {}", e.getMessage());
                emitter.completeWithError(e);
                deadEmitters.add(emitter);
            }
        }
        // Debug log
        log.info("Broadcast sent to {} active clients", emitters.size());
        // Remove all disconnected emitters to avoid future errors
        emitters.removeAll(deadEmitters);

        if (type.startsWith("HL7")) {
            HL7Count++;
        }
        else if(type.startsWith("FHIR")) {
            FHIRCount++;
        }
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

    // Convert Hl7 to FHIR Json
    @PostMapping("/convert/hl7-to-fhir")
    public ResponseEntity<String> convertHl7ToFhir(@RequestBody String hl7) {
        long startTime = System.currentTimeMillis(); // Start latency timer
        System.out.println("\nReceived HL7:\n" + hl7); // Debug print

        // Validate
        InputValidator.ValidationResult validationResult = validator.validateHl7Input(hl7);
        if (!validationResult.isValid()) {
            log.error("HL7 validation failed: {}", validationResult.getErrorMessage());
            long latency = System.currentTimeMillis() - startTime;
            broadcastAudit(new Date(), "HL7 -> FHIR", "Failure", "ADMIN", latency);
            recordConversion(false);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "Validation failed",
                            "details", validationResult.getErrors()
                    ).toString());
        }

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
            // Send success request to website
            long latency = System.currentTimeMillis() - startTime;
            broadcastAudit(new Date(),"HL7 -> FHIR", "Success", "ADMIN", latency);

            // record conversion success
            recordConversion(true);

            log.info("Conversion complete! FHIR JSON output saved.");
            return ResponseEntity.ok(fhirJson);
        } catch (Exception e) {

            // Send failure request to website
            long latency = System.currentTimeMillis() - startTime;
            broadcastAudit(new Date(),"HL7 -> FHIR", "Failure", "ADMIN", latency);

            // record conversion success
            recordConversion(false);

            log.error("Failed to convert HL7 to FHIR: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"error\":\"Failed to parse HL7: " + e.getMessage() + "\"}");
        }
    }

    // Convert FHIR Json -> Hl7
    @PostMapping("/convert/fhir-to-hl7")
    public ResponseEntity<String> convertFhirToHl7(@RequestBody String fhirJson) {
        long startTime = System.currentTimeMillis();
        System.out.println("\nReceived FHIR Json:\n" + fhirJson); // Debug print

        // Validate
        InputValidator.ValidationResult validationResult = validator.validateFhirInput(fhirJson);
        if (!validationResult.isValid()) {
            log.error("FHIR validation failed: {}", validationResult.getErrorMessage());
            long latency = System.currentTimeMillis() - startTime;
            broadcastAudit(new Date(), "FHIR -> HL7", "Failure", "ADMIN", latency);
            recordConversion(false);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "Validation failed",
                            "details", validationResult.getErrors()
                    ).toString());
        }

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

            // Send this to website
            long latency = System.currentTimeMillis() - startTime;
            broadcastAudit(new Date(),"FHIR -> HL7", "Success", "ADMIN", latency);


            // record conversion success
            recordConversion(true);
            log.info("Conversion complete! HL7 message saved.");
            return ResponseEntity.ok(hl7Message);

        } catch (Exception e) {

            // Send success request to website
            long latency = System.currentTimeMillis() - startTime;
            broadcastAudit(new Date(),"FHIR -> HL7", "Failure", "ADMIN", latency);

            // record conversion failure

            recordConversion(false);

            log.error("Failed to convert FHIR to HL7: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"Conversion failed: " + e.getMessage() + "\"}");
        }
    }

    private synchronized void recordConversion(boolean success) {
        totalConversions++;
        if (success) successCount++;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        double successRate = totalConversions == 0 ? 0 : (successCount * 100.0 / totalConversions);
        int decimalPlaces = 2;
        double factor = Math.pow(10, decimalPlaces);
        successRate = (double) Math.round(successRate * factor) / factor;
        Map<String, Object> stats = Map.of(
                "totalConversions", totalConversions,
                "successRate", successRate,
                "FHIRCount", FHIRCount,
                "HL7Count", HL7Count
        );
        return ResponseEntity.ok(stats);
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
