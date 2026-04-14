/**
 * FraudShield UI/UX Enhancements
 * Loading states, error handling, and improved user feedback
 */

(function() {
    'use strict';

    // =============================================
    // LOADING STATE MANAGEMENT
    // =============================================

    /**
     * Show a loading spinner inside an element
     * @param {string|HTMLElement} target - Element ID or element
     * @param {string} message - Loading message to display
     */
    window.showLoading = function(target, message = 'Loading...') {
        const el = typeof target === 'string' ? document.getElementById(target) : target;
        if (!el) return;

        el.dataset.originalContent = el.innerHTML;
        el.innerHTML = `
            <div class="loading-state">
                <div class="loading-spinner"></div>
                <span class="loading-text">${message}</span>
            </div>
        `;
        el.classList.add('is-loading');
    };

    /**
     * Hide loading spinner and restore original content
     * @param {string|HTMLElement} target - Element ID or element
     */
    window.hideLoading = function(target) {
        const el = typeof target === 'string' ? document.getElementById(target) : target;
        if (!el) return;

        if (el.dataset.originalContent) {
            el.innerHTML = el.dataset.originalContent;
            delete el.dataset.originalContent;
        }
        el.classList.remove('is-loading');
    };

    /**
     * Show loading overlay on the entire page
     * @param {string} message - Loading message
     */
    window.showPageLoading = function(message = 'Please wait...') {
        let overlay = document.getElementById('page-loading-overlay');
        if (!overlay) {
            overlay = document.createElement('div');
            overlay.id = 'page-loading-overlay';
            overlay.className = 'page-loading-overlay';
            document.body.appendChild(overlay);
        }
        overlay.innerHTML = `
            <div class="page-loading-content">
                <div class="loading-spinner large"></div>
                <span class="loading-text">${message}</span>
            </div>
        `;
        overlay.classList.add('visible');
    };

    /**
     * Hide page loading overlay
     */
    window.hidePageLoading = function() {
        const overlay = document.getElementById('page-loading-overlay');
        if (overlay) {
            overlay.classList.remove('visible');
            setTimeout(() => overlay.remove(), 300);
        }
    };

    // =============================================
    // BUTTON LOADING STATES
    // =============================================

    /**
     * Set button to loading state
     * @param {string|HTMLElement} button - Button ID or element
     * @param {string} loadingText - Text to show while loading
     */
    window.setButtonLoading = function(button, loadingText = 'Processing...') {
        const btn = typeof button === 'string' ? document.getElementById(button) : button;
        if (!btn) return;

        btn.dataset.originalText = btn.textContent;
        btn.disabled = true;
        btn.classList.add('btn-loading');
        btn.innerHTML = `<span class="btn-spinner"></span>${loadingText}`;
    };

    /**
     * Reset button from loading state
     * @param {string|HTMLElement} button - Button ID or element
     */
    window.resetButton = function(button) {
        const btn = typeof button === 'string' ? document.getElementById(button) : button;
        if (!btn) return;

        if (btn.dataset.originalText) {
            btn.textContent = btn.dataset.originalText;
            delete btn.dataset.originalText;
        }
        btn.disabled = false;
        btn.classList.remove('btn-loading');
    };

    // =============================================
    // ENHANCED ERROR HANDLING
    // =============================================

    /**
     * Show an error state in an element
     * @param {string|HTMLElement} target - Element ID or element
     * @param {string} message - Error message
     * @param {Function} retryFn - Optional retry function
     */
    window.showError = function(target, message, retryFn) {
        const el = typeof target === 'string' ? document.getElementById(target) : target;
        if (!el) return;

        const retryBtn = retryFn 
            ? `<button class="btn btn-sm btn-ghost error-retry-btn" onclick="(${retryFn.toString()})()">Try Again</button>`
            : '';

        el.innerHTML = `
            <div class="error-state">
                <div class="error-icon">!</div>
                <span class="error-message">${message}</span>
                ${retryBtn}
            </div>
        `;
        el.classList.add('has-error');
    };

    /**
     * Show an empty state in an element
     * @param {string|HTMLElement} target - Element ID or element
     * @param {string} message - Empty state message
     * @param {string} icon - Optional icon character
     */
    window.showEmptyState = function(target, message, icon = '📭') {
        const el = typeof target === 'string' ? document.getElementById(target) : target;
        if (!el) return;

        el.innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">${icon}</div>
                <span class="empty-text">${message}</span>
            </div>
        `;
    };

    // =============================================
    // CONFIRMATION DIALOGS
    // =============================================

    /**
     * Show a confirmation dialog
     * @param {Object} options - Dialog options
     * @returns {Promise<boolean>} - Resolves to true if confirmed
     */
    window.confirmAction = function(options = {}) {
        return new Promise((resolve) => {
            const {
                title = 'Confirm Action',
                message = 'Are you sure you want to proceed?',
                confirmText = 'Confirm',
                cancelText = 'Cancel',
                danger = false
            } = options;

            const overlay = document.createElement('div');
            overlay.className = 'confirm-overlay';
            overlay.innerHTML = `
                <div class="confirm-dialog">
                    <div class="confirm-title">${title}</div>
                    <div class="confirm-message">${message}</div>
                    <div class="confirm-actions">
                        <button class="btn btn-ghost confirm-cancel">${cancelText}</button>
                        <button class="btn ${danger ? 'btn-danger' : 'btn-primary'} confirm-ok">${confirmText}</button>
                    </div>
                </div>
            `;

            const cleanup = (result) => {
                overlay.classList.remove('visible');
                setTimeout(() => overlay.remove(), 200);
                resolve(result);
            };

            overlay.querySelector('.confirm-cancel').onclick = () => cleanup(false);
            overlay.querySelector('.confirm-ok').onclick = () => cleanup(true);
            overlay.onclick = (e) => {
                if (e.target === overlay) cleanup(false);
            };

            document.body.appendChild(overlay);
            requestAnimationFrame(() => overlay.classList.add('visible'));
        });
    };

    // =============================================
    // ENHANCED TOAST NOTIFICATIONS
    // =============================================

    /**
     * Show an enhanced toast notification
     * @param {string} message - Toast message
     * @param {string} type - Toast type: success, error, warning, info
     * @param {number} duration - Duration in ms
     */
    window.showToast = function(message, type = 'info', duration = 4000) {
        const host = document.getElementById('toasts') || createToastContainer();
        
        const icons = {
            success: '✓',
            error: '✕',
            warning: '⚠',
            info: 'ℹ'
        };

        const el = document.createElement('div');
        el.className = `toast toast-enhanced toast-${type}`;
        el.innerHTML = `
            <span class="toast-icon">${icons[type] || icons.info}</span>
            <span class="toast-message">${message}</span>
            <button class="toast-close" onclick="this.parentElement.remove()">×</button>
        `;

        host.appendChild(el);
        
        // Animate in
        requestAnimationFrame(() => el.classList.add('visible'));

        // Auto dismiss
        setTimeout(() => {
            el.classList.remove('visible');
            setTimeout(() => el.remove(), 300);
        }, duration);
    };

    function createToastContainer() {
        const container = document.createElement('div');
        container.id = 'toasts';
        container.className = 'toast-container';
        document.body.appendChild(container);
        return container;
    }

    // =============================================
    // SKELETON LOADERS
    // =============================================

    /**
     * Create skeleton loading placeholder
     * @param {number} rows - Number of skeleton rows
     * @param {number} cols - Number of columns per row
     * @returns {string} - HTML string
     */
    window.createSkeleton = function(rows = 3, cols = 4) {
        const rowHtml = `<div class="skeleton-row">${
            Array(cols).fill('<div class="skeleton-cell"></div>').join('')
        }</div>`;
        return `<div class="skeleton-loader">${Array(rows).fill(rowHtml).join('')}</div>`;
    };

    /**
     * Create table skeleton
     * @param {number} rows - Number of rows
     * @param {number} cols - Number of columns
     * @returns {string} - HTML string
     */
    window.createTableSkeleton = function(rows = 5, cols = 6) {
        const rowHtml = `<tr>${Array(cols).fill('<td><div class="skeleton-cell"></div></td>').join('')}</tr>`;
        return Array(rows).fill(rowHtml).join('');
    };

    // =============================================
    // FORM VALIDATION HELPERS
    // =============================================

    /**
     * Validate form fields and show inline errors
     * @param {HTMLFormElement} form - Form element
     * @returns {boolean} - True if valid
     */
    window.validateForm = function(form) {
        let isValid = true;
        
        // Clear previous errors
        form.querySelectorAll('.field-error').forEach(el => el.remove());
        form.querySelectorAll('.has-error').forEach(el => el.classList.remove('has-error'));

        // Check required fields
        form.querySelectorAll('[required]').forEach(field => {
            if (!field.value.trim()) {
                showFieldError(field, 'This field is required');
                isValid = false;
            }
        });

        // Check email fields
        form.querySelectorAll('[type="email"]').forEach(field => {
            if (field.value && !isValidEmail(field.value)) {
                showFieldError(field, 'Please enter a valid email');
                isValid = false;
            }
        });

        // Check password length
        form.querySelectorAll('[type="password"][minlength]').forEach(field => {
            const minLen = parseInt(field.getAttribute('minlength'));
            if (field.value && field.value.length < minLen) {
                showFieldError(field, `Password must be at least ${minLen} characters`);
                isValid = false;
            }
        });

        return isValid;
    };

    function showFieldError(field, message) {
        field.classList.add('has-error');
        const error = document.createElement('div');
        error.className = 'field-error';
        error.textContent = message;
        field.parentNode.appendChild(error);
    }

    function isValidEmail(email) {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
    }

    // =============================================
    // NETWORK STATUS INDICATOR
    // =============================================

    let wasOffline = false;

    window.addEventListener('online', () => {
        if (wasOffline) {
            showToast('Connection restored', 'success');
            wasOffline = false;
        }
    });

    window.addEventListener('offline', () => {
        showToast('You are offline. Some features may not work.', 'warning', 10000);
        wasOffline = true;
    });

    // =============================================
    // KEYBOARD SHORTCUTS
    // =============================================

    document.addEventListener('keydown', (e) => {
        // ESC to close modals
        if (e.key === 'Escape') {
            const modal = document.querySelector('.modal-overlay.open, .confirm-overlay.visible');
            if (modal) {
                modal.classList.remove('open', 'visible');
                setTimeout(() => {
                    if (modal.classList.contains('confirm-overlay')) modal.remove();
                }, 200);
            }
        }
    });

    // =============================================
    // AUTO-REFRESH INDICATOR
    // =============================================

    window.showRefreshIndicator = function() {
        let indicator = document.getElementById('refresh-indicator');
        if (!indicator) {
            indicator = document.createElement('div');
            indicator.id = 'refresh-indicator';
            indicator.className = 'refresh-indicator';
            indicator.innerHTML = '<span class="refresh-dot"></span>Refreshing...';
            document.body.appendChild(indicator);
        }
        indicator.classList.add('visible');
        setTimeout(() => indicator.classList.remove('visible'), 2000);
    };

    console.log('[FraudShield] UI enhancements loaded');
})();
