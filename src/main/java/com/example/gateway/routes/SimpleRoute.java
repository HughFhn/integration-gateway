package com.example.gateway.routes;

import com.example.gateway.mapper.DemoMapper;
import com.example.gateway.mapper.DemoMapper.SourcePerson;
import com.example.gateway.mapper.DemoMapper.TargetPerson;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class SimpleRoute extends RouteBuilder {

    private final DemoMapper mapper;

    public SimpleRoute(DemoMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void configure() {
        from("file:input?noop=true")
                .routeId("json-transform")
                .unmarshal().json(SourcePerson.class) // Convert JSON to SourcePerson
                .process(exchange -> {
                    SourcePerson source = exchange.getIn().getBody(SourcePerson.class);
                    TargetPerson target = mapper.map(source); // Map using MapStruct
                    System.out.println("Mapped TargetPerson: " + target.givenName + " " + target.lastName);
                })
                .process(exchange -> {
                    // Stop Camel after processing all files
                    exchange.getContext().stop();
                });
    }
}
