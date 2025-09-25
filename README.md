# FHIR Gateway
---
The following project is a FHIR Gateway. It retrieves data from a source in a certain format and when determining its destination it will transform the data as required. (Eg: HL7 -> FHIR Json)  
In the future this will implement APIs for REDCap, DHIS2, MN-CMS, CPIP2 possibly EPIC and other destinations in the future like the registry. The following is a document written to track the progress with the development of the foundation of transforming this data, common errors and problems encountered when making and using this application.

--- 

# The Following is the Stack of the Project.

| Layer        | Choice                | Version                 | Notes                                                                                       |
|--------------|-----------------------|-------------------------|---------------------------------------------------------------------------------------------|
| JDK          | Temurin / Oracle Java | 21 LTS                  | Current LTS. Plan a test branch for Java 25 LTS after it drops in Sept 2025. (Oracle, Java) |
| Spring Boot  | 3.5.x                 | 3.5.5 current           | Aligns with Spring Framework 6.2.x production line. (Home, GitHub)                          |
| Apache Camel | 4.14.0 (LTS)          | Aug 19, 2025            | If you want a slightly longer-tested LTS, 4.10.6 LTS is also available. (Apache Camel)      |
| Build        | Maven                 | 3.9.11                  | Latest general availability. (Apache Maven)                                                 |
| FHIR SDK     | HAPI FHIR             | 8.x (R4/R4B/R5 support) | R4B supported since 6.2; current 8.x artifacts available. (hapifhir.io, Maven Repository)   |
| JSONPath     | jayway/json-path      | 2.9.0                   | Stable; widely used. (Maven Repository, GitHub)                                             |
| XPath        | JAXP (built-in)       | —                       | Use Java’s XPathFactory (no extra dep)                                                      |
| Mapper       | MapStruct             | 1.6.x                   | Latest line; 1.6.3 announced Nov 2024. (GitHub, mapstruct.org)                              |

---
# How to Use:

### How to run HL7 to FHIR Json:
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
- Navigate to the directory with the test file where the HL7 is located.
- Next, invoke a web request by pasting the following in the terminal:
```  
Invoke-WebRequest -Uri http://localhost:8080/fhir/convert/hl7-to-fhir `  
>>   -Method POST `                                                                     >>   -ContentType "text/plain" `                                                        >>   -InFile "test.hl7" `  
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

---
### How to Convert:

***HL7 -> FHIR Json***  
To add a new feature to convert  
There should be a converter java file located in the ``src.main.java.com.example.gateway`` file path. 
This file is where you specify the attributes in the patient object that you want to collect from the message. The convert function checks the type of message depending on what it detects in a simple switch case. 
It then jumps to the actual convert function. Using HAPI Terser to parse the HL7 message and using the HL7 FHIR library to assign attributes to a patient object using the PID segments.  
Some attributes need an object of their own such as ContactPoint and others which are not native to FHIR require an extension on their decleration (This is shown later). 
The file then moves on to the Apache Camel Route so the program will know how the message flows through the system.

Basic HL7 Message overview:  
MSH|...         (Message Header)  
EVN|...          (Event Type)  
PID|...           (Patient Identification) ** IMPORTANT INFORMATION **  
NK1|...          (Next of Kin – optional, repeatable)  
NK1|...          (Additional next of kin if present)  
PV1|...          (Patient Visit)

---
***FHIR Json -> HL7***  
To add a new feature to convert  
There should be a converter java file located in the `src.main.java.com.example.gateway` file path similar to converting HL7 to FHIR Json. The converter function starts with creating a message object. You must specify the message code, trigger event and processing ID such as ADT, A01, P.
Then you must start mapping features from the Json to the HL7 format.  
Knowledge or a cheat sheet of the HL7 format is required to correctly map the features to the correct section.
Here is a link to some helpful links later in the page: [Link to Section Documentation](#section_id)

When mapping to HL7 PIDs, most features require a certain type object to be created. This object is used to get from the Json and is then used in the ``.setValue()`` method. Any deviations to this formula is listed below under the **Problems Encountered** section. 
Extension implementation is similar to the other conversion of HL7 to FHIR Json.  
The file then moves on to the Apache Camel Route so the program will know how the message flows through the system.

---
### Overview
My one observation between the two conversions is that FHIR Json to HL7 is much more verbose and complicated. The terser and hl7 fhir libraries are very simple and easy to use. When writing it flows much better.  
This maybe due to the fact that HL7 is more complex so formatting is more difficult or that it is a legacy format so moving away from it should be simple.

---
# Problems Encountered:

The following are problems with converting data and running the program.
## Converting non-native Data

To convert a PID that is not native to FHIR you must add an extension URL so FHIR knows what to convert the HL7 data to.
An example I encountered was converting a patients maiden name which is located in PID-6. In my route since it is an extension, instead of doing something like
```java
.addMothersMaiden(maidenName)
``` 

you must do the following:

```java
addExtension()
	.setUrl("http://hl7.org/fhir/StructureDefinition/patient-mothersMaidenName")
	.setValue(new StringType(motherMaidenName));
