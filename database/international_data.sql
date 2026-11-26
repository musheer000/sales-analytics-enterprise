-- =============================================================================
-- Sales Analytics — International Customers & Global Sales Data
-- Adds 20 customers from USA, Pakistan, China, Europe, UAE, Australia,
-- South America, Africa, Japan, South Korea, Saudi Arabia, Kuwait
-- Run: Get-Content "D:\Project\database\international_data.sql" | & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p77889966 sales_analytics
-- =============================================================================

USE sales_analytics;

-- =============================================================================
-- INTERNATIONAL CUSTOMERS (IDs 11–30)
-- =============================================================================
INSERT INTO customers (id, first_name, last_name, email, phone, city, country) VALUES

-- 🇺🇸 USA
(11, 'John',      'Smith',        'john.smith@gmail.com',         '+1 212 555 1234', 'New York',       'USA'),
(12, 'Emily',     'Johnson',      'emily.johnson@gmail.com',       '+1 310 555 5678', 'Los Angeles',    'USA'),

-- 🇵🇰 Pakistan
(13, 'Muhammad',  'Ali',          'muhammad.ali@gmail.com',        '+92 300 1234567', 'Karachi',        'Pakistan'),
(14, 'Fatima',    'Khan',         'fatima.khan@gmail.com',         '+92 321 7654321', 'Lahore',         'Pakistan'),

-- 🇨🇳 China
(15, 'Li',        'Wei',          'li.wei@gmail.com',              '+86 139 0013 1234','Beijing',        'China'),
(16, 'Zhang',     'Ming',         'zhang.ming@gmail.com',          '+86 158 0021 5678','Shanghai',       'China'),

-- 🇫🇷 France / 🇩🇪 Germany (Europe)
(17, 'Marie',     'Dubois',       'marie.dubois@gmail.com',        '+33 6 12 34 56 78','Paris',          'France'),
(18, 'Hans',      'Mueller',      'hans.mueller@gmail.com',        '+49 170 1234567', 'Berlin',          'Germany'),

-- 🇦🇪 UAE / Arab
(19, 'Ahmed',     'Al-Rashid',    'ahmed.alrashid@gmail.com',      '+971 50 123 4567', 'Dubai',         'UAE'),
(20, 'Fatima',    'Al-Maktoum',   'fatima.almaktoum@gmail.com',    '+971 55 987 6543', 'Abu Dhabi',     'UAE'),

-- 🇸🇦 Saudi Arabia
(21, 'Mohammed',  'Al-Hassan',    'mohammed.hassan@gmail.com',     '+966 50 111 2222', 'Riyadh',        'Saudi Arabia'),

-- 🇰🇼 Kuwait
(22, 'Layla',     'Al-Farsi',     'layla.alfarsi@gmail.com',       '+965 9999 8888',   'Kuwait City',   'Kuwait'),

-- 🇦🇺 Australia
(23, 'James',     'Wilson',       'james.wilson@gmail.com',        '+61 400 123 456',  'Sydney',        'Australia'),
(24, 'Sarah',     'Thompson',     'sarah.thompson@gmail.com',      '+61 412 987 654',  'Melbourne',     'Australia'),

-- 🇧🇷 Brazil / 🇦🇷 Argentina (South America)
(25, 'Carlos',    'Rodriguez',    'carlos.rodriguez@gmail.com',    '+55 11 99999 1234','Sao Paulo',     'Brazil'),
(26, 'Isabella',  'Fernandez',    'isabella.fernandez@gmail.com',  '+54 11 1234 5678', 'Buenos Aires',  'Argentina'),

-- 🇬🇭 Ghana / 🇳🇬 Nigeria (Africa)
(27, 'Kwame',     'Asante',       'kwame.asante@gmail.com',        '+233 20 123 4567', 'Accra',         'Ghana'),
(28, 'Amara',     'Diallo',       'amara.diallo@gmail.com',        '+234 802 345 6789','Lagos',         'Nigeria'),

-- 🇯🇵 Japan / 🇰🇷 South Korea
(29, 'Yuki',      'Tanaka',       'yuki.tanaka@gmail.com',         '+81 90 1234 5678', 'Tokyo',         'Japan'),
(30, 'Park',      'Ji-ho',        'park.jiho@gmail.com',           '+82 10 9876 5432', 'Seoul',         'South Korea');


-- =============================================================================
-- INTERNATIONAL SALES — NOVEMBER 2026 (Sales 21–40)
-- =============================================================================

