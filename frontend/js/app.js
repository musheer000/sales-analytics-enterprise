/**
 * Sales Analytics & Reporting Dashboard — Enterprise UI Controller
 */

// ─── Global State ────────────────────────────────────────────────────────────
let revenueChartInstance = null;
let categoryChartInstance = null;
let analyticsRevenueChartInstance = null;
let analyticsOrdersChartInstance = null;
let analyticsPaymentChartInstance = null;
let realtimeEventSource = null;

let cachedCategories = [];
let cachedProducts = [];
let cachedCustomers = [];
let cachedCustomerStats = [];
let allCachedSales = [];

let lastSummary = null;
let lastMonthlyRev = [];
let lastCatSales = [];
let lastTopProducts = [];

let leaderboardSortBy = 'spend';
let liveAutoSyncEnabled = localStorage.getItem('liveAutoSync') !== 'false';
let liveSyncTimer = null;
let notifications = [];
let razorpayKeyId = 'rzp_test_TWMghMtesxGj8a';
let ownerEmail = 'shaikmusheer07@gmail.com';
let currentInvoiceSaleId = null;

const dashboardDateFilter = {
  preset: 'all', // 'all' | 'today' | '7d' | 'month' | '3m' | 'year' | 'custom'
  startDate: '',
  endDate: ''
};

// ─── Multi-Currency FX Engine ────────────────────────────────────────────────
const CURRENCIES = {
  INR: { code: 'INR', symbol: '₹',    name: 'Indian Rupee',    flag: '🇮🇳', rate: 84.50,  locale: 'en-IN', decimals: 2 },
  USD: { code: 'USD', symbol: '$',    name: 'US Dollar',       flag: '🇺🇸', rate: 1.00,   locale: 'en-US', decimals: 2 },
  AED: { code: 'AED', symbol: 'AED ', name: 'UAE Dirham',      flag: '🇦🇪', rate: 3.67,   locale: 'en-AE', decimals: 2 },
  KWD: { code: 'KWD', symbol: 'KD ',  name: 'Kuwaiti Dinar',   flag: '🇰🇼', rate: 0.31,   locale: 'en-KW', decimals: 3 },
  PKR: { code: 'PKR', symbol: 'Rs ',  name: 'Pakistani Rupee', flag: '🇵🇰', rate: 278.50, locale: 'en-PK', decimals: 2 },
  CNY: { code: 'CNY', symbol: '¥',    name: 'Chinese Yuan',    flag: '🇨🇳', rate: 7.24,   locale: 'zh-CN', decimals: 2 },
  JPY: { code: 'JPY', symbol: '¥',    name: 'Japanese Yen',    flag: '🇯🇵', rate: 154.00, locale: 'ja-JP', decimals: 0 },
  EUR: { code: 'EUR', symbol: '€',    name: 'Euro',            flag: '🇪🇺', rate: 0.95,   locale: 'de-DE', decimals: 2 },
  GBP: { code: 'GBP', symbol: '£',    name: 'British Pound',   flag: '🇬🇧', rate: 0.79,   locale: 'en-GB', decimals: 2 }
};

let currentCurrency = localStorage.getItem('selectedCurrency') || 'INR';
let monthlyTargetUSD = Number(localStorage.getItem('monthlySalesTargetUSD')) || 5000;

function getCurrencyConfig() {
  return CURRENCIES[currentCurrency] || CURRENCIES.INR;
}

function formatCurrency(usdVal, includeSymbol = true) {
  const cfg = getCurrencyConfig();
  const num = (Number(usdVal) || 0) * cfg.rate;
  const formatted = num.toLocaleString(cfg.locale, {
    minimumFractionDigits: cfg.decimals,
    maximumFractionDigits: cfg.decimals
  });
  return includeSymbol ? `${cfg.symbol}${formatted}` : formatted;
}

// ─── Helpers ─────────────────────────────────────────────────────────────────
function refreshIcons() {
  if (window.lucide && typeof window.lucide.createIcons === 'function') {
    window.lucide.createIcons();
  }
}

function escapeHtml(str) {
  return String(str ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function showToast(message, type = 'info') {
  const container = document.getElementById('toastContainer');
  if (!container) return;
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  const icon = type === 'success' ? 'check-circle-2' : type === 'error' ? 'alert-circle' : 'info';
  toast.innerHTML = `<i data-lucide="${icon}" style="width:16px;height:16px;flex-shrink:0;"></i><span>${escapeHtml(message)}</span>`;
  container.appendChild(toast);
  refreshIcons();
  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transition = 'opacity 0.2s ease';
    setTimeout(() => toast.remove(), 220);
  }, 3200);
}

function addNotification(title, detail = '') {
  const now = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  notifications.unshift({ title, detail, time: now });
  if (notifications.length > 30) notifications.pop();
  renderNotifications();
}

function renderNotifications() {
  const badge = document.getElementById('notifBadge');
  const list = document.getElementById('notifList');
  if (badge) {
    badge.style.display = notifications.length > 0 ? 'inline-flex' : 'none';
    badge.textContent = notifications.length > 9 ? '9+' : String(notifications.length);
  }
  if (list) {
    if (notifications.length === 0) {
      list.innerHTML = `<div class="empty-state-mini">No new notifications</div>`;
    } else {
      list.innerHTML = notifications.map(n => `
        <div class="notif-item">
          <div class="notif-item-title">${escapeHtml(n.title)}</div>
          ${n.detail ? `<div style="color:var(--text-muted);font-size:12px;margin-top:2px;">${escapeHtml(n.detail)}</div>` : ''}
          <div class="notif-item-time">${escapeHtml(n.time)}</div>
        </div>
      `).join('');
    }
  }
}

function getPresetDates(preset) {
  const now = new Date();
  const pad = n => String(n).padStart(2, '0');
  const fmt = d => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  const today = fmt(now);

  if (preset === 'today') return { startDate: today, endDate: today };
  if (preset === '7d') {
    const d = new Date(now);
    d.setDate(d.getDate() - 6);
    return { startDate: fmt(d), endDate: today };
  }
  if (preset === 'month') {
    const d = new Date(now.getFullYear(), now.getMonth(), 1);
    return { startDate: fmt(d), endDate: today };
  }
  if (preset === '3m') {
    const d = new Date(now);
    d.setMonth(d.getMonth() - 2);
    d.setDate(1);
    return { startDate: fmt(d), endDate: today };
  }
  if (preset === 'year') return { startDate: `${now.getFullYear()}-01-01`, endDate: today };
  return { startDate: '', endDate: '' };
}

// ─── Initialization ──────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  initTheme();
  initNavigation();
  initAuthUI();
  initCurrencySwitcher();
  initDateFilterBar();
  initModals();
  initToolbarAndExports();
  initCommandPalette();
  initCsvProductImport();
  setupRealTimeSync();
  initRealtimeSSE();
  refreshIcons();
  loadAllData();
});

// ─── Theme (Light / Dark) ────────────────────────────────────────────────────
function initTheme() {
  const saved = localStorage.getItem('theme') || 'light';
  applyTheme(saved);
  document.getElementById('themeToggle')?.addEventListener('click', () => {
    const next = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
    applyTheme(next);
    localStorage.setItem('theme', next);
    renderCharts();
    renderAnalyticsCharts();
  });
}

function applyTheme(theme) {
  document.documentElement.setAttribute('data-theme', theme);
  const btn = document.getElementById('themeToggle');
  if (btn) {
    const isDark = theme === 'dark';
    btn.innerHTML = `<i data-lucide="${isDark ? 'sun' : 'moon'}"></i><span class="toolbar-btn-label">${isDark ? 'Light' : 'Dark'}</span>`;
    refreshIcons();
  }
}

// ─── Sidebar Navigation ──────────────────────────────────────────────────────
function initNavigation() {
  const titles = {
    dashboard: 'Dashboard Overview',
    categories: 'Categories',
    products: 'Products Catalog',
    sales: 'Sales & Orders',
    customers: 'Customers',
    analytics: 'Advanced Reports'
  };
  document.querySelectorAll('.nav-item[data-tab]').forEach(btn => {
    btn.addEventListener('click', () => {
      const tab = btn.dataset.tab;
      document.querySelectorAll('.nav-item').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
      document.getElementById(`tab-${tab}`)?.classList.add('active');
      const titleEl = document.getElementById('pageTitle');
      if (titleEl) titleEl.textContent = titles[tab] || 'Dashboard';
      if (tab === 'analytics') renderAnalyticsCharts();
    });
  });
}

// ─── Authentication & 2FA Email OTP ──────────────────────────────────────────
let pendingOtpState = {
  purpose: 'LOGIN', // 'LOGIN' | 'REGISTER'
  username: '',
  email: '',
  displayName: '',
  expiryTimerId: null,
  cooldownTimerId: null
};

function switchAuthView(mode) {
  const loginView = document.getElementById('authLoginView');
  const regView = document.getElementById('authRegisterView');
  const otpView = document.getElementById('authOtpView');
  const tabs = document.getElementById('authModeTabs');
  const loginBtn = document.getElementById('loginSubmitBtn');
  const regBtn = document.getElementById('registerSubmitBtn');
  const otpBtn = document.getElementById('verifyOtpSubmitBtn');
  const title = document.getElementById('authModalTitle');
  const sub = document.getElementById('authModalSub');

  document.querySelectorAll('#authModeTabs .seg-btn').forEach(b => {
    b.classList.toggle('active', b.dataset.authMode === mode);
  });

  if (mode === 'otp') {
    if (tabs) tabs.style.display = 'none';
    if (loginView) loginView.style.display = 'none';
    if (regView) regView.style.display = 'none';
    if (otpView) otpView.style.display = 'block';
    if (loginBtn) loginBtn.style.display = 'none';
    if (regBtn) regBtn.style.display = 'none';
    if (otpBtn) otpBtn.style.display = 'inline-flex';
    if (title) title.textContent = 'Email OTP Verification';
    if (sub) sub.textContent = 'Enter the 6-digit security code';
    return;
  }

  if (tabs) tabs.style.display = 'inline-flex';
  if (otpView) otpView.style.display = 'none';
  if (otpBtn) otpBtn.style.display = 'none';
  if (mode === 'register') {
    if (loginView) loginView.style.display = 'none';
    if (regView) regView.style.display = 'block';
    if (loginBtn) loginBtn.style.display = 'none';
    if (regBtn) regBtn.style.display = 'inline-flex';
    if (title) title.textContent = 'Create Account';
    if (sub) sub.textContent = 'Verify your email with a 6-digit OTP';
  } else {
    if (loginView) loginView.style.display = 'block';
    if (regView) regView.style.display = 'none';
    if (loginBtn) loginBtn.style.display = 'inline-flex';
    if (regBtn) regBtn.style.display = 'none';
    if (title) title.textContent = 'Sign In';
    if (sub) sub.textContent = '2-Factor Email OTP secured access';
  }
}