```
*(PS: I have left a list at the bottom listing the fields that require extensions and their URLs )*

Links to helpful resources:
Defining extensions, sub-extensions and retrieving values from extensions: https://hapifhir.iso/hapi-fhir/docs/model/profiles_and_extensions.html#profiles-and-extensions

<a id="section_id"></a>
List of PID and there corresponding value and if they require extensions: https://build.fhir.org/ig/HL7/v2-to-fhir/ConceptMap-segment-pid-to-patient.html

Extension Registry: https://build.fhir.org/ig/HL7/fhir-extensions/extension-registry.html

---
## Misc. Intricacies

When adding information to the `Patient` object, for adding X there is usually a ``.setX`` method but some of these take only the types of what they are setting. 
For example, if setting an address, you would use ``.setAddress(*Address goes here*)`` but that address you are adding must be an address object. 
When creating an Address object there are methods to set streets, city, etc.

---
# Considerations:

The following are ideas for the program that may help improve aspects of its design, control, security, ect ...
## Security
https://camel.apache.org/components/4.14.x/oauth-component.html
https://camel.apache.org/manual/security.html
https://spring.io/projects/spring-security#overview
https://build.fhir.org/security.html
* Role Based Access Control/ Attribute Based Access Control. (OAuth)
* TLS/ SSL (https) rules must apply. (TLS 1.2  as it offers strong encryption during transmission and it is supported by a wide range of websites and browsers.)
* Audit traffic.
* Using FHIR's security labels and read access to grant or prevent certain people access to reading some confidential information.
    * Implementing a break the glass feature for restricted data so in an emergency context someone like a clinician can view the information to help the patient.
* Check DoS and over flooding the queries and prevent it from happening. And Json injection. Done with Spring Security filters or API limits.
* Users must protect their token if leaving device unattended.
* Verify HL7 and FHIR messages and ignore or reject unknown forms of messages.
* Log suspicious activities. Use token introspection for this*
* Store client secrets, tokens and API keys on a server-side vault in case of needed retrieval.
* The AuditEvent is a record that logs who did what, when and why in a healthcare system.
  *AuditEvent* by FHIR read more: https://build.fhir.org/auditevent.html
  The *AuditEvent Codebook*: https://build.fhir.org/valueset-audit-event-type.html

## Authentication
Best practices:
Link: https://docs.smarthealthit.org/authorization/best-practices/
- Authentication codes, client credentials and other sensitive information should be conducted over links that have been secured using TLS.
- OAuth Grant Models:
    - RFC 6749  - Enables authentication server to share directly to end users not exposing to user agents like browsers. Requires user implements CSRF (Cross-Site Request Forgery) protection.
    - If the transmitting of a JSON Web Token is required then RFC7523 is recommended.
- Refresh tokens should have a significantly longer lifetime than access tokens.
- Refresh tokens are good as they reduce the risk of unwanted access.
- Client Credential flow OAuth2. (Good for FHIR Gateway -> REDCap/ DHIS2)
    - Used for machine to machine communication allowing an application to authenticate resources without user involvement. It uses client ID and a client secret 
  (Long and confidential string of characters issued by the authentication server after application registration.) to gain an access token from the authentication server which is required to gain access to the resources.

## Implementation:
* Using the available Hl7 to FHIR would simplify the process. It is open source so modifications can be applied if needed.  
*(GitRepo to FHIR to Hl7V2 converter: https://github.com/CDCgov/prime-fhir-converter?tab=readme-ov-file )*
* Hl7 to FHIR is currently implemented. 
* Server needed to audit and authenticate GET and POST requests.
---
# Links and Help:

## Non-native data from HL7 -> FHIR Json

The following table displays the fields that are not directly supported by FHIR and need an extension created to be converted. These are just the ones I have found from the patient type in FHIR HL7 format.

| HL7 Element         | PID Number | FHIR URIs                                                         |
| ------------------- | ---------- | ----------------------------------------------------------------- |
| Mothers Maiden Name | PID-5      | http://hl7.org/fhir/StructureDefinition/patient-mothersMaidenName |
| Religion            | PID-17     | http://hl7.org/fhir/StructureDefinition/patient-religion          |
| Citizenship         | PID-26     | http://hl7.org/fhir/StructureDefinition/patient-citizenship       |

For any other extensions refer to the comprehensive FHIR Extension Registry: https://build.fhir.org/ig/HL7/fhir-extensions/extension-registry.html

### Defining people Alongside the Patient
When defining a relationship between a patient and another person, (Eg: emergency contact) a code for their relationship is needed. This link shows all RoleCodes: https://terminology.hl7.org/6.5.0/ValueSet-v3-RoleCode.html

### Encoding Problems
If you receive a code error when trying to run the code, maybe the feature you are trying to get is encoded. Features like gender, marital status and relationship are coded and require the ``toCode()``method.   
The following is a link to the list of all encoded values in FHIR HL7: https://terminology.hl7.org/codesystems.html 