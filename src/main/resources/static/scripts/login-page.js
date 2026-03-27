const loginForm = document.getElementById('loginForm');
const authActions = document.querySelector('.auth-actions');
const emailInput = document.getElementById('email');
const passwordInput = document.getElementById('password');
const loginRoleSelect = document.getElementById('loginRole');
const loginEmailLabel = document.getElementById('loginEmailLabel');
const toggleBtn = document.getElementById('togglePassword');
const submitBtn = document.getElementById('submitBtn');
const errorMsg = document.getElementById('errorMsg');
const setupStatus = document.getElementById('setupStatus');

const forgotPasswordForm = document.getElementById('forgotPasswordForm');
const forgotEmailInput = document.getElementById('forgotEmail');
const forgotMsg = document.getElementById('forgotMsg');
const forgotPasswordBtn = document.getElementById('forgotPasswordBtn');
const forceChangeForm = document.getElementById('forceChangeForm');
const forceNewPassword = document.getElementById('forceNewPassword');
const forceConfirmPassword = document.getElementById('forceConfirmPassword');
const forceChangeBtn = document.getElementById('forceChangeBtn');
const forceMsg = document.getElementById('forceMsg');

const openCreateAccountBtn = document.getElementById('openCreateAccount');
const openForgotPasswordBtn = document.getElementById('openForgotPassword');
const cancelForgotPasswordBtn = document.getElementById('cancelForgotPassword');
const cancelForceChangeBtn = document.getElementById('cancelForceChange');

toggleBtn.addEventListener('click', () => {
    const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
    passwordInput.setAttribute('type', type);
    toggleBtn.textContent = type === 'password' ? 'SHOW' : 'HIDE';
});

openCreateAccountBtn.addEventListener('click', () => {
    window.location.href = '/pages/superadmin-setup.html';
});

document.addEventListener('DOMContentLoaded', async () => {
    const token = localStorage.getItem('fraud_token');
    if (token) {
        try {
            const response = await fetch('/auth/me', {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (response.ok) {
                window.location.href = '/pages/dashboard.html';
                return;
            }
        } catch (e) {
            localStorage.removeItem('fraud_token');
        }
    }
    loadBootstrapStatus();
    updateLoginRoleLabel();
    updateForgotRoleLabel();
});

loginRoleSelect?.addEventListener('change', () => {
    updateLoginRoleLabel();
    updateForgotRoleLabel();
});

openForgotPasswordBtn.addEventListener('click', () => {
    clearMessages();
    forgotEmailInput.value = emailInput.value.trim();
    updateForgotRoleLabel();
    enterForgotMode();
});

cancelForgotPasswordBtn.addEventListener('click', () => {
    clearMessages();
    exitAuxMode();
});

cancelForceChangeBtn?.addEventListener('click', () => {
    clearMessages();
    exitAuxMode();
});

loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const email = emailInput.value.trim();
    const password = passwordInput.value;

    showMessage(errorMsg, '');
    setButtonLoading(submitBtn, true);

    try {
        const response = await fetch('/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password, role: loginRoleSelect?.value || 'ADMIN' })
        });

        if (response.ok) {
            const data = await response.json();
            if (data.token) {
                localStorage.setItem('fraud_token', data.token);
            }
            if (data.mustChangePassword) {
                forceNewPassword.value = '';
                forceConfirmPassword.value = '';
                enterForceChangeMode();
                showMessage(forceMsg, 'Temporary password detected. Set a new password to continue.');
                return;
            }
            window.location.href = '/pages/dashboard.html';
            return;
        }

        if (response.status === 401) {
            showMessage(errorMsg, 'Invalid credentials. For SUPERADMIN, keep role as ADMIN and use superadmin email/password.');
        } else {
            showMessage(errorMsg, 'Authentication failed. (' + response.status + ')');
        }
    } catch (error) {
        showMessage(errorMsg, 'Cannot reach server. Is Spring Boot running?');
    } finally {
        setButtonLoading(submitBtn, false);
    }
});

forgotPasswordForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const email = forgotEmailInput.value.trim();
    if (!email) {
        showMessage(forgotMsg, 'Email is required.');
        return;
    }

    showMessage(forgotMsg, '');
    setButtonLoading(forgotPasswordBtn, true);

    try {
        const response = await fetch('/auth/forgot-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, role: loginRoleSelect?.value || 'ADMIN' })
        });

        const data = await safeJson(response);
        if (response.ok) {
            showMessage(
                forgotMsg,
                data.message || 'Reset request processed.',
                data.status === 'RESET_SENT'
            );
            if (data.status === 'EMAIL_FAILED') {
                showMessage(
                    forgotMsg,
                    'Email delivery failed. SUPERADMIN can still reset via profile > update credentials after login.',
                    false
                );
            }
            return;
        }

        showMessage(forgotMsg, data.error || 'Unable to process reset request.');
    } catch (error) {
        showMessage(forgotMsg, 'Cannot reach server. Is Spring Boot running?');
    } finally {
        setButtonLoading(forgotPasswordBtn, false);
    }
});