-- Sale 21: John Smith (USA) — MacBook Air + Logitech Mouse
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status, notes) VALUES
(21, 'ORD-2026-021', 11, '2026-11-21 08:30:00', 995.00, 'CREDIT_CARD', 'COMPLETED', 'Shipped to New York');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(21, 1, 1, 900.00, 900.00),
(21, 5, 1,  95.00,  95.00);

-- Sale 22: Emily Johnson (USA) — iPhone 15 Pro + Sony Headphones
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status, notes) VALUES
(22, 'ORD-2026-022', 12, '2026-11-21 14:00:00', 1030.00, 'CREDIT_CARD', 'COMPLETED', 'Shipped to Los Angeles');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(22, 2, 1, 750.00, 750.00),
(22, 4, 1, 280.00, 280.00);

-- Sale 23: Muhammad Ali (Pakistan) — Samsung TV + ErgoMax Chair
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status, notes) VALUES
(23, 'ORD-2026-023', 13, '2026-11-20 10:00:00', 949.00, 'BANK_TRANSFER', 'COMPLETED', 'Karachi delivery');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(23, 3, 1, 650.00, 650.00),
(23, 9, 1, 299.00, 299.00);

-- Sale 24: Fatima Khan (Pakistan) — Adidas Shoes + Nike Air Max
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status, notes) VALUES
(24, 'ORD-2026-024', 14, '2026-11-20 15:30:00', 260.00, 'CREDIT_CARD', 'COMPLETED', 'Lahore store pickup');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(24, 12, 1, 140.00, 140.00),
(24, 10, 1, 120.00, 120.00);

-- Sale 25: Li Wei (China) — MacBook Air + Corsair Keyboard
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status, notes) VALUES
(25, 'ORD-2026-025', 15, '2026-11-19 09:15:00', 989.00, 'BANK_TRANSFER', 'COMPLETED', 'Beijing delivery');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(25, 1, 1, 900.00, 900.00),
(25, 15, 1,  89.00,  89.00);

-- Sale 26: Zhang Ming (China) — Bowflex Dumbbells + ROG Mouse
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status, notes) VALUES
(26, 'ORD-2026-026', 16, '2026-11-19 16:00:00', 364.00, 'CREDIT_CARD', 'COMPLETED', 'Shanghai express');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(26, 13, 1, 299.00, 299.00),
(26, 14, 1,  65.00,  65.00);

-- Sale 27: Marie Dubois (France) — iPhone 15 Pro + Logitech Mouse
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status, notes) VALUES
(27, 'ORD-2026-027', 17, '2026-11-18 11:00:00', 845.00, 'CREDIT_CARD', 'COMPLETED', 'Paris international');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(27, 2, 1, 750.00, 750.00),
(27, 5, 1,  95.00,  95.00);

-- Sale 28: Hans Mueller (Germany) — Samsung TV + Sony Headphones
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status, notes) VALUES
(28, 'ORD-2026-028', 18, '2026-11-17 13:30:00', 930.00, 'BANK_TRANSFER', 'COMPLETED', 'Berlin corporate order');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(28, 3, 1, 650.00, 650.00),
(28, 4, 1, 280.00, 280.00);

-- Sale 29: Ahmed Al-Rashid (UAE) — MacBook Air + Sony Headphones
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status, notes) VALUES
(29, 'ORD-2026-029', 19, '2026-11-17 16:45:00', 1180.00, 'CREDIT_CARD', 'COMPLETED', 'Dubai VIP delivery');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(29, 1, 1, 900.00, 900.00),
(29, 4, 1, 280.00, 280.00);

-- Sale 30: Fatima Al-Maktoum (UAE) — Dyson Vacuum + Instant Pot
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status, notes) VALUES
(30, 'ORD-2026-030', 20, '2026-11-16 10:00:00', 688.00, 'CREDIT_CARD', 'COMPLETED', 'Abu Dhabi delivery');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(30, 7, 1, 599.00, 599.00),
(30, 6, 1,  89.00,  89.00);

-- Sale 31: Mohammed Al-Hassan (Saudi Arabia) — MacBook Air + iPhone 15 Pro
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status, notes) VALUES
(31, 'ORD-2026-031', 21, '2026-11-15 12:00:00', 1650.00, 'CREDIT_CARD', 'COMPLETED', 'Riyadh VIP');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(31, 1, 1, 900.00, 900.00),
(31, 2, 1, 750.00, 750.00);

