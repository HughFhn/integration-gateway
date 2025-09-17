# The Following is the Stack of the Project.

| Layer        | Choice                | Version                 | Notes                                                                                       |
| ------------ | --------------------- | ----------------------- | ------------------------------------------------------------------------------------------- |
| JDK          | Temurin / Oracle Java | 21 LTS                  | Current LTS. Plan a test branch for Java 25 LTS after it drops in Sept 2025. (Oracle, Java) |
| Spring Boot  | 3.5.x                 | 3.5.5 current           | Aligns with Spring Framework 6.2.x production line. (Home, GitHub)                          |
| Apache Camel | 4.14.0 (LTS)          | Aug 19, 2025            | If you want a slightly longer-tested LTS, 4.10.6 LTS is also available. (Apache Camel)      |
| Build        | Maven                 | 3.9.11                  | Latest GA. (Apache Maven)                                                                   |
| FHIR SDK     | HAPI FHIR             | 8.x (R4/R4B/R5 support) | R4B supported since 6.2; current 8.x artifacts available. (hapifhir.io, Maven Repository)   |
| JSONPath     | jayway/json-path      | 2.9.0                   | Stable; widely used. (Maven Repository, GitHub)                                             |
| XPath        | JAXP (built-in)       | —                       | Use Java’s XPathFactory (no extra dep)                                                      |
| Mapper       | MapStruct             | 1.6.x                   | Latest line; 1.6.3 announced Nov 2024. (GitHub, mapstruct.org)                              |

<br>

# How to Build

In the terminal type the following to build the jar file:

```
mvn clean package spring-boot:repackage
```

Next type this to run the jar file:

```
java -jar target/gateway-0.0.1-SNAPSHOT.jar
```

Then the local host will start up. The input file will be sent and the result will end up in the output file. <br>There should be a message in the terminal to tell the user that the server was created and that the message was converted.

The user may experience many HTTP errors when testing and altering the route files.
