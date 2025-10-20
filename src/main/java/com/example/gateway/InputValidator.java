package com.example.gateway;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.util.Terser;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class InputValidator {

    private final FhirContext fhirContext;
    private static final int MAX_INPUT_SIZE = 1_000_000; // 1MB
    private static final Pattern HL7_SEGMENT_PATTERN = Pattern.compile("^[A-Z]{3}\\|");
    private static final Pattern MALICIOUS_PATTERN = Pattern.compile(
            "(<script|javascript:|onerror=|onload=|<iframe|eval\\(|exec\\()",
            Pattern.CASE_INSENSITIVE
    );

    public InputValidator() {
        this.fhirContext = FhirContext.forR4();
    }


    //  Validates HL7 message input
    public ValidationResult validateHl7Input(String hl7Input) {
        List<String> errors = new ArrayList<>();

        // Check for null or empty
        if (hl7Input == null || hl7Input.trim().isEmpty()) {
            errors.add("HL7 input cannot be null or empty");
            return new ValidationResult(false, errors);
        }

        // Check size limits
        if (hl7Input.length() > MAX_INPUT_SIZE) {
            errors.add("HL7 input exceeds maximum size of " + MAX_INPUT_SIZE + " characters");
            return new ValidationResult(false, errors);
        }

        // Check for malicious content
        if (MALICIOUS_PATTERN.matcher(hl7Input).find()) {
            errors.add("HL7 input contains potentially malicious content");
            return new ValidationResult(false, errors);
        }

        // Validate HL7 structure
        if (!hl7Input.contains("MSH")) {
            errors.add("HL7 message must contain MSH segment");
            return new ValidationResult(false, errors);
        }

        // Attempt to parse
        try {
            String normalized = hl7Input.replace("\n", "\r");
            PipeParser parser = new PipeParser();
            parser.getParserConfiguration().setValidating(false);
            Message message = parser.parse(normalized);

            // Validate message type
            Terser terser = new Terser(message);
            String messageType = terser.get("/MSH-9-1");
            String triggerEvent = terser.get("/MSH-9-2");

            if (messageType == null || messageType.isEmpty()) {
                errors.add("HL7 message type (MSH-9-1) is required");
            }

            if (triggerEvent == null || triggerEvent.isEmpty()) {
                errors.add("HL7 trigger event (MSH-9-2) is required");
            }

            // Validate supported message types
            String fullType = messageType + "_" + triggerEvent;
            if (!isSupportedMessageType(fullType)) {
                errors.add("Unsupported HL7 message type: " + fullType +
                        ". Supported types: ADT_A01");
            }

        } catch (Exception e) {
            errors.add("Failed to parse HL7 message: " + e.getMessage());
            return new ValidationResult(false, errors);
        }

        return errors.isEmpty()
                ? new ValidationResult(true, null)
                : new ValidationResult(false, errors);
    }


    // ====Validates FHIR JSON input====
    public ValidationResult validateFhirInput(String fhirJson) {
        // Create list to store possible errors
        List<String> errors = new ArrayList<>();

        ValidationResult jsonCheck = isValidJson(fhirJson);
        if (!jsonCheck.isValid()) {
            // Immediately return with those errors
            return jsonCheck;
        }

        // Attempt to parse as FHIR resource
        try {
            var resource = fhirContext.newJsonParser().parseResource(fhirJson);

            // Validate it's a supported resource type
            if (!(resource instanceof Patient) && !(resource instanceof Bundle)) {
                errors.add("FHIR resource must be either Patient or Bundle containing Patient");
                return new ValidationResult(false, errors);
            }

            // If it's a Bundle, ensure it contains a Patient
            if (resource instanceof Bundle) {
                Bundle bundle = (Bundle) resource;
                boolean hasPatient = bundle.getEntry().stream()
                        .anyMatch(entry -> entry.getResource() instanceof Patient);

                if (!hasPatient) {
                    errors.add("FHIR Bundle must contain at least one Patient resource");
                }
            }

            // Validate required Patient fields
            Patient patient = extractPatient(resource);
            if (patient != null) {
                validatePatientResource(patient, errors);
            }

        } catch (DataFormatException e) {
            errors.add("Invalid FHIR JSON format: " + e.getMessage());
            return new ValidationResult(false, errors);
        }
        catch (Exception e) {
            errors.add("Failed to parse FHIR resource: " + e.getMessage());
            return new ValidationResult(false, errors);
        }

        return errors.isEmpty()
                ? new ValidationResult(true, null)
                : new ValidationResult(false, errors);
    }


    // Validates Patient resource has required fields
    private void validatePatientResource(Patient patient, List<String> errors) {
        // While FHIR doesn't strictly require these, for conversions we need certain data
        if (!patient.hasName() || patient.getName().isEmpty()) {
            errors.add("Patient must have at least one name");
        }

        if (patient.hasName()) {
            var name = patient.getNameFirstRep();
            if (!name.hasFamily() && !name.hasGiven()) {
                errors.add("Patient name must have either family name or given name");
            }
        }

        // Warn about missing recommended fields (not errors)
        if (!patient.hasGender()) {
            errors.add("Warning: Patient gender not specified");
        }

        if (!patient.hasBirthDate()) {
            errors.add("Warning: Patient birth date not specified");
        }
    }


    // Extract Patient from resource or Bundle
    private Patient extractPatient(org.hl7.fhir.instance.model.api.IBaseResource resource) {
        if (resource instanceof Patient) {
            return (Patient) resource;
        } else if (resource instanceof Bundle) {
            Bundle bundle = (Bundle) resource;
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.getResource() instanceof Patient) {
                    return (Patient) entry.getResource();
                }
            }
        }
        return null;
    }


    // ====Validates REDCap Json Format====
    public ValidationResult validateRedcapInput(String redcapJson){
        // Create list for errors
        List<String> errors = new ArrayList();

        ValidationResult jsonCheck = isValidJson(redcapJson);
            if (!jsonCheck.isValid()) {
            // Immediately return with those errors
            return jsonCheck;
        }

        try {
            // First section of the extracted json
            if (!redcapJson.contains("study_id")){
                errors.add("REDCap json contains no study_id field");
            }

            // Last section of the json
            if (!redcapJson.contains("test_complete")){
                errors.add("REDCap json contains no test_complete field");
            }

            // These 2 checks can check if the complete message was taken
        }

        catch (DataFormatException e) {
            errors.add("Invalid REDCap JSON format: " + e.getMessage());
            return new ValidationResult(false, errors);
        }
        catch (Exception e) {
            errors.add("Failed to parse REDCap: " + e.getMessage());
            return new ValidationResult(false, errors);
        }

        return errors.isEmpty()
        ? new ValidationResult(true, null)
        : new ValidationResult(false, errors);
    }


    // ====Validates Json Format====
    private ValidationResult isValidJson(String message){
        List<String> errors = new ArrayList<>();

        // Check for null or empty
        if (message == null || message.trim().isEmpty()) {
            errors.add("FHIR input cannot be null or empty");
            return new ValidationResult(false, errors);
        }

        // Check size limits
        if (message.length() > MAX_INPUT_SIZE) {
            errors.add("FHIR input exceeds maximum size of " + MAX_INPUT_SIZE + " characters");
            return new ValidationResult(false, errors);
        }

        // Check for malicious content
        if (MALICIOUS_PATTERN.matcher(message).find()) {
            errors.add("FHIR input contains potentially malicious content");
            return new ValidationResult(false, errors);
        }

        // Validate JSON structure
        if (!message.trim().startsWith("{")) {
            errors.add("FHIR input must be valid JSON");
            return new ValidationResult(false, errors);
        }

        return errors.isEmpty()
                ? new ValidationResult(true, null)
                : new ValidationResult(false, errors);
    }


    // Check if message type is supported
    private boolean isSupportedMessageType(String messageType) {
        // Add more as you implement them
        return "ADT_A01".equals(messageType);
    }


    //  Validation result class

    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors != null ? errors : new ArrayList<>();
        }

        public String getErrorMessage() {
            if (errors == null || errors.isEmpty()) {
                return "";
            }
            return String.join("; ", errors);
        }
    }
}