-- Sale 32: Layla Al-Farsi (Kuwait) — Dyson Vacuum + ErgoMax Chair
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status, notes) VALUES
(32, 'ORD-2026-032', 22, '2026-11-15 15:30:00', 898.00, 'CREDIT_CARD', 'COMPLETED', 'Kuwait City express');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(32, 7, 1, 599.00, 599.00),
(32, 9, 1, 299.00, 299.00);

-- Sale 33: James Wilson (Australia) — Bowflex + Adidas Shoes
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status, notes) VALUES
(33, 'ORD-2026-033', 23, '2026-11-14 08:00:00', 439.00, 'BANK_TRANSFER', 'COMPLETED', 'Sydney delivery');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(33, 13, 1, 299.00, 299.00),
(33, 12, 1, 140.00, 140.00);

-- Sale 34: Sarah Thompson (Australia) — iPhone 15 Pro
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status, notes) VALUES
(34, 'ORD-2026-034', 24, '2026-11-13 14:00:00', 750.00, 'CREDIT_CARD', 'COMPLETED', 'Melbourne pickup');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(34, 2, 1, 750.00, 750.00);

-- Sale 35: Carlos Rodriguez (Brazil) — Samsung TV
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status, notes) VALUES
(35, 'ORD-2026-035', 25, '2026-11-13 10:30:00', 650.00, 'BANK_TRANSFER', 'COMPLETED', 'Sao Paulo delivery');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(35, 3, 1, 650.00, 650.00);

-- Sale 36: Isabella Fernandez (Argentina) — Levi's Jeans x3 + Nike Air Max
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status, notes) VALUES
(36, 'ORD-2026-036', 26, '2026-11-12 16:00:00', 285.00, 'CREDIT_CARD', 'COMPLETED', 'Buenos Aires fashion order');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(36, 11, 3, 55.00, 165.00),
(36, 10, 1, 120.00, 120.00);

-- Sale 37: Kwame Asante (Ghana) — Corsair Keyboard + ROG Mouse
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status, notes) VALUES
(37, 'ORD-2026-037', 27, '2026-11-11 09:00:00', 154.00, 'BANK_TRANSFER', 'COMPLETED', 'Accra gaming setup');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(37, 15, 1, 89.00, 89.00),
(37, 14, 1, 65.00, 65.00);

-- Sale 38: Amara Diallo (Nigeria) — Instant Pot x2 + Logitech Mouse
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status, notes) VALUES
(38, 'ORD-2026-038', 28, '2026-11-10 11:00:00', 273.00, 'BANK_TRANSFER', 'COMPLETED', 'Lagos delivery');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(38, 6, 2, 89.00, 178.00),
(38, 5, 1, 95.00,  95.00);

-- Sale 39: Yuki Tanaka (Japan) — MacBook Air + Logitech Mouse
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status, notes) VALUES
(39, 'ORD-2026-039', 29, '2026-11-09 07:30:00', 995.00, 'CREDIT_CARD', 'COMPLETED', 'Tokyo express');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(39, 1, 1, 900.00, 900.00),
(39, 5, 1,  95.00,  95.00);

-- Sale 40: Park Ji-ho (South Korea) — iPhone 15 Pro + Sony Headphones
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status, notes) VALUES
(40, 'ORD-2026-040', 30, '2026-11-08 09:45:00', 1030.00, 'CREDIT_CARD', 'COMPLETED', 'Seoul tech order');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(40, 2, 1, 750.00, 750.00),
(40, 4, 1, 280.00, 280.00);


-- =============================================================================
-- INTERNATIONAL SALES — OCTOBER 2026 (Sales 41–47)
-- =============================================================================

-- Sale 41: John Smith USA (repeat) — Bowflex Dumbbells
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status) VALUES
(41, 'ORD-2026-041', 11, '2026-10-20 14:00:00', 299.00, 'CREDIT_CARD', 'COMPLETED');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(41, 13, 1, 299.00, 299.00);

-- Sale 42: Li Wei China (repeat) — Samsung TV
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status) VALUES
(42, 'ORD-2026-042', 15, '2026-10-15 10:30:00', 650.00, 'BANK_TRANSFER', 'COMPLETED');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(42, 3, 1, 650.00, 650.00);

-- Sale 43: Ahmed Al-Rashid UAE (repeat) — ErgoMax Chair x2 + HP Printer
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status, notes) VALUES
(43, 'ORD-2026-043', 19, '2026-10-10 11:00:00', 818.00, 'CREDIT_CARD', 'COMPLETED', 'Dubai office bulk order');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(43, 9, 2, 299.00, 598.00),
(43, 8, 1, 220.00, 220.00);

