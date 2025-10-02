package com.example.gateway.converter.FhirToHl7;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v26.message.ADT_A01;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.parser.PipeParser;
import org.hl7.fhir.r4.model.MessageHeader;

import java.io.IOException;

public class FhirToMsh {
    public static String mapToMsh(ADT_A01 message) throws HL7Exception, IOException {

        // MSH Section init
        // Trigger event is hardcoded **
        message.initQuickstart("ADT", "A01", "P");

        // Message header object
        MSH msh = message.getMSH();

        msh.getFieldSeparator().setValue("|");
        // Set Encoding Characters
        msh.getEncodingCharacters().setValue("^~\\&");

        msh.getSendingApplication().getNamespaceID().setValue("FhirGateway");
        msh.getSendingFacility().getNamespaceID().setValue("Example-Facility");
        msh.getReceivingFacility().getNamespaceID().setValue("HL7-Reciever");
        msh.getReceivingFacility().getNamespaceID().setValue("Example-Facility");

        PipeParser parser = new PipeParser();
        String hl7Message = parser.encode(message);

        // Return only the MSH segment
        return hl7Message.split("\r")[0];
    }
}
