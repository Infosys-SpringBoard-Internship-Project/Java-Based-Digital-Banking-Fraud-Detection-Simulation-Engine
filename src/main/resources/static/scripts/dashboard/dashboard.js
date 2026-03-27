/* Auth guard runs immediately on page load. */
const TOKEN_KEY = 'fraud_token';
const LOGIN_START = 'fraud_login_start';
const PENDING_TXN_KEY = 'fraud_pending_txn_id';
const LOGIN_PAGE = '/pages/login.html';
let lastAlertCount = 0;
let alertCountPrimed = false;
let sessionInterval = null;
let alertPollInterval = null;
let dashboardRefreshInterval = null;
let currentUser = null;
const API = window.location.protocol === 'file:'
  ? 'http://localhost:8080'
  : window.location.origin;
const TAB_PAGE_MAP = {
  dashboard: '/pages/dashboard.html',
  profile: '/pages/profile.html',
  transactions: '/pages/dashboard-transactions.html',
  manual: '/pages/dashboard-manual.html',
  analytics: '/pages/dashboard-analytics.html',
  simulation: '/pages/dashboard-simulation.html',
  system: '/pages/dashboard-system.html',
  'system-logs': '/pages/dashboard-system-logs.html',
  audit: '/pages/dashboard-audit.html',
  users: '/pages/user-management.html'
};

function getCurrentPath() {
  const path = window.location.pathname || '/';
  return path.endsWith('/') && path.length > 1 ? path.slice(0, -1) : path;
}

function getCurrentTabFromPath() {
  const path = getCurrentPath();
  if (path.endsWith('/profile.html')) return 'profile';
  if (path.endsWith('/dashboard-transactions.html')) return 'transactions';
  if (path.endsWith('/dashboard-manual.html')) return 'manual';
  if (path.endsWith('/dashboard-analytics.html')) return 'analytics';
  if (path.endsWith('/dashboard-simulation.html')) return 'simulation';
  if (path.endsWith('/dashboard-system.html')) return 'system';
  if (path.endsWith('/dashboard-system-logs.html')) return 'system-logs';
  if (path.endsWith('/dashboard-audit.html')) return 'audit';
  if (path.endsWith('/user-management.html')) return 'users';
  return 'dashboard';
}

function ensureSharedNavEnhancements() {
  ensureUsersTab();
  ensureNavActionButtons();
  ensureProfileMenu();
}

function ensureUsersTab() {
  const navTabs = document.querySelector('.nav-tabs');
  if (!navTabs) return;
}

function ensureProfileMenu() {
  const navAdmin = document.querySelector('.nav-admin');
  if (!navAdmin) return;

  navAdmin.querySelectorAll('.btn-logout').forEach(btn => btn.remove());

  if (document.getElementById('profile-menu')) return;

  const wrapper = document.createElement('div');
  wrapper.className = 'profile-menu';
  wrapper.id = 'profile-menu';
  wrapper.innerHTML = `
    <button class="profile-avatar-btn" id="profile-avatar-btn" type="button" aria-label="Open profile menu" onclick="toggleProfileMenu(event)">
      <span id="profile-avatar-text">FS</span>
    </button>
    <div class="profile-menu-panel" id="profile-menu-panel">
      <button class="profile-menu-link" id="menu-profile-link" type="button" onclick="goToProfile()">Profile</button>
      <button class="profile-menu-link profile-admin-item" id="menu-system-link" type="button" onclick="goToAdminTool('system')">System</button>
      <button class="profile-menu-link profile-admin-item" id="menu-audit-link" type="button" onclick="goToAdminTool('audit')">Audit</button>
      <button class="profile-menu-link profile-admin-item" id="menu-users-link" type="button" onclick="goToAdminTool('users')">Users</button>
      <button class="profile-menu-link danger" type="button" onclick="logout()">Logout</button>
    </div>
  `;
  navAdmin.appendChild(wrapper);
}

function ensureNavActionButtons() {
  const nav = document.querySelector('.nav');
  if (!nav) return;
  const tabs = nav.querySelector('.nav-tabs');
  if (!tabs) return;
  if (tabs.querySelector('#nav-simulation-tab')) return;

  const simulationTab = document.createElement('div');
  simulationTab.className = 'nav-tab';
  simulationTab.id = 'nav-simulation-tab';
  simulationTab.dataset.tab = 'simulation';
  simulationTab.innerHTML = '<span>Simulation</span>';
  tabs.appendChild(simulationTab);
}

function updateProfileMenu(admin) {
  const avatarText = document.getElementById('profile-avatar-text');
  const avatarBtn = document.getElementById('profile-avatar-btn');
  if (!avatarText || !avatarBtn) return;
  const initials = String(admin?.name || 'FS')
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map(part => part[0] || '')
    .join('')
    .toUpperCase() || 'FS';
  avatarText.textContent = initials;
  avatarBtn.title = 'Profile menu';

  const bell = document.getElementById('bell-container');
  const navAdmin = document.querySelector('.nav-admin');
  if (bell && navAdmin && navAdmin.parentElement === document.querySelector('.nav')) {
    const nav = document.querySelector('.nav');
    if (nav && bell.nextElementSibling !== navAdmin) {
      nav.insertBefore(bell, navAdmin);
    }
  }
}

function toggleProfileMenu(event) {
  event?.stopPropagation();
  const panel = document.getElementById('profile-menu-panel');
  if (!panel) return;
  panel.classList.toggle('open');
}

function closeProfileMenu() {
  document.getElementById('profile-menu-panel')?.classList.remove('open');
}

function goToProfile() {
  closeProfileMenu();
  navigateToTab('profile');
}

function goToAdminTool(tab) {
  closeProfileMenu();
  navigateToTab(tab);
}

function applyRoleAccess(admin) {
  const isAnalyst = admin.role === 'ANALYST';
  const canManageUsers = admin.role === 'SUPERADMIN' || admin.role === 'ADMIN';

  const isAdminToolUser = admin.role === 'SUPERADMIN' || admin.role === 'ADMIN' || admin.role === 'ANALYST';
  const systemLink = document.getElementById('menu-system-link');
  const auditLink = document.getElementById('menu-audit-link');
  const usersLink = document.getElementById('menu-users-link');
  if (systemLink) systemLink.style.display = isAdminToolUser ? 'block' : 'none';
  if (auditLink) auditLink.style.display = isAdminToolUser ? 'block' : 'none';
  if (usersLink) usersLink.style.display = canManageUsers ? 'block' : 'none';

  const manualTab = document.querySelector('[data-tab="manual"]');
  if (manualTab) {
    manualTab.style.display = isAnalyst ? 'none' : 'flex';
  }

  document.querySelectorAll('[data-requires-write="true"]').forEach(el => {
    el.disabled = isAnalyst;
    el.classList.toggle('is-disabled', isAnalyst);
  });

  const readOnlyBanner = document.getElementById('read-only-banner');
  if (readOnlyBanner) {
    readOnlyBanner.style.display = isAnalyst ? 'flex' : 'none';
  }

}

async function toggleBackgroundGeneration() {
  if (currentUser?.role === 'ANALYST') {
    toast('Analyst access is read-only', 'warning');
    return;
  }
  try {
    const current = await fetch(`${API}/system/auto-generation`, { headers: authHeaders() }).then(r => r.json());
    const enabled = !!current.enabled;
    const endpoint = enabled ? `${API}/system/auto-generation/stop` : `${API}/system/auto-generation/start`;
    const r = await fetch(endpoint, { method: 'POST', headers: authHeaders() });
    const data = await r.json().catch(() => ({}));
    if (!r.ok) {
      toast(data.error || 'Failed to update background generation', 'error');
      return;
    }
    toast(enabled ? 'Background generation stopped' : 'Background generation started', 'success');
    loadAutoGenerationStatus();
  } catch {
    toast('Failed to update background generation', 'error');
  }
}

async function toggleMyEmailAlerts() {
  if (!currentUser || currentUser.role === 'ANALYST') return;
  try {
    const r = await fetch(`${API}/auth/toggle-alerts`, {
      method: 'PUT',
      headers: authHeaders()
    });
    const data = await r.json().catch(() => ({}));
    if (!r.ok) {
      toast(data.error || 'Failed to update email alerts', 'error');
      return;
    }
    currentUser.emailAlertsEnabled = !!data.emailAlertsEnabled;
    loadProfilePage();
    toast(data.message || 'Email alert preference updated', 'success');
  } catch (err) {
    toast('Failed to update email alerts', 'error');
  }
}

function isAnalyst() {
  return currentUser?.role === 'ANALYST';
}

function maskAccountValue(value) {
  if (!value) return '—';
  if (!isAnalyst()) return value;
  const normalized = String(value).replace(/\s+/g, '');
  const last4 = normalized.slice(-4);
  return `**** **** **** ${last4}`;
}

function maskAmountValue(value) {
  const amount = Number(value || 0);
  if (!isAnalyst()) return `₹${amount.toLocaleString('en-IN')}`;
  if (amount >= 100000) return '₹100K+';
  if (amount >= 50000) return '₹50K-100K';
  if (amount >= 10000) return '₹10K-50K';
  return `₹${amount.toLocaleString('en-IN')}`;
}

function maskIpValue(value) {
  if (!value) return '—';
  if (!isAnalyst()) return value;
  const parts = String(value).split('.');
  if (parts.length !== 4) return '***.***.***.***';
  return `${parts[0]}.${parts[1]}.***.***`;
}

function maskEmailValue(value) {
  if (!value) return '—';
  if (!isAnalyst()) return value;
  const [local, domain] = String(value).split('@');
  if (!local || !domain) return '***@***';
  return `***@${domain}`;
}

function formatDateTimeValue(value) {
  if (!value) return '—';
  const normalized = String(value).replace(' ', 'T');
  const parsed = new Date(normalized);
  if (Number.isNaN(parsed.getTime())) return String(value);
  return parsed.toLocaleString('en-IN', {
    year: 'numeric',
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: true
  });
}

function navigateToTab(tab) {
  const target = TAB_PAGE_MAP[tab] || TAB_PAGE_MAP.dashboard;
  if (getCurrentPath() !== target) {
    window.location.href = target;
  }
}

function syncActiveTabUi() {
  const currentTab = getCurrentTabFromPath();
  document.querySelectorAll('.nav-tab').forEach(tab => {
    tab.classList.toggle('active', tab.dataset.tab === currentTab);
  });

  document.querySelectorAll('.profile-menu-link').forEach(link => link.classList.remove('active'));
  if (currentTab === 'profile') {
    document.getElementById('menu-profile-link')?.classList.add('active');
  } else if (currentTab === 'system' || currentTab === 'system-logs') {
    document.getElementById('menu-system-link')?.classList.add('active');
  } else if (currentTab === 'audit') {
    document.getElementById('menu-audit-link')?.classList.add('active');
  } else if (currentTab === 'users') {
    document.getElementById('menu-users-link')?.classList.add('active');
  }
}

function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

function authHeaders() {
  return {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + getToken()
  };
}

function ensureCredentialUpdateButton() {
  ensureCredentialUpdateModal();
}

function ensureCredentialUpdateModal() {
  if (document.getElementById('credential-modal')) return;

  const overlay = document.createElement('div');
  overlay.id = 'credential-modal';
  overlay.className = 'modal-overlay credential-modal-overlay';
  overlay.innerHTML = `
    <div class="modal credential-modal" id="credential-modal-card">
      <div class="modal-header">
        <div class="modal-title">Update Login Credentials</div>
        <button class="modal-close" type="button" onclick="closeCredentialModal()">✕</button>
      </div>
      <div class="modal-body">
        <form id="credential-form" class="credential-form">
          <div class="form-group span-3">
            <label class="form-label" for="cred-email">Mail<span class="required">*</span></label>
            <input class="form-input" type="email" id="cred-email" required autocomplete="email">
          </div>

          <div class="form-group span-3">
            <label class="form-label" for="cred-current-password">Current Password<span class="required">*</span></label>
            <input class="form-input" type="password" id="cred-current-password" required autocomplete="current-password">
          </div>

          <div class="form-group span-3">
            <label class="form-label" for="cred-new-password">New Password</label>
            <input class="form-input" type="password" id="cred-new-password" minlength="6" autocomplete="new-password" placeholder="Leave blank to keep current">
          </div>

          <div class="form-group span-3">
            <label class="form-label" for="cred-confirm-password">Again New Password</label>
            <input class="form-input" type="password" id="cred-confirm-password" minlength="6" autocomplete="new-password" placeholder="Re-enter new password">
          </div>

          <div class="credential-actions">
            <button class="btn btn-ghost" type="button" onclick="closeCredentialModal()">Cancel</button>
            <button class="btn btn-primary" id="credential-save-btn" type="submit">Save Changes</button>
          </div>
        </form>
      </div>
    </div>
  `;

  overlay.addEventListener('click', closeCredentialModal);
  document.body.appendChild(overlay);

  const modalCard = document.getElementById('credential-modal-card');
  if (modalCard) {
    modalCard.addEventListener('click', (event) => event.stopPropagation());
  }

  const form = document.getElementById('credential-form');
  if (form) {
    form.addEventListener('submit', submitCredentialUpdate);
  }
}

