package com.example.gateway.maps;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Service;

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
}
