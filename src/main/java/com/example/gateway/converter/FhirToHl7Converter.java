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
            // Made it a string so switch case worked
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
        // Map Extensions ** Expand if reuired or using Religion or Citizenship (Look at README for links)
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
        // Map Patient Address
        if (fhirPatient.hasAddress()){
            Address fhirAddress = fhirPatient.getAddress().get(0);
            var hl7Address = message.getPID().getPatientAddress(0);
            if (fhirAddress.hasLine()) {
                hl7Address.getStreetAddress().getStreetOrMailingAddress().setValue(fhirAddress.getLine().get(0).getValue());
            }
            if (fhirAddress.hasCity()) {
                hl7Address.getCity().setValue(fhirAddress.getCity());
            }
            if (fhirAddress.hasState()) {
                hl7Address.getStateOrProvince().setValue(fhirAddress.getState());
            }
            if (fhirAddress.hasPostalCode()) {
                hl7Address.getZipOrPostalCode().setValue(fhirAddress.getPostalCode());
            }
            if (fhirAddress.hasCountry()) {
                hl7Address.getCountry().setValue(fhirAddress.getCountry());
            }
        }
        // Map Patient Telecom (Phone)
        if (fhirPatient.hasTelecom()) {
            for (ContactPoint cp : fhirPatient.getTelecom()) {
                if (cp.getSystem() == ContactPoint.ContactPointSystem.PHONE) {
                    String digits = cp.getValue().replaceAll("\\D", ""); // remove +, spaces
                    if (!digits.isEmpty()) {
                        var phone = message.getPID().getPhoneNumberHome(0);
                        phone.getAnyText().setValue(digits);
                    }
                }

                if ("email".equals(cp.getSystem().toCode())) {
                    message.getPID().getPhoneNumberBusiness(0).getTelephoneNumber().setValue(cp.getValue());
                }
            }
        }
        // Map Martial Status
        if (fhirPatient.hasMaritalStatus()) {
            // Decode the marital status portion
            String maritalCode = fhirPatient.getMaritalStatus().getCodingFirstRep().getCode();
            // Set the PID according to the code
            message.getPID().getMaritalStatus().getIdentifier().setValue(maritalCode);
        }

        PipeParser parser = new PipeParser();
        return parser.encode(message);
    }
}
