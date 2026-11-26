-- =============================================================================
-- Sales Analytics — Real Sample Data (Indian Electronics & Retail Business)
-- Run: mysql -u root -p77889966 sales_analytics < sample_data.sql
-- =============================================================================

USE sales_analytics;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE sale_items;
TRUNCATE TABLE sales;
TRUNCATE TABLE products;
TRUNCATE TABLE customers;
TRUNCATE TABLE categories;

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 1. CATEGORIES (6)
-- =============================================================================
INSERT INTO categories (id, name, description) VALUES
(1, 'Electronics',         'Smartphones, laptops, TVs, headphones and smart gadgets'),
(2, 'Home & Kitchen',      'Appliances, cookware, vacuum cleaners and home essentials'),
(3, 'Office & Stationery', 'Ergonomic furniture, printers and office accessories'),
(4, 'Apparel & Fashion',   'Shoes, jeans, jackets and everyday fashion wear'),
(5, 'Fitness & Sports',    'Dumbbells, resistance bands, running shoes and gym gear'),
(6, 'Gaming Gear',         'Gaming mice, keyboards, headsets and controllers');

-- =============================================================================
-- 2. PRODUCTS (15)
-- =============================================================================
INSERT INTO products (id, name, sku, price, stock_quantity, category_id, description) VALUES
(1,  'MacBook Air M2 13"',          'SKU-ELEC-001', 900.00,  12, 1, 'Apple MacBook Air with M2 chip, 8GB RAM, 256GB SSD'),
(2,  'iPhone 15 Pro 256GB',         'SKU-ELEC-002', 750.00,  25, 1, 'Apple iPhone 15 Pro with titanium design, 48MP camera'),
(3,  'Samsung 55" 4K Smart TV',     'SKU-ELEC-003', 650.00,   8, 1, 'Samsung Crystal 4K UHD Smart TV with Alexa built-in'),
(4,  'Sony WH-1000XM5 Headphones',  'SKU-ELEC-004', 280.00,  30, 1, 'Industry-leading noise cancelling wireless headphones'),
(5,  'Logitech MX Master 3S Mouse', 'SKU-ELEC-005',  95.00,  40, 1, 'Advanced wireless mouse for professionals, 8K DPI'),
(6,  'Instant Pot Duo 7-in-1',      'SKU-KITC-001',  89.00,  20, 2, '6-quart multi-use pressure cooker, slow cooker, rice cooker'),
(7,  'Dyson V15 Detect Vacuum',     'SKU-KITC-002', 599.00,   6, 2, 'Cordless vacuum with laser dust detection, 60min runtime'),
(8,  'HP LaserJet Pro M428fdw',     'SKU-OFFC-001', 220.00,  15, 3, 'All-in-one laser printer, scanner, copier with WiFi'),
(9,  'ErgoMax Pro Office Chair',    'SKU-OFFC-002', 299.00,  10, 3, 'Ergonomic mesh chair with lumbar support, armrests'),
(10, 'Nike Air Max 2024 Sneakers',  'SKU-APRL-001', 120.00,  35, 4, 'Premium running sneakers with Air Max cushioning'),
(11, "Levi's 511 Slim Fit Jeans",   'SKU-APRL-002',  55.00,  50, 4, "Classic slim fit jeans in stretch denim, multiple colours"),
(12, 'Adidas Ultraboost 23 Shoes',  'SKU-FITS-001', 140.00,  28, 5, 'High-performance running shoes with Boost midsole technology'),
(13, 'Bowflex SelectTech Dumbbells','SKU-FITS-002', 299.00,   7, 5, 'Adjustable dumbbells 5-52.5 lbs, replaces 15 sets of weights'),
(14, 'ASUS ROG Gladius III Mouse',  'SKU-GAME-001',  65.00,  45, 6, 'Wired gaming mouse, 36000 DPI, swappable switches'),
(15, 'Corsair K95 RGB Keyboard',    'SKU-GAME-002',  89.00,  22, 6, 'Mechanical gaming keyboard, Cherry MX switches, per-key RGB');

