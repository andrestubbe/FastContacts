@echo off
setlocal
chcp 65001 > nul
cd /d "%~dp0"
set "MAVEN_OPTS=--enable-native-access=ALL-UNNAMED -Dorg.slf4j.simpleLogger.defaultLogLevel=warn -Dfile.encoding=UTF-8"

echo ===================================================
echo  Building FastContacts ^& JMH Benchmarks Uber-Jar
echo ===================================================

if exist "C:\Users\andre\tools\apache-maven-3.9.9\bin\mvn.cmd" (
    set "MVN_CMD=C:\Users\andre\tools\apache-maven-3.9.9\bin\mvn.cmd"
) else (
    set "MVN_CMD=mvn"
)

call "%MVN_CMD%" -q clean install -DskipTests 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] FastContacts install failed!
    pause
    exit /b %ERRORLEVEL%
)

cd examples\Benchmark
call "%MVN_CMD%" -q clean package 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Benchmark packaging failed!
    pause
    exit /b %ERRORLEVEL%
)

echo ===================================================
echo  Running JMH Benchmarks (Throughput: ops/ms)
echo ===================================================
java --enable-native-access=ALL-UNNAMED -jar target\benchmarks.jar -f 1 -wi 2 -i 3 -tu ms -bm thrpt
pause
