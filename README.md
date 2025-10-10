# FHIRGateway

---
The following project is a FHIR Gateway. It retrieves data from a source in a certain format and when determining its destination it will transform the data as required. (Eg: HL7 -> FHIR Json)  
In the future this will implement APIs for REDCap, DHIS2, MN-CMS, CPIP2 possibly EPIC and other destinations in the future like the registry. The following is a document written to track the progress with the development of the foundation of transforming this data, common errors and problems encountered when making and using this application.

---

# The Following is the Stack of the Project.

| Layer                 | Choice                | Version                 | Notes                                                                                       |
| --------------------- | --------------------- | ----------------------- | ------------------------------------------------------------------------------------------- |
| JDK                   | Temurin / Oracle Java | 21 LTS                  | Current LTS. Plan a test branch for Java 25 LTS after it drops in Sept 2025. (Oracle, Java) |
| Application Framework | Spring Boot           | 3.5.5 current           | Aligns with Spring Framework 6.2.x production line. (Home, GitHub)                          |
| Middle-ware Framework | Apache Camel          | 4.14.0 (LTS)            | If you want a slightly longer-tested LTS, 4.10.6 LTS is also available. (Apache Camel)      |
| Build                 | Maven                 | 3.9.11                  | Latest general availability. (Apache Maven)                                                 |
| FHIR SDK              | HAPI FHIR             | 8.x (R4/R4B/R5 support) | R4B supported since 6.2; current 8.x artifacts available. (hapifhir.io, Maven Repository)   |
| IDP + Authenticator   | JsonWebToken (jjwt)   | 0.12.5                  | Local implementation of issuing, adding and issuing.                                        |
| TLS Library           | OpenSSL               | 3.0 (GA + LTS)          | Latest line; 1.6.3 announced Nov 2024. (GitHub, mapstruct.org)                              |

---

# Current File Structure

```Directory
├── Dockerfile
├── pom.xml
├── README.md
├── target
	├── gateway-0.0.1-SNAPSHOT.jar
	└── classes
		├── application.properties
		└── Maps
			├── GenderMap.json
			├── LanguageMap.json
			├── MaritalStatus.json
			├── EthnicityMap.json
			└── ReligionMap.json
├── input
	└── .camel
		├── test.hl7
		└── test.json
├── output
	├── audit.log
	├── test-fhir.json
	├── token.txt
	└── test-hl7.hl7
└── src
    └── main
	    ├── java
		    └── com
			    └── example
					└── gateway
						├── controller
							├── AuthController.java
							└── FhirController.java

						├── converter
							├── FhirToHl7Converter.java
							├── Hl7ToFhirConverter.java
							├── FhirToHl7
								├── FhirToMsh.java
								└── FhirToPid.java
							└── Hl7ToFhir
								├── PidToFhir.java
								└── MshToFhir.java

						├── maps
							└── MapperService.java

						├── routes
							├── FhirToHl7Route.java
							└── Hl7ToFhirRoute.java

						├── token
							├── Convert.java
							└── RequestToken.java

						├── security
							├── JwtUtil.java
							├── JwtRequestFilter.java
							├── DetailsService.java
							└── config
								└── SecurityConfig.java

						├── utils
							├── Hl7ParserUtil.java
							└── SslUtil.java

						├── Hl7ParserUtil.java
						├── application.properties
						└── GatewayApplication.java
		└── resources
			└── Maps
				├── GenderMap.json
				├── LanguageMap.json
				├── MaritalStatus.json
				├── EthnicityMap.json
				└── ReligionMap.json
```

---

# FHIRGateway Process

#### Initiation and Authorization:

After the jar file is ran the apache camel service is initiated and is secured by Spring Security only allowing requests with a valid JWT ensuring only authorized personnel can access it. There is Role Based Access defined and only the specified role is allowed to utilise the gateway.

#### REST API:

The REST API endpoints then open and there are two main operations. Patient endpoints to create, retrieve and list patients and data conversion, which is used to translate different data formats from one to the other.
The post request to create or retrieve patient data is received and the gateway operates using the in memory patient database (located in `FhirController.java` and is subject to change.)

For conversion it is sent to the certain conversion class depending on the endpoint.

#### Storing Data:

The gateway avoids data storage which will reflect the end product. (Audit log may be an exception)

#### Returning Response:

After, the gateway sends back a HTTP response of the received and converted data or an error message if true.

#### Logging and Auditing:

In this stage of development which is focused on ironing out issues and debugging, the gateway logs the last conversion and time of conversion which is useful for monitoring if a conversion method was successful or not even if it did not receive any errors.

---

# How to Use:

### Prerequisites:

- Make sure the appropriate software that is listed in the stack is installed.
- Install Postman for easy HTTP requests.

### How to run HL7 to FHIR Json:

