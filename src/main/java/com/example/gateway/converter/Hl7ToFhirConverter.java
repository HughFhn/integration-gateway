package com.example.gateway.converter;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.util.Terser;

import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Enumerations;
import ca.uhn.fhir.context.FhirContext;

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
        Terser terser = new Terser(message);
        Patient patient = new Patient();
        patient.addName().setFamily(terser.get("/PID-5-1")).addGiven(terser.get("/PID-5-2"));
        String mothersMaidenName = terser.get("/PID-6-1");
        if (mothersMaidenName != null && !mothersMaidenName.isEmpty()) {
            patient.addExtension()
                    .setUrl("http://hl7.org/fhir/StructureDefinition/patient-mothersMaidenName")
                    .setValue(new org.hl7.fhir.r4.model.StringType(mothersMaidenName));
        }        patient.setGender("M".equalsIgnoreCase(terser.get("/PID-8-1")) ? Enumerations.AdministrativeGender.MALE : Enumerations.AdministrativeGender.FEMALE);
        String dob = terser.get("/PID-7-1");
        if (dob != null) {
            patient.setBirthDate(new java.text.SimpleDateFormat("yyyyMMdd").parse(dob));
        }
        return context.newJsonParser().encodeResourceToString(patient);
    }

}