function openCredentialModal() {
  ensureCredentialUpdateModal();
  const overlay = document.getElementById('credential-modal');
  if (!overlay) return;

  const currentEmail = (document.getElementById('admin-email')?.textContent || '').trim();
  const emailInput = document.getElementById('cred-email');
  const currentPwdInput = document.getElementById('cred-current-password');
  const newPwdInput = document.getElementById('cred-new-password');
  const confirmPwdInput = document.getElementById('cred-confirm-password');

  if (emailInput) emailInput.value = currentEmail;
  if (currentPwdInput) currentPwdInput.value = '';
  if (newPwdInput) newPwdInput.value = '';
  if (confirmPwdInput) confirmPwdInput.value = '';

  overlay.classList.add('open');
  setTimeout(() => currentPwdInput?.focus(), 30);
}

function closeCredentialModal() {
  document.getElementById('credential-modal')?.classList.remove('open');
}

async function submitCredentialUpdate(e) {
  e.preventDefault();

  const email = (document.getElementById('cred-email')?.value || '').trim();
  const currentPassword = (document.getElementById('cred-current-password')?.value || '').trim();
  const newPassword = (document.getElementById('cred-new-password')?.value || '').trim();
  const confirmPassword = (document.getElementById('cred-confirm-password')?.value || '').trim();
  const currentEmail = (document.getElementById('admin-email')?.textContent || '').trim();

  if (!email) {
    toast('Mail is required', 'warning');
    return;
  }
  if (!currentPassword) {
    toast('Current password is required', 'warning');
    return;
  }
  if (newPassword || confirmPassword) {
    if (newPassword.length < 6) {
      toast('New password must be at least 6 characters', 'warning');
      return;
    }
    if (newPassword !== confirmPassword) {
      toast('New password and confirm password do not match', 'error');
      return;
    }
  }

  const payload = { currentPassword };
  if (email.toLowerCase() !== currentEmail.toLowerCase()) {
    payload.newEmail = email;
  }
  if (newPassword) {
    payload.newPassword = newPassword;
  }

  if (!payload.newEmail && !payload.newPassword) {
    toast('No changes to update', 'warning');
    return;
  }

  const saveBtn = document.getElementById('credential-save-btn');
  if (saveBtn) saveBtn.disabled = true;

  try {
    const r = await fetch(`${API}/auth/update-credentials`, {
      method: 'PUT',
      headers: authHeaders(),
      body: JSON.stringify(payload)
    });

    const data = await r.json().catch(() => ({}));
    if (!r.ok) {
      toast(data.error || 'Failed to update credentials', 'error');
      return;
    }

    if (data.email) {
      const emailEl = document.getElementById('admin-email');
      if (emailEl) emailEl.textContent = data.email;
    }

    closeCredentialModal();
    toast('Credentials updated successfully', 'success');
  } catch (err) {
    toast('Failed to update credentials', 'error');
  } finally {
    if (saveBtn) saveBtn.disabled = false;
  }
}

/* Check auth on every page load */
(async function authGuard() {
  const token = getToken();

  /* No token at all → go to login immediately */
  if (!token) {
    window.location.href = LOGIN_PAGE;
    return;
  }

  /* Verify token is still valid with the server */
  try {
    const r = await fetch(`${API}/auth/me`, {
      headers: authHeaders()
    });

    if (r.status === 401) {
      localStorage.removeItem(TOKEN_KEY);
      window.location.href = LOGIN_PAGE;
      return;
    }

    const admin = await r.json();
    currentUser = admin;
    ensureSharedNavEnhancements();
    
    /* Show admin info in nav */
    document.getElementById('admin-name').textContent =
      admin.name?.toUpperCase() || 'ADMIN';
    document.getElementById('admin-email').textContent =
      admin.email || '';
    updateProfileMenu(admin);
    applyRoleAccess(admin);
    ensureCredentialUpdateButton();
    loadProfilePage();

    /* Start session timer */
    const loginStart = localStorage.getItem(LOGIN_START) || Date.now();
    localStorage.setItem(LOGIN_START, loginStart);
    updateSessionTimer(parseInt(loginStart));
    sessionInterval = setInterval(() => 
      updateSessionTimer(parseInt(loginStart)), 60000);

  } catch(e) {
    /* Server unreachable — let them stay but warn */
    document.getElementById('admin-name').textContent = 'ADMIN';
    console.warn('Auth check failed:', e.message);
  } finally {
    /* Hide the checking overlay */
    const overlay = document.getElementById('auth-overlay');
    if (overlay) {
      overlay.style.transition = 'opacity 0.3s';
      overlay.style.opacity = '0';
      setTimeout(() => overlay.remove(), 300);
    }
  }
})();

function updateSessionTimer(startMs) {
  const elapsedMs   = Date.now() - startMs;
  const totalMin    = Math.floor(elapsedMs / 60000);
  const hours       = Math.floor(totalMin / 60);
  const mins        = totalMin % 60;
  const display     = hours > 0
    ? `SESSION: ${hours}h ${mins}m`
    : `SESSION: ${mins}m`;
  const el = document.getElementById('admin-session');
  if (el) el.textContent = display;
}

/* Logout flow. */
async function logout() {
  try {
    await fetch(`${API}/auth/logout`, {
      method: 'POST',
      headers: authHeaders()
    });
  } catch(e) { /* ignore network errors on logout */ }

  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(LOGIN_START);
  currentUser = null;
  clearInterval(sessionInterval);
  clearInterval(alertPollInterval);
  window.location.href = LOGIN_PAGE;
}

/* Notification bell and panel. */
function toggleNotifPanel() {
  const panel = document.getElementById('notif-panel');
  const isOpen = panel.classList.contains('open');

  if (!isOpen) {
    openNotificationPanel();
  } else {
    panel.classList.remove('open');
  }
}

function openNotificationPanel() {
  const panel = document.getElementById('notif-panel');
  if (!panel) return;
  panel.classList.add('open');
  loadAlerts();
}

/* Close panel when clicking anywhere outside */
document.addEventListener('click', function(e) {
  const container = document.getElementById('bell-container');
  if (container && !container.contains(e.target)) {
    document.getElementById('notif-panel')?.classList.remove('open');
  }

  const profileMenu = document.getElementById('profile-menu');
  if (profileMenu && !profileMenu.contains(e.target)) {
    closeProfileMenu();
  }
});

async function loadAlertCount() {
  try {
    const r = await fetch(`${API}/alerts/count`, {
      headers: authHeaders()
    });

    if (r.status === 401) {
      window.location.href = LOGIN_PAGE;
      return;
    }

    const d = await r.json();
    const count = d.count || 0;
    const badge = document.getElementById('bell-badge');

    badge.textContent = count > 99 ? '99+' : count;

    if (count > 0) {
      badge.classList.add('visible');
    } else {
      badge.classList.remove('visible');
    }

    // Prime once to avoid showing old unread alerts as "new" on first page load.
    if (!alertCountPrimed) {
      lastAlertCount = count;
      alertCountPrimed = true;
      return;
    }

    if (count > lastAlertCount) {
      showFraudToast(count - lastAlertCount);
    }
    lastAlertCount = count;

  } catch(e) {
    console.warn('Alert count fetch failed:', e.message);
  }
}

