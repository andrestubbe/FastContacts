@echo off
setlocal
chcp 65001 > nul
cd /d "%~dp0"

echo ===================================================
echo  Compiling FastContacts (Java 17+)
echo ===================================================

if exist "C:\Users\andre\tools\apache-maven-3.9.9\bin\mvn.cmd" (
    set "MVN_CMD=C:\Users\andre\tools\apache-maven-3.9.9\bin\mvn.cmd"
) else (
    set "MVN_CMD=mvn"
)

call "%MVN_CMD%" clean test
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] FastContacts compilation/test failed!
    exit /b %ERRORLEVEL%
)

echo [SUCCESS] FastContacts built and tested successfully.
