package com.example.gateway.mapper;

public class SourcePerson {
    public String firstName;
    public String lastName;

    // Default constructor needed for jackson serialisation
    public SourcePerson(){}

    // Constructor with fields
    public SourcePerson(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    // Getters and setters 
    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

}