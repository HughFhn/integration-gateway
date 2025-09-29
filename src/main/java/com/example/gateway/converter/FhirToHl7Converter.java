package com.example.gateway.converter;

import ca.uhn.hl7v2.model.v26.datatype.CWE;
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
            // XPN is a data structure of how names are formatted in HL7
            XPN hl7Name = message.getPID().getPatientName(0);

            if (fhirName.hasFamily()) {
                hl7Name.getFamilyName().getSurname().setValue(fhirName.getFamily());
            }

            if (fhirName.hasGiven()) {
                if (!fhirName.getGiven().isEmpty()) {
                    hl7Name.getGivenName().setValue(fhirName.getGiven().get(0).getValue());
                }
                if (fhirName.getGiven().size() > 1) {
                    hl7Name.getSecondAndFurtherGivenNamesOrInitialsThereof()
                            .setValue(fhirName.getGiven().get(1).getValue());
                }
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
        // ========== Map Extensions ** Expand if reuired or using Religion or Citizenship (Look at README for links) ==========================

        // Map Mothers Maiden Name
        Extension maidenName = fhirPatient.getExtensionByUrl("http://hl7.org/fhir/StructureDefinition/patient-mothersMaidenName");
        if (maidenName != null && maidenName.getValue() instanceof StringType) {
            // Cast as String and extract raw String type from java
            String maidenNameValue = ((StringType) maidenName.getValue()).getValue();
            // Get PID section on 1st repitition and cast the name as a surname as it is
            message.getPID().getMotherSMaidenName(0).getFamilyName().getSurname().setValue(maidenNameValue);
        }

        // Add lookup feature ? so --Codes -> Display without input to Json
        // Map Religion
        Extension religion = fhirPatient.getExtensionByUrl("http://hl7.org/fhir/StructureDefinition/patient-religion");
        if (religion != null && religion.getValue() instanceof CodeableConcept) {
            CodeableConcept religionValue = (CodeableConcept) religion.getValue();
            if (religionValue.hasCoding()) {
                // Get the code and display name from the coding
                Coding coding = religionValue.getCodingFirstRep(); // Such as 1001 == Baptist

                // Must create Coded Element to format correctly (Before coded element result was "^1009\S\Baptist" - Happens if the set value is one long string
                CWE hl7Religion = message.getPID().getReligion();
                hl7Religion.getIdentifier().setValue(coding.getCode()); // Religion code
                hl7Religion.getText().setValue(coding.getDisplay());    // Name from display var
            }
        }

        Extension ethnicity = fhirPatient.getExtensionByUrl("http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity");
        if (ethnicity != null) {
            for (Extension innerExt : ethnicity.getExtension()) {
                if ((innerExt.getUrl().equals("ombCategory") || innerExt.getUrl().equals("detailed"))
                        && innerExt.getValue() instanceof Coding) {
                    Coding coding = (Coding) innerExt.getValue();

                    CWE hl7Ethnicity = message.getPID().getEthnicGroup(0);
                    hl7Ethnicity.getIdentifier().setValue(coding.getCode());
                    hl7Ethnicity.getText().setValue(coding.getDisplay());
                    if (coding.hasSystem()) {
                        hl7Ethnicity.getNameOfCodingSystem().setValue(coding.getSystem());
                    }
                }

                // Fallback if no coding is present but text is
                if (innerExt.getUrl().equals("text") && innerExt.getValue() instanceof StringType) {
                    CWE hl7Ethnicity = message.getPID().getEthnicGroup(0);
                    hl7Ethnicity.getText().setValue(((StringType) innerExt.getValue()).getValue());
                }
            }
        }

        // Add more extensions if needed
        // Extension __ = fhirPatient.getExtensionByUrl(__);

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
        // FIX PHONE AS ONLY DUMPS RAW
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
        // Map languages
        if (fhirPatient.hasCommunication()){
            Patient.PatientCommunicationComponent communication = fhirPatient.getCommunicationFirstRep();
            if (communication.hasLanguage() && communication.getLanguage().hasCoding()){
                Coding language = communication.getLanguage().getCodingFirstRep();
                if (language.hasCode()) {
                    message.getPID().getPrimaryLanguage().getIdentifier().setValue(language.getCode());
                }
            }
            else if (communication.hasLanguage() && communication.getLanguage().hasText()){
                // IF NO CODE JUST STORE TEXT
                message.getPID().getPrimaryLanguage().getText().setValue(communication.getLanguage().getText());
            }
        }

        // Create parser to encode message
        PipeParser parser = new PipeParser();
        return parser.encode(message);
    }
}
