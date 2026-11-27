<div align="center">
  <img src="frontend/assets/logo.png" alt="Sales Analytics Enterprise Logo" width="96" />
  <h1>Sales Analytics Enterprise — Global B2B Wholesale &amp; ERP</h1>
  <p><strong>Full-Stack Global B2B Wholesale, Multi-Country E-Commerce, Direct Seller Negotiation &amp; Real-Time Sales Analytics Platform</strong></p>

  <p>
    <a href="https://sales-analytics-erp.netlify.app"><strong>Admin &amp; Owner Portal</strong></a> •
    <a href="https://sales-analytics-erp.netlify.app/user.html"><strong>Customer &amp; B2B Store Portal</strong></a> •
    <a href="https://sales-analytics-enterprise.onrender.com/swagger-ui.html"><strong>Swagger API Docs</strong></a> •
    <a href="https://github.com/musheer000/sales-analytics-enterprise"><strong>GitHub Repo</strong></a>
  </p>

  <p>
    <img src="https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot 3.3.5" />
    <img src="https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java 17" />
    <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white" alt="MySQL 8.0" />
    <img src="https://img.shields.io/badge/Frontend-Mobile%20%2B%20Desktop%20Web-007AFF?style=flat-square" alt="Responsive Web & Mobile" />
    <img src="https://img.shields.io/badge/Auth-JWT%20%2B%202FA%20OTP-AF52DE?style=flat-square" alt="JWT + 2FA OTP" />
  </p>
</div>

---

## Overview

**Sales Analytics Enterprise** is a production-deployed, full-stack **Global B2B Wholesale & Retail Commerce + Business Intelligence ERP** built with **Spring Boot 3.3.5**, **MySQL 8.0**, and a responsive **glassmorphic web frontend** designed for **Mobile, Tablet, and Desktop PC**.

Beyond standard e-commerce, it supports **bulk wholesale ordering from any country**, **private 1-on-1 Buyer ↔ Seller direct chat** (to discuss stock availability, bulk quantities, quality specs, and negotiable pricing), **Request-for-Quote (RFQ) price negotiation**, **seller product submissions with Admin Accept/Reject approval**, and **Regional & Buyer Behavior Analytics** showing which countries, regions, and products drive the most sales.

---

## Live Architecture

