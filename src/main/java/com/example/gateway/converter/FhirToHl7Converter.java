package com.example.gateway.converter;

import ca.uhn.hl7v2.model.v26.datatype.XPN;
import ca.uhn.hl7v2.model.v26.message.ADT_A01;
import ca.uhn.hl7v2.parser.PipeParser;
import org.hl7.fhir.r4.model.*;

import java.text.SimpleDateFormat;

public class FhirToHl7Converter {
    public static String convertFhirToPatient(org.hl7.fhir.r4.model.Patient fhirPatient) throws Exception {
        ADT_A01 message = new ADT_A01();
        message.initQuickstart("ADT", "A01", "P");

        // Map Patient Name
        if (fhirPatient.hasName()) {
            HumanName fhirName = fhirPatient.getNameFirstRep();
            XPN hl7Name = message.getPID().getPatientName(0);

            if (fhirName.hasFamily()) {
                hl7Name.getFamilyName().getSurname().setValue(fhirName.getFamily());
            }

            if (fhirName.hasGiven()) {
                StringBuilder givenNames = new StringBuilder();
                for (StringType given : fhirName.getGiven()) {
                    if (!givenNames.isEmpty()) givenNames.append("^");
                    givenNames.append(given.getValue());
                }
                hl7Name.getGivenName().setValue(givenNames.toString());
            }
        }
        // Map Patient Gender
        if (fhirPatient.hasGender()) {
            String fhirGender = fhirPatient.getGender().toCode();
            switch (fhirGender) {
                case "male":
                    message.getPID().getAdministrativeSex().setValue("M");
                    break;
                case "female":
                    message.getPID().getAdministrativeSex().setValue("F");
                    break;
                case "other":
                    message.getPID().getAdministrativeSex().setValue("OTHER");
                    break;
                case "unknown":
                default:
                    message.getPID().getAdministrativeSex().setValue("UNKNOWN");
                    break;
            }
        }
        // Map Patient Birthday
        if (fhirPatient.hasBirthDate()) {
            // Birthdate with dashes
            java.util.Date birthDate = fhirPatient.getBirthDate();
            if (birthDate != null) {
                // Formats the date without dashes with date format object
                SimpleDateFormat hl7BirthDate = new SimpleDateFormat("yyyyMMdd");
                String dob = hl7BirthDate.format(birthDate);
                // Sets date in Hl7 PID
                message.getPID().getDateTimeOfBirth().setValue(dob);
            }
        }
        // Map Extensions
        if (fhirPatient.hasExtension()) {
            // Map Mothers Maiden Name
            Extension maidenName = fhirPatient.getExtensionByUrl("http://hl7.org/fhir/StructureDefinition/patient-mothersMaidenName");
            if (maidenName != null && maidenName.getValue() instanceof StringType) {
                // Cast as String and extract raw String type from java
                String maidenNameValue = ((StringType) maidenName.getValue()).getValue();
                // Get PID section on 1st repitition and cast the name as a surname as it is
                message.getPID().getMotherSMaidenName(0).getFamilyName().getSurname().setValue(maidenNameValue);
            }
            // Add more extensions if needed
            // Extension __ = fhirPatient.getExtensionByUrl(__);
        }


        PipeParser parser = new PipeParser();
        return parser.encode(message);
    }
}
