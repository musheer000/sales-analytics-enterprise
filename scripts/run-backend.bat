@echo off
echo =====================================================================
echo  Starting Sales Analytics & Reporting Backend (Port: 8080)
echo =====================================================================

cd /d "%~dp0..\backend"

if "%JAVA_HOME%"=="" (
    set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.20.8-hotspot"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

echo Using Java from: %JAVA_HOME%
java -version

echo.
echo Launching Spring Boot Application...
mvn spring-boot:run
if %ERRORLEVEL% neq 0 (
    echo Falling back to Maven wrapper...
    call mvnw.cmd spring-boot:run
)

pause
