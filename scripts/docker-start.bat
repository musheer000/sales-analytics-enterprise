@echo off
echo =====================================================================
echo  Starting Sales Analytics Application via Docker Compose
echo =====================================================================

cd /d "%~dp0.."
docker compose up -d --build

echo.
echo =====================================================================
echo  Services started successfully!
echo   - Frontend: http://localhost:3000
echo   - Backend:  http://localhost:8080/api/categories
echo   - Swagger:  http://localhost:8080/swagger-ui.html
echo   - MySQL DB: localhost:3307
echo =====================================================================
pause
