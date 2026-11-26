@echo off
echo =====================================================================
echo  Initializing MySQL Database: sales_analytics
echo =====================================================================

set MYSQL_BIN="C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if not exist %MYSQL_BIN% (
    set MYSQL_BIN=mysql
)

echo Executing schema.sql...
%MYSQL_BIN% -u root -p < "%~dp0..\database\schema.sql"
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Failed to execute schema.sql. Please verify your MySQL credentials.
    pause
    exit /b 1
)

echo Executing seed_data.sql...
%MYSQL_BIN% -u root -p < "%~dp0..\database\seed_data.sql"
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Failed to execute seed_data.sql.
    pause
    exit /b 1
)

echo.
echo =====================================================================
echo  Database 'sales_analytics' initialized successfully with sample data!
echo =====================================================================
pause