forceChangeForm?.addEventListener('submit', async (e) => {
    e.preventDefault();

    const newPassword = forceNewPassword.value;
    const confirmPassword = forceConfirmPassword.value;

    if (!newPassword || !confirmPassword) {
        showMessage(forceMsg, 'Both fields are required.');
        return;
    }

    if (newPassword.length < 6) {
        showMessage(forceMsg, 'New password must be at least 6 characters.');
        return;
    }

    if (newPassword !== confirmPassword) {
        showMessage(forceMsg, 'Passwords do not match.');
        return;
    }

    showMessage(forceMsg, '');
    setButtonLoading(forceChangeBtn, true);

    try {
        const token = localStorage.getItem('fraud_token');
        const response = await fetch('/auth/update-credentials', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                newPassword: newPassword
            })
        });

        const data = await safeJson(response);
        if (!response.ok) {
            showMessage(forceMsg, data.error || 'Unable to update password.');
            return;
        }

        showMessage(forceMsg, 'Password updated. Redirecting to dashboard...', true);
        setTimeout(() => {
            window.location.href = '/pages/dashboard.html';
        }, 700);
    } catch (error) {
        showMessage(forceMsg, 'Cannot reach server. Is Spring Boot running?');
    } finally {
        setButtonLoading(forceChangeBtn, false);
    }
});

function clearMessages() {
    showMessage(errorMsg, '');
    showMessage(forgotMsg, '');
    showMessage(forceMsg, '');
}

async function loadBootstrapStatus() {
    try {
        const response = await fetch('/auth/bootstrap-status');
        const data = await safeJson(response);
        if (!response.ok) {
            showMessage(setupStatus, 'System ready. Use your assigned credentials to sign in.');
            return;
        }

        if (data.setupRequired) {
            showMessage(setupStatus, 'Initial setup required. Create the first SUPERADMIN account to unlock the platform.', true);
            openCreateAccountBtn.style.display = 'inline-flex';
        } else {
            showMessage(setupStatus, 'Initial setup completed. New accounts are created from the User Management page by SUPERADMIN or ADMIN.', true);
            openCreateAccountBtn.style.display = 'none';
        }
    } catch (error) {
        showMessage(setupStatus, 'Unable to check setup status. If this is first launch, use Create Superadmin.', false);
    }
}

function enterForgotMode() {
    loginForm.style.display = 'none';
    authActions.style.display = 'none';
    forceChangeForm.style.display = 'none';
    forgotPasswordForm.style.display = 'block';
}

function enterForceChangeMode() {
    loginForm.style.display = 'none';
    authActions.style.display = 'none';
    forgotPasswordForm.style.display = 'none';
    forceChangeForm.style.display = 'block';
}

function exitAuxMode() {
    forgotPasswordForm.style.display = 'none';
    forceChangeForm.style.display = 'none';
    loginForm.style.display = 'block';
    authActions.style.display = 'flex';
}

function updateLoginRoleLabel() {
    const role = loginRoleSelect?.value || 'ADMIN';
    if (loginEmailLabel) {
        loginEmailLabel.textContent = 'WORK EMAIL';
    }
    if (emailInput) {
        emailInput.placeholder = role === 'ANALYST' ? 'analyst@fraudshield.com' : role === 'SUPERADMIN' ? 'superadmin@fraudshield.com' : 'admin@fraudshield.com';
    }

    const roleHelp = document.getElementById('roleHelp');
    if (roleHelp) {
        roleHelp.textContent = role === 'ANALYST'
            ? 'Sign in with your analyst work account.'
            : 'Sign in with your admin work account.';
    }
}

function updateForgotRoleLabel() {
    const label = document.querySelector('label[for="forgotEmail"]');
    if (!label) return;
    const role = loginRoleSelect?.value || 'ADMIN';
    label.textContent = `${role} EMAIL`;

    if (forgotEmailInput) {
        forgotEmailInput.placeholder = role === 'ANALYST'
            ? 'analyst@fraudshield.com'
            : role === 'SUPERADMIN'
                ? 'superadmin@fraudshield.com'
                : 'admin@fraudshield.com';
    }
}

function showMessage(target, message, isSuccess = false) {
    target.textContent = message;
    target.classList.toggle('success', isSuccess);
    target.style.display = message ? 'block' : 'none';
}

function setButtonLoading(button, isLoading) {
    button.disabled = isLoading;
    button.classList.toggle('loading', isLoading);
}

async function safeJson(response) {
    try {
        return await response.json();
    } catch (error) {
        return {};
    }
}
