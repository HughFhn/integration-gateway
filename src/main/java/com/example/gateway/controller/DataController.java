package com.example.gateway.controller;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.gateway.mapper.SourcePerson;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/data")
public class DataController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/path")
    public List<SourcePerson> getPeople() throws IOException {
        // Absolute path to test.json
        File file = new File("C:\\Users\\HFeehan\\OneDrive - University College Cork\\Desktop\\INFANT\\integration-gateway\\input\\test.json");

        if (!file.exists()) {
            throw new IOException("File not found: " + file.getAbsolutePath());
        }

        // Read JSON array from file
        List<SourcePerson> people = objectMapper.readValue(file, new TypeReference<List<SourcePerson>>() {
        });

        // Filter names containing "e" (case-insensitive)
        return people.stream()
                .filter(p -> p.getFirstName().toLowerCase().contains("e")
                || p.getLastName().toLowerCase().contains("e"))
                .collect(Collectors.toList());
    }
}
