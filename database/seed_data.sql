-- =============================================================================
-- Seed Data for Sales Analytics & Reporting System
-- Database: sales_analytics
-- =============================================================================

USE sales_analytics;

-- Categories
INSERT INTO categories (id, name, description) VALUES
(1, 'Electronics', 'Consumer electronics, displays, computing accessories and audio devices'),
(2, 'Home & Kitchen', 'Home appliances, cookware, electric makers and interior lifestyle goods'),
(3, 'Office & Stationery', 'Ergonomic office furniture, desk accessories, and stationary essentials'),
(4, 'Apparel & Fashion', 'Activewear, jackets, everyday comfort clothes, and accessories'),
(5, 'Fitness & Sports', 'Workout equipment, dumbbells, resistance gear, and outdoor accessories')
ON DUPLICATE KEY UPDATE name=VALUES(name), description=VALUES(description);

-- Products
INSERT INTO products (id, name, sku, price, stock_quantity, category_id, description) VALUES
(1, 'Ultra HD 4K Monitor 27"', 'ELEC-MON-4K', 349.99, 45, 1, 'IPS panel with 144Hz refresh rate, HDR400 and USB-C hub'),
(2, 'Wireless Noise-Canceling Headphones', 'ELEC-HEAD-NC', 199.50, 80, 1, 'Active noise cancellation with 40h battery and multipoint Bluetooth'),
(3, 'Mechanical Gaming Keyboard', 'ELEC-KEY-MEC', 89.99, 120, 1, 'Hot-swappable tactile switches with customizable RGB lighting'),
(4, 'Smart Home Air Purifier', 'HOME-AIR-01', 129.00, 60, 2, 'True HEPA air filter with real-time PM2.5 display and app control'),
(5, 'Espresso Coffee Machine', 'HOME-COFF-02', 249.00, 35, 2, '15-bar Italian pressure pump with milk frother wand'),
(6, 'Ergonomic Mesh Office Chair', 'OFF-CHAIR-ERG', 189.99, 50, 3, 'Breathable mesh chair with 3D lumbar support and headrest'),
(7, 'Electric Standing Desk 55"', 'OFF-DESK-STD', 399.00, 25, 3, 'Dual-motor motorized standing desk with 4 memory height presets'),
(8, 'Breathable Running Jacket', 'APP-JKT-RUN', 69.95, 90, 4, 'Reflective wind-resistant thermal sports jacket'),
(9, 'Adjustable Dumbbell Set (50 lbs)', 'FIT-DUMB-50', 179.00, 40, 5, 'Quick-adjust selector dial dumbbell set with storage tray')
ON DUPLICATE KEY UPDATE price=VALUES(price), stock_quantity=VALUES(stock_quantity);

-- Customers
INSERT INTO customers (id, first_name, last_name, email, phone, city, country) VALUES
(1, 'Alexander', 'Wright', 'alex.wright@example.com', '+1-555-0101', 'San Francisco', 'USA'),
(2, 'Sophia', 'Chen', 'sophia.chen@example.com', '+1-555-0102', 'Seattle', 'USA'),
(3, 'Marcus', 'Johnson', 'marcus.j@example.com', '+1-555-0103', 'Austin', 'USA'),
(4, 'Elena', 'Rostova', 'elena.rostova@example.com', '+44-20-7946-0104', 'London', 'UK'),
(5, 'Liam', 'Miller', 'liam.miller@example.com', '+1-555-0105', 'Toronto', 'Canada'),
(6, 'Priya', 'Patel', 'priya.patel@example.com', '+91-98765-43210', 'Bangalore', 'India')
ON DUPLICATE KEY UPDATE email=VALUES(email);

-- Sales / Orders
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status, notes) VALUES
(1, 'ORD-2026-1001', 1, '2026-10-15 10:30:00', 549.49, 'CREDIT_CARD', 'COMPLETED', 'Standard ground shipping'),
(2, 'ORD-2026-1002', 2, '2026-10-22 14:15:00', 399.00, 'PAYPAL', 'COMPLETED', 'Express priority delivery'),
(3, 'ORD-2026-1101', 3, '2026-11-02 09:45:00', 438.99, 'CREDIT_CARD', 'COMPLETED', 'Corporate workstation setup'),
(4, 'ORD-2026-1102', 4, '2026-11-10 16:20:00', 249.00, 'BANK_TRANSFER', 'COMPLETED', 'Gift package requested'),
(5, 'ORD-2026-1103', 5, '2026-11-18 11:10:00', 529.49, 'CREDIT_CARD', 'COMPLETED', 'Customer portal order'),
(6, 'ORD-2026-1104', 6, '2026-11-21 13:50:00', 159.94, 'UPI', 'COMPLETED', 'Mobile checkout')
ON DUPLICATE KEY UPDATE total_amount=VALUES(total_amount);

-- Sale Items
INSERT INTO sale_items (id, sale_id, product_id, quantity, unit_price, subtotal) VALUES
(1, 1, 1, 1, 349.99, 349.99),
(2, 1, 2, 1, 199.50, 199.50),
(3, 2, 7, 1, 399.00, 399.00),
(4, 3, 6, 1, 189.99, 189.99),
(5, 3, 5, 1, 249.00, 249.00),
(6, 4, 5, 1, 249.00, 249.00),
(7, 5, 1, 1, 349.99, 349.99),
(8, 5, 9, 1, 179.50, 179.50),
(9, 6, 3, 1, 89.99, 89.99),
(10, 6, 8, 1, 69.95, 69.95)
ON DUPLICATE KEY UPDATE subtotal=VALUES(subtotal);