async function loadAlerts() {
  const list = document.getElementById('notif-list');
  list.innerHTML = '<div class="notif-empty">Loading...</div>';

  try {
    const r = await fetch(`${API}/alerts`, {
      headers: authHeaders()
    });
    const alerts = await r.json();

    if (!alerts.length) {
      list.innerHTML = '<div class="notif-empty">No fraud alerts yet</div>';
      return;
    }

    list.innerHTML = alerts.map(a => {
      const isCritical = a.riskLevel === 'CRITICAL';
      const cls = isCritical ? 'critical' : 'high';
      const badgeCls = isCritical
        ? 'badge badge-critical'
        : 'badge badge-high';
      const timeAgo = formatTimeAgo(a.createdAt);
      const rules = a.fraudReason || 'No rules';

      const safeTxnId = String(a.transactionId || '').replace(/'/g, "\\'");
      return `
           <div class="notif-item ${cls} ${!a.isRead ? 'unread' : ''}"
             onclick="handleAlertClick(${a.id}, '${safeTxnId}')">
          ${!a.isRead ? '<div class="notif-unread-dot"></div>' : ''}
          <div class="notif-item-top">
            <span class="${badgeCls}" style="font-size:9px;padding:2px 6px;">
              <span class="badge-dot"></span>${a.riskLevel}
            </span>
            <span class="notif-item-name">${a.accountHolderName}</span>
            <span class="notif-item-amount">
              ₹${(a.amount || 0).toLocaleString('en-IN')}
            </span>
          </div>
          <div class="notif-item-rules">${rules.split('|')[0].trim()}</div>
          <div class="notif-item-time">${timeAgo}</div>
        </div>
      `;
    }).join('');

  } catch(e) {
    list.innerHTML = 
      '<div class="notif-empty">Failed to load alerts</div>';
  }
}

async function handleAlertClick(alertId, transactionId) {
  /* Mark as read */
  try {
    await fetch(`${API}/alerts/${alertId}/read`, {
      method: 'PUT',
      headers: authHeaders()
    });
  } catch(e) { /* ignore */ }

  /* Refresh count + list */
  loadAlertCount();
  loadAlerts();

  const normalizedTxnId = (transactionId || '').toString().trim().replace(/^\"+|\"+$/g, '');

  // Ensure we are on the transactions page before trying to open details.
  if (getCurrentTabFromPath() !== 'transactions') {
    if (normalizedTxnId) {
      localStorage.setItem(PENDING_TXN_KEY, normalizedTxnId);
    }
    navigateToTab('transactions');
    return;
  }

  if (!normalizedTxnId) {
    toast('Transaction ID not available for this alert', 'warning');
    return;
  }

  try {
    await loadTransactions();
    const matched = allTxns.find(t =>
      (t.transactionId || '').toString().trim().toLowerCase() ===
      normalizedTxnId.toLowerCase()
    );
    if (matched) {
      showDetail(matched.transactionId);
    } else {
      // Fallback: fetch directly by transaction ID and open modal if available.
      const r = await fetch(`${API}/transaction/${encodeURIComponent(normalizedTxnId)}`, {
        headers: authHeaders()
      });
      if (r.ok) {
        const tx = await r.json();
        if (!allTxns.find(t => t.transactionId === tx.transactionId)) {
          allTxns.push(tx);
        }
        showDetail(tx.transactionId);
      } else {
        toast('Transaction detail not found for this alert', 'warning');
      }
    }
  } catch (e) {
    toast('Failed to open transaction detail', 'error');
  }
}

async function markAllRead() {
  try {
    await fetch(`${API}/alerts/read-all`, {
      method: 'PUT',
      headers: authHeaders()
    });
    loadAlertCount();
    loadAlerts();
    toast('All alerts marked as read', 'success');
  } catch(e) {
    toast('Failed to mark alerts', 'error');
  }
}

function switchToTransactions() {
  document.getElementById('notif-panel')?.classList.remove('open');
  navigateToTab('transactions');
}

/* Fraud alert toast. */
function showFraudToast(newCount) {
  const container = document.getElementById('toasts');
  const el = document.createElement('div');
  el.className = 'fraud-toast';
  el.innerHTML = `
    <span class="fraud-toast-icon">⚠</span>
    <span>NEW FRAUD ALERT — ${newCount} new unread</span>
  `;
  el.onclick = (e) => {
    // Prevent document click handler from instantly closing the panel.
    e.stopPropagation();
    el.remove();
    openNotificationPanel();
  };
  container.appendChild(el);
  setTimeout(() => {
    el.style.transition = 'opacity 0.3s';
    el.style.opacity = '0';
    setTimeout(() => el.remove(), 300);
  }, 5000);
}

/* Convert timestamp to relative time. */
function formatTimeAgo(isoString) {
  if (!isoString) return 'Unknown';
  const date = new Date(isoString);
  const diffMs = Date.now() - date.getTime();
  const diffMin = Math.floor(diffMs / 60000);
  if (diffMin < 1)  return 'Just now';
  if (diffMin < 60) return `${diffMin}m ago`;
  const diffHr = Math.floor(diffMin / 60);
  if (diffHr < 24)  return `${diffHr}h ago`;
  return `${Math.floor(diffHr / 24)}d ago`;
}

/* Inject auth headers for API calls and auto-handle 401. */
const _originalFetch = window.fetch;
window.fetch = function(url, options = {}) {
  const token = getToken();

  /* Only inject headers for calls to our own API */
  if (token && typeof url === 'string' && url.includes('localhost:8080')) {
    options.headers = {
      ...options.headers,
      'Authorization': 'Bearer ' + token
    };
  }
  return _originalFetch(url, options).then(response => {
    /* Auto-logout on any 401 from our API */
    if (response.status === 401 && 
        typeof url === 'string' && 
        url.includes('localhost:8080')) {
      localStorage.removeItem(TOKEN_KEY);
      window.location.href = LOGIN_PAGE;
    }
    return response;
  });
};

/* Start polling after auth guard finishes. */
/* Delay first poll until auth guard resolves */
  setTimeout(() => {
  loadAlertCount();
  alertPollInterval = setInterval(loadAlertCount, 30000);
  setInterval(() => {
    if (document.getElementById('live-feed')) {
      loadSummary();
    }
    if (document.getElementById('txn-tbody')) {
      loadTransactions();
    }
  }, 20000);
}, 1000);

function loadSystemStatus() {
  const statusEl = document.getElementById('system-status');
  const textEl = document.getElementById('system-status-text');
  if (!statusEl || !textEl) return;

  fetch(`${API}/transaction/system-status`, { headers: authHeaders() })
    .then(r => r.ok ? r.json() : Promise.reject(new Error('status endpoint failed')))
    .then(d => {
      const mlUp = d.mlApi === 'UP';
      statusEl.style.borderColor = mlUp ? 'rgba(0,230,118,0.2)' : 'rgba(255,145,0,0.3)';
      statusEl.style.background = mlUp ? 'rgba(0,230,118,0.05)' : 'rgba(255,145,0,0.08)';
      statusEl.style.color = mlUp ? 'var(--green)' : 'var(--orange)';
      textEl.textContent = mlUp ? 'SYSTEM ONLINE (ML:UP)' : 'SYSTEM DEGRADED (ML:DOWN)';
    })
    .catch(() => {
      statusEl.style.borderColor = 'rgba(255,61,87,0.35)';
      statusEl.style.background = 'rgba(255,61,87,0.08)';
      statusEl.style.color = 'var(--red)';
      textEl.textContent = 'BACKEND OFFLINE';
    });
}

document.addEventListener('click', (e) => {
  const tab = e.target.closest('.nav-tab');
  if (!tab) return;
  e.preventDefault();
  const targetTab = tab.dataset.tab;
  if (targetTab && targetTab !== getCurrentTabFromPath()) {
    navigateToTab(targetTab);
    return;
  }
  document.querySelectorAll('.nav-tab').forEach(t => t.classList.remove('active'));
  document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
  tab.classList.add('active');
  document.getElementById('tab-' + tab.dataset.tab)?.classList.add('active');
  if (tab.dataset.tab === 'transactions') loadTransactions();
  if (tab.dataset.tab === 'profile') loadProfilePage();
  if (tab.dataset.tab === 'analytics') loadAnalytics();
  if (tab.dataset.tab === 'simulation') loadSimulationStatus();
  if (tab.dataset.tab === 'system') loadSystemDashboard();
  if (tab.dataset.tab === 'audit') loadAuditLogs();
  if (tab.dataset.tab === 'users') loadUsers();
});

function toast(msg, type = 'info') {
  const host = document.getElementById('toasts');
  if (!host) return;
  const el = document.createElement('div');
  el.className = `toast ${type}`;
  el.textContent = msg;
  host.appendChild(el);
  setTimeout(() => el.remove(), 3500);
}

async function loadSummary() {
  try {
    const r = await fetch(`${API}/transaction/summary`, { headers: authHeaders() });
    if (!r.ok) throw new Error('summary endpoint failed');
    const d = await r.json();
    document.getElementById('stat-total').textContent = d.totalTransactions ?? 0;
    document.getElementById('stat-fraud').textContent = d.fraudCount ?? 0;
    document.getElementById('stat-fraud-rate').textContent = `${d['fraudRate%'] ?? 0}% RATE`;
    document.getElementById('stat-critical').textContent = d.critical ?? 0;
    document.getElementById('stat-tor').textContent = d.torDetected ?? 0;
    document.getElementById('stat-ipmismatch').textContent = `${d.ipLocationMismatch ?? 0} IP MISMATCHES`;
    document.getElementById('stat-avg-risk').textContent = d.avgRiskScore ?? '0.0';
    renderRuleBreakdown(d.ruleBreakdown || {});

    if (document.getElementById('live-feed')) {
      const txResp = await fetch(`${API}/transaction/search?page=0&size=20`, { headers: authHeaders() });
      const txPayload = await txResp.json().catch(() => ({}));
      const txns = Array.isArray(txPayload.items) ? txPayload.items : [];
      if (txns.length) {
        const feed = document.getElementById('live-feed');
        if (feed) {
          feed.innerHTML = '';
          txns.slice().reverse().forEach(tx => {
            const status = tx.isFraud ? 'FRAUD_DETECTED' : (tx.riskLevel === 'MEDIUM' ? 'REVIEW_NEEDED' : 'CLEAN');
            addFeedItem({ status, transaction: tx });
          });
        }
      }
    }
  } catch {
    toast('Cannot load summary', 'error');
  }
}

function renderRuleBreakdown(breakdown) {
  const names = {
    R01: 'High Amount', R02: 'Odd Hours', R03: 'Balance Drain', R04: 'Rapid Fire',
    R05: 'Risky Merchant', R06: 'Location Jump', R07: 'New Device', R08a: 'VPN Flag',
    R08b: 'IP Mismatch', R08c: 'IP Tag', R09: 'International', R10: 'New Receiver',
    R11: 'Amount Spike', R12: 'New Account', R13: 'High Volume', R14: 'Round Amount'
  };
  const ul = document.getElementById('rule-breakdown');
  if (!ul) return;
  const entries = Object.entries(breakdown).sort((a, b) => b[1] - a[1]);
  if (!entries.length) {
    ul.innerHTML = '<li style="color:var(--text-muted);font-family:var(--font-mono);font-size:11px;padding:20px 0;text-align:center;">No fraud data yet</li>';
    return;
  }
  const max = entries[0][1] || 1;
  ul.innerHTML = entries.slice(0, 10).map(([code, count]) => `
    <li class="rule-item">
      <span class="rule-code">${code}</span>
      <span class="rule-name">${names[code] || code}</span>
      <div class="rule-bar-bg"><div class="rule-bar-fill" style="width:${Math.round(count / max * 100)}%"></div></div>
      <span class="rule-count">${count}</span>
    </li>
  `).join('');
}

function refreshAll() {
  loadSystemStatus();
  loadSummary();
  loadTransactions();
  loadAutoGenerationStatus();
}

async function generateOne() {
  toast('Manual generation is disabled. Auto generation runs in background.', 'warning');
}

async function generateBatch() {
  toast('Manual generation is disabled. Auto generation runs in background.', 'warning');
}

async function loadAutoGenerationStatus() {
  const statusEl = document.getElementById('auto-gen-status');
  const toggleBtn = document.getElementById('auto-generation-toggle-btn');
  if (!statusEl && !toggleBtn) return;
  const setText = (id, value) => {
    const el = document.getElementById(id);
    if (el) el.textContent = value;
  };
  try {
    const r = await fetch(`${API}/system/auto-generation`, { headers: authHeaders() });
    const data = await r.json().catch(() => ({}));
    if (!r.ok) {
      setText('auto-gen-status', 'Status unavailable');
      return;
    }
    const statusText = data.lastMessage
      ? `${data.enabled ? 'Enabled' : 'Disabled'} · ${data.lastStatus || 'IDLE'} · ${data.lastMessage}`
      : (data.enabled ? 'Enabled' : 'Disabled');
    setText('auto-gen-status', statusText);
    setText('auto-gen-interval', `${data.intervalMinSec || 10}s to ${data.intervalMaxSec || 300}s interval`);
    setText('auto-gen-last-run', `Last run: ${formatDateTimeValue(data.lastRunAt)}`);
    setText('auto-gen-next-run', `Next run: ${formatDateTimeValue(data.nextRunAt)}`);
    if (toggleBtn) {
      toggleBtn.textContent = data.enabled ? 'STOP AUTO' : 'START AUTO';
    }
  } catch {
    setText('auto-gen-status', 'Status unavailable');
  }
}

function addFeedItem(d) {
  const feed = document.getElementById('live-feed');
  if (!feed) return;
  feed.querySelector('.empty-state')?.remove();
  const tx = d?.transaction || {};
  if (!tx || !tx.transactionId) return;
  const existing = feed.querySelector(`[data-txn-id="${tx.transactionId}"]`);
  if (existing) {
    existing.remove();
  }
  const isFraud = d?.status === 'FRAUD_DETECTED';
  const isMed = d?.status === 'REVIEW_NEEDED';
  const cls = isFraud ? 'fraud' : isMed ? 'medium' : 'clean';
  const now = new Date().toLocaleTimeString('en', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  const item = document.createElement('div');
  item.className = `feed-item ${cls}`;
  item.dataset.txnId = tx.transactionId;
  item.innerHTML = `
    <span class="feed-time">${now}</span>
    <span class="feed-name">${tx.accountHolderName || '—'}</span>
    <span class="feed-amount">₹${(tx.amount || 0).toLocaleString('en-IN')}</span>
    <span class="feed-rule">${isFraud ? `⚠ ${(tx.fraudReason || '').split('|')[0]?.trim() || 'FRAUD'}` : isMed ? '⚡ MEDIUM RISK' : '✓ CLEAN'}</span>
  `;
  feed.insertBefore(item, feed.firstChild);
  while (feed.children.length > 80) feed.removeChild(feed.lastChild);
}

function clearFeed() {
  const feed = document.getElementById('live-feed');
  if (!feed) return;
  feed.innerHTML = '<div class="empty-state"><div class="empty-icon">◈</div><div class="empty-text">Feed cleared.</div></div>';
}

let allTxns = [];
let currentTransactionResults = [];
let activeQuickFilter = 'all';
let transactionDetailCache = null;
const analyticsCharts = {};
let advancedFilters = {
  query: '',
  account: '',
  status: 'all',
  dateFrom: '',
  dateTo: '',
  amountMin: '',
  amountMax: '',
  riskLevel: 'all',
  fraudType: 'all',
  ipRiskTag: 'all'
};

async function loadTransactions() {
  const tbody = document.getElementById('txn-tbody');
  if (tbody) {
    tbody.innerHTML = '<tr><td colspan="9" style="text-align:center;padding:40px;color:var(--text-muted);font-family:var(--font-mono);">Loading...</td></tr>';
  }
  const params = buildAdvancedFilterParams();
  const hasAdvancedFilters = Array.from(params.keys()).length > 2;
  const endpoint = hasAdvancedFilters ? `${API}/transaction/search?${params.toString()}` : `${API}/transaction/all`;
  const r = await fetch(endpoint, { headers: authHeaders() });
  const payload = await r.json().catch(() => ({}));
  if (!r.ok) {
    toast('Unable to load transactions', 'error');
    allTxns = [];
    currentTransactionResults = [];
    renderTxnTable([]);
    return;
  }
  if (hasAdvancedFilters) {
    allTxns = payload.items || [];
    currentTransactionResults = allTxns;
    updateTransactionMeta(payload.total || allTxns.length, true);
  } else {
    allTxns = payload;
    currentTransactionResults = allTxns;
    updateTransactionMeta(allTxns.length, false);
  }
  applyQuickFilter(activeQuickFilter);

  const pendingTxnId = localStorage.getItem(PENDING_TXN_KEY);
  if (pendingTxnId) {
    localStorage.removeItem(PENDING_TXN_KEY);
    const normalizedPending = pendingTxnId.toString().trim().toLowerCase();
    const matched = allTxns.find(t =>
      (t.transactionId || '').toString().trim().toLowerCase() === normalizedPending
    );
    if (matched) {
      showDetail(matched.transactionId);
    } else {
      const r = await fetch(`${API}/transaction/${encodeURIComponent(pendingTxnId)}`, {
        headers: authHeaders()
      });
      if (r.ok) {
        const tx = await r.json();
        if (!allTxns.find(t => t.transactionId === tx.transactionId)) {
          allTxns.push(tx);
        }
        showDetail(tx.transactionId);
      } else {
        toast('Transaction detail not found for selected alert', 'warning');
      }
    }
  }
}

function filterTxns(btn, filter) {
  document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
  btn?.classList.add('active');
  activeQuickFilter = filter;
  applyQuickFilter(filter);
}

function applyQuickFilter(filter) {
  let filtered = [...allTxns];
  const panel = document.getElementById('advanced-filters-panel');
  if (panel) {
    readAdvancedFiltersFromUi();
    filtered = filtered.filter(tx => {
      if (advancedFilters.query) {
        const q = advancedFilters.query.toLowerCase();
        const txt = [tx.accountHolderName, tx.transactionId, tx.merchantCategory, tx.location]
          .map(v => (v || '').toString().toLowerCase())
          .join(' ');
        if (!txt.includes(q)) return false;
      }
      if (advancedFilters.account) {
        const q = advancedFilters.account.toLowerCase();
        const txt = [tx.senderAccount, tx.receiverAccount]
          .map(v => (v || '').toString().toLowerCase())
          .join(' ');
        if (!txt.includes(q)) return false;
      }
      if (advancedFilters.status === 'fraud' && !tx.isFraud) return false;
      if (advancedFilters.status === 'non_fraud' && tx.isFraud) return false;
      if (advancedFilters.riskLevel !== 'all' && tx.riskLevel !== advancedFilters.riskLevel) return false;
      if (advancedFilters.ipRiskTag !== 'all' && (tx.ipRiskTag || 'CLEAN') !== advancedFilters.ipRiskTag) return false;
      if (advancedFilters.amountMin !== '' && Number(tx.amount || 0) < Number(advancedFilters.amountMin)) return false;
      if (advancedFilters.amountMax !== '' && Number(tx.amount || 0) > Number(advancedFilters.amountMax)) return false;
      if (advancedFilters.dateFrom) {
        const from = new Date(`${advancedFilters.dateFrom}T00:00:00`);
        const t = new Date(String(tx.timestamp || '').replace(' ', 'T'));
        if (!Number.isNaN(from.getTime()) && !Number.isNaN(t.getTime()) && t < from) return false;
      }
      if (advancedFilters.dateTo) {
        const to = new Date(`${advancedFilters.dateTo}T23:59:59`);
        const t = new Date(String(tx.timestamp || '').replace(' ', 'T'));
        if (!Number.isNaN(to.getTime()) && !Number.isNaN(t.getTime()) && t > to) return false;
      }
      if (advancedFilters.fraudType !== 'all') {
        const fr = (tx.fraudReason || '').toLowerCase();
        if (!fr.includes(advancedFilters.fraudType.toLowerCase().replace('-', ''))) return false;
      }
      return true;
    });
  }
  if (filter === 'fraud') filtered = filtered.filter(t => t.isFraud);
  else if (filter !== 'all') filtered = filtered.filter(t => t.riskLevel === filter);
  currentTransactionResults = filtered;
  renderTxnTable(filtered);
  updateVisibleCount(filtered.length);
}

function toggleAdvancedFilters() {
  const panel = document.getElementById('advanced-filters-panel');
  if (!panel) return;
  panel.style.display = panel.style.display === 'none' ? 'block' : 'none';
}

function buildAdvancedFilterParams() {
  const params = new URLSearchParams();
  const panel = document.getElementById('advanced-filters-panel');
  if (!panel) {
    params.set('page', '0');
    params.set('size', '500');
    return params;
  }
  Object.entries(advancedFilters).forEach(([key, value]) => {
    if (value !== '' && value !== null && value !== undefined && value !== 'all') {
      params.set(key, value);
    }
  });
  params.set('page', '0');
  params.set('size', '500');
  return params;
}

function readAdvancedFiltersFromUi() {
  if (!document.getElementById('advanced-filters-panel')) {
    advancedFilters = {
      query: '',
      account: '',
      status: 'all',
      dateFrom: '',
      dateTo: '',
      amountMin: '',
      amountMax: '',
      riskLevel: 'all',
      fraudType: 'all',
      ipRiskTag: 'all'
    };
    return;
  }
  advancedFilters = {
    query: document.getElementById('filter-search')?.value?.trim() || '',
    account: document.getElementById('filter-account')?.value?.trim() || '',
    status: document.getElementById('filter-status')?.value || 'all',
    dateFrom: document.getElementById('filter-date-from')?.value || '',
    dateTo: document.getElementById('filter-date-to')?.value || '',
    amountMin: document.getElementById('filter-amount-min')?.value || '',
    amountMax: document.getElementById('filter-amount-max')?.value || '',
    riskLevel: document.getElementById('filter-risk-level')?.value || 'all',
    fraudType: document.getElementById('filter-fraud-type')?.value || 'all',
    ipRiskTag: document.getElementById('filter-ip-risk-tag')?.value || 'all'
  };
}

async function applyAdvancedFilters() {
  if (!document.getElementById('advanced-filters-panel')) {
    await loadTransactions();
    return;
  }
  readAdvancedFiltersFromUi();
  const summary = document.getElementById('advanced-filter-summary');
  if (summary) {
    const activeCount = Object.values(advancedFilters).filter(value => value && value !== 'all').length;
    summary.textContent = activeCount ? `${activeCount} advanced filter(s) active` : 'Showing latest transactions';
  }
  await loadTransactions();
}

function clearAdvancedFilters() {
  if (!document.getElementById('advanced-filters-panel')) {
    loadTransactions();
    return;
  }
  advancedFilters = {
    query: '',
    account: '',
    status: 'all',
    dateFrom: '',
    dateTo: '',
    amountMin: '',
    amountMax: '',
    riskLevel: 'all',
    fraudType: 'all',
    ipRiskTag: 'all'
  };

  ['filter-search', 'filter-account', 'filter-date-from', 'filter-date-to', 'filter-amount-min', 'filter-amount-max'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.value = '';
  });
  ['filter-status', 'filter-risk-level', 'filter-fraud-type', 'filter-ip-risk-tag'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.value = 'all';
  });
  const summary = document.getElementById('advanced-filter-summary');
  if (summary) summary.textContent = 'Showing latest transactions';
  loadTransactions();
}

function updateTransactionMeta(total, filteredFromSearch) {
  const meta = document.getElementById('txn-results-meta');
  if (!meta) return;
  meta.textContent = filteredFromSearch
    ? `Search returned ${total} transaction(s)`
    : `Loaded ${total} transaction(s)`;
}

function updateVisibleCount(visibleCount) {
  const meta = document.getElementById('txn-results-meta');
  if (!meta) return;
  const summary = document.getElementById('advanced-filter-summary');
  const filterText = summary?.textContent || 'Showing latest transactions';
  meta.textContent = `${filterText} · ${visibleCount} visible row(s)`;
}

function exportFilteredTransactions() {
  const params = buildAdvancedFilterParams();
  const url = `${API}/transaction/export-csv/search?${params.toString()}`;
  window.open(url, '_blank');
}

function renderTxnTable(txns) {
  const tbody = document.getElementById('txn-tbody');
  if (!tbody) return;
  if (!txns?.length) {
    tbody.innerHTML = '<tr><td colspan="9" style="text-align:center;padding:40px;color:var(--text-muted);font-family:var(--font-mono);">No transactions found</td></tr>';
    return;
  }
  tbody.innerHTML = [...txns].slice(0, 200).map(t => {
    const badgeCls = t.riskLevel === 'CRITICAL' ? 'badge-critical' : t.riskLevel === 'HIGH' ? 'badge-high' : t.riskLevel === 'MEDIUM' ? 'badge-medium' : 'badge-clean';
    const scoreColor = t.riskScore >= 7.5 ? 'var(--red)' : t.riskScore >= 5 ? 'var(--orange)' : t.riskScore >= 3 ? 'var(--yellow)' : 'var(--green)';
    return `<tr onclick="showDetail('${t.transactionId}')">
      <td class="id-cell">${t.transactionId?.slice(0, 8)}…</td>
      <td>${t.accountHolderName || '—'}</td>
      <td style="color:var(--yellow);">${maskAmountValue(t.amount)}</td>
      <td>${t.merchantCategory || '—'}</td>
      <td>${t.location || '—'}</td>
      <td><span class="iptag iptag-${t.ipRiskTag || 'CLEAN'}">${t.ipRiskTag || 'CLEAN'}</span></td>
      <td><span class="badge ${badgeCls}"><span class="badge-dot"></span>${t.riskLevel || 'NORMAL'}</span></td>
      <td><div class="score-bar-wrap"><div class="score-bar"><div class="score-bar-fill" style="width:${(t.riskScore || 0) * 10}%;background:${scoreColor}"></div></div><span class="score-val">${t.riskScore || 0}</span></div></td>
      <td>${t.isFraud ? '<span style="color:var(--red);font-family:var(--font-mono);font-size:11px;">⚠ FRAUD</span>' : '<span style="color:var(--green);font-family:var(--font-mono);font-size:11px;">✓ CLEAN</span>'}</td>
    </tr>`;
  }).join('');
}

async function showDetail(id) {
  try {
    const r = await fetch(`${API}/transaction/detail/${encodeURIComponent(id)}`, { headers: authHeaders() });
    const payload = await r.json().catch(() => ({}));
    if (!r.ok) {
      toast(payload.error || 'Unable to load transaction detail', 'error');
      return;
    }
    const t = payload.transaction || {};
    transactionDetailCache = {
      transactionId: t.transactionId || id,
      historyRows: payload.history || [],
      relatedRows: payload.related || []
    };
    const badgeCls = t.riskLevel === 'CRITICAL' ? 'badge-critical' : t.riskLevel === 'HIGH' ? 'badge-high' : t.riskLevel === 'MEDIUM' ? 'badge-medium' : 'badge-clean';
    document.getElementById('modal-title').innerHTML =
      `TXN: ${t.transactionId?.slice(0, 16)}… <span class="badge ${badgeCls}" style="margin-left:10px;">${t.riskLevel}</span>`;

    const fields = obj => Object.entries(obj).map(([k, v]) => `
      <div class="detail-field"><div class="detail-label">${k}</div><div class="detail-value">${v === null || v === undefined ? '—' : String(v)}</div></div>
    `).join('');
    const ruleRows = (payload.ruleBreakdown || []).map(rule => `
      <div class="detail-field"><div class="detail-label">${rule.code}</div><div class="detail-value"><strong>${rule.trigger}</strong><br>${rule.explanation}</div></div>
    `).join('') || '<div class="detail-field"><div class="detail-label">RULES</div><div class="detail-value">No fraud rules triggered.</div></div>';
    document.getElementById('modal-body').innerHTML = `
      <div class="detail-toolbar">
        <button class="btn btn-ghost detail-cta" type="button" onclick="openDetailTableModal('history')">User Transaction History</button>
        <button class="btn btn-ghost detail-cta" type="button" onclick="openDetailTableModal('related')">Related Transactions</button>
      </div>
      <div class="detail-section"><div class="detail-section-title">Account</div><div class="detail-grid">${fields({
        Name: t.accountHolderName, Mobile: t.mobileNumber, Bank: t.bankName,
        'Sender Acct': maskAccountValue(t.senderAccount),
        'Receiver Acct': maskAccountValue(t.receiverAccount),
        'Account Age': `${t.accountAgeDays || 0} days`
      })}</div></div>
      <div class="detail-section"><div class="detail-section-title">Transaction</div><div class="detail-grid">${fields({
        Amount: maskAmountValue(t.amount),
        Balance: maskAmountValue(t.balance),
        Type: t.type, Currency: t.currency, Merchant: t.merchantCategory, Mode: t.transactionMode,
        Time: formatDateTimeValue(t.timestamp),
        'Avg 30d': maskAmountValue(t.avgTxnAmount30Days),
        'Txns/hr': t.txnCountLastHour, 'Txns/day': t.txnCountLast24Hours
      })}</div></div>
      <div class="detail-section"><div class="detail-section-title">Network & Location</div><div class="detail-grid">${fields({
        Location: t.location, 'Previous Location': t.previousLocation,
        'IP Address': maskIpValue(t.ipAddress), 'IP Country': t.ipCountry, 'IP Tag': t.ipRiskTag
      })}</div></div>
      <div class="detail-section"><div class="detail-section-title">Fraud Analysis</div><div class="detail-grid">${fields({
        'Is Fraud': t.isFraud ? '⚠ TRUE' : 'FALSE',
        'Risk Score': `${t.riskScore || 0} / 10`,
        'Risk Level': t.riskLevel,
        'ML Probability': `${((t.mlFraudProbability || 0) * 100).toFixed(1)}%`
      })}</div></div>
      <div class="detail-section"><div class="detail-section-title">Rule Breakdown With Explanation</div><div class="detail-grid">${ruleRows}</div></div>
    `;
    document.getElementById('modal').classList.add('open');
  } catch {
    toast('Unable to load transaction detail', 'error');
  }
}

function openDetailTableModal(section) {
  if (!transactionDetailCache) return;

  const isRelated = section === 'related';
  const title = isRelated ? 'RELATED TRANSACTIONS' : 'USER TRANSACTION HISTORY (LAST 10)';
  const rows = isRelated ? transactionDetailCache.relatedRows : transactionDetailCache.historyRows;
  const empty = isRelated ? 'No related transactions found' : 'No transaction history found';

  const tableRows = (rows || []).map(tx => {
    if (isRelated) {
      return `<tr><td>${tx.accountHolderName || '—'}</td><td>${maskAmountValue(tx.amount)}</td><td>${tx.location || '—'}</td><td>${tx.riskLevel || '—'}</td></tr>`;
    }
    return `<tr><td>${formatDateTimeValue(tx.timestamp)}</td><td>${maskAmountValue(tx.amount)}</td><td>${tx.riskLevel || '—'}</td><td>${tx.merchantCategory || '—'}</td></tr>`;
  }).join('') || `<tr><td colspan="4">${empty}</td></tr>`;

  const header = isRelated
    ? '<tr><th>USER</th><th>AMOUNT</th><th>LOCATION</th><th>RISK</th></tr>'
    : '<tr><th>TIME</th><th>AMOUNT</th><th>RISK</th><th>MERCHANT</th></tr>';

  const modalBody = document.getElementById('modal-body');
  const modalTitle = document.getElementById('modal-title');
  if (!modalBody || !modalTitle) return;

  const backTxnId = String(transactionDetailCache?.transactionId || '').replace(/'/g, "\\'");

  modalTitle.textContent = title;
  modalBody.innerHTML = `
    <div class="detail-section">
      <div class="detail-toolbar" style="margin-bottom: 12px; justify-content: space-between;">
        <button class="btn btn-ghost btn-sm" type="button" onclick="showDetail('${backTxnId}')">← Back to transaction detail</button>
      </div>
      <div class="table-wrap">
        <table>
          <thead>${header}</thead>
          <tbody>${tableRows}</tbody>
        </table>
      </div>
      <div class="detail-toolbar" style="margin-top: 12px; justify-content: flex-end;">
        <button class="btn btn-primary btn-sm" type="button" onclick="closeModal()">Close</button>
      </div>
    </div>
  `;
}

function closeModal(e) {
  if (!e || e.target.id === 'modal' || e.target.classList.contains('modal-close')) {
    document.getElementById('modal').classList.remove('open');
  }
}

const toggleState = {};

function toggleField(id) {
  toggleState[id] = !toggleState[id];
  const el = document.getElementById(id);
  if (!el) return;
  if (toggleState[id]) el.classList.add('on');
  else el.classList.remove('on');
}

function getToggle(id) {
  if (id === 'f-ipmatch') return toggleState[id] === undefined ? true : toggleState[id];
  return !!toggleState[id];
}

function resetForm() {
  ['f-name', 'f-mobile', 'f-sender', 'f-receiver', 'f-amount', 'f-ip', 'f-ipcountry'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.value = '';
  });
  const defaults = { 'f-balance': '200000', 'f-age': '365', 'f-avg': '5000', 'f-txnhr': '1', 'f-txn24': '3', 'f-distance': '0' };
  Object.entries(defaults).forEach(([id, value]) => {
    const el = document.getElementById(id);
    if (el) el.value = value;
  });
  ['f-newloc', 'f-newdev', 'f-vpn', 'f-intl', 'f-firstrec'].forEach(id => {
    toggleState[id] = false;
    document.getElementById(id)?.classList.remove('on');
  });
  toggleState['f-ipmatch'] = true;
  document.getElementById('f-ipmatch')?.classList.add('on');
  document.getElementById('result-panel').style.display = 'none';
}

function fillSampleFraud() {
  const values = {
    'f-name': 'Rohan Gupta', 'f-mobile': '+919876543210', 'f-sender': '123456789012', 'f-receiver': '987654321098',
    'f-bank': 'HDFC', 'f-age': '12', 'f-amount': '90000', 'f-balance': '95000', 'f-type': 'debit', 'f-currency': 'USD',
    'f-merchant': 'gambling', 'f-mode': 'CARD', 'f-avg': '4000', 'f-location': 'London', 'f-prev-location': 'Mumbai',
    'f-distance': '1400', 'f-device': 'iPhone', 'f-ip': '185.220.101.50', 'f-ipcountry': 'Netherlands', 'f-iptag': 'TOR',
    'f-txnhr': '9', 'f-txn24': '24'
  };
  Object.entries(values).forEach(([id, value]) => {
    const el = document.getElementById(id);
    if (el) el.value = value;
  });
  ['f-newloc', 'f-newdev', 'f-vpn', 'f-intl', 'f-firstrec'].forEach(id => {
    toggleState[id] = true;
    document.getElementById(id)?.classList.add('on');
  });
  toggleState['f-ipmatch'] = false;
  document.getElementById('f-ipmatch')?.classList.remove('on');
  toast('Fraud sample loaded — hit ANALYZE', 'warning');
}

async function submitManual() {
  if (currentUser?.role === 'ANALYST') {
    toast('Analyst access is read-only', 'warning');
    return;
  }
  const name = document.getElementById('f-name')?.value?.trim();
  const mobile = document.getElementById('f-mobile')?.value?.trim();
  const sender = document.getElementById('f-sender')?.value?.trim();
  const receiver = document.getElementById('f-receiver')?.value?.trim();
  const amount = parseFloat(document.getElementById('f-amount')?.value || '0');
  if (!name || !mobile || !sender || !receiver || !amount) {
    toast('Please fill all required fields', 'error');
    return;
  }

  const payload = {
    accountHolderName: name,
    mobileNumber: mobile,
    senderAccount: sender,
    receiverAccount: receiver,
    bankName: document.getElementById('f-bank')?.value,
    accountAgeDays: parseInt(document.getElementById('f-age')?.value || '365', 10),
    type: document.getElementById('f-type')?.value,
    amount,
    balance: parseFloat(document.getElementById('f-balance')?.value || '200000'),
    currency: document.getElementById('f-currency')?.value,
    merchantCategory: document.getElementById('f-merchant')?.value,
    merchantId: 'MRC-MANUAL-0001',
    transactionMode: document.getElementById('f-mode')?.value,
    location: document.getElementById('f-location')?.value,
    previousLocation: document.getElementById('f-prev-location')?.value,
    distanceFromLastTxnKm: parseFloat(document.getElementById('f-distance')?.value || '0'),
    device: document.getElementById('f-device')?.value,
    ipAddress: document.getElementById('f-ip')?.value || '1.2.3.4',
    ipCountry: document.getElementById('f-ipcountry')?.value || 'India',
    ipRiskTag: document.getElementById('f-iptag')?.value,
    txnCountLastHour: parseInt(document.getElementById('f-txnhr')?.value || '0', 10),
    txnCountLast24Hours: parseInt(document.getElementById('f-txn24')?.value || '0', 10),
    avgTxnAmount30Days: parseFloat(document.getElementById('f-avg')?.value || '5000'),
    isNewLocation: getToggle('f-newloc'),
    isNewDevice: getToggle('f-newdev'),
    isVpnOrProxy: getToggle('f-vpn'),
    ipMatchesLocation: getToggle('f-ipmatch'),
    isInternational: getToggle('f-intl'),
    isFirstTimeReceiver: getToggle('f-firstrec')
  };

  try {
    const r = await fetch(`${API}/transaction/validate`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify(payload)
    });
    const d = await r.json();
    showResult(d);
    setTimeout(loadAlertCount, 350);
  } catch {
    toast('Submit failed', 'error');
  }
}

