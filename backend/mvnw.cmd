@REM ----------------------------------------------------------------------------
@REM Maven Wrapper Batch Script for Windows
@REM ----------------------------------------------------------------------------
@echo off
setlocal

set "DIR=%~dp0"
if "%DIR%"=="" set "DIR=.\%~n0\"

set "MAVEN_PROJECTBASEDIR=%DIR%"
set "WRAPPER_JAR=%DIR%.mvn\wrapper\maven-wrapper.jar"
set "WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain"

@REM Look for installed mvn if wrapper jar is absent
if not exist "%WRAPPER_JAR%" (
    where mvn >nul 2>nul
    if %ERRORLEVEL% equ 0 (
        mvn %*
        exit /b %ERRORLEVEL%
    )
)

if "%JAVA_HOME%"=="" (
    for %%i in (java.exe) do set "JAVACMD=%%~$PATH:i"
) else (
    set "JAVACMD=%JAVA_HOME%\bin\java.exe"
)

if not exist "%JAVACMD%" (
    echo Error: JAVA_HOME is not set and no 'java' command could be found in your PATH. >&2
    exit /b 1
)

"%JAVACMD%" -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR%" -cp "%WRAPPER_JAR%" %WRAPPER_LAUNCHER% %*
if %ERRORLEVEL% neq 0 (
    @REM Fallback direct maven call if available
    where mvn >nul 2>nul
    if %ERRORLEVEL% equ 0 (
        mvn %*
        exit /b %ERRORLEVEL%
    )
)
exit /b %ERRORLEVEL%
