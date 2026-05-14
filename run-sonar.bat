@echo off
set SONAR_TOKEN=sqp_2698e48dc81a4b3368866a5a1c6816e0b05ddd11
set SONAR_URL=http://localhost:9000

for %%s in (api-gateway appointment-service auth-service eureka-server notification-service payment-service provider-service record-service review-service schedule-service) do (
    echo.
    echo ========================================
    echo Running SonarQube for %%s
    echo ========================================
    cd %%s
    call mvnw clean verify sonar:sonar ^
        -Dsonar.host.url=%SONAR_URL% ^
        -Dsonar.token=%SONAR_TOKEN% ^
        -Dsonar.projectKey=medibook-online-appointment-booking ^
        -Dsonar.projectName="MediBook Online Appointment Booking"
    cd ..
)

echo.
echo ========================================
echo Done! Open http://localhost:9000
echo ========================================
pause