function showResult(d) {
  const panel = document.getElementById('result-panel');
  panel.style.display = 'block';
  panel.scrollIntoView({ behavior: 'smooth', block: 'start' });
  const tx = d.transaction || {};
  const status = d.status || 'UNKNOWN';
  const badge = document.getElementById('result-badge');
  badge.className = 'badge ' + (
    status === 'FRAUD_DETECTED' ? (tx.riskLevel === 'CRITICAL' ? 'badge-critical' : 'badge-high') :
    status === 'REVIEW_NEEDED' ? 'badge-medium' :
    status === 'REJECTED' ? 'badge-rejected' : 'badge-clean'
  );
  badge.innerHTML = `<span class="badge-dot"></span>${status.replace('_', ' ')}`;

  const score = tx.riskScore || 0;
  const scoreColor = score >= 7.5 ? 'var(--red)' : score >= 5 ? 'var(--orange)' : score >= 3 ? 'var(--yellow)' : 'var(--green)';
  document.getElementById('result-gauge').style.width = `${score * 10}%`;
  document.getElementById('result-gauge').style.background = scoreColor;
  document.getElementById('result-score-val').style.color = scoreColor;
  document.getElementById('result-score-val').textContent = score;

  document.getElementById('result-fields').innerHTML = `
    <div class="result-field"><div class="result-field-label">Risk Level</div><div class="result-field-value" style="color:${scoreColor}">${tx.riskLevel || '—'}</div></div>
    <div class="result-field"><div class="result-field-label">ML Probability</div><div class="result-field-value">${((tx.mlFraudProbability || 0) * 100).toFixed(1)}%</div></div>
    <div class="result-field"><div class="result-field-label">Saved to DB</div><div class="result-field-value" style="color:${d.saved ? 'var(--green)' : 'var(--red)'}">${d.saved ? 'YES' : 'NO'}</div></div>
    <div class="result-field"><div class="result-field-label">Is Fraud</div><div class="result-field-value" style="color:${tx.isFraud ? 'var(--red)' : 'var(--green)'}">${tx.isFraud ? 'TRUE' : 'FALSE'}</div></div>
  `;

  const rulesSection = document.getElementById('result-rules-section');
  const ruleTags = document.getElementById('result-rule-tags');
  if (tx.fraudReason && tx.fraudReason !== 'None') {
    rulesSection.style.display = 'block';
    ruleTags.innerHTML = tx.fraudReason.split('|').map(r => `<span class="rule-tag">${r.trim()}</span>`).join('');
  } else {
    rulesSection.style.display = 'none';
  }
  document.getElementById('result-message').textContent = d.message || '—';
}

