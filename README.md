# 📊 Sales Analytics & Reporting System

A full-stack, enterprise-grade Java Spring Boot and MySQL application designed to manage business data, orders, and products, while delivering real-time analytical reporting and performance caching.

---

## 📂 Project Structure

```text
D:\Project\
├── backend\               # Spring Boot 3 + Java 17 + JPA + Cache backend
│   ├── src\main\java\     # Layered architecture (controller, service, repository, entity, dto, mapper)
│   ├── src\main\resources\# application.properties, database configuration
│   ├── Dockerfile         # Backend container definition
│   ├── pom.xml            # Maven build configuration
│   └── mvnw.cmd           # Maven Wrapper for Windows
├── frontend\              # Responsive web dashboard with Chart.js analytics
│   ├── css\styles.css     # Clean modern CSS
│   ├── js\api.js          # REST client communicating with port 8080
│   ├── js\app.js          # Interactive UI and chart rendering
│   ├── index.html         # Main dashboard page
│   ├── Dockerfile         # Nginx container definition
│   └── package.json       # Frontend scripts
├── database\              # Database scripts
│   ├── schema.sql         # MySQL schema DDL
│   └── seed_data.sql      # Initial sample dataset
├── docs\                  # Project documentation & Phase plan
│   ├── PHASE_ROADMAP.md   # Complete 6-phase development roadmap
│   ├── API_SPECIFICATION.md# REST API documentation
│   ├── ARCHITECTURE.md    # Architecture and caching diagrams
│   └── ENVIRONMENT_SETUP.md# Guide for IntelliJ, VS Code, MySQL
├── scripts\               # Convenience automation scripts
│   ├── setup-database.bat # Run MySQL schema and seed data
│   ├── run-backend.bat    # Build and launch Spring Boot app
│   ├── run-frontend.bat   # Launch frontend dashboard
│   ├── docker-start.bat   # Build & run full stack in Docker Desktop
│   └── docker-stop.bat    # Stop all Docker containers
├── docker-compose.yml     # Multi-container Docker configuration
└── README.md              # Project overview (this file)
```

---

## ⚡ Quickstart

### Option A: Run Full Stack in Docker Desktop (One-Click)
Make sure Docker Desktop is running, then run:
```cmd
.\scripts\docker-start.bat
```
* **Frontend Dashboard**: `http://localhost:3000`
* **Backend API & Swagger**: `http://localhost:8080/swagger-ui.html`
* **MySQL Container**: `localhost:3307`

### Option B: Run Locally (IntelliJ / Command Line)
1. **Initialize Database**:
   ```cmd
   .\scripts\setup-database.bat
   ```
2. **Run Backend** in IntelliJ IDEA or:
   ```cmd
   .\scripts\run-backend.bat
   ```
3. **Launch Frontend**:
   ```cmd
   .\scripts\run-frontend.bat
   ```