function getOtpInputValue() {
  return Array.from(document.querySelectorAll('#otpBoxes .otp-digit'))
    .map(i => i.value.trim())
    .join('');
}

function clearOtpBoxes() {
  document.querySelectorAll('#otpBoxes .otp-digit').forEach(i => { i.value = ''; });
}

function startOtpTimers(expiresInSeconds = 300) {
  if (pendingOtpState.expiryTimerId) clearInterval(pendingOtpState.expiryTimerId);
  if (pendingOtpState.cooldownTimerId) clearInterval(pendingOtpState.cooldownTimerId);

  let exp = expiresInSeconds;
  const expEl = document.getElementById('otpExpiryTimer');
  const updateExp = () => {
    const m = String(Math.floor(exp / 60)).padStart(2, '0');
    const s = String(exp % 60).padStart(2, '0');
    if (expEl) expEl.textContent = `${m}:${s}`;
  };
  updateExp();
  pendingOtpState.expiryTimerId = setInterval(() => {
    exp = Math.max(0, exp - 1);
    updateExp();
    if (exp === 0) clearInterval(pendingOtpState.expiryTimerId);
  }, 1000);

  let cd = 30;
  const resendBtn = document.getElementById('resendOtpBtn');
  if (resendBtn) {
    resendBtn.disabled = true;
    resendBtn.textContent = `Resend in ${cd}s`;
    pendingOtpState.cooldownTimerId = setInterval(() => {
      cd -= 1;
      if (cd <= 0) {
        clearInterval(pendingOtpState.cooldownTimerId);
        resendBtn.disabled = false;
        resendBtn.textContent = 'Resend OTP';
      } else {
        resendBtn.textContent = `Resend in ${cd}s`;
      }
    }, 1000);
  }
}

function openOtpStep(info) {
  pendingOtpState.purpose = info.purpose || 'LOGIN';
  pendingOtpState.username = info.username || info.email || '';
  pendingOtpState.email = info.email || '';
  pendingOtpState.displayName = info.displayName || info.username || '';
  const maskedEl = document.getElementById('otpMaskedEmail');
  if (maskedEl) maskedEl.textContent = info.maskedEmail || info.email;
  const errEl = document.getElementById('otpError');
  if (errEl) errEl.style.display = 'none';
  clearOtpBoxes();
  switchAuthView('otp');
  startOtpTimers(info.expiresInSeconds || 300);
  setTimeout(() => document.querySelector('#otpBoxes .otp-digit')?.focus(), 80);
}

function initAuthUI() {
  updateAuthUI();
  document.getElementById('authBtn')?.addEventListener('click', () => {
    if (SalesAPI.getToken()) {
      SalesAPI.logout();
      updateAuthUI();
      showToast('Signed out', 'info');
    } else {
      switchAuthView('login');
      openModal('loginModal');
    }
  });
  document.getElementById('loginCancelBtn')?.addEventListener('click', () => closeModal('loginModal'));

  document.querySelectorAll('#authModeTabs .seg-btn').forEach(btn => {
    btn.addEventListener('click', () => switchAuthView(btn.dataset.authMode));
  });

  document.getElementById('fillOwnerDemoBtn')?.addEventListener('click', () => {
    document.getElementById('loginUsername').value = 'shaikmusheer07@gmail.com';
    document.getElementById('loginPassword').value = '77889966';
  });
  document.getElementById('fillAdminDemoBtn')?.addEventListener('click', () => {
    document.getElementById('loginUsername').value = 'admin';
    document.getElementById('loginPassword').value = 'admin123';
  });
  document.getElementById('fillViewerDemoBtn')?.addEventListener('click', () => {
    document.getElementById('loginUsername').value = 'viewer';
    document.getElementById('loginPassword').value = 'viewer123';
  });

  document.getElementById('loginSubmitBtn')?.addEventListener('click', handleLogin);
  document.getElementById('loginPassword')?.addEventListener('keydown', e => {
    if (e.key === 'Enter') handleLogin();
  });
  document.getElementById('registerSubmitBtn')?.addEventListener('click', handleRegister);
  document.getElementById('verifyOtpSubmitBtn')?.addEventListener('click', handleVerifyOtp);
  document.getElementById('otpBackBtn')?.addEventListener('click', () => {
    switchAuthView(pendingOtpState.purpose === 'REGISTER' ? 'register' : 'login');
  });
  document.getElementById('resendOtpBtn')?.addEventListener('click', async () => {
    const errEl = document.getElementById('otpError');
    if (errEl) errEl.style.display = 'none';
    try {
      const res = await SalesAPI.resendOtp(pendingOtpState.email, pendingOtpState.purpose, pendingOtpState.displayName);
      showToast(`New OTP sent to ${res.maskedEmail}`, 'success');
      startOtpTimers(res.expiresInSeconds || 300);
    } catch (e) {
      if (errEl) { errEl.textContent = e.message; errEl.style.display = 'block'; }
    }
  });

  // 6-box OTP keyboard + paste UX
  const boxes = Array.from(document.querySelectorAll('#otpBoxes .otp-digit'));
  boxes.forEach((box, idx) => {
    box.addEventListener('input', e => {
      const v = e.target.value.replace(/\D/g, '');
      e.target.value = v.slice(-1);
      if (e.target.value && idx < boxes.length - 1) boxes[idx + 1].focus();
      if (getOtpInputValue().length === 6) handleVerifyOtp();
    });
    box.addEventListener('keydown', e => {
      if (e.key === 'Backspace' && !box.value && idx > 0) boxes[idx - 1].focus();
      if (e.key === 'Enter') handleVerifyOtp();
    });
    box.addEventListener('paste', e => {
      e.preventDefault();
      const text = (e.clipboardData || window.clipboardData).getData('text').replace(/\D/g, '').slice(0, 6);
      text.split('').forEach((ch, i) => { if (boxes[i]) boxes[i].value = ch; });
      if (text.length === 6) handleVerifyOtp();
    });
  });
}

async function handleLogin() {
  const u = document.getElementById('loginUsername').value.trim();
  const p = document.getElementById('loginPassword').value;
  const errEl = document.getElementById('loginError');
  const btn = document.getElementById('loginSubmitBtn');
  errEl.style.display = 'none';
  if (!u || !p) {
    errEl.textContent = 'Enter your username/email and password.';
    errEl.style.display = 'block';
    return;
  }
  if (btn) btn.disabled = true;
  try {
    const data = await SalesAPI.login(u, p);
    if (data && data.otpRequired) {
      showToast(`OTP sent to ${data.maskedEmail}`, 'info');
      openOtpStep({ ...data, purpose: 'LOGIN', displayName: u });
    } else {
      closeModal('loginModal');
      updateAuthUI();
      showToast(`Signed in as ${data.username} (${data.role})`, 'success');
      addNotification('Session authenticated', `Signed in as ${data.username}`);
      loadAllData();
    }
  } catch (err) {
    errEl.textContent = err.message || 'Invalid credentials';
    errEl.style.display = 'block';
  } finally {
    if (btn) btn.disabled = false;
  }
}

async function handleRegister() {
  const fullName = document.getElementById('regFullName').value.trim();
  const email = document.getElementById('regEmail').value.trim();
  const password = document.getElementById('regPassword').value;
  const role = document.getElementById('regRole').value || 'ADMIN';
  const errEl = document.getElementById('registerError');
  const btn = document.getElementById('registerSubmitBtn');
  errEl.style.display = 'none';
  if (!fullName || !email || password.length < 6) {
    errEl.textContent = 'Enter your name, valid email, and a password (min 6 characters).';
    errEl.style.display = 'block';
    return;
  }
  if (btn) btn.disabled = true;
  try {
    const data = await SalesAPI.register(fullName, email, password, role);
    showToast(`Verification OTP sent to ${data.maskedEmail}`, 'info');
    openOtpStep({ ...data, purpose: 'REGISTER', displayName: fullName });
  } catch (err) {
    errEl.textContent = err.message || 'Registration failed';
    errEl.style.display = 'block';
  } finally {
    if (btn) btn.disabled = false;
  }
}

async function handleVerifyOtp() {
  const otp = getOtpInputValue();
  const errEl = document.getElementById('otpError');
  const btn = document.getElementById('verifyOtpSubmitBtn');
  errEl.style.display = 'none';
  if (otp.length !== 6) {
    errEl.textContent = 'Please enter all 6 digits of your email OTP.';
    errEl.style.display = 'block';
    return;
  }
  if (btn) btn.disabled = true;
  try {
    const data = pendingOtpState.purpose === 'REGISTER'
      ? await SalesAPI.verifyRegisterOtp(pendingOtpState.email, otp)
      : await SalesAPI.verifyLoginOtp(pendingOtpState.username, pendingOtpState.email, otp);

    if (pendingOtpState.expiryTimerId) clearInterval(pendingOtpState.expiryTimerId);
    if (pendingOtpState.cooldownTimerId) clearInterval(pendingOtpState.cooldownTimerId);
    closeModal('loginModal');
    updateAuthUI();
    showToast(`2FA verified! Signed in as ${data.username} (${data.role})`, 'success');
    addNotification('2FA OTP Verified', `Signed in as ${data.username}`);
    loadAllData();
  } catch (err) {
    errEl.textContent = err.message || 'Invalid OTP code';
    errEl.style.display = 'block';
  } finally {
    if (btn) btn.disabled = false;
  }
}

function updateAuthUI() {
  const user = SalesAPI.getCurrentUser();
  const isAdmin = user && String(user.role).toUpperCase().includes('ADMIN');
  document.body.classList.toggle('is-admin', Boolean(isAdmin));

  const nameEl = document.getElementById('userStatusText');
  const roleEl = document.getElementById('userRoleTag');
  const authBtn = document.getElementById('authBtn');

  if (user) {
    if (nameEl) nameEl.textContent = user.username;
    if (roleEl) {
      roleEl.textContent = isAdmin ? 'ADMIN' : 'VIEWER';
      roleEl.style.display = 'inline-block';
    }
    if (authBtn) authBtn.innerHTML = `<i data-lucide="log-out"></i><span>Sign Out</span>`;
  } else {
    if (nameEl) nameEl.textContent = 'Guest';
    if (roleEl) roleEl.style.display = 'none';
    if (authBtn) authBtn.innerHTML = `<i data-lucide="log-in"></i><span>Sign In</span>`;
  }
  refreshIcons();
}

