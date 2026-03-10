@echo off
setlocal
cd /d "%~dp0"

:: Check if the local Maven exists
if not exist "apache-maven-3.9.9\bin\mvn.cmd" (
    echo Error: Local Maven directory "apache-maven-3.9.9" not found.
    pause
    exit /b 1
)

echo Starting Network Simulator...
call "apache-maven-3.9.9\bin\mvn.cmd" compile exec:java -Dexec.cleanupDaemonThreads=false

if %ERRORLEVEL% neq 0 (
    echo.
    echo Application exited with an error.
    pause
)
endlocal
