package com.example.gateway.controller;

import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.gateway.mapper.SourcePerson;


@RestController
@RequestMapping("/data")

public class DataController {
    @GetMapping("path")
    public List<SourcePerson> getPeople() {
        return List.of(
            new SourcePerson("John", "Doe"),
            new SourcePerson("Alice", "Smith")
        );
    }
}
