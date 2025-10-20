# Step 1: Get JWT Token
$loginBody = @{
    username = "admin"
    password = "password"
} | ConvertTo-Json

$tokenResponse = Invoke-RestMethod -Uri "http://localhost:8081/auth/login" `
    -Method POST `
    -ContentType "application/json" `
    -Body $loginBody

$token = $tokenResponse.token
Write-Host "Token obtained: $token" -ForegroundColor Green

# Step 2: Create a SINGLE REDCap record (not an array!)
$redcapRecord = @{
    record_id = "123"
    first_name = "John"
    last_name = "Doe"
    dob = "1985-06-10"
    study_id = "TEST001"
    test_complete = "2"
} | ConvertTo-Json

Write-Host "`nSending REDCap JSON:" -ForegroundColor Cyan
Write-Host $redcapRecord

# Step 3: Send REDCap conversion request
Write-Host "`nSending REDCap to FHIR conversion request..." -ForegroundColor Yellow

try {
    $conversionResponse = Invoke-RestMethod -Uri "http://localhost:8081/fhir/convert/redcap-to-fhir" `
        -Method POST `
        -Headers @{
        "Authorization" = "Bearer $token"
        "Content-Type" = "application/json"
    } `
        -Body $redcapRecord

    Write-Host "`nConversion Successful!" -ForegroundColor Green
    Write-Host $conversionResponse
}
catch {
    Write-Host "`nConversion Failed!" -ForegroundColor Red
    Write-Host "Status Code:" $_.Exception.Response.StatusCode.value__
    Write-Host "Error:" $_.ErrorDetails.Message
}