package com.example.gateway.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DemoMapper {
    @Mapping(source = "firstName", target = "givenName")
    TargetPerson map(SourcePerson source);

    class SourcePerson {
        public String firstName;
        public String lastName;
    }

    class TargetPerson {
        public String givenName;
        public String lastName;
    }
}