// ─── Currency Switcher ───────────────────────────────────────────────────────
function initCurrencySwitcher() {
  const sel = document.getElementById('currencySelect');
  const flag = document.getElementById('currencyFlag');
  if (sel) {
    sel.value = currentCurrency;
    sel.addEventListener('change', e => setCurrency(e.target.value));
  }
  if (flag) flag.textContent = getCurrencyConfig().flag;
}

function setCurrency(code) {
  if (!CURRENCIES[code]) return;
  currentCurrency = code;
  localStorage.setItem('selectedCurrency', code);
  const flag = document.getElementById('currencyFlag');
  if (flag) flag.textContent = getCurrencyConfig().flag;
  renderAll();
  showToast(`Currency switched to ${code}`, 'info');
}

// ─── Date Filter Bar ─────────────────────────────────────────────────────────
function initDateFilterBar() {
  document.querySelectorAll('#dateSegmented .seg-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('#dateSegmented .seg-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      const preset = btn.dataset.preset;
      const { startDate, endDate } = getPresetDates(preset);
      dashboardDateFilter.preset = preset;
      dashboardDateFilter.startDate = startDate;
      dashboardDateFilter.endDate = endDate;
      document.getElementById('filterStartDate').value = startDate;
      document.getElementById('filterEndDate').value = endDate;
      applyDashboardFilter();
    });
  });

  document.getElementById('applyFilterBtn')?.addEventListener('click', () => {
    const start = document.getElementById('filterStartDate').value;
    const end = document.getElementById('filterEndDate').value;
    if (!start && !end) {
      showToast('Select at least a start or end date', 'info');
      return;
    }
    document.querySelectorAll('#dateSegmented .seg-btn').forEach(b => b.classList.remove('active'));
    dashboardDateFilter.preset = 'custom';
    dashboardDateFilter.startDate = start;
    dashboardDateFilter.endDate = end;
    applyDashboardFilter();
  });

  document.querySelectorAll('#leaderboardSort .seg-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('#leaderboardSort .seg-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      leaderboardSortBy = btn.dataset.sort;
      renderLeaderboard();
    });
  });

  document.getElementById('productSearch')?.addEventListener('input', () => renderProductsTable());
}

function getFilteredSales() {
  const { startDate, endDate } = dashboardDateFilter;
  if (!startDate && !endDate) return allCachedSales;
  return allCachedSales.filter(s => {
    const d = String(s.orderDate || '').slice(0, 10);
    if (!d) return false;
    if (startDate && d < startDate) return false;
    if (endDate && d > endDate) return false;
    return true;
  });
}

function applyDashboardFilter() {
  renderKPIs();
  renderSalesTables();
  renderTopProducts();
}

// ─── Real-time Auto-Sync ─────────────────────────────────────────────────────
function setupRealTimeSync() {
  updateSyncButtonUI();
  document.getElementById('autoSyncToggle')?.addEventListener('click', () => {
    liveAutoSyncEnabled = !liveAutoSyncEnabled;
    localStorage.setItem('liveAutoSync', String(liveAutoSyncEnabled));
    updateSyncButtonUI();
    restartSyncTimer();
    showToast(liveAutoSyncEnabled ? 'Real-time 30s auto-sync enabled' : 'Auto-sync paused', 'info');
  });
  restartSyncTimer();
}

function updateSyncButtonUI() {
  const btn = document.getElementById('autoSyncToggle');
  const pill = document.getElementById('liveSyncPill');
  if (btn) btn.classList.toggle('active', liveAutoSyncEnabled);
  if (pill) pill.classList.toggle('paused', !liveAutoSyncEnabled);
}

function restartSyncTimer() {
  if (liveSyncTimer) clearInterval(liveSyncTimer);
  if (liveAutoSyncEnabled) {
    liveSyncTimer = setInterval(() => loadAllData(true), 30000);
  }
}

// ─── Toolbar & Exports ───────────────────────────────────────────────────────
function initToolbarAndExports() {
  document.getElementById('refreshBtn')?.addEventListener('click', () => {
    loadAllData(false);
    showToast('Dashboard refreshed', 'success');
  });

  document.getElementById('notifBtn')?.addEventListener('click', () => {
    document.getElementById('notifDrawer')?.classList.add('open');
    document.getElementById('notifBackdrop')?.classList.add('open');
  });
  const closeNotif = () => {
    document.getElementById('notifDrawer')?.classList.remove('open');
    document.getElementById('notifBackdrop')?.classList.remove('open');
  };
  document.getElementById('closeNotifBtn')?.addEventListener('click', closeNotif);
  document.getElementById('notifBackdrop')?.addEventListener('click', closeNotif);
  document.getElementById('clearNotifBtn')?.addEventListener('click', () => {
    notifications = [];
    renderNotifications();
  });

  const exportSales = async () => {
    try {
      await SalesAPI.downloadSalesCsv();
      showToast('Sales CSV exported', 'success');
    } catch (e) {
      showToast('Export failed: ' + e.message, 'error');
    }
  };
  document.getElementById('exportSalesBtn')?.addEventListener('click', exportSales);
  document.getElementById('exportCsvBtn')?.addEventListener('click', exportSales);
  document.getElementById('exportPdfBtn')?.addEventListener('click', () => window.print());
  document.getElementById('exportCustomersBtn')?.addEventListener('click', exportCustomersCsv);
  document.getElementById('emailReportBtn')?.addEventListener('click', sendExecutiveReportEmail);
}

async function sendExecutiveReportEmail() {
  const recipient = prompt('Send Executive Sales Report via Gmail SMTP to:', ownerEmail);
  if (!recipient) return;
  const totalRev = lastSummary?.totalRevenue ?? allCachedSales.reduce((a, s) => a + (Number(s.totalAmount) || 0), 0);
  const totalOrders = lastSummary?.totalOrders ?? allCachedSales.length;
  const totalCust = cachedCustomerStats.length || cachedCustomers.length;
  const topRows = lastTopProducts.slice(0, 5).map((p, i) =>
    `<tr><td style="padding:8px;border-bottom:1px solid #e5e7eb;">#${i + 1} ${escapeHtml(p.productName || p.name)}</td><td style="padding:8px;border-bottom:1px solid #e5e7eb;">${Number(p.totalQuantitySold || 0)} units</td><td style="padding:8px;border-bottom:1px solid #e5e7eb;text-align:right;">$${Number(p.totalRevenue || 0).toFixed(2)}</td></tr>`
  ).join('');
  const htmlBody = `
    <div style="font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,sans-serif;max-width:620px;margin:0 auto;border:1px solid #e5e7eb;border-radius:14px;overflow:hidden;">
      <div style="background:linear-gradient(135deg,#007AFF,#34C759);color:#fff;padding:24px 28px;">
        <h2 style="margin:0;">Sales Analytics — Executive Report</h2>
        <p style="margin:6px 0 0;opacity:.9;font-size:13px;">Generated on ${new Date().toLocaleString()}</p>
      </div>
      <div style="padding:24px 28px;color:#111827;">
        <p><strong>Total Revenue:</strong> $${Number(totalRev).toFixed(2)} USD (${formatCurrency(totalRev)})</p>
        <p><strong>Total Orders:</strong> ${totalOrders} &nbsp;|&nbsp; <strong>Customers:</strong> ${totalCust}</p>
        <h3 style="margin:18px 0 8px;">Top Performing Products</h3>
        <table style="width:100%;border-collapse:collapse;font-size:14px;">${topRows}</table>
      </div>
    </div>`;
  try {
    showToast('Sending report via Gmail SMTP…', 'info');
    await SalesAPI.sendEmail(recipient, 'Sales Analytics Executive Report', htmlBody);
    showToast(`Report emailed to ${recipient}`, 'success');
    addNotification('Executive Report Emailed', `Sent to ${recipient}`);
  } catch (e) {
    showToast('Email failed: ' + e.message, 'error');
  }
}