| Layer | Technology | Hosting |
|---|---|---|
| **Admin & Customer Portals** | HTML5, CSS3 (Dark/Light Glassmorphism), Vanilla JS, Chart.js | [Netlify](https://sales-analytics-erp.netlify.app) |
| **Backend API** | Java 17, Spring Boot 3.3.5, Spring Security (JWT + 2FA), SSE | [Render (Docker)](https://sales-analytics-enterprise.onrender.com) |
| **Database** | MySQL 8.0 (`schema.sql`, `sample_data.sql`, `international_data.sql`) | Aiven Cloud MySQL (SSL) |
| **2FA Email Delivery** | HTTPS Google Apps Script Webhook / Brevo API / STARTTLS SMTP | Google Workspace / Brevo |
| **Payments** | Razorpay International (Card/UPI/NetBanking) + Scan & Pay QR + COD | Razorpay |

---

## Modular Domain Architecture

The backend is organized into clean, domain-isolated service modules:

1. **Identity & 2FA Security (`/api/auth/**`)**: Stateless JWT authentication, strict **Admin ↔ Customer email & portal separation**, progressive account lockout, and 6-digit `SecureRandom` 2FA OTP email verification.
2. **Catalog & Approval Service (`/api/products/**`, `/api/user/products/**`)**: Supports retail + wholesale pricing tiers, MOQ, unit types (`pcs`, `boxes`, `kg`, `cartons`, `sets`), origin country, and seller listings queued for Admin **Accept / Reject**.
3. **Wholesale & RFQ Negotiation Service (`/api/wholesale/**`)**: Full B2B Request-for-Quote lifecycle (`PENDING` → `COUNTERED` / `ACCEPTED` / `REJECTED` → `ORDERED`) with 1-click conversion of accepted quotes into bulk orders.
4. **Private 1-on-1 Direct Messaging Service (`/api/chat/**`)**: Strict participant-only private Buyer ↔ Seller threads (plus Admin support & Admin B2B oversight) with product context attachments.
5. **Regional & Behavior Analytics Service (`/api/analytics/**`)**: Aggregates combined ERP & Global Portal sales by **Country & Region**, **Wholesale vs. Retail split**, **RFQ conversion rate**, and **Top-Selling Products**.
6. **Real-Time Event Stream (`/api/realtime/**`)**: Server-Sent Events (SSE) for live orders, RFQ updates, product approvals, and chat notifications.

---

## Key Features

### 🌍 Global Wholesale & Multi-Country Marketplace (`user.html`)
- **Retail + Bulk Wholesale Tiers:** Each product displays its retail unit price, discounted **wholesale bulk price** (`qty >= bulkMinQty`), **Minimum Order Quantity (MOQ)**, **Origin Country**, and **Seller**.
- **Order from Any Country:** Checkout includes a global destination country selector, automatic wholesale tier price calculation, and Razorpay / QR / COD payment options.
- **Sell Bulk Products:** Any registered user/supplier can submit wholesale listings (with MOQ, bulk price, unit type, origin country, and quality specs) for Admin review.
- **Mobile + Desktop Flexible UI:** Mobile bottom navigation bar (`Shop`, `Quotes`, `Sell`, `Chat`, `Orders`), collapsible drawer, responsive grids, and split-pane DM chat.

### 💬 Private 1-on-1 Direct Seller Chat & RFQ Negotiation
- **Direct Buyer ↔ Seller Chat:** Clicking **💬 Chat Seller** on any product opens a private 1-on-1 conversation directly with that seller—isolated from other users—to ask about availability, bulk quantities, quality issues, or custom pricing.
- **Structured Bulk Quotes (RFQ):** Buyers can submit formal bulk offers with target unit price, quantity, destination country, and quality/packaging notes; sellers can **Accept**, **Counter-Offer**, or **Reject**.

### 🛡️ Admin & Owner Command Center (`index.html`)
- **Product Approvals (Accept / Reject):** Dedicated **Approvals & Wholesale** tab to review pending seller listings, approve them live to the marketplace, or reject with a reason.
- **Regional & Behavior Analytics:** Interactive charts and tables answering **which region & country sells more**, **which products sell most**, **Wholesale vs. Retail revenue split**, and **RFQ negotiation win rate**.
- **Admin Chat Oversight & Support:** Reply to customer support threads and monitor Buyer ↔ Seller B2B negotiation chats from a unified inbox.
- **Branded Invoices & Exports:** Printable/emailable invoices with watermark and `"VERIFIED • PAID"` seal, CSV/PDF exports, multi-currency switcher (`INR`, `USD`, `AED`, `KWD`, `EUR`, `GBP`, `JPY`, etc.), and `Ctrl+K` Command Palette.

---

## Core API Endpoints

| Module | Endpoint | Description |
|---|---|---|
| **Auth** | `POST /api/auth/login` | Portal-validated login + 2FA OTP dispatch |
| **Auth** | `POST /api/auth/register` | Separate Admin & Customer registration with email/role isolation |
| **Marketplace** | `GET /api/user/products` | Browse approved retail & wholesale products |
| **Marketplace** | `POST /api/user/products/submit` | Submit bulk product listing (pending Admin approval) |
| **Approvals** | `PUT /api/user/admin/products/{id}/approve` | Admin accepts seller product listing |
| **Approvals** | `PUT /api/user/admin/products/{id}/reject` | Admin rejects seller product listing with reason |
| **Wholesale RFQ** | `POST /api/wholesale/quotes` | Submit bulk quote / price negotiation |
| **Wholesale RFQ** | `PUT /api/wholesale/quotes/{id}/respond` | Seller/Admin `ACCEPT`, `COUNTER`, or `REJECT` quote |
| **Wholesale RFQ** | `POST /api/wholesale/quotes/{id}/convert-order` | Convert accepted quote into a wholesale order |
| **Direct Chat** | `GET /api/chat/conversations` | List user's private 1-on-1 conversations |
| **Direct Chat** | `GET /api/chat/dm?peerEmail=...` | Fetch private 1-on-1 DM history with a seller/buyer |
| **Direct Chat** | `POST /api/chat/send` | Send private DM (with optional `productId` attachment) |
| **Analytics** | `GET /api/analytics/regional-sales` | Sales, units & revenue breakdown by Country & Region |
| **Analytics** | `GET /api/analytics/behavior` | Wholesale vs. Retail split, RFQ conversion & top products |

---

## Project Structure

```text
├── backend/
│   ├── Dockerfile                          # Multi-stage Maven 3.9.9 + JRE 17 Alpine build
│   ├── pom.xml                             # Spring Boot 3.3.5 dependencies
│   └── src/main/
│       ├── java/com/salesanalytics/
│       │   ├── config/                     # CacheConfig, CorsConfig
│       │   ├── security/                   # SecurityConfig, JwtService, JwtAuthenticationFilter
│       │   ├── controller/                 # AuthController, UserPortalController, WholesaleController,
│       │   │                               # ChatController, AnalyticsController, ProductController,
│       │   │                               # CategoryController, CustomerController, SaleController,
│       │   │                               # RealtimeController, IntegrationController
│       │   ├── entity/                     # AppUser, Product, Category, Customer, Sale, SaleItem,
│       │   │                               # UserOrder, WholesaleQuote, ProductReview, ChatMessage
│       │   ├── repository/                 # Spring Data JPA repositories (JOIN FETCH & analytics)
│       │   └── service/                    # EmailService, OtpService, RealtimeEventService, impl/*
│       └── resources/
│           └── application.properties
├── database/
│   ├── schema.sql                          # MySQL 8.0 schema
│   ├── sample_data.sql                     # Core catalog, customers & orders
│   └── international_data.sql              # Multi-country international dataset
└── frontend/
    ├── assets/logo.png                     # Official brand logo
    ├── css/styles.css                      # Admin glassmorphic theme, print styles, responsive grids
    ├── js/api.js                           # Auto-switching local/cloud REST API client
    ├── js/app.js                           # Admin dashboard, approvals, regional charts, SSE, Razorpay
    ├── index.html                          # Admin & Owner Portal SPA
    ├── user.html                           # Customer & Global B2B Wholesale Portal (Mobile + PC)
    └── netlify.toml                        # Netlify publish & security headers
```

---

## Local Development Setup

### Prerequisites
- **Java 17+** & **Maven 3.9+** (or Docker)
- **MySQL 8.0+**

### 1. Database Setup
```sql
SOURCE database/schema.sql;
SOURCE database/sample_data.sql;
SOURCE database/international_data.sql;
```

### 2. Run Backend
Configure environment variables (or `backend/src/main/resources/application.properties`):
```bash
export DB_URL="jdbc:mysql://localhost:3306/sales_analytics?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export DB_USER="root"
export DB_PASSWORD="your_mysql_password"
export MAIL_WEBHOOK_URL="your_apps_script_webhook_url"
export BREVO_API_KEY="optional_brevo_api_key"
export RAZORPAY_KEY_ID="your_razorpay_key_id"

cd backend
mvn spring-boot:run
```

### 3. Run Frontend
Serve `frontend/` locally (e.g. `npx serve frontend -l 3000`). `frontend/js/api.js` and `frontend/user.html` automatically detect `localhost` and route API requests to `http://localhost:8080/api`.

- **Admin Portal:** `http://localhost:3000/index.html`
- **Customer & B2B Wholesale Portal:** `http://localhost:3000/user.html`

---

## Author
**Shaik Musheer**  
GitHub: [@musheer000](https://github.com/musheer000)
