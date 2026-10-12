@echo off
echo ====================================================
echo Starting AgroVerse Smart Farming Platform...
echo ====================================================

start "AgroVerse Backend (Spring Boot + ONNX Engine)" cmd /c "start_backend.bat"
start "AgroVerse Frontend (SPA Port 3001)" cmd /c "start_frontend.bat"

echo.
echo ====================================================
echo Backend running on:  http://localhost:8080
echo Frontend running on: http://localhost:3001
echo ONNX Inference:     Canonical in-process Java Engine
echo ====================================================
echo.
pause
