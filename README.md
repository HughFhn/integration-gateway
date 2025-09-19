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

---
# How to Build and Use:  

In the terminal type the following to build the jar file:

```
mvn clean package spring-boot:repackage
```

Next type this to run the jar file:

```
java -jar target/gateway-0.0.1-SNAPSHOT.jar
```

- Then the server will start up and await a POST request to it.

- Open another terminal. This will be used to send a POST request and simulate a client.

- Navigate to the dir with the test file where the HL7 is located.

- Next, invoke a web request by pasting the following in the terminal:
```
Invoke-WebRequest -Uri http://localhost:8080/fhir/convert `
>>   -Method POST `                                                                                                                                                                                                                 
>>   -ContentType "text/plain" `                                                                                                                                                                                                    
>>   -InFile "test.hl7" `
```

- Navigating back to the terminal with the gateway running, a result like the following should display:
```
Received HL7:
MSH|^~\&|HIS|RIH|EKG|EKG|202509101430||ADT^A01|123456|P|2.6
PID|1||12345^^^Hospital^MR||Doe^John^A|Coleman|19800101|M|||123 Main St^^Metropolis^NY^10001||555-1234|||S||123456789
PV1|1|I|2000^2012^01||||004777^Smith^Adam|||||||||||V
FHIR JSON written to: output/test-fhir.json

Conversion complete!
Converted HL7: {"resourceType":"Patient","extension":[{"url":"http://hl7.org/fhir/StructureDefinition/patient-mothersMaidenName","valueString":"Coleman"}],"name":[{"family":"Doe","given":["John"]}],"gender":"male","birthDate":"1980-01-01"}
```

The result is also stored in the ``test-fhir.json`` file. It displays the time and date the request was sent for auditing purposes.