-- =============================================================================
-- 3. CUSTOMERS (10)
-- =============================================================================
INSERT INTO customers (id, first_name, last_name, email, phone, city, country) VALUES
(1,  'Rahul',   'Sharma',  'rahul.sharma@gmail.com',   '+91 98201 11234', 'Mumbai',    'India'),
(2,  'Priya',   'Patel',   'priya.patel@gmail.com',    '+91 97301 22345', 'Delhi',     'India'),
(3,  'Amit',    'Kumar',   'amit.kumar@gmail.com',     '+91 96401 33456', 'Bangalore', 'India'),
(4,  'Sneha',   'Reddy',   'sneha.reddy@gmail.com',    '+91 95501 44567', 'Hyderabad', 'India'),
(5,  'Vikram',  'Singh',   'vikram.singh@gmail.com',   '+91 94601 55678', 'Chennai',   'India'),
(6,  'Anjali',  'Gupta',   'anjali.gupta@gmail.com',   '+91 93701 66789', 'Pune',      'India'),
(7,  'Rohit',   'Verma',   'rohit.verma@gmail.com',    '+91 92801 77890', 'Kolkata',   'India'),
(8,  'Deepika', 'Nair',    'deepika.nair@gmail.com',   '+91 91901 88901', 'Ahmedabad', 'India'),
(9,  'Arjun',   'Mehta',   'arjun.mehta@gmail.com',    '+91 90001 99012', 'Jaipur',    'India'),
(10, 'Kavya',   'Iyer',    'kavya.iyer@gmail.com',     '+91 89101 00123', 'Kochi',     'India');

-- =============================================================================
-- 4. SALES + SALE_ITEMS (20 orders across Sep, Oct, Nov 2026)
-- =============================================================================

-- ── NOVEMBER 2026 (Sales 1–12) ──────────────────────────────────────────────

-- Sale 1: Rahul — MacBook Air + iPhone 15 Pro
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status) VALUES
(1, 'ORD-2026-001', 1, '2026-11-01 10:30:00', 1650.00, 'CREDIT_CARD', 'COMPLETED');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(1, 1, 1, 900.00, 900.00),
(1, 2, 1, 750.00, 750.00);

-- Sale 2: Priya — Dyson V15 Vacuum
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status) VALUES
(2, 'ORD-2026-002', 2, '2026-11-03 14:15:00', 599.00, 'UPI', 'COMPLETED');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(2, 7, 1, 599.00, 599.00);

-- Sale 3: Amit — ROG Mouse x2 + Corsair Keyboard
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status) VALUES
(3, 'ORD-2026-003', 3, '2026-11-05 09:45:00', 219.00, 'BANK_TRANSFER', 'COMPLETED');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(3, 14, 2, 65.00, 130.00),
(3, 15, 1, 89.00,  89.00);

-- Sale 4: Sneha — Nike Air Max + Levi's Jeans x2
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status) VALUES
(4, 'ORD-2026-004', 4, '2026-11-07 16:00:00', 230.00, 'CREDIT_CARD', 'COMPLETED');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(4, 10, 1, 120.00, 120.00),
(4, 11, 2,  55.00, 110.00);

-- Sale 5: Vikram — Samsung 55" TV
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status) VALUES
(5, 'ORD-2026-005', 5, '2026-11-08 11:20:00', 650.00, 'CREDIT_CARD', 'COMPLETED');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(5, 3, 1, 650.00, 650.00);

-- Sale 6: Anjali — Instant Pot + ErgoMax Chair
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status) VALUES
(6, 'ORD-2026-006', 6, '2026-11-10 13:00:00', 388.00, 'UPI', 'COMPLETED');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(6, 6, 1,  89.00,  89.00),
(6, 9, 1, 299.00, 299.00);

-- Sale 7: Rohit — Bowflex Dumbbells + Adidas Shoes
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status) VALUES
(7, 'ORD-2026-007', 7, '2026-11-12 15:30:00', 439.00, 'BANK_TRANSFER', 'COMPLETED');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(7, 13, 1, 299.00, 299.00),
(7, 12, 1, 140.00, 140.00);

-- Sale 8: Deepika — Sony Headphones + Logitech Mouse
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status) VALUES
(8, 'ORD-2026-008', 8, '2026-11-14 10:10:00', 375.00, 'CREDIT_CARD', 'COMPLETED');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(8, 4, 1, 280.00, 280.00),
(8, 5, 1,  95.00,  95.00);

-- Sale 9: Arjun — HP Printer + ErgoMax Chair
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status) VALUES
(9, 'ORD-2026-009', 9, '2026-11-16 09:00:00', 519.00, 'BANK_TRANSFER', 'COMPLETED');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(9, 8, 1, 220.00, 220.00),
(9, 9, 1, 299.00, 299.00);

