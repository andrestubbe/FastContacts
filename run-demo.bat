@echo off
setlocal
chcp 65001 > nul
cd /d "%~dp0"
set "MAVEN_OPTS=--enable-native-access=ALL-UNNAMED -Dorg.slf4j.simpleLogger.defaultLogLevel=warn -Dfile.encoding=UTF-8"

echo ===================================================
echo  Building FastContacts ^& Launching Live Hero Demo
echo ===================================================

if exist "C:\Users\andre\tools\apache-maven-3.9.9\bin\mvn.cmd" (
    set "MVN_CMD=C:\Users\andre\tools\apache-maven-3.9.9\bin\mvn.cmd"
) else (
    set "MVN_CMD=mvn"
)

call "%MVN_CMD%" -q -Dorg.slf4j.simpleLogger.defaultLogLevel=warn compile 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build failed!
    pause
    exit /b %ERRORLEVEL%
)

call "%MVN_CMD%" -q exec:java "-Dexec.mainClass=fastcontacts.Demo" "-Dexec.args=" 2>nul
pause
