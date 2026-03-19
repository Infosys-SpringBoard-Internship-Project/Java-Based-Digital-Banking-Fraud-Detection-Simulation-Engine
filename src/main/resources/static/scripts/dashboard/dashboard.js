/* Auth guard runs immediately on page load. */
const TOKEN_KEY = 'fraud_token';
const LOGIN_START = 'fraud_login_start';
const PENDING_TXN_KEY = 'fraud_pending_txn_id';
const LOGIN_PAGE = '/pages/admin-login.html';
let lastAlertCount = 0;
let alertCountPrimed = false;
let sessionInterval = null;
let alertPollInterval = null;
const API = window.location.protocol === 'file:'
  ? 'http://localhost:8080'
  : window.location.origin;
const TAB_PAGE_MAP = {
  dashboard: '/pages/dashboard.html',
  transactions: '/pages/dashboard-transactions.html',
  manual: '/pages/dashboard-manual.html',
  analytics: '/pages/dashboard-analytics.html'
};

function getCurrentPath() {
  const path = window.location.pathname || '/';
  return path.endsWith('/') && path.length > 1 ? path.slice(0, -1) : path;
}

function getCurrentTabFromPath() {
  const path = getCurrentPath();
  if (path.endsWith('/dashboard-transactions.html')) return 'transactions';
  if (path.endsWith('/dashboard-manual.html')) return 'manual';
  if (path.endsWith('/dashboard-analytics.html')) return 'analytics';
  return 'dashboard';
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
  if (document.getElementById('btn-update-credentials')) return;

  const navAdmin = document.querySelector('.nav-admin');
  if (!navAdmin) return;

  const btn = document.createElement('button');
  btn.id = 'btn-update-credentials';
  btn.className = 'btn-logout';
  btn.textContent = 'UPDATE LOGIN';
  btn.style.marginRight = '8px';
  btn.onclick = openCredentialModal;

  const logoutBtn = navAdmin.querySelector('.btn-logout');
  if (logoutBtn) {
    navAdmin.insertBefore(btn, logoutBtn);
  } else {
    navAdmin.appendChild(btn);
  }

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
    
    /* Show admin info in nav */
    document.getElementById('admin-name').textContent =
      admin.name?.toUpperCase() || 'ADMIN';
    document.getElementById('admin-email').textContent =
      admin.email || '';
    ensureCredentialUpdateButton();

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

document.querySelectorAll('.nav-tab').forEach(tab => {
  tab.addEventListener('click', (e) => {
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
    if (tab.dataset.tab === 'analytics') loadAnalytics();
  });
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
}

async function generateOne() {
  try {
    const r = await fetch(`${API}/transaction/autoValidate`, { headers: authHeaders() });
    const d = await r.json();
    addFeedItem(d);
    loadSummary();
    setTimeout(loadAlertCount, 350);
  } catch {
    toast('Generate failed', 'error');
  }
}

async function generateBatch() {
  const count = parseInt(document.getElementById('gen-count')?.value || '10', 10);
  const btn = document.getElementById('gen-btn');
  const bar = document.getElementById('gen-progress');
  const fill = document.getElementById('gen-fill');
  const status = document.getElementById('gen-status');
  if (btn) btn.disabled = true;
  if (bar) bar.style.display = 'block';
  if (status) status.style.display = 'block';

  let done = 0;
  try {
    for (let i = 0; i < count; i++) {
      const r = await fetch(`${API}/transaction/autoValidate`, { headers: authHeaders() });
      const d = await r.json();
      addFeedItem(d);
      done += 1;
      if (fill) fill.style.width = `${Math.round((done / count) * 100)}%`;
      if (status) status.textContent = `${done} / ${count}`;
    }
    loadSummary();
    setTimeout(loadAlertCount, 450);
    toast(`Generated ${count} transactions`, 'success');
  } catch {
    toast('Generation failed', 'error');
  } finally {
    if (btn) btn.disabled = false;
    if (bar) bar.style.display = 'none';
    if (status) status.style.display = 'none';
    if (fill) fill.style.width = '0%';
  }
}

function addFeedItem(d) {
  const feed = document.getElementById('live-feed');
  if (!feed) return;
  feed.querySelector('.empty-state')?.remove();
  const tx = d?.transaction || {};
  const isFraud = d?.status === 'FRAUD_DETECTED';
  const isMed = d?.status === 'REVIEW_NEEDED';
  const cls = isFraud ? 'fraud' : isMed ? 'medium' : 'clean';
  const now = new Date().toLocaleTimeString('en', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  const item = document.createElement('div');
  item.className = `feed-item ${cls}`;
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

async function loadTransactions() {
  const tbody = document.getElementById('txn-tbody');
  if (tbody) {
    tbody.innerHTML = '<tr><td colspan="9" style="text-align:center;padding:40px;color:var(--text-muted);font-family:var(--font-mono);">Loading...</td></tr>';
  }
  const r = await fetch(`${API}/transaction/all`, { headers: authHeaders() });
  allTxns = await r.json();
  renderTxnTable(allTxns);

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
  let filtered = allTxns;
  if (filter === 'fraud') filtered = allTxns.filter(t => t.isFraud);
  else if (filter !== 'all') filtered = allTxns.filter(t => t.riskLevel === filter);
  renderTxnTable(filtered);
}

function renderTxnTable(txns) {
  const tbody = document.getElementById('txn-tbody');
  if (!tbody) return;
  if (!txns?.length) {
    tbody.innerHTML = '<tr><td colspan="9" style="text-align:center;padding:40px;color:var(--text-muted);font-family:var(--font-mono);">No transactions found</td></tr>';
    return;
  }
  tbody.innerHTML = [...txns].reverse().slice(0, 200).map(t => {
    const badgeCls = t.riskLevel === 'CRITICAL' ? 'badge-critical' : t.riskLevel === 'HIGH' ? 'badge-high' : t.riskLevel === 'MEDIUM' ? 'badge-medium' : 'badge-clean';
    const scoreColor = t.riskScore >= 7.5 ? 'var(--red)' : t.riskScore >= 5 ? 'var(--orange)' : t.riskScore >= 3 ? 'var(--yellow)' : 'var(--green)';
    return `<tr onclick="showDetail('${t.transactionId}')">
      <td class="id-cell">${t.transactionId?.slice(0, 8)}…</td>
      <td>${t.accountHolderName || '—'}</td>
      <td style="color:var(--yellow);">₹${(t.amount || 0).toLocaleString('en-IN')}</td>
      <td>${t.merchantCategory || '—'}</td>
      <td>${t.location || '—'}</td>
      <td><span class="iptag iptag-${t.ipRiskTag || 'CLEAN'}">${t.ipRiskTag || 'CLEAN'}</span></td>
      <td><span class="badge ${badgeCls}"><span class="badge-dot"></span>${t.riskLevel || 'LOW'}</span></td>
      <td><div class="score-bar-wrap"><div class="score-bar"><div class="score-bar-fill" style="width:${(t.riskScore || 0) * 10}%;background:${scoreColor}"></div></div><span class="score-val">${t.riskScore || 0}</span></div></td>
      <td>${t.isFraud ? '<span style="color:var(--red);font-family:var(--font-mono);font-size:10px;">⚠ FRAUD</span>' : '<span style="color:var(--green);font-family:var(--font-mono);font-size:10px;">✓ CLEAN</span>'}</td>
    </tr>`;
  }).join('');
}

function showDetail(id) {
  const t = allTxns.find(tx => tx.transactionId === id);
  if (!t) return;
  const badgeCls = t.riskLevel === 'CRITICAL' ? 'badge-critical' : t.riskLevel === 'HIGH' ? 'badge-high' : t.riskLevel === 'MEDIUM' ? 'badge-medium' : 'badge-clean';
  document.getElementById('modal-title').innerHTML =
    `TXN: ${t.transactionId?.slice(0, 16)}… <span class="badge ${badgeCls}" style="margin-left:10px;">${t.riskLevel}</span>`;

  const fields = obj => Object.entries(obj).map(([k, v]) => `
    <div class="detail-field"><div class="detail-label">${k}</div><div class="detail-value">${v === null || v === undefined ? '—' : String(v)}</div></div>
  `).join('');

  document.getElementById('modal-body').innerHTML = `
    <div class="detail-section"><div class="detail-section-title">Account</div><div class="detail-grid">${fields({
      Name: t.accountHolderName, Mobile: t.mobileNumber, Bank: t.bankName,
      'Sender Acct': t.senderAccount?.slice(-4)?.padStart(12, '*'),
      'Receiver Acct': t.receiverAccount?.slice(-4)?.padStart(12, '*'),
      'Account Age': `${t.accountAgeDays || 0} days`
    })}</div></div>
    <div class="detail-section"><div class="detail-section-title">Transaction</div><div class="detail-grid">${fields({
      Amount: `₹${(t.amount || 0).toLocaleString('en-IN')}`,
      Balance: `₹${(t.balance || 0).toLocaleString('en-IN')}`,
      Type: t.type, Currency: t.currency, Merchant: t.merchantCategory, Mode: t.transactionMode,
      'Avg 30d': `₹${(t.avgTxnAmount30Days || 0).toLocaleString('en-IN')}`,
      'Txns/hr': t.txnCountLastHour, 'Txns/day': t.txnCountLast24Hours
    })}</div></div>
    <div class="detail-section"><div class="detail-section-title">Fraud Analysis</div><div class="detail-grid">${fields({
      'Is Fraud': t.isFraud ? '⚠ TRUE' : 'FALSE',
      'Risk Score': `${t.riskScore || 0} / 10`,
      'Risk Level': t.riskLevel,
      'ML Probability': `${((t.mlFraudProbability || 0) * 100).toFixed(1)}%`
    })}</div>
      <div style="margin-top:12px;"><div class="detail-label" style="margin-bottom:8px;">TRIGGERED RULES</div><div class="rule-tags">${(t.fraudReason || 'None').split('|').map(r => `<span class="rule-tag">${r.trim()}</span>`).join('')}</div></div>
    </div>
  `;
  document.getElementById('modal').classList.add('open');
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
    const r = await fetch(`${API}/transaction/summary`, { headers: authHeaders() });
    const d = await r.json();
    const riskEl = document.getElementById('risk-dist-panel');
    const ipEl = document.getElementById('ip-dist-panel');
    const amountEl = document.getElementById('amount-panel');
    const ruleEl = document.getElementById('rule-freq-panel');
    if (riskEl) {
      riskEl.innerHTML = ['critical', 'high', 'medium', 'low'].map(level => {
        const val = d[level] || 0;
        const color = level === 'critical' ? 'var(--red)' : level === 'high' ? 'var(--orange)' : level === 'medium' ? 'var(--yellow)' : 'var(--green)';
        const pct = Math.round((val / Math.max(d.totalTransactions || 1, 1)) * 100);
        return `<div style="margin-bottom:14px;"><div style="display:flex;justify-content:space-between;margin-bottom:5px;"><span style="font-family:var(--font-display);font-size:11px;font-weight:700;letter-spacing:1px;color:${color};text-transform:uppercase;">${level}</span><span style="font-family:var(--font-mono);font-size:11px;color:var(--text-dim);">${val}</span></div><div style="height:6px;background:var(--border);border-radius:3px;overflow:hidden;"><div style="height:100%;width:${pct}%;background:${color};border-radius:3px;"></div></div></div>`;
      }).join('');
    }
    if (ipEl) {
      const rows = [
        ['TOR', d.torDetected || 0, 'var(--red)'],
        ['IP Mismatch', d.ipLocationMismatch || 0, 'var(--orange)'],
        ['VPN', d.vpnDetected || 0, 'var(--yellow)']
      ];
      const max = Math.max(...rows.map(rw => rw[1]), 1);
      ipEl.innerHTML = rows.map(([label, val, color]) => `<div style="margin-bottom:14px;"><div style="display:flex;justify-content:space-between;margin-bottom:5px;"><span style="font-family:var(--font-display);font-size:11px;font-weight:700;letter-spacing:1px;color:${color};text-transform:uppercase;">${label}</span><span style="font-family:var(--font-mono);font-size:11px;color:var(--text-dim);">${val}</span></div><div style="height:6px;background:var(--border);border-radius:3px;overflow:hidden;"><div style="height:100%;width:${Math.round((val / max) * 100)}%;background:${color};border-radius:3px;"></div></div></div>`).join('');
    }
    if (amountEl) {
      amountEl.innerHTML = `<div style="display:flex;flex-direction:column;gap:14px;"><div><div style="font-family:var(--font-display);font-size:10px;letter-spacing:2px;color:var(--text-dim);margin-bottom:4px;">TOTAL PROCESSED</div><div style="font-family:var(--font-mono);font-size:22px;color:#fff;">₹${(d.totalAmountINR || 0).toLocaleString('en-IN')}</div></div><div><div style="font-family:var(--font-display);font-size:10px;letter-spacing:2px;color:var(--text-dim);margin-bottom:4px;">FRAUD AMOUNT</div><div style="font-family:var(--font-mono);font-size:22px;color:var(--red);">₹${(d.fraudAmountINR || 0).toLocaleString('en-IN')}</div></div><div><div style="font-family:var(--font-display);font-size:10px;letter-spacing:2px;color:var(--text-dim);margin-bottom:4px;">FRAUD RATE</div><div style="font-family:var(--font-mono);font-size:22px;color:var(--orange);">${d['fraudRate%'] || 0}%</div></div></div>`;
    }
    if (ruleEl) {
      const entries = Object.entries(d.ruleBreakdown || {}).sort((a, b) => b[1] - a[1]);
      if (!entries.length) {
        ruleEl.innerHTML = '<div class="empty-state"><div class="empty-text">No fraud data yet</div></div>';
      } else {
        const max = entries[0][1] || 1;
        ruleEl.innerHTML = `<div style="display:flex;flex-direction:column;gap:10px;">${entries.map(([code, count]) => `<div style="display:flex;align-items:center;gap:12px;"><span style="font-family:var(--font-mono);font-size:10px;color:var(--accent-cyan);min-width:48px;background:rgba(0,212,255,0.06);padding:2px 6px;border:1px solid rgba(0,212,255,0.12);border-radius:1px;text-align:center;">${code}</span><div style="flex:1;height:6px;background:var(--border);border-radius:3px;overflow:hidden;"><div style="height:100%;width:${Math.round((count / max) * 100)}%;background:var(--accent-cyan);border-radius:3px;"></div></div><span style="font-family:var(--font-mono);font-size:12px;color:var(--text-dim);min-width:36px;text-align:right;">${count}</span></div>`).join('')}</div>`;
      }
    }
  } catch {
    toast('Analytics failed to load', 'error');
  }
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
  syncActiveTabUi();
  loadSystemStatus();
  if (document.getElementById('stat-total')) {
    loadSummary();
  }
  if (document.getElementById('txn-tbody')) {
    loadTransactions();
  }
  if (document.getElementById('risk-dist-panel')) {
    loadAnalytics();
  }
});
