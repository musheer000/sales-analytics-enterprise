<div align="center">
  <img src="frontend/assets/logo.png" alt="Sales Analytics Enterprise Logo" width="96" />
  <h1>Sales Analytics Enterprise</h1>
  <p><strong>Full-Stack Global Sales Analytics, Order Management &amp; Real-Time ERP Platform</strong></p>

  <p>
    <a href="https://sales-analytics-erp.netlify.app"><strong>Live Demo</strong></a> •
    <a href="https://sales-analytics-enterprise.onrender.com/swagger-ui.html"><strong>API Docs (Swagger)</strong></a> •
    <a href="https://github.com/musheer000/sales-analytics-enterprise"><strong>GitHub Repo</strong></a>
  </p>
</div>

---

## Overview

**Sales Analytics Enterprise** is a production-deployed, full-stack business intelligence and sales operations platform built with **Spring Boot 3.3.5**, **MySQL 8.0**, and a responsive **glassmorphic JavaScript frontend**. It enables teams to track multi-country revenue in real time, process international and QR payments via **Razorpay**, authenticate with **JWT + 2FA Email OTP**, and generate branded printable/emailable invoices.

---

## Live Architecture

| Layer | Technology | Hosting |
|---|---|---|
| **Frontend** | HTML5, CSS3 (Dark/Light Glassmorphism), Vanilla JS, Chart.js | [Netlify](https://sales-analytics-erp.netlify.app) |
| **Backend API** | Java 17, Spring Boot 3.3.5, Spring Security (JWT + 2FA), SSE | [Render (Docker)](https://sales-analytics-enterprise.onrender.com) |
| **Database** | MySQL 8.0 (`schema.sql`, `sample_data.sql`, `international_data.sql`) | Aiven Cloud MySQL (SSL) |
| **Monitoring** | HTTP(S) 5-minute health checks | UptimeRobot |

---

## Key Features

- **Executive Analytics Dashboard:** Real-time KPIs (Total Revenue, Orders, AOV, Repeat Customer Rate, Customer LTV), Monthly Sales Target quota/pacing tracker, Revenue Trend, Category Mix, and Top Products charts.
- **2FA Email OTP Authentication & RBAC:** JWT stateless security paired with 6-digit `SecureRandom` OTP verification (5-minute TTL, attempt limits, resend cooldown) delivered via Gmail SSL SMTP (port 465).
- **International & QR Payments (Razorpay):** Supports Razorpay International multi-currency checkout as well as **Scan & Pay (QR/UPI)** order flows.
- **Branded Invoice & Receipt Engine:** Printable and email-ready invoices featuring the official brand watermark, circular `"VERIFIED • PAID"` stamp, authorized signatory block, and one-click **Email Invoice**.
- **Real-Time Event Stream (SSE) & Telemetry:** Server-Sent Events (`/api/realtime/stream`) push live order notifications, activity feed updates, and JVM memory/uptime telemetry (`/api/realtime/telemetry`).
- **Multi-Currency & Global Market Coverage:** Live currency switcher (`INR`, `USD`, `EUR`, `GBP`, `AED`, `SAR`, `KWD`, `JPY`, etc.) backed by multi-country customer and order data across 16 countries.
- **Productivity Tools:** Spotlight Command Palette (`Ctrl+K`), CSV Export (`/api/analytics/export/sales-csv`), bulk CSV import, and OpenAPI/Swagger documentation.

---

## Project Structure

```text
├── backend/
│   ├── Dockerfile                 # Multi-stage Maven 3.9.9 + JRE 17 Alpine build
│   ├── pom.xml                    # Spring Boot 3.3.5 dependencies
│   └── src/main/
│       ├── java/com/salesanalytics/
│       │   ├── config/            # SecurityConfig (JWT + RBAC), CORS, Cache
│       │   ├── controller/        # Auth, Sale, Product, Category, Customer, Analytics, Realtime, Integration
│       │   ├── entity/            # AppUser, Sale, SaleItem, Product, Category, Customer
│       │   ├── repository/        # Spring Data JPA repositories
│       │   └── service/           # OtpService, EmailService, RealtimeEventService, business services
│       └── resources/
│           └── application.properties
├── database/
│   ├── schema.sql                 # MySQL 8.0 schema
│   ├── sample_data.sql            # Core catalog, customers & orders
│   └── international_data.sql     # 16-country international dataset
└── frontend/
    ├── assets/logo.png            # Official brand logo
    ├── css/styles.css             # Glassmorphic theme, print styles, responsive grids
    ├── js/api.js                  # Auto-switching local/cloud REST API client
    ├── js/app.js                  # Dashboard, SSE stream, Razorpay, 2FA OTP, Ctrl+K
    ├── index.html                 # SPA entry point
    └── netlify.toml               # Netlify publish & security headers
```

---

## Local Development Setup

### Prerequisites
- Java 17+ & Maven 3.9+
- MySQL 8.0+

### 1. Database Setup
```sql
SOURCE database/schema.sql;
SOURCE database/sample_data.sql;
SOURCE database/international_data.sql;
```

### 2. Run Backend
Set environment variables (or configure `backend/src/main/resources/application.properties`):
```bash
export DB_URL="jdbc:mysql://localhost:3306/sales_analytics?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export DB_USER="root"
export DB_PASSWORD="your_mysql_password"
export MAIL_USERNAME="your_gmail@gmail.com"
export MAIL_PASSWORD="your_gmail_app_password"
export RAZORPAY_KEY_ID="your_razorpay_key_id"

cd backend
mvn spring-boot:run
```

### 3. Run Frontend
Serve `frontend/` on `http://localhost:3000` (e.g., `npx serve frontend -l 3000` or Nginx/Docker). `frontend/js/api.js` automatically detects `localhost` and routes API calls to `http://localhost:8080/api`.

---

## Author
**Shaik Musheer**  
GitHub: [@musheer000](https://github.com/musheer000)
