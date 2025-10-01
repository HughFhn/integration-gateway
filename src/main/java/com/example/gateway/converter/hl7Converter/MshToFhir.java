package com.example.gateway.converter.hl7Converter;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.util.Terser;

import com.example.gateway.maps.MapperService;
import org.hl7.fhir.r4.model.*;
import ca.uhn.fhir.context.FhirContext;

public class MshToFhir {
    public static MessageHeader toMessageHeader(Terser terser) throws Exception {

        MessageHeader messageHeader = new MessageHeader();
        MessageHeader.MessageSourceComponent messageSourceComponent = new MessageHeader.MessageSourceComponent();

        // Sending application
        String sourceName = terser.get("/MSH-3");
        if (sourceName == null || !sourceName.isEmpty()) {
            messageSourceComponent.setName(sourceName);
        }
        // Sending facility/ hospital
        String sourceEndpoint = terser.get("/MSH-4");
        if (sourceEndpoint == null || !sourceEndpoint.isEmpty()) {
            messageSourceComponent.setEndpoint(sourceEndpoint);
        }
        messageHeader.setSource(messageSourceComponent);

        // Receiving point (Can be a location or application or process eg: EKG == ECG System)
        MessageHeader.MessageDestinationComponent messageDestinationComponent = new MessageHeader.MessageDestinationComponent();

        String destinationName = terser.get("/MSH-5");
        if (destinationName != null || !destinationName.isEmpty()) {
            messageDestinationComponent.setName(destinationName);
        }
        String destinationEndpoint = terser.get("/MSH-6");
        if (destinationEndpoint != null || !destinationEndpoint.isEmpty()) {
            messageDestinationComponent.setEndpoint(destinationEndpoint);
        }
        messageHeader.addDestination(messageDestinationComponent);

        return messageHeader;
    }
}
