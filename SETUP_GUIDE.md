# Setup and Running Guide (Docker)

This guide will walk you through the process of setting up and running the FHIR Gateway application using Docker. This is the recommended approach as it simplifies setup and ensures a consistent environment.

## Prerequisites

Before you begin, ensure you have the following software installed on your system:

- **Docker:** For building and running the application containers.
- **Docker Compose:** For orchestrating the multi-container application.

## 1. Clone the Repository

Clone the project repository from GitHub to your local machine. Open your terminal or command prompt and run the following command:

```bash
git clone <repository-url>
```

_(Replace `<repository-url>` with the actual URL of the Git repository.)_

### 2. Run the Backend Server

With Docker and Docker Compose installed, running the application is as simple as a single command.  
In the root dir of the project type the following:

```bash
docker compose up --build
```

This command will:

1.  Build the Docker images for both the backend and frontend services.
2.  Start the containers for both services.

The backend will be accessible at `http://localhost:8081`, and the frontend will be accessible at `http://localhost:3000`.

_Note: This will take awhile upon first use, subsequent occassions will have some data cached and improve the speed._

## 3. Using the Application

Now that the application is running, you can use it.

### a. Logging In

Open your web browser and navigate to `http://localhost:3000`. You will be presented with a login screen. To log in, you will need a username and password.

The backend is configured to use an in-memory user with the following credentials:

- **Username:** `admin`
- **Password:** `password`

Enter these credentials into the login form and click the "Login" button.

### b. Sending a Message for Conversion

After logging in, you will see the main dashboard. On the left side of the dashboard, you will find the "Manual Conversion" section.

1.  **Select Message Type:** Choose the type of conversion you want to perform from the dropdown menu (e.g., "REDCap to FHIR").
2.  **Enter Message:** Paste your message (e.g., a REDCap JSON object) into the text area.
3.  **Click Convert:** Click the "Convert" button to send the message to the backend for processing.

The result of the conversion will be displayed in the "Conversion Result" box below the "Convert" button.

## 4. Stopping the Application

To stop the application, press `Ctrl + C` in the terminal where `docker compose up` is running. To remove the containers, you can run:

```bash
docker compose down
```
