package com.example.gateway.utils;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.DefaultModelClassFactory;
import ca.uhn.hl7v2.parser.PipeParser;

// Created Hl7 parser. Handle Hl7 and normalisation
public class Hl7ParserUtil {
    public static Message parseHL7(String hl7) throws Exception {
        // Normalize line endings: HAPI expects carriage returns between segments
        hl7 = hl7.replace("\n", "\r");

        // Force parser to use version 2.6 model classes
        PipeParser parser = new PipeParser(new DefaultModelClassFactory());
        parser.getParserConfiguration().setValidating(false);

        // Parse and return
        return parser.parse(hl7);
    }
}
