package com.example.gateway.converter;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.util.Terser;

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
        String genderCode = terser.get("/PID-8-1");
        if (genderCode != null) {
            MapperService mapperService = new MapperService();
            mapperService.setGender(patient, genderCode);
        }


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
        ContactPoint contactPoint = new ContactPoint(); // Holds contact info - number, email, etc

        // EMERGENCY CONTACT DETAILS ARE NOT IN HL7 UNTIL NK1 (Next of Kin) SECTION LATER

        // To set a phone number you must first set a contact point eg: Phone
        contactPoint.setSystem(ContactPoint.ContactPointSystem.PHONE);
        contactPoint.setValue(terser.get("/PID-13"));
        patient.addTelecom(contactPoint); // This requires a ContactPoint type


        // Language mapping
        String languageInput = terser.get("/PID-15");
        if (languageInput != null && !languageInput.isEmpty()) {
            Patient.PatientCommunicationComponent communication = new Patient.PatientCommunicationComponent();
            CodeableConcept languageConcept = new CodeableConcept();

            String code = new Hl7ToFhirConverter().getLanguageCode(languageInput);

            languageConcept.addCoding()
                    .setSystem("urn:ietf:bcp:47")
                    .setCode(code)
                    .setDisplay(languageInput);

            communication.setLanguage(languageConcept);
            communication.setPreferred(true);
            patient.addCommunication(communication);
        }

        // Marital Status
        String maritalStatusCode = terser.get("/PID-16");

        if (maritalStatusCode != null && !maritalStatusCode.isEmpty()) {
            CodeableConcept maritalStatus = new CodeableConcept();

            MapperService mapperService = new MapperService();

            maritalStatus.addCoding()
                    .setSystem("http://terminology.hl7.org/CodeSystem/v3-MaritalStatus")
                    .setCode(maritalStatusCode)
                    .setDisplay(mapperService.getMaritalStatus(maritalStatusCode));

            patient.setMaritalStatus(maritalStatus);
        }

        // Change to Religious Affiliation Codes
        String religionCode = terser.get("/PID-17");
        if (religionCode != null) {
            MapperService mapperService = new MapperService();
            mapperService.setReligionToFhir(patient, religionCode);
        }

        // Ethnicity
        String ethnicInput =  terser.get("/PID-22");
        if (ethnicInput != null && !ethnicInput.isEmpty()) {
            MapperService mapperService = new MapperService();
            String ethnicCode = mapperService.getEthnicCode(ethnicInput);
            CodeableConcept ethnicity = new CodeableConcept();

            ethnicity.addCoding()
                    .setSystem("http://hl7.org/fhir/v3/Ethnicity")
                    .setCode(ethnicCode)
                    .setDisplay(ethnicInput);

            patient.addExtension()
                    .setUrl("http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity")
                    .setValue(ethnicity);
        }

        // Return Json
        return context.newJsonParser().encodeResourceToString(patient);
    }

}
