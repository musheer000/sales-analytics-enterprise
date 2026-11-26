# 📚 REST API Specification - Sales Analytics & Reporting System

Base URL: `http://localhost:8080/api`  
Swagger UI: `http://localhost:8080/swagger-ui.html`  
OpenAPI JSON: `http://localhost:8080/v3/api-docs`  

---

## 1. Categories (`/api/categories`)

| Method | Endpoint | Description | Status Codes |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/categories` | Retrieve all categories (Cached) | 200 OK |
| `GET` | `/api/categories/{id}` | Retrieve single category by ID (Cached) | 200 OK, 404 Not Found |
| `POST` | `/api/categories` | Create new category (Evicts cache) | 201 Created, 400 Bad Request |
| `PUT` | `/api/categories/{id}` | Update existing category | 200 OK, 400, 404 |
| `DELETE` | `/api/categories/{id}` | Delete category | 200 OK, 404 |

### Sample Category JSON Payload:
```json
{
  "name": "Smart Electronics",
  "description": "Connected home and portable smart gadgets"
}
```

---

## 2. Products (`/api/products`)

| Method | Endpoint | Description | Status Codes |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/products` | Retrieve all products (Supports `?categoryId=1`) | 200 OK |
| `GET` | `/api/products/{id}` | Get product details by ID | 200 OK, 404 |
| `POST` | `/api/products` | Create product (Checks duplicate SKU) | 201 Created, 400 |
| `PUT` | `/api/products/{id}` | Update product | 200 OK, 400, 404 |
| `DELETE` | `/api/products/{id}` | Delete product | 200 OK, 404 |

---

## 3. Sales / Orders (`/api/sales`)

| Method | Endpoint | Description | Status Codes |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/sales` | List all sales orders | 200 OK |
| `GET` | `/api/sales/{id}` | Get order details with line items | 200 OK, 404 |
| `POST` | `/api/sales` | Create new sale (Validates stock quantity) | 201 Created, 400 |
| `DELETE` | `/api/sales/{id}` | Delete sale order | 200 OK, 404 |

### Sample Create Sale Payload:
```json
{
  "customerId": 1,
  "paymentMethod": "CREDIT_CARD",
  "notes": "Express delivery requested",
  "items": [
    { "productId": 1, "quantity": 1 },
    { "productId": 2, "quantity": 2 }
  ]
}
```

---

## 4. Analytics & Reports (`/api/analytics`)

| Method | Endpoint | Description | Status Codes |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/analytics/summary` | Top-level KPIs (Total Revenue, Orders, AOV) | 200 OK |
| `GET` | `/api/analytics/category-sales` | Aggregated revenue and units sold by category | 200 OK |
| `GET` | `/api/analytics/monthly-revenue` | Time-series monthly revenue trends | 200 OK |
| `GET` | `/api/analytics/top-products` | Top selling products sorted by quantity | 200 OK |
