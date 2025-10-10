# Admin Dashboard

The following is an audit tool that should only be accessible by the admin. 
It provides the data that may be needed after deployment of this project.  
Timestamps, status, user and latency would be very important when the project is liev as it will be easy to track if anything went wrong
or any user who was not authorised try to access the gateway services.

### Stats
The stats important from the backend is the following:  
* Total conversions
* Conversion success rates
* Counters for conversion types

### Logs
The following data is imported with every conversion:
* Timestamp of conversion.
* Type of conversion
* Status of conversion
* User
* Latency of conversion

### Diagrams
I utilised a MUI pie chart for simple visual representation on the most common conversion type and using a donut component on it, 
I displayed the success to failure ratio also.
