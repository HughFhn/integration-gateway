# Setup and User Guide

This guide provides step-by-step instructions on how to set up and run the FHIR Gateway application on your local machine.

## Prerequisites

Before you start, ensure you have the following software installed:

-   **Git:** For cloning the project repository.
-   **Java 17:** Required for the backend services.
-   **Maven:** To build and run the Java backend.
-   **Node.js:** To run the frontend application.
-   **npm:** For managing frontend dependencies (usually comes with Node.js).

---

## Setup Instructions

### 1. Clone the Repository

Open your terminal or command prompt and clone the repository to your local machine:

```bash
git clone <repository-url>
```
*(Replace `<repository-url>` with the actual URL of the Git repository.)*

### 2. Run the Backend Server

The backend is a Spring Boot application. To start the server, navigate to the root directory of the project in your terminal and run the following command:

```bash
mvn spring-boot:run
```

The backend server will start on `http://localhost:8081`.

### 3. Run the Frontend Application

For this step, you will need a new terminal window.

First, navigate to the frontend application's directory:

```bash
cd src/frontEnd/fhir-gateway-ui
```

Next, install the required dependencies using npm:

```bash
npm install
```

Once the dependencies are installed, start the frontend development server:

```bash
npm start
```

Your default web browser should open to `http://localhost:3000`, displaying the application's login screen.

---

## User Guide

### Logging In

To use the application, you first need to log in. The default credentials for the application are:

-   **Username:** `admin`
-   **Password:** `password`

Enter these credentials on the login page to access the main dashboard.

### Manual Message Conversion

Once logged in, you can use the "Manual Conversion" tool on the dashboard to convert messages.

1.  **Select Conversion Type:** Use the dropdown menu to choose the desired conversion format (e.g., "REDCap to FHIR").

2.  **Enter Your Message:** Paste the message you want to convert into the text input field. For example, if you are converting from REDCap, you would paste a REDCap JSON object here.

3.  **Convert:** Click the "Convert" button to process the message.

The result of the conversion will appear in the "Conversion Result" box below the button. The dashboard will also display live statistics and logs for all conversion activities.
