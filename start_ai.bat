@echo off
cd /d "%~dp0ai-service"
echo Starting AgroVerse Python AI Service on http://127.0.0.1:8001 ...
if exist "venv\Scripts\python.exe" (
    venv\Scripts\python.exe main.py
) else (
    python main.py
)
pause