Now opening a new terminal in the dir of the FHIRGate project,
In the terminal type the following to build the jar file:

```Powershell
mvn clean package spring-boot:repackage
```

Next type this to run the jar file:

```Powershell
java -jar target/gateway-0.0.1-SNAPSHOT.jar
```

- Then the apache camel server will start up and await a POST request to it.

* For testing and development purposes, I created classes so that the valid JWT can be retrieved and applied and then send a request and retrieve the result in the output. This is located in the `token/convert.java` file.
* When ran while the gateway is up, a token will be retrieved and applied, then a conversion will be sent with the token and the result will be saved and shown.

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

The time of conversion is written to the audit.log file in the output folder to see if the conversion was successful.

**_Congratulations you converted data formats!_**

---

### How to Add Conversion Fields:

(_The following was written while converting the PID and MSH of HL7 messages. Other headers may vary_)

**_HL7 -> FHIR Json_**
To add a new feature to convert
There should be a converter java file located in the `src.main.java.com.example.gateway` file path. This file is where you specify the attributes in the patient object that you want to collect from the message. The convert function checks the type of message depending on what it detects in a simple switch case. It then jumps to the actual convert function. Using HAPI Terser to parse the HL7 message and using the HL7 FHIR library to assign attributes to a patient object using the PID segments. Some attributes need an object of their own such as ContactPoint and others which are not native to FHIR require an extension on their decleration (This is shown later). The file then moves on to the Apache Camel Route so the program will know how the message flows through the system.

Basic HL7 Message overview:
MSH|... (Message Header)
EVN|... (Event Type)
PID|... (Patient Identification) <- This is what is done so far
NK1|... (Next of Kin – optional, repeatable)
NK1|... (Additional next of kin if present)
PV1|... (Patient Visit)

---