async function loadAnalytics() {
  try {
    const days = document.getElementById('analytics-range')?.value || '30';
    const r = await fetch(`${API}/transaction/analytics?days=${encodeURIComponent(days)}`, { headers: authHeaders() });
    const data = await r.json();
    if (!r.ok) {
      throw new Error(data.error || 'analytics endpoint failed');
    }
    renderAnalyticsSummary(data.summary || {}, data.comparison || {});
    renderTrendChart(data.trendSeries || []);
    renderVolumeChart(data.trendSeries || []);
    renderRiskChart(data.riskDistribution || []);
    renderIpChart(data.ipDistribution || []);
    renderMerchantChart(data.merchantDistribution || []);
    renderRuleChart(data.ruleDistribution || []);
  } catch {
    toast('Analytics failed to load', 'error');
  }
}

function baseChartOptions(includeScales = true) {
  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        labels: {
          color: '#c9d8e8',
          font: { family: 'Barlow Condensed', size: 12, weight: '700' }
        }
      },
      tooltip: {
        backgroundColor: '#111820',
        borderColor: '#2a3f55',
        borderWidth: 1,
        titleColor: '#ffffff',
        bodyColor: '#c9d8e8'
      }
    }
  };

  if (includeScales) {
    options.scales = {
      x: {
        ticks: { color: '#5a7a96', font: { family: 'Space Mono', size: 10 } },
        grid: { color: 'rgba(42,63,85,0.4)' }
      },
      y: {
        ticks: { color: '#5a7a96', font: { family: 'Space Mono', size: 10 } },
        grid: { color: 'rgba(42,63,85,0.35)' }
      }
    };
  }

  return options;
}

