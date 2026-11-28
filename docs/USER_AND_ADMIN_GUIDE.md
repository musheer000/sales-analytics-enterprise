# Global B2B Wholesale & Retail Marketplace — Complete Role & User Guide

Welcome to the **Sales Analytics & Global B2B Wholesale Marketplace** platform.
This system has **two connected portals** sharing one live MySQL database and Spring Boot backend:

| Portal | URL | Who Uses It |
|---|---|---|
| **Admin / Owner Portal** | https://sales-analytics-erp.netlify.app | **Platform Owner / Admin** (`shaikmusheer07@gmail.com`) |
| **Customer & Seller Store Portal** | https://sales-analytics-erp.netlify.app/user.html | **Buyers & Wholesale Sellers** (worldwide accounts) |

---

## 1. Admin / Owner Role — What the Admin Can Do

The Admin is the **marketplace owner, approver, and analytics controller**.

| Menu Section | What It Shows & What Admin Can Do |
|---|---|
| **Dashboard** | Live KPIs: Total Revenue, Total Orders, Avg Order Value, Repeat Customer Rate, Customer LTV, Inventory Count, Monthly Sales Target, and Revenue Trend chart. |
| **Categories** | View, add, edit, or delete product categories (7 global agri-food & bulk categories are auto-seeded: Vegetables, Fruits, Spices, Grains/Pulses, Food Items, Textiles, Industrial). |
| **Products Catalog** | Add new products with **image upload/URL**, retail price, wholesale price, **MOQ**, bulk tier qty, unit (`kg`, `pcs`, `boxes`, `cartons`, `tons`), origin country, grade/certification specs, packaging info, shelf life, and negotiable toggle. Products added by Admin are **immediately `APPROVED`**. |
| **Sales & Orders** | Master order ledger. Shows **all** ERP sales AND **synced Customer Store orders** (`UO-...`), with `+ New Sale Order` for manual offline/B2B orders. |
| **Customers** | Master customer directory with order counts, Lifetime Value (LTV), country, and CSV export. Store buyers are **auto-added** here when they place an order. |
| **Advanced Reports** | Regional & Country Sales breakdown, Wholesale vs Retail behavior analytics, Top Selling Products, Category Revenue, and CSV/PDF report exports. |
| **Customer Chat & Store** | **1)** Reply to customer support messages. **2)** Monitor **B2B Buyer ↔ Seller negotiation threads** (full oversight). **3)** View **Self-Service Store Orders** and update order lifecycle status: `PENDING` → `PAID` → `APPROVED` → `ORDERED` → `SHIPPED` → `DELIVERED` (or `CANCELLED`). |
| **Approvals & Wholesale** | **1)** **Accept or Reject** products submitted by third-party sellers before they appear in the public store. **2)** Monitor all global **Wholesale RFQ Negotiations**. **3)** **Clear All Sample Data** button (with confirmation) to reset demo records. |
| **User Store Portal ↗** | Quick link to open the Customer Portal in a new tab. |

---

## 2. Customer / Buyer / Seller Role — What Customers Can Do

Any registered user on the Customer Portal can **buy single retail units**, **buy bulk wholesale lots**, **negotiate prices**, **chat directly with product owners**, and **submit their own bulk products to sell** (subject to Admin approval).

| Menu Section | What It Shows & What Customer Can Do |
|---|---|
| **Browse & Wholesale** | Search by name/seller/country, filter by category or origin country, sort by Trending / Price / Name. View product photos, MOQ, Origin Country, Retail vs Bulk price, Grade/Packaging/Shelf Life specs, **save private business notes** (`📝`), **Buy / Bulk Order**, **💬 Chat directly with that product's Owner**, or **🤝 Request Bulk Quote (RFQ)**. |
| **Negotiations & RFQ** | **My Sent Quotes (Buyer):** Track bulk price quotes you sent, accept seller counter-offers, and convert accepted quotes into orders.<br>**Received Quotes (Seller):** If you list products to sell, accept, reject, or counter-offer buyer RFQs here. |
| **Sell Bulk / Listings** | Submit your own wholesale/agri-food product with photo, MOQ, wholesale price, grade, packaging, and shelf life. Status starts as **`PENDING`** until the **Admin approves** it in *Approvals & Wholesale*. |
| **My Orders** | Track every order placed (`PENDING` → `PAID` → `APPROVED` → `ORDERED` → `SHIPPED` → `DELIVERED`) with shipping country, payment method, and order reference (`UO-...`). |
| **Direct Messages** | Private **1-on-1 chat** with specific product owners/sellers (ask about stock, quality, samples, or negotiable pricing) or Admin Support. Other buyers cannot see your private thread. |
| **My Dashboard** | Personal summary of total orders, paid/approved orders, total spent, and recent orders. |
| **My Profile** | Update your name, phone, default shipping address, or change password. |

---

## 3. End-to-End Business Workflows

### A. Buying Single or Bulk Products (with Dynamic 3-Tier Pricing)
1. Customer clicks **Buy / Bulk** on any approved product.
2. Backend automatically calculates tiered pricing:
   - `Qty < Bulk Min` → Retail price
   - `Qty >= Bulk Min` → Wholesale Tier 1 price
   - `Qty >= 50` → Wholesale Tier 2 (extra **5% off**)
   - `Qty >= 200` → Wholesale Tier 3 Container Rate (extra **10% off**)
3. Customer chooses shipping country (**all countries allowed worldwide**, except direct **India ↔ Pakistan** trade is blocked by compliance policy).
4. Customer pays via **UPI QR Code Scanner**, **Cards / Razorpay**, or **Cash on Delivery (COD)**.
5. Order appears in **Customer → My Orders**, **Admin → Customer Chat & Store**, and **Admin → Sales & Orders**.
6. Admin approves and updates status (`APPROVED` → `ORDERED` → `SHIPPED` → `DELIVERED`), which updates live on the Customer's dashboard.

### B. Direct Buyer ↔ Seller Price Negotiation (RFQ)
1. Customer clicks **🤝 Quote** on a product, enters desired bulk quantity & target unit price, and submits.
2. Seller (or Admin for platform-owned products) sees it under **Received Quotes** / **Approvals & Wholesale** and can **Accept**, **Counter-Offer**, or **Reject**.
3. Once **Accepted**, the Customer clicks **Convert to Order** to lock in the negotiated price and pay.

### C. Security & Authentication
- **Registration:** Requires email **OTP verification** (5-minute expiry).
- **Fast Login:** Correct password signs in **immediately without OTP** (`otpRequired: false`).
- **Brute-Force Protection:** 5 failed password attempts trigger progressive account lockout (10m → 20m → 40m).
- **Portal Isolation:** Admin accounts sign in on the Admin Portal; Customer accounts sign in on the Customer Portal.
