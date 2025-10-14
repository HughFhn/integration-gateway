package com.example.gateway.converter;

import ca.uhn.fhir.context.FhirContext;
import com.example.gateway.converter.REDCapToFhir.REDCapToPatient;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class REDCapToFhirConverter {

    public static String convert(Map<String, Object> redcapRecord, FhirContext fhirContext) throws Exception {
        // Create FHIR Message Bundle
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.MESSAGE);

        // Convert REDCap record -> FHIR Patient
        Patient patient = REDCapToPatient.toPatient(redcapRecord);

        // Add Message Header
        bundle.addEntry()
                .setFullUrl("urn:uuid:" + UUID.randomUUID())
                .setResource(createMessageHeader(patient));

        // Add Patient resource
        bundle.addEntry()
                .setFullUrl("urn:uuid:" + UUID.randomUUID())
                .setResource(patient);

        // Return FHIR JSON
        return fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle);
    }

    // Set message header for proper validation and possibly convert to HL7 if needed
    private static MessageHeader createMessageHeader(Patient patient) {
        MessageHeader header = new MessageHeader();
        header.setEvent(new Coding()
                .setSystem("http://hl7.org/fhir/message-events")
                .setCode("REDCap-Patient-Import")
                .setDisplay("REDCap Patient Import"));

        header.addFocus(new Reference("Patient/" + patient.getIdElement().getIdPart()));
        return header;
    }
}
