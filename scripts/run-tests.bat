@echo off
echo =====================================================================
echo  Running Sales Analytics Automated Test Suite (JUnit 5 + MockMvc)
echo =====================================================================

cd /d "%~dp0..\backend"

set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.20.8-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"

set "MVN_BIN=D:\IntelliJ IDEA 2026.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd"
if not exist "%MVN_BIN%" (
    set "MVN_BIN=mvn"
)

call "%MVN_BIN%" test

echo.
echo =====================================================================
echo  Test execution finished!
echo =====================================================================
pause
