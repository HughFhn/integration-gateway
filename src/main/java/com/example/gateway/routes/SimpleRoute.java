package com.example.gateway.routes;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.springframework.stereotype.Component;

import com.example.gateway.mapper.DemoMapper;
import com.example.gateway.mapper.SourcePerson;
import com.example.gateway.mapper.TargetPerson;

@Component
public class SimpleRoute extends RouteBuilder {

    private final DemoMapper mapper;

    public SimpleRoute(DemoMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void configure() {

        JacksonDataFormat jsonFormat = new JacksonDataFormat(SourcePerson[].class);

        from("file:input?noop=true")
                .routeId("json-transform")
                .log("Processing file: ${header.CamelFilename}")
                .unmarshal(jsonFormat)
                .process(exchange -> {

                    SourcePerson[] sourceArray = exchange.getIn().getBody(SourcePerson[].class);

                    // Create map for the people
                    List<TargetPerson> targets = Arrays.stream(sourceArray)
                            // Filter names if they have e
                            .filter(source -> source.getFirstName().contains("e") || source.getLastName().contains("e"))
                            .map(mapper::map)
                            .collect(Collectors.toList());

                    // Print mapped persons
                    targets.forEach(t -> System.out.println("Mapped TargetPerson: " + t.getFirstName() + " " + t.getLastName()));

                    // Put list of mapped persons into the exchange
                    exchange.getMessage().setBody(targets);
                })
                .marshal().json() // TargetPerson -> JSON
                .to("file:output?fileName=${file:name.noext}-mapped.json") // Save to output folder
                .log("Mapped JSON written to output folder: ${file:name.noext}-mapped.json");
    }
}
