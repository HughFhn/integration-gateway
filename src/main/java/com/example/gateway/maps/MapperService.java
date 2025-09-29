package com.example.gateway.maps;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

import java.io.InputStream;
import java.util.Map;

@Service
public class MapperService {

    // Generic lookup for any map file
    public String getCode(String fileName, String input) {
        if (input == null || input.isEmpty()) return "UNKNOWN_CODE";
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = getClass().getClassLoader().getResourceAsStream(fileName);
            if (is == null) {
                throw new RuntimeException("Mapping file not found: " + fileName);
            }
            Map<String, String> mappings = mapper.readValue(is, Map.class);
            return mappings.getOrDefault(input.trim().toUpperCase(), "UNKNOWN_CODE");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load mapping file: " + fileName, e);
        }
    }

    // Specific language lookup (backward compatible)
    public String getLanguageCode(String languageInput) {
        if (languageInput == null) return "UNKNOWN_CODE";
        return getCode("Maps/LanguageMap.json", languageInput);
    }

    // Specific gender lookup (matches language pattern)
    public String getGenderCode(String genderInput) {
        if (genderInput == null) return "UNKNOWN_CODE";
        return getCode("Maps/GenderMap.json", genderInput);
    }

    // Mapper for gender using lookup file
    public void setGender(Patient patient, String code) {
        String genderCode = getGenderCode(code);

        switch (genderCode) {
            case "male":
                patient.setGender(Enumerations.AdministrativeGender.MALE);
                break;
            case "female":
                patient.setGender(Enumerations.AdministrativeGender.FEMALE);
                break;
            case "other":
                patient.setGender(Enumerations.AdministrativeGender.OTHER);
                break;
            default:
                patient.setGender(Enumerations.AdministrativeGender.UNKNOWN);
        }
    }

    // Marital status get func with Json
    public String getMaritalStatus(String code) {
        if (code == null) return "UNKNOWN_CODE";
        return getCode("Maps/MaritalStatus.json", code);
    }

    public Map<String, String> getReligion(String code) {
        try{
            ObjectMapper objectMapper = new ObjectMapper();
            InputStream is = getClass().getClassLoader().getResourceAsStream("Maps/ReligionMap.json");
            if (is == null) {
                throw new RuntimeException("Mapping file not found: Maps/ReligionMap.json");
            }
        Map<String, Map<String, String>> mappings = objectMapper.readValue(is, Map.class);
        return mappings.get(code.trim());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load mapping:", e);
        }
    }

    public void setReligionToFhir(Patient patient, String code) {
        Map<String, String> entry = getReligion(code);

        if (entry != null) {
            // Adding the extension
            patient.addExtension()
                    .setUrl("http://hl7.org/fhir/StructureDefinition/patient-religion")
                    .setValue(new org.hl7.fhir.r4.model.CodeableConcept()
                        .addCoding(new org.hl7.fhir.r4.model.Coding()
                            .setSystem("http://terminology.hl7.org/CodeSystem/v3-ReligiousAffiliation")
                            .setCode(entry.get("code"))
                            .setDisplay(entry.get("display")))
                        .setText(entry.get("fhir")));
        }
        else {
            // If unknown religion
            patient.addExtension()
                    .setUrl("http://hl7.org/fhir/StructureDefinition/patient-religion")
                    .setValue(new org.hl7.fhir.r4.model.CodeableConcept().setText("UNKNOWN"));
        }
    }
    public String getEthnicCode(String ethnicityInput){
        if (ethnicityInput == null) return "UNKNOWN_CODE";
        return getCode("Maps/EthnicityMap.json", ethnicityInput);

    }

}

