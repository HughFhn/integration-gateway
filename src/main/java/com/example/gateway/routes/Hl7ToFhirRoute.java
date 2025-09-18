package com.example.gateway.routes;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.hl7.HL7DataFormat;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.util.Terser;

@Component
public class Hl7ToFhirRoute extends RouteBuilder {

    @Override
    public void configure() {

        FhirContext fhirContext = FhirContext.forR4();
        HL7DataFormat hl7 = new HL7DataFormat();

        from("file:input?include=.*\\.hl7$")
                .unmarshal(hl7)
                .routeId("hl7-to-fhir")
                .process(exchange -> {
                    // Get the HL7 message from Camel body
                    Message hl7Message = exchange.getIn().getBody(Message.class);

                    // Use Terser for dynamic field extraction
                    Terser terser = new Terser(hl7Message);

                    // Extract necessary fields from the Hl7 message
                    String familyName = terser.get("/PID-5-1");
                    String givenName = terser.get("/PID-5-2");
                    String genderCode = terser.get("/PID-8-1");
                    String dob = terser.get("/PID-7-1");
                    String identifier = terser.get("/PID-3-1");
                    String addressLine = terser.get("/PID-11-1");
                    String city = terser.get("/PID-11-3");
                    String state = terser.get("/PID-11-4");
                    String postalCode = terser.get("/PID-11-5");
                    String country = terser.get("/PID-11-6");

                    // Build FHIR Patient
                    Patient patient = new Patient();

                    // Name setting
                    if (familyName != null || givenName != null) {
                        patient.addName().setFamily(familyName).addGiven(givenName);
                    }

                    // Gender setting 
                    if ("M".equalsIgnoreCase(genderCode)) {
                        patient.setGender(Enumerations.AdministrativeGender.MALE);
                    } else if ("F".equalsIgnoreCase(genderCode)) {
                        patient.setGender(Enumerations.AdministrativeGender.FEMALE);
                    } else {
                        patient.setGender(Enumerations.AdministrativeGender.UNKNOWN);
                    }

                    // Date of Birth setting
                    if (dob != null && !dob.isEmpty()) {
                        try {
                            patient.setBirthDate(new java.text.SimpleDateFormat("yyyyMMdd").parse(dob));
                        } catch (Exception ignored) {
                        }
                    }

                    // identifier setting
                    if (identifier != null && !identifier.isEmpty()) {
                        patient.addIdentifier().setSystem("unknown").setValue(identifier);
                    }

                    // Address setting
                    if (addressLine != null || city != null || state != null || postalCode != null || country != null) {
                        patient.addAddress()
                                .addLine(addressLine)
                                .setCity(city)
                                .setState(state)
                                .setPostalCode(postalCode)
                                .setCountry(country);
                    }

                    // Convert FHIR Patient to JSON
                    String fhirJson = fhirContext.newJsonParser().encodeResourceToString(patient);

                    // Set JSON as message body
                    exchange.getMessage().setBody(fhirJson);
                })
                // Write FHIR JSON to output file
                .to("file:output?fileName=${file:name.noext}-fhir.json")
                // Writes to terminal to show conversion is completed
                .log("HL7 -> FHIR conversion completed: ${file:name.noext}-fhir.json");
    }
}
