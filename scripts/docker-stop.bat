@echo off
echo =====================================================================
echo  Stopping Sales Analytics Docker Containers
echo =====================================================================

cd /d "%~dp0.."
docker compose down

echo Containers stopped.
pause