**_FHIR Json -> HL7_**
To add a new feature to convert
There should be a converter java file located in the `src.main.java.com.example.gateway` file path similar to converting HL7 to FHIR Json. The converter function starts with creating a message object. You must specify the message code, trigger event and processing ID such as ADT, A01, P. These are all found in the Message Header (first line) of HL7 messages.
Then you must start mapping features from the Json to the HL7 format. Knowledge or a cheat sheet of the HL7 format is required to correctly map the features to the correct section. Here is a link to the HL7 format: [Link to Section Documentation](#section_id)
When mapping to HL7 fields, most features require a certain type object to be created. This object is used to get from the Json and is then used in the `.setValue()` method. Any deviations to this formula is listed below under the **Problems Encountered** section. Extension implementation is similar to the other conversion of HL7 to FHIR Json. The file then moves on to the Apache Camel Route so the program will know how the message flows through the system.

---

### Overview

My one observation between the two conversions is that FHIR Json to HL7 is much more verbose and complicated. The terser and HL7 FHIR libraries are very simple and easy to use. When writing it flows much better. This maybe due to the fact that HL7 is more complex so formatting is more difficult or that it is a legacy format so moving away from it should be simple.

---

# Authorization

Authorization is handled by providing username and passwords. The Java Web Token (or JWT) received will manage to work as long as the admin/user has their password and username.
The distribution, authentication and configuration of the security is handled in the `security` folder.

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

_(PS: I have left a list at the bottom listing the fields that require extensions and their URLs )_

Links to helpful resources:
Defining extensions, sub-extensions and retrieving values from extensions: https://hapifhir.iso/hapi-fhir/docs/model/profiles_and_extensions.html#profiles-and-extensions
<a id="section_id"></a>
List of PID and there corresponding value and if they require extensions: https://build.fhir.org/ig/HL7/v2-to-fhir/ConceptMap-segment-pid-to-patient.html
This Should be your bible when converting PID to Patient Map. Data types, links, and objects are specified per PID with additional notes available.

Extension Registry: https://build.fhir.org/ig/HL7/fhir-extensions/extension-registry.html

---

## Misc. Intricacies

When adding information to the `Patient` object, for adding X there is usually a `.setX` method but some of these take only the types of what they are setting. For example, if setting an address, you would use `.setAddress(*Address goes here*)` but that address you are adding must be an address object. When creating an Address object there are methods to set streets, city, etc.

### Status Codes Commonly Encountered:

**401 - Unauthorized:** If you get this status code then you are unauthorized to perform a certain action. In most cases the wrong access token or access role is used. Double check in Postman that the access token in the GET and POST methods are the same.

**403 - Forbidden:** This status code can be retrieved when trying to convert data using an expired access token or incorrect access token. Check the token received and the token used carefully.

**404 - Not Found:** If you receive this error the problem is most likely due to the route or PostMapping in your Controller file or in your Postman, using the wrong URL. This means that the endpoint could not be located.

**500 - Internal Server Error:** This is a generic HTTP code but in my experience with FHIR Gate, I have found it to be an issue with the code that is not picked up by the IDE. Check spellings, route in the http and check the input (Don't try to convert a HL7 when it is looking for a Json).

**200 - Ok:** This code means that the request was successful so if you are still encountering a problem it is not something to do with the POST or GET requests, routing or input. Most likely to due with the converters.

---

# Considerations:

The following are ideas for the program that may help improve aspects of its design, control, security, etc..

## Security

https://camel.apache.org/components/4.14.x/oauth-component.html
https://camel.apache.org/manual/security.html
https://spring.io/projects/spring-security#overview
https://build.fhir.org/security.html

- Role Based Access Control/ Attribute Based Access Control. (OAuth)
- TLS/ SSL (https) rules must apply. (TLS 1.2 as it offers strong encryption during transmission and it is supported by a wide range of websites and browsers.)
- Audit traffic.
- Using FHIR's security labels and read access to grant or prevent certain people access to reading some confidential information.
  - Implementing a break the glass feature for restricted data so in an emergency context someone like a clinician can view the information to help the patient.
- Check DoS and over flooding the queries and prevent it from happening. And Json injection. Done with Spring Security filters or API limits.
- Users must protect their token if leaving device unattended. (Token Timeout?)
- Verify HL7 and FHIR messages and ignore or reject unknown forms of messages.
- Log suspicious activities. _Use token introspection for this_
- Store client secrets, tokens and API keys on a server-side vault in case of needed retrieval.
- The AuditEvent is a record that logs who did what, when and why in a healthcare system.
  _AuditEvent_ by FHIR read more: https://build.fhir.org/auditevent.html
  The _AuditEvent Codebook_: https://build.fhir.org/valueset-audit-event-type.html

## Authentication

Best practices:
Link: https://docs.smarthealthit.org/authorization/best-practices/

- Authentication codes, client credentials and other sensitive information should be conducted over links that have been secured using TLS.
- Refresh tokens should have a significantly longer lifetime than access tokens.
- Refresh tokens are good as they reduce the risk of unwanted access.
- Client Credential flow OAuth2. (Good for FHIR Gateway -> REDCap/ DHIS2)
  - Used for machine to machine communication allowing an application to authenticate resources without user involvement. It uses client ID and a client secret (Long and confidential string of characters issued by the authentication server after application registration.) to gain an access token from the authentication server which is required to gain access to the resources.
- Implementing a third party Multi-Factor Authentication tool like Google Authenticator/ Authy/ Microsoft Authenticator.

## Implementation:

- Using the available Hl7 to FHIR would simplify the process. It is open source so modifications can be applied if needed.  
  _(GitRepo to FHIR to Hl7V2 converter: https://github.com/CDCgov/prime-fhir-converter?tab=readme-ov-file )_
- Server needed to audit and authenticate GET and POST requests.
- The following link is Implementation considerations from there HL7 to FHIR website: https://build.fhir.org/ig/HL7/v2-to-fhir/implementation_considerations.html
- Here is a Java REDCap API Library on GitHub as Java is not natively supported: https://github.com/altierifIOV/REDCapAPILibrary/tree/main
  _Python is if implementation on that is required and Java is too difficult_
- DHIS2 supports transformation to FHIR: https://dhis2.org/integration/fhir/
  _Includes video in which they show code of conversion from DHIS2 to FHIR_
- DHIS2 -> FHIR repo: https://github.com/dhis2/integration-examples/tree/main/dhis2-to-fhir-bundle _important can be implemented_

For REDCap mapping is required. Annotations in the fields (@fhir:Patient.birthDate) could assist but this would have to be naming conventions all around the country.

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

If you receive a code error when trying to run the code, maybe the feature you are trying to get is encoded. Features like gender, marital status and relationship are coded and require the `toCode()`method. The following is a link to the list of all encoded values in FHIR HL7: https://terminology.hl7.org/codesystems.html

### Format Validation Tools

The following are validation for certain data formats. I would suggest checking the input HL7 and the output Json (and vice versa) to see if the converter created valid outputs.
**FHIR Json validator:** https://validator.fhir.org/
**HL7 Validator:** https://freeonlineformatter.com/hl7-validator/run

### References for Creating the JWT Service:

JWT Authentication Filter:
https://stackoverflow.com/questions/41975045/how-to-design-a-good-jwt-authentication-filter

General Outline to stick to:
https://medium.com/@prateekjadhav8/implementing-jwt-authentication-with-spring-security-in-a-spring-boot-application-048c94ed60ba

Link to decode JWT (Check the role, expiry and Issue is correct):
https://www.jwt.io/

What is a JWT:
https://www.geeksforgeeks.org/web-tech/json-web-token-jwt/