function exportCustomersCsv() {
  const rows = [['ID', 'First Name', 'Last Name', 'Email', 'Phone', 'City', 'Country', 'Orders', 'Lifetime Value']];
  const source = cachedCustomerStats.length ? cachedCustomerStats : cachedCustomers;
  source.forEach(c => {
    rows.push([
      c.id ?? '',
      c.firstName ?? '',
      c.lastName ?? '',
      c.email ?? '',
      c.phone ?? '',
      c.city ?? '',
      c.country ?? '',
      c.totalOrders ?? 0,
      c.totalSpent ?? 0
    ]);
  });
  const csv = rows.map(r => r.map(v => `"${String(v).replace(/"/g, '""')}"`).join(',')).join('\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'customers_report.csv';
  a.click();
  URL.revokeObjectURL(url);
  showToast('Customers CSV exported', 'success');
}

// ─── Data Loading ────────────────────────────────────────────────────────────
async function loadAllData(silent = false) {
  const t0 = performance.now();
  const healthy = await SalesAPI.checkBackendHealth();
  const dot = document.getElementById('serverStatusDot');
  const label = document.getElementById('serverStatusLabel');
  if (dot) dot.className = `server-dot ${healthy ? 'online' : 'offline'}`;
  if (label) label.textContent = healthy ? 'Backend Connected' : 'Backend Offline';

  if (!healthy) {
    if (!silent) showToast('Cannot reach backend at localhost:8080', 'error');
    return;
  }

  try {
    const [categories, products, customers, customerStats, sales, summary, monthlyRev, catSales, topProducts] =
      await Promise.all([
        SalesAPI.getCategories().catch(() => []),
        SalesAPI.getProducts().catch(() => []),
        SalesAPI.getCustomers().catch(() => []),
        SalesAPI.getCustomersWithStats().catch(() => []),
        SalesAPI.getSales().catch(() => []),
        SalesAPI.getAnalyticsSummary().catch(() => null),
        SalesAPI.getMonthlyRevenue().catch(() => []),
        SalesAPI.getCategorySales().catch(() => []),
        SalesAPI.getTopProducts().catch(() => [])
      ]);

    cachedCategories = Array.isArray(categories) ? categories : [];
    cachedProducts = Array.isArray(products) ? products : [];
    cachedCustomers = Array.isArray(customers) ? customers : [];
    cachedCustomerStats = Array.isArray(customerStats) ? customerStats : [];
    allCachedSales = Array.isArray(sales) ? sales : [];
    lastSummary = summary;
    lastMonthlyRev = Array.isArray(monthlyRev) ? monthlyRev : [];
    lastCatSales = Array.isArray(catSales) ? catSales : [];
    lastTopProducts = Array.isArray(topProducts) ? topProducts : [];

    const elapsed = Math.round(performance.now() - t0);
    const cacheEl = document.getElementById('cacheLatencyLabel');
    if (cacheEl) cacheEl.textContent = `${elapsed}ms`;
    const timeEl = document.getElementById('lastSyncedTime');
    if (timeEl) timeEl.textContent = new Date().toLocaleTimeString([], { hour12: true });

    checkLowStockNotifications();
    renderAll();
  } catch (err) {
    if (!silent) showToast('Error loading data: ' + err.message, 'error');
  }
}

function checkLowStockNotifications() {
  const low = cachedProducts.filter(p => Number(p.stockQuantity) <= 10);
  const badge = document.getElementById('lowStockAlert');
  if (badge) {
    badge.style.display = low.length > 0 ? 'inline-flex' : 'none';
    badge.innerHTML = `<i data-lucide="alert-triangle"></i> ${low.length} Low Stock`;
  }
}

function renderAll() {
  renderKPIs();
  renderSalesTarget();
  renderCharts();
  renderTopProducts();
  renderLeaderboard();
  renderSalesTables();
  renderCategoriesTable();
  renderProductsTable();
  renderCustomersTable();
  populateModalSelects();
  refreshIcons();
}

// ─── KPIs ────────────────────────────────────────────────────────────────────
function renderKPIs() {
  const filteredSales = getFilteredSales();
  const isFiltered = Boolean(dashboardDateFilter.startDate || dashboardDateFilter.endDate);

  const revenue = isFiltered
    ? filteredSales.reduce((acc, s) => acc + (Number(s.totalAmount) || 0), 0)
    : Number(lastSummary?.totalRevenue ?? filteredSales.reduce((a, s) => a + (Number(s.totalAmount) || 0), 0));

  const orders = isFiltered
    ? filteredSales.length
    : Number(lastSummary?.totalOrders ?? filteredSales.length);

  const aov = orders > 0 ? revenue / orders : 0;

  const custStats = cachedCustomerStats.length ? cachedCustomerStats : [];
  const totalCustomers = custStats.length || cachedCustomers.length;
  const repeatCustomers = custStats.filter(c => Number(c.totalOrders) > 1).length;
  const repeatRate = totalCustomers > 0 ? (repeatCustomers / totalCustomers) * 100 : 0;
  const avgClv = totalCustomers > 0 ? revenue / totalCustomers : 0;

  // MoM growth
  let momText = 'No prior month';
  if (lastMonthlyRev.length >= 2) {
    const prev = Number(lastMonthlyRev[lastMonthlyRev.length - 2].revenue) || 0;
    const curr = Number(lastMonthlyRev[lastMonthlyRev.length - 1].revenue) || 0;
    if (prev > 0) {
      const pct = ((curr - prev) / prev) * 100;
      momText = `${pct >= 0 ? '+' : ''}${pct.toFixed(1)}% MoM`;
    }
  }

  document.getElementById('kpiRevenue').textContent = formatCurrency(revenue);
  document.getElementById('kpiRevenueMoM').textContent = momText;
  document.getElementById('kpiOrders').textContent = orders.toLocaleString();
  document.getElementById('kpiOrdersSub').textContent = `${orders} order${orders === 1 ? '' : 's'} completed`;
  document.getElementById('kpiAOV').textContent = formatCurrency(aov);
  document.getElementById('kpiAOVSub').textContent = `Avg basket: ${formatCurrency(aov)}`;
  document.getElementById('kpiRepeat').textContent = `${repeatRate.toFixed(1)}%`;
  document.getElementById('kpiRepeatSub').textContent = `${repeatCustomers} of ${totalCustomers} returning`;
  document.getElementById('kpiCLV').textContent = formatCurrency(avgClv);
  document.getElementById('kpiCLVSub').textContent = `Across ${totalCustomers} customer${totalCustomers === 1 ? '' : 's'}`;
  document.getElementById('kpiProducts').textContent = `${cachedProducts.length} Product${cachedProducts.length === 1 ? '' : 's'}`;
  const inStockCount = cachedProducts.filter(p => Number(p.stockQuantity) > 0).length;
  document.getElementById('kpiCategoryCount').textContent = `${cachedCategories.length} Categories • ${inStockCount} in stock`;
}

// ─── Monthly Sales Target Widget ─────────────────────────────────────────────
function renderSalesTarget() {
  const now = new Date();
  const ym = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  const monthSales = allCachedSales.filter(s => String(s.orderDate || '').startsWith(ym));
  const achievedUSD = monthSales.reduce((acc, s) => acc + (Number(s.totalAmount) || 0), 0);

  const daysInMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate();
  const dayOfMonth = now.getDate();
  const daysLeft = Math.max(0, daysInMonth - dayOfMonth);
  const monthElapsedPct = (dayOfMonth / daysInMonth) * 100;

  const pct = monthlyTargetUSD > 0 ? (achievedUSD / monthlyTargetUSD) * 100 : 0;
  const projectedUSD = dayOfMonth > 0 ? (achievedUSD / dayOfMonth) * daysInMonth : 0;

  document.getElementById('targetAchievedVal').textContent = formatCurrency(achievedUSD);
  document.getElementById('targetGoalVal').textContent = formatCurrency(monthlyTargetUSD);
  document.getElementById('targetGoalSub').textContent = `Base: $${monthlyTargetUSD.toLocaleString()} USD`;
  document.getElementById('targetPercentVal').textContent = `${pct.toFixed(1)}%`;
  document.getElementById('targetPercentSub').textContent = `${monthElapsedPct.toFixed(0)}% of month done`;
  document.getElementById('targetProjectedVal').textContent = formatCurrency(projectedUSD);
  document.getElementById('targetDaysRemaining').textContent = `${daysLeft} days left`;

  document.getElementById('targetProgressBar').style.width = `${Math.min(100, pct)}%`;
  document.getElementById('targetTimeElapsedMarker').style.left = `${Math.min(100, monthElapsedPct)}%`;

  const badge = document.getElementById('targetPacingBadge');
  const reqEl = document.getElementById('targetRequirementText');
  if (achievedUSD >= monthlyTargetUSD) {
    badge.className = 'kpi-badge positive';
    badge.textContent = 'Target Met';
    reqEl.textContent = 'Monthly quota achieved!';
  } else if (pct >= monthElapsedPct - 5) {
    badge.className = 'kpi-badge positive';
    badge.textContent = 'On Track';
    const perDay = daysLeft > 0 ? (monthlyTargetUSD - achievedUSD) / daysLeft : 0;
    reqEl.textContent = `Need ${formatCurrency(perDay)}/day for the remaining ${daysLeft} days`;
  } else {
    badge.className = 'kpi-badge warning';
    badge.textContent = 'Behind Pace';
    const perDay = daysLeft > 0 ? (monthlyTargetUSD - achievedUSD) / daysLeft : 0;
    reqEl.textContent = `Need ${formatCurrency(perDay)}/day to hit monthly target`;
  }
}

// ─── Charts ──────────────────────────────────────────────────────────────────
function renderCharts() {
  const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
  const gridColor = isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.06)';
  const tickColor = isDark ? '#9CA3AF' : '#6B7280';

  const revCanvas = document.getElementById('revenueChart');
  if (revCanvas && window.Chart) {
    if (revenueChartInstance) revenueChartInstance.destroy();
    const labels = lastMonthlyRev.length ? lastMonthlyRev.map(m => m.month || m.label || '') : ['No Data'];
    const data = lastMonthlyRev.length ? lastMonthlyRev.map(m => (Number(m.revenue) || 0) * getCurrencyConfig().rate) : [0];
    document.getElementById('revTrendPeriod').textContent = `${lastMonthlyRev.length || 0} months`;

    revenueChartInstance = new Chart(revCanvas.getContext('2d'), {
      type: 'line',
      data: {
        labels,
        datasets: [{
          label: `Revenue (${currentCurrency})`,
          data,
          borderColor: '#007AFF',
          backgroundColor: 'rgba(0, 122, 255, 0.14)',
          borderWidth: 2.5,
          tension: 0.35,
          fill: true,
          pointRadius: 3.5
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          x: { grid: { display: false }, ticks: { color: tickColor } },
          y: { grid: { color: gridColor }, ticks: { color: tickColor } }
        }
      }
    });
  }

  const catCanvas = document.getElementById('categoryChart');
  if (catCanvas && window.Chart) {
    if (categoryChartInstance) categoryChartInstance.destroy();
    const hasData = lastCatSales.length > 0;
    const labels = hasData ? lastCatSales.map(c => c.categoryName || c.name || 'Category') : ['No Sales Yet'];
    const data = hasData ? lastCatSales.map(c => (Number(c.totalRevenue) || 0) * getCurrencyConfig().rate) : [1];
    const colors = hasData
      ? ['#007AFF', '#34C759', '#FF9500', '#AF52DE', '#FF3B30', '#00C7BE']
      : [isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.08)'];

    categoryChartInstance = new Chart(catCanvas.getContext('2d'), {
      type: 'doughnut',
      data: {
        labels,
        datasets: [{ data, backgroundColor: colors, borderWidth: 0 }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '66%',
        plugins: {
          legend: { position: 'bottom', labels: { color: tickColor, boxWidth: 11, padding: 14 } }
        }
      }
    });
  }
}

function renderAnalyticsCharts() {
  const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
  const gridColor = isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.06)';
  const tickColor = isDark ? '#9CA3AF' : '#6B7280';

  const revEl = document.getElementById('analyticsRevenueChart');
  if (revEl && window.Chart) {
    if (analyticsRevenueChartInstance) analyticsRevenueChartInstance.destroy();
    const labels = lastMonthlyRev.length ? lastMonthlyRev.map(m => m.month || '') : ['No Data'];
    const data = lastMonthlyRev.length ? lastMonthlyRev.map(m => (Number(m.revenue) || 0) * getCurrencyConfig().rate) : [0];
    analyticsRevenueChartInstance = new Chart(revEl.getContext('2d'), {
      type: 'bar',
      data: { labels, datasets: [{ label: `Revenue (${currentCurrency})`, data, backgroundColor: '#007AFF', borderRadius: 6 }] },
      options: {
        responsive: true, maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: { x: { grid: { display: false }, ticks: { color: tickColor } }, y: { grid: { color: gridColor }, ticks: { color: tickColor } } }
      }
    });
  }

  const ordEl = document.getElementById('analyticsOrdersChart');
  if (ordEl && window.Chart) {
    if (analyticsOrdersChartInstance) analyticsOrdersChartInstance.destroy();
    const labels = lastMonthlyRev.length ? lastMonthlyRev.map(m => m.month || '') : ['No Data'];
    const data = lastMonthlyRev.length ? lastMonthlyRev.map(m => Number(m.orderCount ?? m.orders ?? 0)) : [0];
    analyticsOrdersChartInstance = new Chart(ordEl.getContext('2d'), {
      type: 'line',
      data: { labels, datasets: [{ label: 'Orders', data, borderColor: '#AF52DE', backgroundColor: 'rgba(175,82,222,0.14)', fill: true, tension: 0.35 }] },
      options: {
        responsive: true, maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: { x: { grid: { display: false }, ticks: { color: tickColor } }, y: { grid: { color: gridColor }, ticks: { color: tickColor } } }
      }
    });
  }

  const payEl = document.getElementById('analyticsPaymentChart');
  if (payEl && window.Chart) {
    if (analyticsPaymentChartInstance) analyticsPaymentChartInstance.destroy();
    const byMethod = {};
    allCachedSales.forEach(s => {
      const m = s.paymentMethod || 'OTHER';
      byMethod[m] = (byMethod[m] || 0) + (Number(s.totalAmount) || 0) * getCurrencyConfig().rate;
    });
    const labels = Object.keys(byMethod).length ? Object.keys(byMethod) : ['No Orders'];
    const data = Object.values(byMethod).length ? Object.values(byMethod) : [0];
    analyticsPaymentChartInstance = new Chart(payEl.getContext('2d'), {
      type: 'bar',
      data: {
        labels,
        datasets: [{
          label: `Revenue (${currentCurrency})`,
          data,
          backgroundColor: ['#007AFF', '#34C759', '#FF9500', '#AF52DE', '#00C7BE'],
          borderRadius: 8
        }]
      },
      options: {
        responsive: true, maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: { x: { grid: { display: false }, ticks: { color: tickColor } }, y: { grid: { color: gridColor }, ticks: { color: tickColor } } }
      }
    });
  }
}

// ─── Top Products ────────────────────────────────────────────────────────────
function renderTopProducts() {
  const el = document.getElementById('topProductsList');
  if (!el) return;
  if (!lastTopProducts.length) {
    el.innerHTML = `<div class="empty-state-mini">No product sales recorded yet.</div>`;
    return;
  }
  el.innerHTML = lastTopProducts.slice(0, 5).map((p, idx) => `
    <div class="top-product-row">
      <div class="tp-left">
        <div class="tp-rank">#${idx + 1}</div>
        <div>
          <div class="tp-name">${escapeHtml(p.productName || p.name || 'Product')}</div>
          <div class="tp-meta">${Number(p.totalQuantitySold ?? p.unitsSold ?? 0)} units sold</div>
        </div>
      </div>
      <div class="tp-rev">${formatCurrency(p.totalRevenue ?? 0)}</div>
    </div>
  `).join('');
}

// ─── Country Flag Utility ─────────────────────────────────────────────────────
const COUNTRY_FLAGS = {
  'India': '🇮🇳', 'USA': '🇺🇸', 'Pakistan': '🇵🇰', 'China': '🇨🇳',
  'France': '🇫🇷', 'Germany': '🇩🇪', 'UAE': '🇦🇪', 'Saudi Arabia': '🇸🇦',
  'Kuwait': '🇰🇼', 'Australia': '🇦🇺', 'Brazil': '🇧🇷', 'Argentina': '🇦🇷',
  'Ghana': '🇬🇭', 'Nigeria': '🇳🇬', 'Japan': '🇯🇵', 'South Korea': '🇰🇷',
  'UK': '🇬🇧', 'Canada': '🇨🇦', 'Mexico': '🇲🇽', 'Turkey': '🇹🇷',
  'Italy': '🇮🇹', 'Spain': '🇪🇸', 'Netherlands': '🇳🇱', 'Sweden': '🇸🇪',
  'Switzerland': '🇨🇭', 'Singapore': '🇸🇬', 'Malaysia': '🇲🇾', 'Indonesia': '🇮🇩',
  'Thailand': '🇹🇭', 'Vietnam': '🇻🇳', 'Philippines': '🇵🇭', 'Bangladesh': '🇧🇩',
  'Sri Lanka': '🇱🇰', 'Egypt': '🇪🇬', 'Kenya': '🇰🇪', 'South Africa': '🇿🇦',
  'Morocco': '🇲🇦', 'Ethiopia': '🇪🇹', 'Tanzania': '🇹🇿', 'Russia': '🇷🇺',
  'Poland': '🇵🇱', 'Ukraine': '🇺🇦', 'Romania': '🇷🇴', 'Czech Republic': '🇨🇿',
  'Hungary': '🇭🇺', 'Portugal': '🇵🇹', 'Greece': '🇬🇷', 'Israel': '🇮🇱',
  'Jordan': '🇯🇴', 'Qatar': '🇶🇦', 'Bahrain': '🇧🇭', 'Oman': '🇴🇲',
  'Iraq': '🇮🇶', 'Iran': '🇮🇷', 'Lebanon': '🇱🇧', 'Yemen': '🇾🇪',
  'Chile': '🇨🇱', 'Colombia': '🇨🇴', 'Peru': '🇵🇪', 'Venezuela': '🇻🇪',
  'Ecuador': '🇪🇨', 'Bolivia': '🇧🇴', 'Paraguay': '🇵🇾', 'Uruguay': '🇺🇾',
  'New Zealand': '🇳🇿', 'Norway': '🇳🇴', 'Denmark': '🇩🇰', 'Finland': '🇫🇮',
  'Belgium': '🇧🇪', 'Austria': '🇦🇹', 'Ireland': '🇮🇪'
};

function getCountryFlag(country) {
  return COUNTRY_FLAGS[country] || '🌍';
}

// ─── VIP Customer Leaderboard ────────────────────────────────────────────────
function renderLeaderboard() {
  const podium = document.getElementById('leaderboardPodium');
  const tbody = document.getElementById('leaderboardTableBody');
  if (!podium || !tbody) return;

  const list = [...cachedCustomerStats].sort((a, b) => {
    if (leaderboardSortBy === 'orders') return (Number(b.totalOrders) || 0) - (Number(a.totalOrders) || 0);
    return (Number(b.totalSpent) || 0) - (Number(a.totalSpent) || 0);
  });

  if (list.length === 0) {
    podium.innerHTML = `<div class="podium-loading">No customer activity yet — add customers &amp; sales to see rankings.</div>`;
    tbody.innerHTML = `<tr><td colspan="7" class="empty-cell">No customer records found.</td></tr>`;
    return;
  }

  const top3 = list.slice(0, 3);
  const podiumOrder = top3.length === 3 ? [top3[1], top3[0], top3[2]] : top3;
  const ranks = top3.length === 3 ? [2, 1, 3] : top3.map((_, i) => i + 1);

  podium.innerHTML = podiumOrder.map((c, idx) => {
    const rank = ranks[idx];
    const name = `${c.firstName || ''} ${c.lastName || ''}`.trim() || c.email || 'Customer';
    const initials = name.split(' ').map(s => s[0]).join('').slice(0, 2).toUpperCase();
    const flag = getCountryFlag(c.country || '');
    const countryLabel = c.country ? `${flag} ${c.country}` : '';
    return `
      <div class="podium-card rank-${rank}">
        <span class="podium-rank-pill">#${rank} ${rank === 1 ? '🏆 VIP' : ''}</span>
        <div class="podium-avatar">${escapeHtml(initials || 'C')}</div>
        <div class="podium-name">${escapeHtml(name)}</div>
        ${countryLabel ? `<div class="podium-country">${escapeHtml(countryLabel)}</div>` : ''}
        <div class="podium-spend">${formatCurrency(c.totalSpent || 0)}</div>
        <div class="podium-orders">${Number(c.totalOrders || 0)} orders</div>
      </div>
    `;
  }).join('');

  tbody.innerHTML = list.slice(0, 20).map((c, idx) => {
    const spent = Number(c.totalSpent || 0);
    const orders = Number(c.totalOrders || 0);
    const avg = orders > 0 ? spent / orders : 0;
    const tier = spent >= 2000 ? '💎 Diamond' : spent >= 1000 ? '🥇 Platinum' : spent >= 400 ? '🥈 Gold' : '🥉 Standard';
    const tierClass = spent >= 2000 ? 'positive' : spent >= 1000 ? 'positive' : spent >= 400 ? 'warning' : 'info';
    const name = `${c.firstName || ''} ${c.lastName || ''}`.trim() || c.email || 'Customer';
    const flag = getCountryFlag(c.country || '');
    return `
      <tr>
        <td><strong>#${idx + 1}</strong></td>
        <td><strong>${escapeHtml(name)}</strong><br><small style="color:var(--text-muted)">${escapeHtml(c.email || '')}</small></td>
        <td>${flag} ${escapeHtml(c.country || '—')}</td>
        <td><strong>${formatCurrency(spent)}</strong></td>
        <td>${orders}</td>
        <td>${formatCurrency(avg)}</td>
        <td><span class="kpi-badge ${tierClass}">${tier}</span></td>
      </tr>
    `;
  }).join('');

  // Country breakdown
  renderCountryBreakdown(list);
}

// ─── Country Breakdown Chart ──────────────────────────────────────────────────
function renderCountryBreakdown(customerList) {
  const el = document.getElementById('countryBreakdownList');
  if (!el) return;

  const byCountry = {};
  customerList.forEach(c => {
    const country = c.country || 'Unknown';
    if (!byCountry[country]) byCountry[country] = { orders: 0, revenue: 0, customers: 0 };
    byCountry[country].orders += Number(c.totalOrders || 0);
    byCountry[country].revenue += Number(c.totalSpent || 0);
    byCountry[country].customers += 1;
  });

  const sorted = Object.entries(byCountry).sort((a, b) => b[1].revenue - a[1].revenue);
  const maxRev = sorted.length ? sorted[0][1].revenue : 1;

  el.innerHTML = sorted.map(([country, data]) => {
    const pct = maxRev > 0 ? (data.revenue / maxRev) * 100 : 0;
    const flag = getCountryFlag(country);
    return `
      <div class="country-row">
        <div class="country-info">
          <span class="country-flag-label">${flag} ${escapeHtml(country)}</span>
          <span class="country-meta">${data.customers} cust · ${data.orders} orders</span>
        </div>
        <div class="country-bar-wrap">
          <div class="country-bar" style="width:${pct.toFixed(1)}%"></div>
        </div>
        <div class="country-rev">${formatCurrency(data.revenue)}</div>
      </div>
    `;
  }).join('');
}

// ─── Sales Tables ────────────────────────────────────────────────────────────
function renderSalesTables() {
  const recentBody = document.getElementById('salesTableBody');
  const allBody = document.getElementById('allSalesTableBody');
  const filtered = getFilteredSales();

  const buildRows = (rows) => {
    if (!rows.length) {
      return `<tr><td colspan="7" class="empty-cell">No sales orders recorded yet. Click "+ New Sale" to create one.</td></tr>`;
    }
    return rows.map(s => {
      const firstItem = Array.isArray(s.items) && s.items.length ? s.items[0] : null;
      const productLabel = firstItem
        ? `${firstItem.productName || 'Product'}${s.items.length > 1 ? ` (+${s.items.length - 1})` : ''}`
        : '—';
      const qty = Array.isArray(s.items) ? s.items.reduce((a, i) => a + (Number(i.quantity) || 0), 0) : 0;
      const dateStr = s.orderDate ? new Date(s.orderDate).toLocaleDateString() : '—';
      return `
        <tr style="cursor:pointer" onclick="openInvoice(${s.id})">
          <td><strong>${escapeHtml(s.orderNumber || `#${s.id}`)}</strong></td>
          <td>${escapeHtml(s.customerName || 'Customer')}</td>
          <td>${escapeHtml(productLabel)}</td>
          <td>${qty}</td>
          <td><strong>${formatCurrency(s.totalAmount || 0)}</strong></td>
          <td>${escapeHtml(dateStr)}</td>
          <td><span class="kpi-badge positive">${escapeHtml(s.status || 'COMPLETED')}</span></td>
        </tr>
      `;
    }).join('');
  };

  if (recentBody) recentBody.innerHTML = buildRows(filtered.slice(0, 8));
  if (allBody) allBody.innerHTML = buildRows(allCachedSales);
}

window.openInvoice = function (saleId) {
  const sale = allCachedSales.find(s => s.id === saleId);
  if (!sale) return;
  currentInvoiceSaleId = saleId;
  document.getElementById('invOrderTitle').textContent = `Invoice ${sale.orderNumber || '#' + sale.id}`;
  document.getElementById('invOrderMeta').textContent = sale.orderDate ? new Date(sale.orderDate).toLocaleString() : '';
  document.getElementById('invStatusBadge').textContent = sale.status || 'COMPLETED';
  document.getElementById('invCustomerName').textContent = sale.customerName || '—';
  document.getElementById('invPaymentMethod').textContent = sale.paymentMethod || '—';
  document.getElementById('invGrandTotal').textContent = formatCurrency(sale.totalAmount || 0);

  const items = Array.isArray(sale.items) ? sale.items : [];
  document.getElementById('invItemsBody').innerHTML = items.length
    ? items.map(i => `
        <tr>
          <td>${escapeHtml(i.productName || 'Item')}</td>
          <td>${Number(i.quantity || 0)}</td>
          <td>${formatCurrency(i.unitPrice || 0)}</td>
          <td><strong>${formatCurrency(i.subtotal || 0)}</strong></td>
        </tr>`).join('')
    : `<tr><td colspan="4" class="empty-cell">No line items</td></tr>`;
  openModal('invoiceModal');
};

// ─── Categories, Products, Customers Tables ──────────────────────────────────
function renderCategoriesTable() {
  const tbody = document.getElementById('categoriesTableBody');
  if (!tbody) return;
  if (!cachedCategories.length) {
    tbody.innerHTML = `<tr><td colspan="5" class="empty-cell">No categories yet. Sign in as Admin and add your first category.</td></tr>`;
    return;
  }
  tbody.innerHTML = cachedCategories.map(c => `
    <tr>
      <td>#${c.id}</td>
      <td><strong>${escapeHtml(c.name)}</strong></td>
      <td>${escapeHtml(c.description || '—')}</td>
      <td><span class="kpi-badge info">${Number(c.productCount ?? 0)}</span></td>
      <td class="admin-only">
        <button class="btn btn-danger btn-sm" onclick="deleteCategory(${c.id})">Delete</button>
      </td>
    </tr>
  `).join('');
}

function renderProductsTable() {
  const tbody = document.getElementById('productsTableBody');
  if (!tbody) return;
  const q = (document.getElementById('productSearch')?.value || '').trim().toLowerCase();
  const list = q
    ? cachedProducts.filter(p => String(p.name || '').toLowerCase().includes(q) || String(p.sku || '').toLowerCase().includes(q))
    : cachedProducts;

  if (!list.length) {
    tbody.innerHTML = `<tr><td colspan="7" class="empty-cell">No products found. Add categories first, then add products.</td></tr>`;
    return;
  }
  tbody.innerHTML = list.map(p => {
    const stock = Number(p.stockQuantity || 0);
    const statusBadge = stock === 0
      ? `<span class="kpi-badge danger">Out of Stock</span>`
      : stock <= 10
        ? `<span class="kpi-badge warning">Low Stock</span>`
        : `<span class="kpi-badge positive">In Stock</span>`;
    return `
      <tr>
        <td>#${p.id}</td>
        <td><strong>${escapeHtml(p.name)}</strong><br><small style="color:var(--text-muted)">${escapeHtml(p.sku || '')}</small></td>
        <td>${escapeHtml(p.categoryName || '—')}</td>
        <td><strong>${formatCurrency(p.price || 0)}</strong></td>
        <td>${stock}</td>
        <td>${statusBadge}</td>
        <td class="admin-only">
          <div style="display:flex;gap:6px;">
            <button class="btn btn-secondary btn-sm" onclick="openRestock(${p.id}, '${escapeHtml(p.name)}')">Restock</button>
            <button class="btn btn-danger btn-sm" onclick="deleteProduct(${p.id})">Delete</button>
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

function renderCustomersTable() {
  const tbody = document.getElementById('customersTableBody');
  if (!tbody) return;
  const source = cachedCustomerStats.length ? cachedCustomerStats : cachedCustomers;
  if (!source.length) {
    tbody.innerHTML = `<tr><td colspan="7" class="empty-cell">No customers registered yet. Click "+ Add Customer" to add one.</td></tr>`;
    return;
  }
  tbody.innerHTML = source.map(c => `
    <tr>
      <td>#${c.id}</td>
      <td><strong>${escapeHtml(`${c.firstName || ''} ${c.lastName || ''}`.trim())}</strong></td>
      <td>${escapeHtml(c.email || '—')}</td>
      <td>${escapeHtml(c.phone || '—')}</td>
      <td>${Number(c.totalOrders ?? 0)}</td>
      <td><strong>${formatCurrency(c.totalSpent ?? 0)}</strong></td>
      <td class="admin-only"><span class="kpi-badge info">${escapeHtml(c.country || 'Active')}</span></td>
    </tr>
  `).join('');
}

// ─── Modals & CRUD Actions ───────────────────────────────────────────────────
function openModal(id) {
  document.getElementById(id)?.classList.add('open');
  refreshIcons();
}

function closeModal(id) {
  document.getElementById(id)?.classList.remove('open');
}

function populateModalSelects() {
  const catSel = document.getElementById('newProdCategory');
  if (catSel) {
    catSel.innerHTML = `<option value="">Select category…</option>` +
      cachedCategories.map(c => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join('');
  }
  const custSel = document.getElementById('newSaleCustomer');
  if (custSel) {
    custSel.innerHTML = `<option value="">Select customer…</option>` +
      cachedCustomers.map(c => `<option value="${c.id}">${escapeHtml(`${c.firstName} ${c.lastName}`)} (${escapeHtml(c.email)})</option>`).join('');
  }
  const prodSel = document.getElementById('newSaleProduct');
  if (prodSel) {
    prodSel.innerHTML = `<option value="">Select product…</option>` +
      cachedProducts.map(p => `<option value="${p.id}" data-price="${p.price}">${escapeHtml(p.name)} — ${formatCurrency(p.price)} (${p.stockQuantity} left)</option>`).join('');
  }
}

function initModals() {
  // Set Target
  document.getElementById('openSetTargetModalBtn')?.addEventListener('click', () => {
    document.getElementById('targetInputUSD').value = monthlyTargetUSD;
    openModal('setTargetModal');
  });
  document.getElementById('cancelTargetBtn')?.addEventListener('click', () => closeModal('setTargetModal'));
  document.getElementById('saveTargetBtn')?.addEventListener('click', () => {
    const val = Number(document.getElementById('targetInputUSD').value);
    if (val > 0) {
      monthlyTargetUSD = val;
      localStorage.setItem('monthlySalesTargetUSD', String(val));
      renderSalesTarget();
      closeModal('setTargetModal');
      showToast('Monthly sales target updated', 'success');
    }
  });

  // New Category
  document.getElementById('addCategoryBtn')?.addEventListener('click', () => openModal('newCategoryModal'));
  document.getElementById('cancelCatBtn')?.addEventListener('click', () => closeModal('newCategoryModal'));
  document.getElementById('saveCatBtn')?.addEventListener('click', async () => {
    const name = document.getElementById('newCatName').value.trim();
    const description = document.getElementById('newCatDesc').value.trim();
    if (!name) return showToast('Category name is required', 'error');
    try {
      await SalesAPI.createCategory({ name, description });
      closeModal('newCategoryModal');
      document.getElementById('newCatName').value = '';
      document.getElementById('newCatDesc').value = '';
      showToast('Category created', 'success');
      addNotification('Category added', name);
      loadAllData();
    } catch (e) { showToast(e.message, 'error'); }
  });

  // New Product
  document.getElementById('addProductBtn')?.addEventListener('click', () => openModal('newProductModal'));
  document.getElementById('cancelProdBtn')?.addEventListener('click', () => closeModal('newProductModal'));
  document.getElementById('saveProdBtn')?.addEventListener('click', async () => {
    const name = document.getElementById('newProdName').value.trim();
    const categoryId = Number(document.getElementById('newProdCategory').value);
    const price = Number(document.getElementById('newProdPrice').value);
    const stockQuantity = Number(document.getElementById('newProdStock').value);
    const sku = document.getElementById('newProdSku').value.trim() || `SKU-${Date.now().toString().slice(-6)}`;
    const description = document.getElementById('newProdDesc').value.trim();
    if (!name || !categoryId || !(price > 0)) return showToast('Name, category, and positive price are required', 'error');
    try {
      await SalesAPI.createProduct({ name, categoryId, price, stockQuantity, sku, description });
      closeModal('newProductModal');
      showToast('Product created', 'success');
      addNotification('Product added', `${name} (${sku})`);
      loadAllData();
    } catch (e) { showToast(e.message, 'error'); }
  });

  // New Customer
  document.getElementById('addCustomerBtn')?.addEventListener('click', () => openModal('newCustomerModal'));
  document.getElementById('cancelCustBtn')?.addEventListener('click', () => closeModal('newCustomerModal'));
  document.getElementById('saveCustBtn')?.addEventListener('click', async () => {
    const firstName = document.getElementById('newCustFirst').value.trim();
    const lastName = document.getElementById('newCustLast').value.trim();
    const email = document.getElementById('newCustEmail').value.trim();
    const phone = document.getElementById('newCustPhone').value.trim();
    const city = document.getElementById('newCustCity').value.trim();
    const country = document.getElementById('newCustCountry').value.trim();
    if (!firstName || !lastName || !email) return showToast('First name, last name, and email are required', 'error');
    try {
      await SalesAPI.createCustomer({ firstName, lastName, email, phone, city, country });
      closeModal('newCustomerModal');
      showToast('Customer added', 'success');
      addNotification('New customer registered', `${firstName} ${lastName}`);
      loadAllData();
    } catch (e) { showToast(e.message, 'error'); }
  });

  // New Sale (with International Razorpay Checkout support)
  const RZP_FX = {
    INR: { rate: 84.50, symbol: '₹', decimals: 2 },
    USD: { rate: 1.00,  symbol: '$', decimals: 2 },
    AED: { rate: 3.67,  symbol: 'AED ', decimals: 2 },
    EUR: { rate: 0.95,  symbol: '€', decimals: 2 },
    GBP: { rate: 0.79,  symbol: '£', decimals: 2 },
    SGD: { rate: 1.34,  symbol: 'S$', decimals: 2 },
    AUD: { rate: 1.54,  symbol: 'A$', decimals: 2 },
    CAD: { rate: 1.40,  symbol: 'C$', decimals: 2 },
    JPY: { rate: 154.00, symbol: '¥', decimals: 0 },
    CNY: { rate: 7.24,  symbol: '¥', decimals: 2 }
  };

  const updateRazorpayPreview = () => {
    const pm = document.getElementById('newSalePayment')?.value;
    const btn = document.getElementById('submitNewSaleBtn');
    const box = document.getElementById('razorpayIntlBox');
    const preview = document.getElementById('razorpayAmountPreview');
    const chargeSel = document.getElementById('razorpayChargeCurrency');
    const isRzp = pm === 'RAZORPAY' || pm === 'RAZORPAY_QR';
    if (btn) {
      btn.innerHTML = pm === 'RAZORPAY_QR'
        ? `<i data-lucide="qr-code"></i> Show Scan &amp; Pay QR`
        : pm === 'RAZORPAY'
          ? `<i data-lucide="credit-card"></i> Pay with Razorpay &amp; Create Order`
          : `<i data-lucide="plus"></i> Create Order`;
      refreshIcons();
    }
    if (box) box.style.display = isRzp ? 'block' : 'none';
    if (isRzp && preview && chargeSel) {
      const qty = Number(document.getElementById('newSaleQty')?.value) || 1;
      const unitUsd = Number(document.getElementById('newSalePrice')?.value) || 0;
      const totalUsd = unitUsd * qty;
      const cur = chargeSel.value || 'INR';
      const fx = RZP_FX[cur] || RZP_FX.INR;
      const converted = (totalUsd * fx.rate).toFixed(fx.decimals);
      preview.innerHTML = totalUsd > 0
        ? `Charge: <strong>${fx.symbol}${converted} ${cur}</strong> (Base: $${totalUsd.toFixed(2)} USD) • Key: <code>${razorpayKeyId}</code>`
        : `Select customer &amp; product to preview international charge`;
    }
  };
  const openSale = () => {
    const chargeSel = document.getElementById('razorpayChargeCurrency');
    if (chargeSel && RZP_FX[currentCurrency]) chargeSel.value = currentCurrency;
    openModal('newSaleModal');
    updateRazorpayPreview();
  };
  document.getElementById('newSaleBtn')?.addEventListener('click', openSale);
  document.getElementById('createSaleBtn')?.addEventListener('click', openSale);
  document.getElementById('cancelNewSaleBtn')?.addEventListener('click', () => closeModal('newSaleModal'));
  document.getElementById('newSalePayment')?.addEventListener('change', updateRazorpayPreview);
  document.getElementById('razorpayChargeCurrency')?.addEventListener('change', updateRazorpayPreview);
  document.getElementById('newSaleQty')?.addEventListener('input', updateRazorpayPreview);
  document.getElementById('newSalePrice')?.addEventListener('input', updateRazorpayPreview);
  document.getElementById('newSaleProduct')?.addEventListener('change', e => {
    const opt = e.target.selectedOptions[0];
    if (opt && opt.dataset.price) document.getElementById('newSalePrice').value = opt.dataset.price;
    updateRazorpayPreview();
  });
  document.getElementById('submitNewSaleBtn')?.addEventListener('click', async () => {
    const customerId = Number(document.getElementById('newSaleCustomer').value);
    const productId = Number(document.getElementById('newSaleProduct').value);
    const quantity = Number(document.getElementById('newSaleQty').value) || 1;
    const paymentMethod = document.getElementById('newSalePayment').value || 'CREDIT_CARD';
    const notes = document.getElementById('newSaleNotes').value.trim();
    const errEl = document.getElementById('newSaleError');
    errEl.style.display = 'none';
    if (!customerId || !productId || quantity < 1) {
      errEl.textContent = 'Select a customer, product, and valid quantity.';
      errEl.style.display = 'block';
      return;
    }

    const finalizeOrder = async (extraNote = '') => {
      const combinedNotes = [notes, extraNote].filter(Boolean).join(' | ');
      await SalesAPI.createSale({ customerId, paymentMethod, notes: combinedNotes, items: [{ productId, quantity }] });
      closeModal('newSaleModal');
      showToast('Sale order created!', 'success');
      addNotification('New sale order created', `Qty ${quantity} • ${paymentMethod}`);
      loadAllData();
    };

    if (paymentMethod === 'RAZORPAY' || paymentMethod === 'RAZORPAY_QR') {
      const prod = cachedProducts.find(p => p.id === productId);
      const cust = cachedCustomers.find(c => c.id === customerId);
      const unitUsd = Number(document.getElementById('newSalePrice').value) || Number(prod?.price || 0);
      const totalUsd = unitUsd * quantity;
      const rzpCurrency = document.getElementById('razorpayChargeCurrency')?.value || 'INR';
      const fx = RZP_FX[rzpCurrency] || RZP_FX.INR;
      const multiplier = rzpCurrency === 'JPY' ? 1 : 100;
      const amountSubunits = Math.max(100, Math.round(totalUsd * fx.rate * multiplier));
      const inrAmount = (totalUsd * RZP_FX.INR.rate).toFixed(2);

      const openRazorpayCheckout = () => {
        if (typeof window.Razorpay !== 'function') {
          errEl.textContent = 'Razorpay Checkout SDK failed to load. Check your internet connection.';
          errEl.style.display = 'block';
          return;
        }
        const rzp = new window.Razorpay({
          key: razorpayKeyId,
          amount: amountSubunits,
          currency: rzpCurrency,
          name: 'Sales Analytics Enterprise',
          description: `${prod?.name || 'Product'} × ${quantity} (${cust?.country || 'Global'})`,
          prefill: {
            name: cust ? `${cust.firstName || ''} ${cust.lastName || ''}`.trim() : 'Customer',
            email: cust?.email || ownerEmail,
            contact: cust?.phone || '+919999999999'
          },
          notes: {
            customer_country: cust?.country || 'International',
            customer_city: cust?.city || '',
            charge_currency: rzpCurrency,
            usd_equivalent: `$${totalUsd.toFixed(2)}`
          },
          theme: { color: '#007AFF' },
          handler: async (response) => {
            try {
              closeModal('razorpayQrModal');
              await finalizeOrder(`Razorpay ID: ${response.razorpay_payment_id} (${rzpCurrency})`);
              showToast(`International Razorpay Payment Success (${response.razorpay_payment_id})`, 'success');
            } catch (e) {
              errEl.textContent = e.message;
              errEl.style.display = 'block';
            }
          }
        });
        rzp.on('payment.failed', (resp) => {
          errEl.textContent = resp?.error?.description || 'Razorpay payment failed';
          errEl.style.display = 'block';
        });
        rzp.open();
      };

      if (paymentMethod === 'RAZORPAY_QR') {
        const upiUrl = `upi://pay?pa=razorpay.test@icici&pn=Sales%20Analytics%20Enterprise&am=${inrAmount}&cu=INR&tn=${encodeURIComponent((prod?.name || 'Order') + ' x' + quantity)}`;
        document.getElementById('rzpQrImage').src = `https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(upiUrl)}`;
        document.getElementById('rzpQrSubtitle').textContent = `${prod?.name || 'Order'} × ${quantity} • ${cust ? `${cust.firstName} ${cust.lastName}` : 'Customer'}`;
        document.getElementById('rzpQrAmountLabel').textContent = `₹${inrAmount} INR (${formatCurrency(totalUsd)})`;
        document.getElementById('rzpQrMeta').textContent = `Razorpay Key: ${razorpayKeyId} • Scan with GPay / PhonePe / Paytm / UPI`;
        document.getElementById('rzpQrCancelBtn').onclick = () => closeModal('razorpayQrModal');
        document.getElementById('rzpQrOpenCheckoutBtn').onclick = () => openRazorpayCheckout();
        document.getElementById('rzpQrConfirmPaidBtn').onclick = async () => {
          try {
            closeModal('razorpayQrModal');
            await finalizeOrder(`Razorpay Scan & Pay QR (${rzpCurrency})`);
            showToast('Scan & Pay QR order recorded!', 'success');
          } catch (e) {
            errEl.textContent = e.message;
            errEl.style.display = 'block';
          }
        };
        openModal('razorpayQrModal');
        return;
      }

      openRazorpayCheckout();
      return;
    }

    try {
      await finalizeOrder();
    } catch (e) {
      errEl.textContent = e.message;
      errEl.style.display = 'block';
    }
  });

  // Restock
  document.getElementById('cancelRestockBtn')?.addEventListener('click', () => closeModal('restockModal'));
  document.getElementById('saveRestockBtn')?.addEventListener('click', async () => {
    const id = Number(document.getElementById('restockProductId').value);
    const addUnits = Number(document.getElementById('restockUnitsInput').value);
    const prod = cachedProducts.find(p => p.id === id);
    if (!prod || !(addUnits > 0)) return;
    try {
      await SalesAPI.updateProduct(id, {
        name: prod.name,
        sku: prod.sku,
        price: prod.price,
        stockQuantity: Number(prod.stockQuantity || 0) + addUnits,
        categoryId: prod.categoryId,
        description: prod.description || ''
      });
      closeModal('restockModal');
      showToast(`Restocked +${addUnits} units for ${prod.name}`, 'success');
      loadAllData();
    } catch (e) { showToast(e.message, 'error'); }
  });

  // Invoice Print / Email / Close
  document.getElementById('closeInvoiceBtn')?.addEventListener('click', () => closeModal('invoiceModal'));
  document.getElementById('printInvoiceBtn')?.addEventListener('click', () => window.print());
  document.getElementById('emailInvoiceBtn')?.addEventListener('click', async () => {
    if (!currentInvoiceSaleId) return;
    const sale = allCachedSales.find(s => s.id === currentInvoiceSaleId);
    const defaultTo = sale?.customerEmail || ownerEmail;
    const to = prompt('Send invoice email to:', defaultTo);
    if (!to) return;
    try {
      showToast('Sending invoice email…', 'info');
      await SalesAPI.emailInvoice(currentInvoiceSaleId, to);
      showToast(`Invoice emailed to ${to}`, 'success');
      addNotification('Invoice emailed', `${sale?.orderNumber || '#' + currentInvoiceSaleId} → ${to}`);
    } catch (e) {
      showToast('Email failed: ' + e.message, 'error');
    }
  });
}

window.openRestock = function (id, name) {
  document.getElementById('restockProductId').value = id;
  document.getElementById('restockProductLabel').textContent = name;
  openModal('restockModal');
};

window.deleteCategory = async function (id) {
  if (!confirm('Delete this category?')) return;
  try {
    await SalesAPI.deleteCategory(id);
    showToast('Category deleted', 'success');
    loadAllData();
  } catch (e) { showToast(e.message, 'error'); }
};

window.deleteProduct = async function (id) {
  if (!confirm('Delete this product?')) return;
  try {
    await SalesAPI.deleteProduct(id);
    showToast('Product deleted', 'success');
    loadAllData();
  } catch (e) { showToast(e.message, 'error'); }
};

// ─── Real-Time SSE Stream & System Telemetry ─────────────────────────────────
function initRealtimeSSE() {
  if (typeof EventSource === 'undefined') return;
  try {
    if (realtimeEventSource) realtimeEventSource.close();
    const sseBase = typeof API_BASE !== 'undefined' ? API_BASE : 'http://localhost:8080/api';
    realtimeEventSource = new EventSource(`${sseBase}/realtime/stream`);
    realtimeEventSource.addEventListener('activity', e => {
      try {
        const evt = JSON.parse(e.data);
        addNotification(evt.type.replace(/_/g, ' '), evt.message);
        if (evt.type === 'SALE_CREATED' || evt.type === 'SALE_DELETED') {
          loadAllData(true);
        }
      } catch (_) {}
    });
    realtimeEventSource.onerror = () => {
      if (realtimeEventSource) {
        realtimeEventSource.close();
        realtimeEventSource = null;
      }
      setTimeout(initRealtimeSSE, 10000);
    };
  } catch (_) {}

  // Fetch initial telemetry
  SalesAPI.getSystemTelemetry().then(t => {
    if (!t) return;
    const el = document.getElementById('cacheLatencyLabel');
    if (el) el.title = `JVM Heap: ${t.jvmUsedMemoryMb}/${t.jvmMaxMemoryMb} MB • SSE Clients: ${t.activeSseClients}`;
  });
}

// ─── Spotlight Command Palette (Ctrl+K) ──────────────────────────────────────
function initCommandPalette() {
  const openCmd = () => {
    openModal('commandModal');
    const inp = document.getElementById('commandSearchInput');
    if (inp) {
      inp.value = '';
      renderCommandResults('');
      setTimeout(() => inp.focus(), 50);
    }
  };

  document.getElementById('commandPaletteBtn')?.addEventListener('click', openCmd);
  document.addEventListener('keydown', e => {
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
      e.preventDefault();
      openCmd();
    } else if (e.key === 'Escape') {
      closeModal('commandModal');
    }
  });
  document.getElementById('commandModal')?.addEventListener('click', e => {
    if (e.target.id === 'commandModal') closeModal('commandModal');
  });
  document.getElementById('commandSearchInput')?.addEventListener('input', e => {
    renderCommandResults(e.target.value);
  });
}

function renderCommandResults(query) {
  const container = document.getElementById('commandResults');
  if (!container) return;
  const q = query.trim().toLowerCase();

  const quickActions = [
    { title: 'Create New Sale Order (Razorpay / Card)', sub: 'Action • Open New Sale modal', badge: 'Action', run: () => document.getElementById('newSaleBtn')?.click() },
    { title: 'Toggle Light / Dark Theme', sub: 'Appearance • Switch macOS theme', badge: 'Theme', run: () => document.getElementById('themeToggle')?.click() },
    { title: 'Export Sales CSV Report', sub: 'Export • Download full sales orders CSV', badge: 'Export', run: () => document.getElementById('exportSalesBtn')?.click() },
    { title: 'Open Advanced Reports & Payment Mix', sub: 'Navigate • Analytics tab', badge: 'Tab', run: () => document.querySelector('.nav-item[data-tab="analytics"]')?.click() }
  ];

  const items = [];
  quickActions.forEach(a => {
    if (!q || a.title.toLowerCase().includes(q) || a.sub.toLowerCase().includes(q)) items.push(a);
  });
  cachedProducts.forEach(p => {
    if (!q || `${p.name} ${p.sku} ${p.categoryName}`.toLowerCase().includes(q)) {
      items.push({
        title: p.name,
        sub: `${p.sku || ''} • Stock: ${p.stockQuantity} • ${formatCurrency(p.price)}`,
        badge: 'Product',
        run: () => document.querySelector('.nav-item[data-tab="products"]')?.click()
      });
    }
  });
  cachedCustomers.forEach(c => {
    const full = `${c.firstName || ''} ${c.lastName || ''}`.trim();
    if (!q || `${full} ${c.email} ${c.country}`.toLowerCase().includes(q)) {
      items.push({
        title: `${getCountryFlag(c.country)} ${full}`,
        sub: `${c.email} • ${c.city || ''}, ${c.country || ''}`,
        badge: 'Customer',
        run: () => document.querySelector('.nav-item[data-tab="customers"]')?.click()
      });
    }
  });
  allCachedSales.slice(0, 20).forEach(s => {
    if (!q || `${s.orderNumber} ${s.customerName} ${s.paymentMethod}`.toLowerCase().includes(q)) {
      items.push({
        title: `Order ${s.orderNumber || '#' + s.id}`,
        sub: `${s.customerName} • ${s.paymentMethod} • ${formatCurrency(s.totalAmount)}`,
        badge: 'Order',
        run: () => openInvoice(s.id)
      });
    }
  });

  const sliced = items.slice(0, 12);
  if (!sliced.length) {
    container.innerHTML = `<div class="empty-state-mini">No matches for "${escapeHtml(query)}"</div>`;
    return;
  }
  container.innerHTML = sliced.map((it, idx) => `
    <div class="cmd-item" data-cmd-idx="${idx}">
      <div>
        <div class="cmd-item-title">${escapeHtml(it.title)}</div>
        <div class="cmd-item-sub">${escapeHtml(it.sub)}</div>
      </div>
      <span class="cmd-item-badge">${escapeHtml(it.badge)}</span>
    </div>
  `).join('');
  container.querySelectorAll('.cmd-item').forEach(el => {
    el.addEventListener('click', () => {
      const idx = Number(el.dataset.cmdIdx);
      closeModal('commandModal');
      sliced[idx]?.run();
    });
  });
}

// ─── Bulk CSV Product Import ─────────────────────────────────────────────────
function initCsvProductImport() {
  document.getElementById('openCsvImportBtn')?.addEventListener('click', () => {
    document.getElementById('csvImportStatus').textContent = '';
    openModal('csvImportModal');
  });
  document.getElementById('cancelCsvImportBtn')?.addEventListener('click', () => closeModal('csvImportModal'));
  document.getElementById('downloadSampleCsvBtn')?.addEventListener('click', () => {
    const sample = 'name,sku,categoryId,price,stockQuantity,description\nPro Wireless Mouse,SKU-9001,1,49.99,40,Ergonomic Bluetooth Mouse\n4K USB-C Hub,SKU-9002,1,79.50,25,Multi-port aluminum hub\n';
    const blob = new Blob([sample], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'sample_products_import.csv';
    a.click();
    URL.revokeObjectURL(url);
  });
  document.getElementById('runCsvImportBtn')?.addEventListener('click', async () => {
    const file = document.getElementById('csvProductFile')?.files?.[0];
    const statusEl = document.getElementById('csvImportStatus');
    if (!file) {
      statusEl.textContent = 'Please select a CSV file first.';
      return;
    }
    const text = await file.text();
    const lines = text.split(/\r?\n/).map(l => l.trim()).filter(Boolean);
    if (lines.length < 2) {
      statusEl.textContent = 'CSV must contain a header row and at least one product row.';
      return;
    }
    let ok = 0;
    let fail = 0;
    statusEl.textContent = `Importing ${lines.length - 1} product(s)…`;
    for (let i = 1; i < lines.length; i++) {
      const cols = lines[i].split(',').map(c => c.trim().replace(/^"|"$/g, ''));
      const [name, sku, categoryId, price, stockQuantity, description] = cols;
      if (!name || !categoryId || !(Number(price) > 0)) { fail++; continue; }
      try {
        await SalesAPI.createProduct({
          name,
          sku: sku || `SKU-${Date.now().toString().slice(-5)}-${i}`,
          categoryId: Number(categoryId),
          price: Number(price),
          stockQuantity: Number(stockQuantity) || 0,
          description: description || ''
        });
        ok++;
      } catch (_) { fail++; }
    }
    statusEl.textContent = `Completed: ${ok} imported, ${fail} failed.`;
    showToast(`CSV Import: ${ok} added (${fail} failed)`, ok > 0 ? 'success' : 'error');
    if (ok > 0) loadAllData();
  });
}
