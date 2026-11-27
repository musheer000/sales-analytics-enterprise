/**
 * Sales Analytics REST API Client
 * Wraps all backend endpoints with JWT authentication and RBAC.
 */
const IS_LOCAL = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
const API_BASE = localStorage.getItem('custom_api_base')
    || (IS_LOCAL ? 'http://localhost:8080/api' : 'https://sales-analytics-enterprise.onrender.com/api');

const SalesAPI = {
    // 0. Authentication & Tokens
    getToken() {
        return localStorage.getItem('auth_token') || null;
    },

    getCurrentUser() {
        try {
            return JSON.parse(localStorage.getItem('auth_user') || 'null');
        } catch (e) {
            return null;
        }
    },

    getAuthHeaders(customHeaders = {}) {
        const headers = { ...customHeaders };
        const token = this.getToken();
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }
        return headers;
    },

    async login(username, password, portal = 'ADMIN') {
        const res = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password, portal })
        });
        const json = await res.json();
        if (!res.ok || !json.success) {
            throw new Error(json.message || 'Authentication failed. Please check credentials.');
        }
        const data = json.data;
        if (data && data.token) {
            localStorage.setItem('auth_token', data.token);
            localStorage.setItem('auth_user', JSON.stringify({ username: data.username, role: data.role }));
        }
        return data;
    },

    async verifyLoginOtp(username, email, otp) {
        const res = await fetch(`${API_BASE}/auth/login/verify-otp`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, email, otp })
        });
        const json = await res.json();
        if (!res.ok || !json.success) {
            throw new Error(json.message || 'Invalid or expired OTP.');
        }
        const data = json.data;
        localStorage.setItem('auth_token', data.token);
        localStorage.setItem('auth_user', JSON.stringify({ username: data.username, role: data.role }));
        return data;
    },

    async register(fullName, email, password, role = 'ADMIN') {
        const res = await fetch(`${API_BASE}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ fullName, email, password, role })
        });
        const json = await res.json();
        if (!res.ok || !json.success) {
            throw new Error(json.message || 'Registration failed.');
        }
        return json.data;
    },

    async verifyRegisterOtp(email, otp) {
        const res = await fetch(`${API_BASE}/auth/register/verify-otp`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, otp })
        });
        const json = await res.json();
        if (!res.ok || !json.success) {
            throw new Error(json.message || 'Invalid or expired OTP.');
        }
        const data = json.data;
        localStorage.setItem('auth_token', data.token);
        localStorage.setItem('auth_user', JSON.stringify({ username: data.username, role: data.role }));
        return data;
    },

    async resendOtp(email, purpose, name = '') {
        const res = await fetch(`${API_BASE}/auth/resend-otp`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, purpose, name })
        });
        const json = await res.json();
        if (!res.ok || !json.success) {
            throw new Error(json.message || 'Failed to resend OTP.');
        }
        return json.data;
    },

    logout() {
        localStorage.removeItem('auth_token');
        localStorage.removeItem('auth_user');
    },

    // Health Check
    async checkBackendHealth() {
        try {
            const res = await fetch(`${API_BASE}/analytics/summary`, {
                method: 'GET',
                mode: 'cors',
                signal: AbortSignal.timeout(8000),
                headers: this.getAuthHeaders()
            });
            return res.ok;
        } catch (e) {
            // Try a lightweight public endpoint as fallback
            try {
                const r2 = await fetch(`${API_BASE.replace('/api', '')}/actuator/health`, {
                    method: 'GET', mode: 'cors', signal: AbortSignal.timeout(5000)
                });
                return r2.ok;
            } catch (e2) {
                return false;
            }
        }
    },

    // 1. Categories
    async getCategories() {
        const res = await fetch(`${API_BASE}/categories`, { headers: this.getAuthHeaders() });
        const json = await res.json();
        return json.data || [];
    },

    async getCategoryById(id) {
        const res = await fetch(`${API_BASE}/categories/${id}`, { headers: this.getAuthHeaders() });
        const json = await res.json();
        return json.data;
    },

    async createCategory(categoryData) {
        const res = await fetch(`${API_BASE}/categories`, {
            method: 'POST',
            headers: this.getAuthHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify(categoryData)
        });
        const json = await res.json();
        if (!res.ok) throw new Error(json.message || 'Failed to create category (Admin role required)');
        return json.data;
    },

    async updateCategory(id, categoryData) {
        const res = await fetch(`${API_BASE}/categories/${id}`, {
            method: 'PUT',
            headers: this.getAuthHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify(categoryData)
        });
        const json = await res.json();
        if (!res.ok) throw new Error(json.message || 'Failed to update category (Admin role required)');
        return json.data;
    },

    async deleteCategory(id) {
        const res = await fetch(`${API_BASE}/categories/${id}`, {
            method: 'DELETE',
            headers: this.getAuthHeaders()
        });
        const json = await res.json();
        if (!res.ok) throw new Error(json.message || 'Failed to delete category (Admin role required)');
        return true;
    },

    // 2. Products
    async getProducts(categoryId = null) {
        const url = categoryId ? `${API_BASE}/products?categoryId=${categoryId}` : `${API_BASE}/products`;
        const res = await fetch(url, { headers: this.getAuthHeaders() });
        const json = await res.json();
        return json.data || [];
    },

    async getProductsPage(page = 0, size = 8, sort = 'name', direction = 'asc', categoryId = null, search = '') {
        let url = `${API_BASE}/products?page=${page}&size=${size}&sort=${sort}&direction=${direction}`;
        if (categoryId) url += `&categoryId=${categoryId}`;
        if (search) url += `&search=${encodeURIComponent(search)}`;
        const res = await fetch(url, { headers: this.getAuthHeaders() });
        const json = await res.json();
        return json.data || null;
    },

    async createProduct(productData) {
        const res = await fetch(`${API_BASE}/products`, {
            method: 'POST',
            headers: this.getAuthHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify(productData)
        });
        const json = await res.json();
        if (!res.ok) throw new Error(json.message || 'Failed to create product (Admin role required)');
        return json.data;
    },

    async updateProduct(id, productData) {
        const res = await fetch(`${API_BASE}/products/${id}`, {
            method: 'PUT',
            headers: this.getAuthHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify(productData)
        });
        const json = await res.json();
        if (!res.ok) throw new Error(json.message || 'Failed to update product (Admin role required)');
        return json.data;
    },

    async deleteProduct(id) {
        const res = await fetch(`${API_BASE}/products/${id}`, {
            method: 'DELETE',
            headers: this.getAuthHeaders()
        });
        const json = await res.json();
        if (!res.ok) throw new Error(json.message || 'Failed to delete product (Admin role required)');
        return true;
    },

    // 3. Customers
    async getCustomers() {
        const res = await fetch(`${API_BASE}/customers`, { headers: this.getAuthHeaders() });
        const json = await res.json();
        return json.data || [];
    },

    async getCustomersWithStats() {
        const res = await fetch(`${API_BASE}/customers/stats`, { headers: this.getAuthHeaders() });
        const json = await res.json();
        return json.data || [];
    },

    async createCustomer(customerData) {
        const res = await fetch(`${API_BASE}/customers`, {
            method: 'POST',
            headers: this.getAuthHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify(customerData)
        });
        const json = await res.json();
        if (!res.ok) throw new Error(json.message || 'Failed to create customer (Admin role required)');
        return json.data;
    },

    // 4. Sales / Orders
    async getSales() {
        const res = await fetch(`${API_BASE}/sales`, { headers: this.getAuthHeaders() });
        const json = await res.json();
        return json.data || [];
    },

    async getSaleById(id) {
        const res = await fetch(`${API_BASE}/sales/${id}`, { headers: this.getAuthHeaders() });
        const json = await res.json();
        return json.data;
    },

    async getSalesPage(page = 0, size = 10, sort = 'orderDate', direction = 'desc', paymentMethod = '', startDate = '', endDate = '') {
        let url = `${API_BASE}/sales?page=${page}&size=${size}&sort=${sort}&direction=${direction}`;
        if (paymentMethod) url += `&paymentMethod=${encodeURIComponent(paymentMethod)}`;
        if (startDate) url += `&startDate=${encodeURIComponent(startDate + 'T00:00:00')}`;
        if (endDate) url += `&endDate=${encodeURIComponent(endDate + 'T23:59:59')}`;
        const res = await fetch(url, { headers: this.getAuthHeaders() });
        const json = await res.json();
        return json.data || null;
    },

    async createSale(saleData) {
        const res = await fetch(`${API_BASE}/sales`, {
            method: 'POST',
            headers: this.getAuthHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify(saleData)
        });
        const json = await res.json();
        if (!res.ok) throw new Error(json.message || 'Failed to create sale order (Admin role required)');
        return json.data;
    },

    async deleteSale(id) {
        const res = await fetch(`${API_BASE}/sales/${id}`, {
            method: 'DELETE',
            headers: this.getAuthHeaders()
        });
        const json = await res.json();
        if (!res.ok) throw new Error(json.message || 'Failed to delete sale order (Admin role required)');
        return true;
    },

    // 5. Analytics
    async getAnalyticsSummary() {
        const res = await fetch(`${API_BASE}/analytics/summary`, { headers: this.getAuthHeaders() });
        const json = await res.json();
        return json.data;
    },

    async getCategorySales() {
        const res = await fetch(`${API_BASE}/analytics/category-sales`, { headers: this.getAuthHeaders() });
        const json = await res.json();
        return json.data || [];
    },

    async getMonthlyRevenue() {
        const res = await fetch(`${API_BASE}/analytics/monthly-revenue`, { headers: this.getAuthHeaders() });
        const json = await res.json();
        return json.data || [];
    },

    async getTopProducts() {
        const res = await fetch(`${API_BASE}/analytics/top-products`, { headers: this.getAuthHeaders() });
        const json = await res.json();
        return json.data || [];
    },

    async getRegionalSales() {
        const res = await fetch(`${API_BASE}/analytics/regional-sales`, { headers: this.getAuthHeaders() });
        const json = await res.json();
        return json.data || [];
    },

    async getBehaviorAnalytics() {
        const res = await fetch(`${API_BASE}/analytics/behavior`, { headers: this.getAuthHeaders() });
        const json = await res.json();
        return json.data || {};
    },

    // 6. CSV Report Exports (Blob-based direct client download)
    async downloadBlob(url, filename) {
        const res = await fetch(url, { method: 'GET', mode: 'cors', headers: this.getAuthHeaders() });
        if (!res.ok) {
            throw new Error(`Server returned status ${res.status}`);
        }
        const blob = await res.blob();
        const blobUrl = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.style.display = 'none';
        a.href = blobUrl;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        setTimeout(() => {
            document.body.removeChild(a);
            window.URL.revokeObjectURL(blobUrl);
        }, 500);
    },

    async downloadSalesCsv() {
        await this.downloadBlob(`${API_BASE}/analytics/export/sales-csv`, 'sales_orders_report.csv');
    },

    async downloadCategoryRevenueCsv() {
        await this.downloadBlob(`${API_BASE}/analytics/export/category-revenue-csv`, 'category_revenue_report.csv');
    },

    async downloadTopProductsCsv() {
        await this.downloadBlob(`${API_BASE}/analytics/export/top-products-csv`, 'top_products_report.csv');
    },

    // 7. Integrations (Razorpay & Gmail SMTP Email)
    async getIntegrationConfig() {
        try {
            const res = await fetch(`${API_BASE}/integrations/config`, { headers: this.getAuthHeaders() });
            const json = await res.json();
            return json.data || null;
        } catch (e) {
            return null;
        }
    },

    async sendEmail(to, subject, htmlBody) {
        const res = await fetch(`${API_BASE}/integrations/email/send`, {
            method: 'POST',
            headers: this.getAuthHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify({ to, subject, htmlBody })
        });
        const json = await res.json();
        if (!res.ok || !json.success) {
            throw new Error(json.message || 'Failed to send email');
        }
        return json.data;
    },

    async emailInvoice(saleId, to = '') {
        const res = await fetch(`${API_BASE}/integrations/email/invoice/${saleId}`, {
            method: 'POST',
            headers: this.getAuthHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify({ to })
        });
        const json = await res.json();
        if (!res.ok || !json.success) {
            throw new Error(json.message || 'Failed to email invoice');
        }
        return json.data;
    },

    // 8. Real-Time Activity & System Telemetry
    async getRealtimeActivity() {
        try {
            const res = await fetch(`${API_BASE}/realtime/activity`, { headers: this.getAuthHeaders() });
            const json = await res.json();
            return json.data || [];
        } catch (e) {
            return [];
        }
    },

    async getSystemTelemetry() {
        try {
            const res = await fetch(`${API_BASE}/realtime/telemetry`, { headers: this.getAuthHeaders() });
            const json = await res.json();
            return json.data || null;
        } catch (e) {
            return null;
        }
    }
};