function destroyChart(key) {
  if (analyticsCharts[key]) {
    analyticsCharts[key].destroy();
    delete analyticsCharts[key];
  }
}

function formatCurrency(value) {
  return `₹${Number(value || 0).toLocaleString('en-IN')}`;
}

function formatDelta(deltaObj) {
  const delta = Number(deltaObj?.delta || 0);
  const direction = delta > 0 ? '▲' : delta < 0 ? '▼' : '•';
  return `${direction} ${Math.abs(delta).toFixed(1)}% vs previous window`;
}

function renderAnalyticsSummary(summary, comparison) {
  const setText = (id, value) => {
    const el = document.getElementById(id);
    if (el) el.textContent = value;
  };

  setText('analytics-total-transactions', summary.totalTransactions ?? 0);
  setText('analytics-fraud-count', `${summary.fraudCount ?? 0} / ${summary.fraudRate ?? 0}%`);
  setText('analytics-fraud-amount', formatCurrency(summary.fraudAmount));
  setText('analytics-average-risk', summary.averageRiskScore ?? '0.0');
  setText('analytics-transaction-delta', formatDelta(comparison.transactionDelta));
  setText('analytics-fraud-delta', formatDelta(comparison.fraudDelta));
  setText('analytics-amount-delta', formatDelta(comparison.amountDelta));
  setText('analytics-risk-delta', formatDelta(comparison.riskDelta));
}

function renderTrendChart(series) {
  const canvas = document.getElementById('trend-chart');
  if (!canvas) return;
  destroyChart('trend');
  analyticsCharts.trend = new Chart(canvas, {
    type: 'line',
    data: {
      labels: series.map(point => point.date),
      datasets: [
        {
          label: 'Fraud Count',
          data: series.map(point => point.fraud),
          borderColor: '#ff3d57',
          backgroundColor: 'rgba(255,61,87,0.14)',
          fill: true,
          tension: 0.35,
          pointRadius: 3,
          pointBackgroundColor: '#ff3d57'
        }
      ]
    },
    options: baseChartOptions()
  });
}

function renderVolumeChart(series) {
  const canvas = document.getElementById('volume-chart');
  if (!canvas) return;
  destroyChart('volume');
  analyticsCharts.volume = new Chart(canvas, {
    type: 'bar',
    data: {
      labels: series.map(point => point.date),
      datasets: [
        {
          label: 'Total Transactions',
          data: series.map(point => point.total),
          backgroundColor: 'rgba(0,212,255,0.45)',
          borderColor: '#00d4ff',
          borderWidth: 1
        },
        {
          label: 'Fraud Transactions',
          data: series.map(point => point.fraud),
          backgroundColor: 'rgba(255,145,0,0.55)',
          borderColor: '#ff9100',
          borderWidth: 1
        }
      ]
    },
    options: {
      ...baseChartOptions(),
      scales: {
        ...baseChartOptions().scales,
        x: { ...baseChartOptions().scales.x, stacked: false },
        y: { ...baseChartOptions().scales.y, beginAtZero: true }
      }
    }
  });
}

function renderRiskChart(series) {
  const canvas = document.getElementById('risk-chart');
  if (!canvas) return;
  destroyChart('risk');
  const normalizedMap = { NORMAL: 0, MEDIUM: 0, HIGH: 0, CRITICAL: 0 };
  series.forEach(item => {
    const key = String(item.label || '').toUpperCase();
    if (Object.prototype.hasOwnProperty.call(normalizedMap, key)) {
      normalizedMap[key] = Number(item.value || 0);
    }
  });

  const chartOrder = ['NORMAL', 'MEDIUM', 'HIGH', 'CRITICAL'];
  const labels = chartOrder.map(label => {
    if (label === 'NORMAL') return 'NORMAL (NO FRAUD ACTIVITY)';
    if (label === 'MEDIUM') return 'MEDIUM (SOME FRAUD ACTIVITY)';
    return label;
  });
  const values = chartOrder.map(label => normalizedMap[label]);

  const normalizedLabels = series.map(item => {
    const label = String(item.label || '').toUpperCase();
    return label === 'NORMAL' ? 'NORMAL (NO FRAUD ACTIVITY)' : label;
  });
  analyticsCharts.risk = new Chart(canvas, {
    type: 'doughnut',
    data: {
      labels: labels.length ? labels : normalizedLabels,
      datasets: [{
        data: values.length ? values : series.map(item => item.value),
        backgroundColor: ['#00e676', '#ffd740', '#ff9100', '#ff3d57'],
        borderColor: '#111820',
        borderWidth: 2
      }]
    },
    options: {
      ...baseChartOptions(false),
      cutout: '62%'
    }
  });
}

function renderIpChart(series) {
  const canvas = document.getElementById('ip-chart');
  if (!canvas) return;
  destroyChart('ip');
  analyticsCharts.ip = new Chart(canvas, {
    type: 'doughnut',
    data: {
      labels: series.map(item => item.label),
      datasets: [{
        data: series.map(item => item.value),
        backgroundColor: ['rgba(0,230,118,0.45)', 'rgba(255,215,64,0.5)', 'rgba(255,145,0,0.5)', 'rgba(255,61,87,0.6)', 'rgba(30,127,212,0.55)', 'rgba(176,38,255,0.45)'],
        borderColor: ['#00e676', '#ffd740', '#ff9100', '#ff3d57', '#1e7fd4', '#b026ff'],
        borderWidth: 1.5
      }]
    },
    options: {
      ...baseChartOptions(false),
      cutout: '58%'
    }
  });
}

function renderMerchantChart(series) {
  const canvas = document.getElementById('merchant-chart');
  if (!canvas) return;
  destroyChart('merchant');
  analyticsCharts.merchant = new Chart(canvas, {
    type: 'bar',
    data: {
      labels: series.map(item => item.label.toUpperCase()),
      datasets: [{
        label: 'Transactions',
        data: series.map(item => item.value),
        backgroundColor: 'rgba(176,38,255,0.45)',
        borderColor: '#b026ff',
        borderWidth: 1
      }]
    },
    options: {
      ...baseChartOptions(),
      indexAxis: 'y',
      scales: {
        x: { ...baseChartOptions().scales.x, beginAtZero: true },
        y: { ...baseChartOptions().scales.y }
      },
      plugins: {
        ...baseChartOptions().plugins,
        legend: { display: false }
      }
    }
  });
}

function renderRuleChart(series) {
  const canvas = document.getElementById('rule-chart');
  if (!canvas) return;
  destroyChart('rule');
  analyticsCharts.rule = new Chart(canvas, {
    type: 'bar',
    data: {
      labels: series.map(item => item.label),
      datasets: [{
        label: 'Rule Hits',
        data: series.map(item => item.value),
        backgroundColor: 'rgba(0,212,255,0.5)',
        borderColor: '#00d4ff',
        borderWidth: 1
      }]
    },
    options: {
      ...baseChartOptions(),
      indexAxis: 'y',
      scales: {
        x: { ...baseChartOptions().scales.x, beginAtZero: true },
        y: { ...baseChartOptions().scales.y }
      },
      plugins: {
        ...baseChartOptions().plugins,
        legend: { display: false }
      }
    }
  });
}

const RULES_REF = [
  ['R01', 'High Amount ≥ ₹50K', '+2.5'], ['R01', 'Critical Amount ≥ ₹1L', '+4.0'],
  ['R02', 'Odd Hours (1–4 AM)', '+2.0'], ['R03', 'Balance Drain ≥ 90%', '+3.5'],
  ['R04', 'Frequent Txns ≥ 4/hr', '+1.5'], ['R04', 'Rapid Fire ≥ 8/hr', '+3.0'],
  ['R05', 'Crypto Merchant', '+3.0'], ['R05', 'Gambling Merchant', '+2.5'],
  ['R05', 'Dark Web Merchant', '+5.0'], ['R06', 'Location Jump >500km', '+2.0'],
  ['R06', 'Impossible Travel >1000km', '+3.5'], ['R07', 'New Device', '+1.5'],
  ['R08a', 'VPN/Proxy Flag', '+1.5'], ['R08b', 'IP-Location Mismatch', '+2.0'],
  ['R08c', 'TOR Network', '+3.5'], ['R08c', 'Datacenter IP', '+2.5'],
  ['R09', 'International Txn', '+2.0'], ['R10', 'New Receiver + High Amt', '+2.0'],
  ['R11', 'Amount Spike 5× Avg', '+2.5'], ['R12', 'New Account <30d', '+2.5'],
  ['R13', 'High Daily Volume', '+2.0'], ['R14', 'Round Number', '+1.0']
];

const rulesRefBody = document.getElementById('rules-ref-tbody');
if (rulesRefBody) {
  rulesRefBody.innerHTML = RULES_REF.map(([code, name, pts]) => `
    <tr style="border-bottom:1px solid rgba(30,45,61,0.4);">
      <td style="padding:5px 8px;font-family:var(--font-mono);font-size:10px;color:var(--accent-cyan);">${code}</td>
      <td style="padding:5px 8px;color:var(--text-primary);font-size:11px;">${name}</td>
      <td style="padding:5px 8px;text-align:right;font-family:var(--font-mono);font-size:10px;color:var(--yellow);">${pts}</td>
    </tr>
  `).join('');
}

document.addEventListener('DOMContentLoaded', () => {
  ensureSharedNavEnhancements();
  syncActiveTabUi();
  loadSystemStatus();
  if (document.getElementById('stat-total')) {
    loadSummary();
    loadAutoGenerationStatus();
    if (dashboardRefreshInterval) {
      clearInterval(dashboardRefreshInterval);
    }
    dashboardRefreshInterval = setInterval(() => {
      loadSummary();
      loadTransactions();
      loadAlertCount();
      const panel = document.getElementById('notif-panel');
      if (panel && panel.classList.contains('open')) {
        loadAlerts();
      }
    }, 20000);
  }
  if (document.getElementById('txn-tbody')) {
    loadTransactions();
  }
  if (document.getElementById('profile-card-grid')) {
    loadProfilePage();
  }
  if (document.getElementById('trend-chart')) {
    loadAnalytics();
  }
  if (document.getElementById('users-tbody')) {
    const form = document.getElementById('create-user-form');
    if (form) {
      form.addEventListener('submit', submitCreateUser);
    }
    loadUsers();
  }
  if (document.getElementById('system-health-cards')) {
    loadSystemDashboard();
  }
  if (document.getElementById('system-logs-all-tbody')) {
    loadSystemLogsPage();
  }
  if (document.getElementById('audit-logs-tbody')) {
    loadAuditLogs();
  }
  if (document.getElementById('simulation-config-form') && getCurrentTabFromPath() === 'simulation') {
    const form = document.getElementById('simulation-config-form');
    if (form && !form.dataset.bound) {
      form.addEventListener('submit', submitSimulation);
      form.dataset.bound = 'true';
    }
    const fraudInput = document.getElementById('simulation-fraud-percentage');
    const normalInput = document.getElementById('simulation-normal-percentage');
    const mediumInput = document.getElementById('simulation-medium-percentage');
    if (fraudInput && !fraudInput.dataset.bound) {
      fraudInput.addEventListener('input', syncSimulationRatioInputs);
      fraudInput.dataset.bound = 'true';
    }
    if (normalInput && !normalInput.dataset.bound) {
      normalInput.addEventListener('input', syncSimulationRatioInputs);
      normalInput.dataset.bound = 'true';
    }
    if (mediumInput && !mediumInput.dataset.bound) {
      mediumInput.addEventListener('input', syncSimulationRatioInputs);
      mediumInput.dataset.bound = 'true';
    }
    syncSimulationRatioInputs();
    loadSimulationStatus();
  }
});

function getSelectedSimulationScenarios() {
  return Array.from(document.querySelectorAll('input[name="simulation-scenario"]:checked')).map(input => input.value);
}

