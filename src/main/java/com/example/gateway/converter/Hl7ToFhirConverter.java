package com.example.gateway.converter;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.util.Terser;

import com.example.gateway.converter.hl7Converter.MshToFhir;
import com.example.gateway.converter.hl7Converter.PidToFhir;
import com.example.gateway.maps.MapperService;
import org.hl7.fhir.r4.model.*;
import ca.uhn.fhir.context.FhirContext;

public class Hl7ToFhirConverter extends MapperService {

    public static String convert(Message hl7Message, FhirContext fhirContext, String messageType) throws Exception {
        switch (messageType) {
            case "ADT_A01":
                return convertAdtToPatient(hl7Message, fhirContext);

            // add for different message types (REDCap, DHIS2, CPIP, Other future types or other Hl7 types)

            default:
                throw new IllegalArgumentException("Unsupported HL7 message type: " + messageType);
        }
    }

    private static String convertAdtToPatient(Message message, FhirContext context) throws Exception {
        Terser terser = new Terser(message);

        // Create Bundle
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.MESSAGE);

        // ================== MessageHeader (MSH) ===================
        MessageHeader header = MshToFhir.toMessageHeader(terser);
        header.setEvent(new Coding()
                .setSystem("http://hl7.org/fhir/message-events")
                .setCode("ADT^A01")
                .setDisplay("ADT Admit/Visit Notification"));

        // ================== Patient (PID) ========================
        Patient patient = PidToFhir.toPatient(terser);

        // Link patient as focus of the MessageHeader
        Reference patientRef = new Reference("Patient/" + patient.getIdElement().getIdPart());
        header.addFocus(patientRef);

        // ================== Add to Bundle ========================
        bundle.addEntry()
                .setFullUrl("urn:uuid:" + java.util.UUID.randomUUID())
                .setResource(header);

        bundle.addEntry()
                .setFullUrl("urn:uuid:" + java.util.UUID.randomUUID())
                .setResource(patient);

        // Return JSON
        return context.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle);
    }

}
