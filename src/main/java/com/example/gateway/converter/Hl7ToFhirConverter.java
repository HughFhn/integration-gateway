package com.example.gateway.converter;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.util.Terser;

import org.hl7.fhir.r4.model.*;
import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.codesystems.Relationship;

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
        String genderCode = terser.get("/PID-8-1");
        if (genderCode != null) {
            getGenderDisplay(patient, genderCode);
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

        // Language
        String languageInput = terser.get("/PID-15");
        if (languageInput != null && !languageInput.isEmpty()) {
            Patient.PatientCommunicationComponent communication = new Patient.PatientCommunicationComponent();
            CodeableConcept languageConcept = new CodeableConcept();

            // Use FHIR standard codes (bcp 47) as in this link: https://terminology.hl7.org/ValueSet-Languages.html
            languageConcept.addCoding()
                    .setSystem("urn:ietf:bcp:47")
                    .setCode(getLanguageDisplay(languageInput))
                    .setDisplay(languageInput);

            communication.setLanguage(languageConcept);
            communication.setPreferred(true);
            patient.addCommunication(communication);
        }

        // Marital Status
        String maritalStatusCode = terser.get("/PID-16"); // e.g. "M" for Married, "S" for Single

        if (maritalStatusCode != null && !maritalStatusCode.isEmpty()) {
            CodeableConcept maritalStatus = new CodeableConcept();

            maritalStatus.addCoding()
                    .setSystem("http://terminology.hl7.org/CodeSystem/v3-MaritalStatus")
                    .setCode(maritalStatusCode)
                    .setDisplay(getMaritalStatusDisplay(maritalStatusCode));

            patient.setMaritalStatus(maritalStatus);
        }

        // Change to Religious Affiliation Codes : Link is in system |
        String religionCode = terser.get("/PID-17-1");           //  ^
        if (religionCode != null && !religionCode.isEmpty()) {
            CodeableConcept religion = new CodeableConcept();
            religion.addCoding()
                    .setSystem("http://terminology.hl7.org/CodeSystem/v3-ReligiousAffiliation")
                    .setCode(religionCode)
                    .setDisplay(getReligionDisplay(religionCode)); // helper for human-readable display

            patient.addExtension(
                    new Extension("http://hl7.org/fhir/StructureDefinition/patient-religion", religion)
            );
        }


        // Return Json
        return context.newJsonParser().encodeResourceToString(patient);
    }

    // ================ Helpers to get display values for encoded features ================

    // Helper for gender
    private static void getGenderDisplay(Patient patient, String code) {
         switch (code.toUpperCase()) {
            case "M" -> patient.setGender(Enumerations.AdministrativeGender.MALE);
            case "F" -> patient.setGender(Enumerations.AdministrativeGender.FEMALE);
            case "O" -> patient.setGender(Enumerations.AdministrativeGender.OTHER);
            case "U" -> patient.setGender(Enumerations.AdministrativeGender.UNKNOWN);
            default -> patient.setGender(Enumerations.AdministrativeGender.UNKNOWN);
        };
    }

    private static String getLanguageDisplay(String languageInput) {
        return switch (languageInput.toUpperCase()) {
            case "ENGLISH", "ENG" -> "en";
            case "GAEILGE", "IRISH", "GA" -> "ga";
            case "FRENCH", "FR" -> "fr";
            case "SPANISH", "SP", "ES" -> "es";
            case "GERMAN", "DE", "GER" -> "de";
            case "POLISH", "PL", "POL" -> "pl";
            case "UKRANIAN", "UK", "UKR" -> "uk";
            case "LITHUANIAN", "LT", "LITH" -> "lt";
            case "ROMANIAN", "RO", "ROMANIA" -> "ro";
            default -> "UNKNOWN_CODE";
        };
    }

    // Helper for marital status
    private static String getMaritalStatusDisplay(String code) {
        return switch (code) {
            case "M" -> "Married";
            case "S" -> "Never Married";
            case "D" -> "Divorced";
            case "W" -> "Widowed";
            case "L" -> "Legally Separated";
            case "U" -> "Unmarried";
            default -> "Unknown";
        };
    }

    // Helper for religion
    private static String getReligionDisplay(String code) {
        return switch (code) {
            case "CATH" -> "Catholic";
            case "JEW"  -> "Judaism";
            case "BAPT" -> "Baptist";
            case "METH" -> "Methodist";
            case "PRES" -> "Presbyterian";
            case "MUS"  -> "Muslim";
            case "HIND" -> "Hindu";
            default     -> "Unknown Religion";
        };
    }


}