-- Sale 10: Kavya — iPhone 15 Pro
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status) VALUES
(10, 'ORD-2026-010', 10, '2026-11-18 17:45:00', 750.00, 'CREDIT_CARD', 'COMPLETED');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(10, 2, 1, 750.00, 750.00);

-- Sale 11: Rahul (repeat) — Corsair Keyboard + ROG Mouse
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status) VALUES
(11, 'ORD-2026-011', 1, '2026-11-19 12:30:00', 154.00, 'UPI', 'COMPLETED');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(11, 15, 1, 89.00, 89.00),
(11, 14, 1, 65.00, 65.00);

-- Sale 12: Priya (repeat) — Bowflex Dumbbells
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status) VALUES
(12, 'ORD-2026-012', 2, '2026-11-20 14:00:00', 299.00, 'CREDIT_CARD', 'COMPLETED');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(12, 13, 1, 299.00, 299.00);

-- ── OCTOBER 2026 (Sales 13–17) ───────────────────────────────────────────────

-- Sale 13: Amit — MacBook Air
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status) VALUES
(13, 'ORD-2026-013', 3, '2026-10-05 10:00:00', 900.00, 'BANK_TRANSFER', 'COMPLETED');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(13, 1, 1, 900.00, 900.00);

-- Sale 14: Sneha — Samsung TV + Dyson Vacuum
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status) VALUES
(14, 'ORD-2026-014', 4, '2026-10-12 15:00:00', 1249.00, 'CREDIT_CARD', 'COMPLETED');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(14, 3, 1, 650.00, 650.00),
(14, 7, 1, 599.00, 599.00);

-- Sale 15: Vikram — Adidas Shoes x2 + Nike Air Max
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status) VALUES
(15, 'ORD-2026-015', 5, '2026-10-18 11:30:00', 400.00, 'UPI', 'COMPLETED');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(15, 12, 2, 140.00, 280.00),
(15, 10, 1, 120.00, 120.00);

-- Sale 16: Anjali — Instant Pot x2
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status) VALUES
(16, 'ORD-2026-016', 6, '2026-10-22 09:15:00', 178.00, 'UPI', 'COMPLETED');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(16, 6, 2, 89.00, 178.00);

-- Sale 17: Rohit — iPhone 15 Pro
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status) VALUES
(17, 'ORD-2026-017', 7, '2026-10-25 16:45:00', 750.00, 'CREDIT_CARD', 'COMPLETED');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(17, 2, 1, 750.00, 750.00);

-- ── SEPTEMBER 2026 (Sales 18–20) ─────────────────────────────────────────────

-- Sale 18: Deepika — ErgoMax Chair + HP Printer
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status) VALUES
(18, 'ORD-2026-018', 8, '2026-09-10 10:00:00', 519.00, 'BANK_TRANSFER', 'COMPLETED');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(18, 9, 1, 299.00, 299.00),
(18, 8, 1, 220.00, 220.00);

-- Sale 19: Arjun — MacBook Air
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status) VALUES
(19, 'ORD-2026-019', 9, '2026-09-15 14:30:00', 900.00, 'CREDIT_CARD', 'COMPLETED');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(19, 1, 1, 900.00, 900.00);

-- Sale 20: Kavya — Sony Headphones + Logitech Mouse + Corsair Keyboard
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status) VALUES
(20, 'ORD-2026-020', 10, '2026-09-20 11:00:00', 464.00, 'CREDIT_CARD', 'COMPLETED');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(20, 4, 1, 280.00, 280.00),
(20, 5, 1,  95.00,  95.00),
(20, 15, 1,  89.00,  89.00);

-- =============================================================================
-- VERIFY
-- =============================================================================
SELECT 'categories' AS tbl, COUNT(*) AS total FROM categories
UNION ALL SELECT 'products',   COUNT(*) FROM products
UNION ALL SELECT 'customers',  COUNT(*) FROM customers
UNION ALL SELECT 'sales',      COUNT(*) FROM sales
UNION ALL SELECT 'sale_items', COUNT(*) FROM sale_items;

SELECT CONCAT('Total Revenue: $', FORMAT(SUM(total_amount),2)) AS summary FROM sales;
