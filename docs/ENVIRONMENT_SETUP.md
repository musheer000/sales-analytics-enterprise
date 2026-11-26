# 🛠️ Tools & Environment Setup Guide

You have:
- **IntelliJ IDEA** or **VS Code**
- **Java 17 (Microsoft OpenJDK 17 LTS)** installed
- **MySQL Server 8.0** installed
- **Node.js (v22+)** and **npm** installed

---

## 1. Setting Up in IntelliJ IDEA
1. Open IntelliJ IDEA.
2. Click **File -> Open...** and select `D:\Projectackend`.
3. IntelliJ will detect `pom.xml` and download dependencies automatically.
4. Ensure Project SDK is set to **Java 17**:
   - Go to **File -> Project Structure -> Project -> SDK** -> Select `Microsoft OpenJDK 17`.
5. Run the application:
   - Navigate to `src/main/java/com/salesanalytics/SalesAnalyticsApplication.java`.
   - Right-click and select **Run 'SalesAnalyticsApplication'**.

---

## 2. Setting Up in VS Code
1. Open VS Code.
2. Click **File -> Open Folder...** and select `D:\Project`.
3. Recommended VS Code Extensions:
   - **Extension Pack for Java** (Microsoft)
   - **Spring Boot Extension Pack** (VMware)
   - **MySQL** / Database Client
4. Open a terminal and run `.\scriptsun-backend.bat`.

---

## 3. Initializing MySQL Database
1. Open Command Prompt or PowerShell.
2. Run `.\scripts\setup-database.bat`.
3. If your MySQL root password is not blank, edit `backend/src/main/resources/application.properties` or set environment variable:
   ```cmd
   set DB_PASSWORD=your_mysql_password
   ```
