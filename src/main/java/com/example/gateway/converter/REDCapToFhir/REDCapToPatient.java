package com.example.gateway.converter.REDCapToFhir;

import com.example.gateway.maps.MapperService;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

import static org.apache.commons.collections4.MapUtils.getString;

public class REDCapToPatient {
    private static final Logger log = LoggerFactory.getLogger(REDCapToPatient.class);
    private static final MapperService mapperService = new MapperService();

    public static Patient toPatient(Map<String, Object> redcapRecord) throws Exception {
        Patient patient = new Patient();

        // Patient ID
        mapPatientIdentifier(patient, redcapRecord);

        // Name
        mapName(patient, redcapRecord);

        // Gender
        mapGender(patient, redcapRecord);

        // Birth Date
        mapBirthDate(patient, redcapRecord);

        return patient;
    }


    private static void mapPatientIdentifier(Patient patient, Map<String, Object> record) {
        String recordId = getString(record, "record_id");
        if (recordId != null && !recordId.isEmpty()) {
            patient.setId(recordId);

            Identifier identifier = new Identifier();
            identifier.setSystem("urn:redcap:record");
            identifier.setValue(recordId);
            patient.addIdentifier(identifier);
        } else {
            log.warn("Patient Record ID is null or empty");
        }
    }

    private static void mapName(Patient patient, Map<String, Object> record) {
        String firstName = getString(record, "first_name");
        String lastName = getString(record, "last_name");

        if ((firstName != null && !firstName.isEmpty()) ||
                (lastName != null && !lastName.isEmpty())) {
            HumanName name = patient.addName();

            if (lastName != null && !lastName.isEmpty()) {
                name.setFamily(lastName);
            }

            if (firstName != null && !firstName.isEmpty()) {
                name.addGiven(firstName);
            }
        }
    }

    private static void mapGender(Patient patient, Map<String, Object> record) {
        String genderVal = getString(record, "gender");
        if (genderVal != null && !genderVal.isEmpty()) {
            try {
                patient.setGender(mapGenderToAdministrativeGender(genderVal));
            } catch (Exception e) {
                log.warn("Could not map gender: {}", genderVal);
            }
        }
    }

    private static Enumerations.AdministrativeGender mapGenderToAdministrativeGender(String genderVal) {
        switch (genderVal.toLowerCase(Locale.ROOT)) {
            case "male":
                return Enumerations.AdministrativeGender.MALE;
            case "female":
                return Enumerations.AdministrativeGender.FEMALE;
            case "other":
                return Enumerations.AdministrativeGender.OTHER;
            default:
                return Enumerations.AdministrativeGender.UNKNOWN;
        }
    }

    private static void mapBirthDate(Patient patient, Map<String, Object> record) {
        String birthDateVal = getString(record, "birth_date");
        if (birthDateVal != null && !birthDateVal.isEmpty()) {
            try {
                // Adjust date format as per your REDCap configuration
                patient.setBirthDate(new SimpleDateFormat("yyyy-MM-dd").parse(birthDateVal));
            } catch (ParseException e) {
                log.warn("Invalid birth date format: {}", birthDateVal);
            }
        }
    }
}