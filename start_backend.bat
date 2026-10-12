@echo off
cd /d "%~dp0backend"
echo Starting AgroVerse Spring Boot backend with Maven...
mvn spring-boot:run
pause