function syncSimulationRatioInputs() {
  const fraudInput = document.getElementById('simulation-fraud-percentage');
  const normalInput = document.getElementById('simulation-normal-percentage');
  const mediumInput = document.getElementById('simulation-medium-percentage');
  if (!fraudInput || !normalInput) return;

  const rawFraud = Number(fraudInput.value || 0);
  const boundedFraud = Math.max(0, Math.min(100, Number.isFinite(rawFraud) ? rawFraud : 0));
  fraudInput.value = String(boundedFraud);

  let medium = mediumInput ? Number(mediumInput.value || 0) : 12;
  medium = Math.max(0, Math.min(100, Number.isFinite(medium) ? medium : 12));
  let normal = Number(normalInput.value || 0);
  normal = Math.max(0, Math.min(100, Number.isFinite(normal) ? normal : 75));

  const total = boundedFraud + medium + normal;
  if (total !== 100) {
    normal = Math.max(0, 100 - boundedFraud - medium);
  }

  if (mediumInput) mediumInput.value = String(medium);
  normalInput.value = String(normal);
}

async function submitSimulation(event) {
  event.preventDefault();
  const payload = {
    volume: parseInt(document.getElementById('simulation-volume')?.value || '100', 10),
    fraudPercentage: parseInt(document.getElementById('simulation-fraud-percentage')?.value || '13', 10),
    normalPercentage: parseInt(document.getElementById('simulation-normal-percentage')?.value || '75', 10),
    mediumPercentage: parseInt(document.getElementById('simulation-medium-percentage')?.value || '12', 10),
    scenarios: getSelectedSimulationScenarios(),
    amountMin: parseFloat(document.getElementById('simulation-amount-min')?.value || '500'),
    amountMax: parseFloat(document.getElementById('simulation-amount-max')?.value || '100000'),
    executionMode: document.getElementById('simulation-mode')?.value || 'INSTANT',
    durationMinutes: parseInt(document.getElementById('simulation-duration')?.value || '1', 10)
  };

  const sum = (payload.fraudPercentage || 0) + (payload.normalPercentage || 0) + (payload.mediumPercentage || 0);
  if (sum !== 100) {
    toast('Fraud + Normal + Medium must total 100%', 'warning');
    return;
  }

  try {
    const r = await fetch(`${API}/simulation/start`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify(payload)
    });
    const data = await r.json().catch(() => ({}));
    if (!r.ok) {
      toast(data.error || 'Simulation could not start', 'error');
      return;
    }
    toast('Simulation started', 'success');
    renderSimulationStatus(data);
    loadSimulationStatus();
  } catch {
    toast('Simulation could not start', 'error');
  }
}

async function stopSimulation() {
  try {
    const r = await fetch(`${API}/simulation/stop`, {
      method: 'POST',
      headers: authHeaders()
    });
    const data = await r.json().catch(() => ({}));
    if (!r.ok) {
      toast(data.error || 'Simulation stop failed', 'error');
      return;
    }
    toast('Simulation stop requested', 'warning');
    renderSimulationStatus(data);
  } catch {
    toast('Simulation stop failed', 'error');
  }
}

async function loadSimulationStatus() {
  try {
    const r = await fetch(`${API}/simulation/status`, { headers: authHeaders() });
    const data = await r.json().catch(() => ({}));
    if (!r.ok) {
      toast('Simulation status failed to load', 'error');
      return;
    }
    renderSimulationStatus(data);
    if (data.running || data.status === 'STOPPING') {
      clearTimeout(window.__simulationPoll);
      window.__simulationPoll = setTimeout(loadSimulationStatus, 1500);
    }
  } catch {
    toast('Simulation status failed to load', 'error');
  }
}

function renderSimulationStatus(data) {
  const setText = (id, value) => {
    const el = document.getElementById(id);
    if (el) el.textContent = value;
  };
  const total = Number(data.totalPlanned || 0);
  const processed = Number(data.processed || 0);
  const progress = total > 0 ? Math.round((processed / total) * 100) : 0;
  const fill = document.getElementById('simulation-progress-fill');
  if (fill) fill.style.width = `${progress}%`;

  setText('simulation-status-text', data.status || 'IDLE');
  setText('simulation-progress-text', `${processed} / ${total}`);
  setText('simulation-fraud-count', data.fraudCount ?? 0);
  setText('simulation-normal-count', data.normalCount ?? 0);
  setText('simulation-average-risk', (data.averageRiskScore ?? 0).toString());
  setText('simulation-started-at', data.startedAt || '—');

  const startBtn = document.getElementById('simulation-start-btn');
  const stopBtn = document.getElementById('simulation-stop-btn');
  if (startBtn) startBtn.disabled = !!data.running;
  if (stopBtn) stopBtn.style.display = (data.running || data.status === 'STOPPING') ? 'inline-flex' : 'none';

  const events = document.getElementById('simulation-events');
  if (events) {
    const rows = data.events || [];
    events.innerHTML = rows.length
      ? rows.map(row => `<div class="sim-log-row"><span>${row.timestamp || ''}</span><strong>${row.title || ''}</strong><span>${row.detail || ''}</span></div>`).join('')
      : '<div class="empty-state"><div class="empty-text">No simulation events yet</div></div>';
  }

  const recent = document.getElementById('simulation-recent-tbody');
  if (recent) {
    const rows = data.recentTransactions || [];
    recent.innerHTML = rows.length
      ? rows.map(row => `<tr><td>${row.name || '—'}</td><td>₹${Number(row.amount || 0).toLocaleString('en-IN')}</td><td>${row.riskLevel || '—'}</td><td>${row.status || '—'}</td></tr>`).join('')
      : '<tr><td colspan="4" style="text-align:center;padding:24px;color:var(--text-muted);font-family:var(--font-mono);">No simulated transactions yet</td></tr>';
  }
}

function statusBadge(status) {
  const normalized = String(status || 'UNKNOWN').toUpperCase();
  const className = normalized === 'UP'
    ? 'badge-clean'
    : normalized === 'DEGRADED'
      ? 'badge-medium'
      : 'badge-critical';
  return `<span class="badge ${className}"><span class="badge-dot"></span>${normalized}</span>`;
}

async function loadSystemDashboard() {
  try {
    const [healthResp, logsResp] = await Promise.all([
      fetch(`${API}/system/health`, { headers: authHeaders() }),
      fetch(`${API}/system/api-logs`, { headers: authHeaders() })
    ]);
    const health = await healthResp.json();
    const logs = await logsResp.json();
    renderSystemHealth(health || {});
    renderApiLogs(Array.isArray(logs) ? logs : []);
  } catch (e) {
    toast('System status failed to load', 'error');
  }
}

async function loadSystemLogsPage() {
  const tbody = document.getElementById('system-logs-all-tbody');
  if (!tbody) return;
  try {
    const r = await fetch(`${API}/system/api-logs`, { headers: authHeaders() });
    const logs = await r.json().catch(() => []);
    if (!r.ok || !Array.isArray(logs)) {
      tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;padding:40px;color:var(--red);font-family:var(--font-mono);">Failed to load API logs</td></tr>';
      return;
    }
    if (!logs.length) {
      tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;padding:40px;color:var(--text-muted);font-family:var(--font-mono);">No API activity yet</td></tr>';
      return;
    }
    tbody.innerHTML = logs.map(log => {
      const status = Number(log.statusCode || 0);
      const badgeClass = status >= 500 ? 'badge-critical' : status >= 400 ? 'badge-medium' : 'badge-clean';
      return `
        <tr>
          <td>${formatDateTimeValue(log.timestamp)}</td>
          <td>${log.method || '—'}</td>
          <td title="${log.endpoint || ''}">${log.endpoint || '—'}</td>
          <td><span class="badge ${badgeClass}"><span class="badge-dot"></span>${status || '—'}</span></td>
          <td>${log.responseTimeMs ?? 0} ms</td>
          <td>${log.userEmail || 'ANONYMOUS'}</td>
        </tr>`;
    }).join('');
  } catch {
    tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;padding:40px;color:var(--red);font-family:var(--font-mono);">Failed to load API logs</td></tr>';
  }
}

function renderSystemHealth(health) {
  const host = document.getElementById('system-health-cards');
  if (!host) return;
  host.innerHTML = `
    <div class="stat-card cyan">
      <div class="stat-label">Database</div>
      <div class="stat-value system-stat-badge">${statusBadge(health.dbStatus)}</div>
      <div class="stat-meta">Updated ${health.lastUpdate || '—'}</div>
    </div>
    <div class="stat-card orange">
      <div class="stat-label">ML Service</div>
      <div class="stat-value system-stat-badge">${statusBadge(health.mlStatus)}</div>
      <div class="stat-meta">Txn/min ${health.txnProcessingRate ?? 0}</div>
    </div>
    <div class="stat-card green">
      <div class="stat-label">Email Service</div>
      <div class="stat-value system-stat-badge">${statusBadge(health.emailStatus)}</div>
      <div class="stat-meta">Active sessions ${health.activeSessions ?? 0}</div>
    </div>
    <div class="stat-card red">
      <div class="stat-label">Errors / 1h</div>
      <div class="stat-value">${health.errorCount1Hr ?? 0}</div>
      <div class="stat-meta">Requests / 1h ${health.requestsLastHour ?? 0}</div>
    </div>
  `;

  const detail = document.getElementById('system-overview-meta');
  if (detail) {
    detail.innerHTML = `Stored txns: <strong>${health.transactionsStored ?? 0}</strong> · Txn/min: <strong>${health.txnProcessingRate ?? 0}</strong> · Active sessions: <strong>${health.activeSessions ?? 0}</strong>`;
  }

  const ml = health.mlInsights || {};
  const mlGrid = document.getElementById('ml-insight-grid');
  if (mlGrid) {
    mlGrid.innerHTML = `
      <div class="detail-item"><div class="detail-item-label">MODEL STATUS</div><div class="detail-item-value">${ml.status || '—'}</div></div>
      <div class="detail-item"><div class="detail-item-label">MODEL VERSION</div><div class="detail-item-value">${ml.modelVersion || '—'}</div></div>
      <div class="detail-item"><div class="detail-item-label">LAST TRAINED</div><div class="detail-item-value">${ml.lastTrained || '—'}</div></div>
      <div class="detail-item"><div class="detail-item-label">PRECISION / RECALL / F1</div><div class="detail-item-value">${ml.precision || 0} / ${ml.recall || 0} / ${ml.f1Score || 0}</div></div>
    `;
  }

  const mlFeature = document.getElementById('ml-feature-importance');
  if (mlFeature) {
    const items = ml.featureImportance || [];
    mlFeature.innerHTML = items.map(item => `
      <div style="margin-bottom:12px;">
        <div style="display:flex;justify-content:space-between;margin-bottom:6px;"><span class="detail-item-label">${item.name}</span><span class="detail-item-value">${Math.round((item.score || 0) * 100)}%</span></div>
        <div class="score-bar"><div class="score-bar-fill" style="width:${Math.round((item.score || 0) * 100)}%;background:linear-gradient(90deg,var(--accent-cyan),var(--accent-purple));"></div></div>
      </div>
    `).join('');
  }

  const mlComparison = document.getElementById('ml-comparison-tbody');
  if (mlComparison) {
    const rows = ml.comparison || [];
    mlComparison.innerHTML = rows.map(row => `<tr><td>${row.metric}</td><td>${row.ruleBased}</td><td>${row.ml}</td></tr>`).join('') || '<tr><td colspan="3">No ML comparison available</td></tr>';
  }
}