-- Sale 44: James Wilson Australia (repeat) — MacBook Air
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status) VALUES
(44, 'ORD-2026-044', 23, '2026-10-05 08:00:00', 900.00, 'BANK_TRANSFER', 'COMPLETED');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(44, 1, 1, 900.00, 900.00);

-- Sale 45: Yuki Tanaka Japan (repeat) — iPhone 15 Pro + Corsair Keyboard
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status) VALUES
(45, 'ORD-2026-045', 29, '2026-10-01 07:00:00', 839.00, 'CREDIT_CARD', 'COMPLETED');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(45, 2, 1, 750.00, 750.00),
(45, 15, 1,  89.00,  89.00);

-- Sale 46: Mohammed Al-Hassan Saudi (repeat) — Samsung TV + Dyson Vacuum
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status, notes) VALUES
(46, 'ORD-2026-046', 21, '2026-10-18 12:00:00', 1249.00, 'CREDIT_CARD', 'COMPLETED', 'Riyadh palace order');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(46, 3, 1, 650.00, 650.00),
(46, 7, 1, 599.00, 599.00);

-- Sale 47: Hans Mueller Germany (repeat) — MacBook Air
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status) VALUES
(47, 'ORD-2026-047', 18, '2026-10-12 13:00:00', 900.00, 'BANK_TRANSFER', 'COMPLETED');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(47, 1, 1, 900.00, 900.00);


-- =============================================================================
-- INTERNATIONAL SALES — SEPTEMBER 2026 (Sales 48–52)
-- =============================================================================

-- Sale 48: Mohammed Al-Hassan Saudi — Samsung TV + Dyson Vacuum (biggest spender)
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status, notes) VALUES
(48, 'ORD-2026-048', 21, '2026-09-25 10:00:00', 1249.00, 'CREDIT_CARD', 'COMPLETED', 'Corporate Q3 order');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(48, 3, 1, 650.00, 650.00),
(48, 7, 1, 599.00, 599.00);

-- Sale 49: Park Ji-ho South Korea — MacBook Air
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status) VALUES
(49, 'ORD-2026-049', 30, '2026-09-20 08:00:00', 900.00, 'CREDIT_CARD', 'COMPLETED');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(49, 1, 1, 900.00, 900.00);

-- Sale 50: Carlos Rodriguez Brazil — Bowflex + Adidas Shoes
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status) VALUES
(50, 'ORD-2026-050', 25, '2026-09-15 16:00:00', 439.00, 'BANK_TRANSFER', 'COMPLETED');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(50, 13, 1, 299.00, 299.00),
(50, 12, 1, 140.00, 140.00);

-- Sale 51: Hans Mueller Germany — iPhone + Sony Headphones
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status) VALUES
(51, 'ORD-2026-051', 18, '2026-09-10 11:00:00', 1030.00, 'BANK_TRANSFER', 'COMPLETED');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(51, 2, 1, 750.00, 750.00),
(51, 4, 1, 280.00, 280.00);

-- Sale 52: Marie Dubois France — ErgoMax Chair + HP Printer
INSERT INTO sales (id, order_number, customer_id, order_date, total_amount, payment_method, status) VALUES
(52, 'ORD-2026-052', 17, '2026-09-05 09:00:00', 519.00, 'CREDIT_CARD', 'COMPLETED');
INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES
(52, 9, 1, 299.00, 299.00),
(52, 8, 1, 220.00, 220.00);


-- =============================================================================
-- VERIFY FINAL COUNTS
-- =============================================================================
SELECT 'categories' AS tbl, COUNT(*) AS total FROM categories
UNION ALL SELECT 'products',   COUNT(*) FROM products
UNION ALL SELECT 'customers',  COUNT(*) FROM customers
UNION ALL SELECT 'sales',      COUNT(*) FROM sales
UNION ALL SELECT 'sale_items', COUNT(*) FROM sale_items;

SELECT
  CONCAT('$', FORMAT(SUM(total_amount), 2)) AS total_revenue_usd,
  COUNT(*)                                   AS total_orders,
  COUNT(DISTINCT customer_id)                AS unique_customers
FROM sales;

SELECT c.country, COUNT(s.id) AS orders, CONCAT('$', FORMAT(SUM(s.total_amount),2)) AS revenue
FROM sales s JOIN customers c ON s.customer_id = c.id
GROUP BY c.country ORDER BY SUM(s.total_amount) DESC;
