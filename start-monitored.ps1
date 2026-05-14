$services = @(
    "eureka-server",
    "api-gateway",
    "auth-service",
    "provider-service",
    "appointment-service",
    "schedule-service",
    "record-service",
    "payment-service",
    "notification-service",
    "review-service"
)

$baseDir = "D:\SpringBootProject\OnlineAppointmentBooking"
$logsDir = "$baseDir\logs"

if (-Not (Test-Path -Path $logsDir)) {
    New-Item -ItemType Directory -Path $logsDir | Out-Null
}

foreach ($service in $services) {
    Write-Host "Starting $service..."
    $serviceDir = Join-Path -Path $baseDir -ChildPath $service
    $logOut = Join-Path -Path $logsDir -ChildPath "$service.out.log"
    $logErr = Join-Path -Path $logsDir -ChildPath "$service.err.log"
    
    Start-Process -FilePath "mvn.cmd" -ArgumentList "spring-boot:run" -WorkingDirectory $serviceDir -RedirectStandardOutput $logOut -RedirectStandardError $logErr -WindowStyle Hidden
    
    if ($service -eq "eureka-server") {
        Write-Host "Waiting 15 seconds for Eureka to initialize..."
        Start-Sleep -Seconds 15
    }
}

Write-Host "All backend services started in background."