function renderApiLogs(logs) {
  const tbody = document.getElementById('system-logs-tbody');
  if (!tbody) return;
  if (!logs.length) {
    tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;padding:40px;color:var(--text-muted);font-family:var(--font-mono);">No API activity yet</td></tr>';
    return;
  }
  const shown = logs.slice(0, 8);
  tbody.innerHTML = shown.map(log => {
    const status = Number(log.statusCode || 0);
    const badgeClass = status >= 500 ? 'badge-critical' : status >= 400 ? 'badge-medium' : 'badge-clean';
    return `
      <tr>
        <td>${formatDateTimeValue(log.timestamp)}</td>
        <td>${log.method || '—'}</td>
        <td title="${log.endpoint || ''}">${log.endpoint || '—'}</td>
        <td><span class="badge ${badgeClass}"><span class="badge-dot"></span>${status || '—'}</span></td>
        <td>${log.responseTimeMs ?? 0} ms</td>
        <td>${log.userEmail || 'ANONYMOUS'}</td>
      </tr>`;
  }).join('');

  const viewAllWrapId = 'system-logs-view-all-wrap';
  document.getElementById(viewAllWrapId)?.remove();
  if (logs.length > shown.length) {
    const tableWrap = document.querySelector('#system-logs-tbody')?.closest('.table-wrap');
    if (tableWrap) {
      const wrap = document.createElement('div');
      wrap.id = viewAllWrapId;
      wrap.style.display = 'flex';
      wrap.style.justifyContent = 'flex-end';
      wrap.style.padding = '10px 12px 12px';
      wrap.innerHTML = '<button class="btn btn-ghost btn-sm" type="button" onclick="navigateToTab(\'system-logs\')">VIEW ALL</button>';
      tableWrap.appendChild(wrap);
    }
  }
}

function getAuditFilterParams() {
  const params = new URLSearchParams();
  const user = document.getElementById('audit-filter-user')?.value?.trim();
  const action = document.getElementById('audit-filter-action')?.value?.trim();
  const date = document.getElementById('audit-filter-date')?.value;
  if (user) params.set('user', user);
  if (action) params.set('action', action);
  if (date) params.set('date', date);
  return params;
}

function clearAuditFilters() {
  ['audit-filter-user', 'audit-filter-action', 'audit-filter-date'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.value = '';
  });
  loadAuditLogs();
}

function exportAuditLogs() {
  const params = getAuditFilterParams();
  window.open(`${API}/audit/export-csv?${params.toString()}`, '_blank');
}

async function loadAuditLogs() {
  const tbody = document.getElementById('audit-logs-tbody');
  if (!tbody) return;
  tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:40px;color:var(--text-muted);font-family:var(--font-mono);">Loading audit activity...</td></tr>';
  try {
    const params = getAuditFilterParams();
    const r = await fetch(`${API}/audit/logs?${params.toString()}`, { headers: authHeaders() });
    const logs = await r.json().catch(() => []);
    if (!r.ok) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:40px;color:var(--red);font-family:var(--font-mono);">Failed to load audit logs</td></tr>';
      return;
    }
    if (!logs.length) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:40px;color:var(--text-muted);font-family:var(--font-mono);">No audit activity yet</td></tr>';
      return;
    }
    tbody.innerHTML = logs.map(log => {
      const role = String(log.userRole || 'UNKNOWN').toUpperCase();
      const action = String(log.actionType || 'UNKNOWN').toUpperCase();
      const roleClass = role === 'SUPERADMIN' ? 'role-superadmin' : role === 'ADMIN' ? 'role-admin' : 'role-analyst';
      return `
      <tr>
        <td class="audit-time">${formatDateTimeValue(log.timestamp)}</td>
        <td class="audit-user">${maskEmailValue(log.userEmail) || '—'}</td>
        <td><span class="role-badge ${roleClass}">${role}</span></td>
        <td><span class="badge badge-clean audit-action-badge">${action}</span></td>
        <td class="audit-target">${log.targetEntity || '—'}</td>
        <td class="audit-target-id">${log.targetId || '—'}</td>
        <td class="audit-details" title="${log.details || ''}">${log.details || '—'}</td>
      </tr>`;
    }).join('');
  } catch {
    tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:40px;color:var(--red);font-family:var(--font-mono);">Failed to load audit logs</td></tr>';
  }
}

function formatProfileValue(value, fallback = '—') {
  return value === null || value === undefined || value === '' ? fallback : value;
}

async function loadProfilePage() {
  if (!currentUser) {
    try {
      const r = await fetch(`${API}/auth/me`, { headers: authHeaders() });
      if (r.ok) {
        currentUser = await r.json();
      }
    } catch {
      return;
    }
  }

  if (!currentUser) return;

  const grid = document.getElementById('profile-card-grid');
  if (grid) {
    grid.innerHTML = `
      <div class="stat-card cyan">
        <div class="stat-label">Full Name</div>
        <div class="stat-value">${formatProfileValue(currentUser.name)}</div>
        <div class="stat-meta">ACTIVE USER PROFILE</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-label">Role</div>
        <div class="stat-value">${formatProfileValue(currentUser.role)}</div>
        <div class="stat-meta">ACCESS TIER</div>
      </div>
      <div class="stat-card green">
        <div class="stat-label">Email Alerts</div>
        <div class="stat-value">${currentUser.role === 'ANALYST' ? 'N/A' : (currentUser.emailAlertsEnabled ? 'ON' : 'OFF')}</div>
        <div class="stat-meta">NOTIFICATION PREF</div>
      </div>
      <div class="stat-card red">
        <div class="stat-label">Status</div>
        <div class="stat-value">${currentUser.isActive === false ? 'INACTIVE' : 'ACTIVE'}</div>
        <div class="stat-meta">ACCOUNT STATE</div>
      </div>
    `;
  }

  const details = document.getElementById('profile-detail-grid');
  if (details) {
    details.innerHTML = `
      <div class="detail-item"><div class="detail-item-label">NAME</div><div class="detail-item-value">${formatProfileValue(currentUser.name)}</div></div>
      <div class="detail-item"><div class="detail-item-label">EMAIL</div><div class="detail-item-value">${formatProfileValue(currentUser.email)}</div></div>
      <div class="detail-item"><div class="detail-item-label">ROLE</div><div class="detail-item-value">${formatProfileValue(currentUser.role)}</div></div>
      <div class="detail-item"><div class="detail-item-label">CREATED BY</div><div class="detail-item-value">${formatProfileValue(currentUser.createdBy, 'SYSTEM')}</div></div>
      <div class="detail-item"><div class="detail-item-label">CREATED AT</div><div class="detail-item-value">${formatDateTimeValue(currentUser.createdAt)}</div></div>
      <div class="detail-item"><div class="detail-item-label">LAST LOGIN</div><div class="detail-item-value">${formatDateTimeValue(currentUser.lastLogin)}</div></div>
      <div class="detail-item"><div class="detail-item-label">CAN BE DELETED</div><div class="detail-item-value">${currentUser.canBeDeleted === true ? 'YES' : 'NO'}</div></div>
      <div class="detail-item"><div class="detail-item-label">EMAIL ALERTS</div><div class="detail-item-value">${currentUser.role === 'ANALYST' ? 'NOT AVAILABLE' : (currentUser.emailAlertsEnabled ? 'ENABLED' : 'DISABLED')}</div></div>
    `;
  }

  const prefText = document.getElementById('profile-alert-pref-text');
  const prefBtn = document.getElementById('profile-alert-pref-btn');
  if (prefText) {
    prefText.textContent = currentUser.role === 'ANALYST'
      ? 'Email alerts are available for admin-side users only.'
      : (currentUser.emailAlertsEnabled ? 'Fraud alert emails are enabled for this account.' : 'Fraud alert emails are disabled for this account.');
  }
  if (prefBtn) {
    prefBtn.style.display = currentUser.role === 'ANALYST' ? 'none' : 'inline-flex';
    prefBtn.textContent = currentUser.emailAlertsEnabled ? 'OPT OUT EMAIL ALERTS' : 'OPT IN EMAIL ALERTS';
  }
}

async function loadUsers() {
  const tbody = document.getElementById('users-tbody');
  if (!tbody) return;

  tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:40px;color:var(--text-muted);font-family:var(--font-mono);">Loading users...</td></tr>';

  try {
    const r = await fetch(`${API}/auth/users`, { headers: authHeaders() });
    const users = await r.json().catch(() => []);
    if (!r.ok) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;padding:40px;color:var(--red);font-family:var(--font-mono);">${users.error || 'Unable to load users'}</td></tr>`;
      return;
    }

    if (!users.length) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:40px;color:var(--text-muted);font-family:var(--font-mono);">No users found</td></tr>';
      return;
    }

    tbody.innerHTML = users.map(user => {
      const badgeClass = `role-badge role-${String(user.role).toLowerCase()}`;
      const canDelete = !!user.canBeDeleted && currentUser && (currentUser.role === 'SUPERADMIN' || (currentUser.role === 'ADMIN' && user.role === 'ANALYST'));
      const roleOptions = currentUser?.role === 'SUPERADMIN' ? ['ADMIN', 'ANALYST'] : ['ANALYST'];
      const safeName = String(user.name || '').replace(/'/g, "\\'");

      return `
        <tr>
          <td>${user.name || '—'}</td>
          <td>${user.email || '—'}</td>
          <td><span class="${badgeClass}">${user.role}</span></td>
          <td>${user.createdBy || 'SYSTEM'}</td>
          <td>
            <label class="mini-switch ${user.emailAlertsEnabled ? 'on' : ''} ${user.role === 'ANALYST' ? 'locked' : ''}">
              <input type="checkbox" ${user.emailAlertsEnabled ? 'checked' : ''} ${user.role === 'ANALYST' ? 'disabled' : ''}
                onchange='updateUserSettings(${user.id}, { "emailAlertsEnabled": this.checked })'>
              <span>${user.emailAlertsEnabled ? 'ON' : 'OFF'}</span>
            </label>
          </td>
          <td>
            <select class="form-select compact-select" onchange='updateUserSettings(${user.id}, { "role": this.value })' ${user.role === 'SUPERADMIN' ? 'disabled' : ''}>
              <option value="${user.role}">${user.role}</option>
              ${roleOptions.filter(role => role !== user.role).map(role => `<option value="${role}">${role}</option>`).join('')}
            </select>
          </td>
          <td>
            ${canDelete ? `<button class="btn btn-danger btn-sm" onclick="deleteUser(${user.id}, '${safeName}')">DELETE</button>` : '<span class="table-muted">LOCKED</span>'}
          </td>
        </tr>`;
    }).join('');
  } catch (err) {
    tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:40px;color:var(--red);font-family:var(--font-mono);">Failed to load users</td></tr>';
  }
}

function openCreateUserModal() {
  const modal = document.getElementById('create-user-modal');
  if (!modal) return;
  const roleSelect = document.getElementById('new-user-role');
  if (roleSelect && currentUser?.role === 'ADMIN') {
    roleSelect.innerHTML = '<option value="ANALYST">ANALYST</option>';
  }
  modal.classList.add('open');
}

function closeCreateUserModal(event) {
  if (!event || event.target.id === 'create-user-modal' || event.target.classList.contains('modal-close')) {
    document.getElementById('create-user-modal')?.classList.remove('open');
  }
}

async function submitCreateUser(event) {
  event.preventDefault();
  const name = document.getElementById('new-user-name')?.value?.trim();
  const email = document.getElementById('new-user-email')?.value?.trim();
  const password = document.getElementById('new-user-password')?.value || '';
  const role = document.getElementById('new-user-role')?.value;

  if (!name || !email || !password || !role) {
    toast('All user fields are required', 'warning');
    return;
  }

  try {
    const r = await fetch(`${API}/auth/create-user`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify({ name, email, password, role })
    });
    const data = await r.json().catch(() => ({}));
    if (!r.ok) {
      toast(data.error || 'Unable to create user', 'error');
      return;
    }
    closeCreateUserModal();
    document.getElementById('create-user-form')?.reset();
    toast(`Created ${data.role} account for ${data.name}`, 'success');
    loadUsers();
  } catch (err) {
    toast('Unable to create user', 'error');
  }
}

async function updateUserSettings(userId, changes) {
  try {
    const r = await fetch(`${API}/auth/users/${userId}`, {
      method: 'PUT',
      headers: authHeaders(),
      body: JSON.stringify(changes)
    });
    const data = await r.json().catch(() => ({}));
    if (!r.ok) {
      toast(data.error || 'Unable to update user', 'error');
      loadUsers();
      return;
    }
    toast('User updated', 'success');
    loadUsers();
  } catch (err) {
    toast('Unable to update user', 'error');
    loadUsers();
  }
}

async function deleteUser(userId, userName) {
  if (!window.confirm(`Delete ${userName}? This cannot be undone.`)) return;
  try {
    const r = await fetch(`${API}/auth/users/${userId}`, {
      method: 'DELETE',
      headers: authHeaders()
    });
    const data = await r.json().catch(() => ({}));
    if (!r.ok) {
      toast(data.error || 'Unable to delete user', 'error');
      return;
    }
    toast(data.message || 'User deleted', 'success');
    loadUsers();
  } catch (err) {
    toast('Unable to delete user', 'error');
  }
}
