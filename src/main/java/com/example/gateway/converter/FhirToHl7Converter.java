package com.example.gateway.converter;

import com.example.gateway.converter.FhirToHl7.FhirToMsh;
import com.example.gateway.converter.FhirToHl7.FhirToPid;
import ca.uhn.hl7v2.model.v26.message.ADT_A01;
import ca.uhn.hl7v2.parser.PipeParser;
import org.hl7.fhir.r4.model.Patient;

public class FhirToHl7Converter {

    public static String convertFhirToPatient(Patient fhirPatient) throws Exception {
        ADT_A01 message = new ADT_A01();

        // MSH mapping — initializes message and sets MSH segment
        FhirToMsh.mapToMsh(message);

        // PID mapping
        FhirToPid.mapPatientToPid(fhirPatient, message);

        PipeParser parser = new PipeParser();
        return parser.encode(message);
    }
}
