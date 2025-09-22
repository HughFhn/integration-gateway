package com.example.gateway.converter;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.util.Terser;

import org.hl7.fhir.r4.model.*;
import ca.uhn.fhir.context.FhirContext;

import java.util.ArrayList;
import java.util.List;

public class Hl7ToFhirConverter {

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
        // Declare patient object and Terser to parse Hl7 messages
        Terser terser = new Terser(message);
        Patient patient = new Patient();
        // last name and first name
        patient.addName().setFamily(terser.get("/PID-5-1")).addGiven(terser.get("/PID-5-2"));
        // mothers maiden name (practice extensions)
        String mothersMaidenName = terser.get("/PID-6-1");
        if (mothersMaidenName != null && !mothersMaidenName.isEmpty()) {
            patient.addExtension()
                    .setUrl("http://hl7.org/fhir/StructureDefinition/patient-mothersMaidenName")
                    .setValue(new org.hl7.fhir.r4.model.StringType(mothersMaidenName));
        }
        // DOB
        String dob = terser.get("/PID-7-1");
        if (dob != null) {
            patient.setBirthDate(new java.text.SimpleDateFormat("yyyyMMdd").parse(dob));
        }
        // gender
        patient.setGender("M".equalsIgnoreCase(terser.get("/PID-8-1")) ? Enumerations.AdministrativeGender.MALE : Enumerations.AdministrativeGender.FEMALE);

        // address object as patient.addAddress only takes address type
        Address address = new Address();
        address.addLine(terser.get("/PID-11-1"));
        address.setCity(terser.get("/PID-11-3"));
        address.setState(terser.get("/PID-11-4"));
        address.setPostalCode(terser.get("/PID-11-5"));
        // Add to patient
        patient.addAddress(address);

        // phone number
        Patient.ContactComponent emergeContact = new Patient.ContactComponent(); // Emergency contact or organisation
        ContactDetail emergContactDetail = new ContactDetail(); // Details on the contact and patient (relationship)
        ContactPoint contactDetails = new ContactPoint(); // Holds contact info - number, email, etc

        // To set a phone number you must first set a contact point eg: Phone
        contactDetails.setSystem(ContactPoint.ContactPointSystem.PHONE);
        contactDetails.setValue(terser.get("/PID-13"));
        patient.addTelecom(contactDetails); // This requires a ContactPoint type
        return context.newJsonParser().encodeResourceToString(patient);
    }

}
