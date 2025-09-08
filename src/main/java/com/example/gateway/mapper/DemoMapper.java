package com.example.gateway.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DemoMapper {
    @Mapping(source = "firstName", target = "firstName")
    TargetPerson map(SourcePerson source);

}
