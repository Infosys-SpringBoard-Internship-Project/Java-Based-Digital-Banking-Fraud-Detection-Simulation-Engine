const registerForm = document.getElementById('registerForm');
const nameInput = document.getElementById('name');
const emailInput = document.getElementById('email');
const passwordInput = document.getElementById('password');
const toggleBtn = document.getElementById('togglePassword');
const submitBtn = document.getElementById('submitBtn');
const registerMsg = document.getElementById('registerMsg');
const setupMsg = document.getElementById('setupMsg');

let setupAllowed = true;

document.addEventListener('DOMContentLoaded', () => {
    loadBootstrapStatus();
});

toggleBtn.addEventListener('click', () => {
    const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
    passwordInput.setAttribute('type', type);
    toggleBtn.textContent = type === 'password' ? 'SHOW' : 'HIDE';
});

registerForm.addEventListener('submit', async (event) => {
    event.preventDefault();

    if (!setupAllowed) {
        showMessage('Superadmin setup is already completed. Please log in with the existing superadmin account.');
        return;
    }

    const name = nameInput.value.trim();
    const email = emailInput.value.trim();
    const password = passwordInput.value;

    if (!name || !email || !password) {
        showMessage('All fields are required.');
        return;
    }

    if (password.length < 6) {
        showMessage('Password must be at least 6 characters.');
        return;
    }

    showMessage('');
    setButtonLoading(true);

    try {
        const response = await fetch('/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, email, password })
        });

        const data = await safeJson(response);
        if (response.status === 201) {
            showMessage('Superadmin created successfully. Redirecting to login...', true);
            setTimeout(() => {
                window.location.href = '/pages/login.html';
            }, 1200);
            return;
        }

        showMessage(data.error || 'Unable to create superadmin right now.');
        if (data.error === 'Admin already exists') {
            setupAllowed = false;
            applySetupState();
        }
    } catch (error) {
        showMessage('Cannot reach server. Is Spring Boot running?');
    } finally {
        setButtonLoading(false);
    }
});

async function loadBootstrapStatus() {
    try {
        const response = await fetch('/auth/bootstrap-status');
        const data = await safeJson(response);
        setupAllowed = !!data.superadminCreationAllowed;
        applySetupState();
    } catch (error) {
        setupAllowed = true;
        setupMsg.textContent = 'Unable to verify setup status. You can still attempt first-time superadmin creation.';
        setupMsg.classList.add('success');
        setupMsg.style.display = 'block';
    }
}

function applySetupState() {
    if (setupAllowed) {
        setupMsg.textContent = 'No superadmin account exists. Create the first SUPERADMIN here.';
        setupMsg.classList.add('success');
        setupMsg.style.display = 'block';
        submitBtn.disabled = false;
        nameInput.disabled = false;
        emailInput.disabled = false;
        passwordInput.disabled = false;
        toggleBtn.disabled = false;
    } else {
        setupMsg.textContent = 'Superadmin setup is already complete. Use the login page to sign in.';
        setupMsg.classList.add('success');
        setupMsg.style.display = 'block';
        submitBtn.disabled = true;
        nameInput.disabled = true;
        emailInput.disabled = true;
        passwordInput.disabled = true;
        toggleBtn.disabled = true;
    }
}

function showMessage(message, isSuccess = false) {
    registerMsg.textContent = message;
    registerMsg.classList.toggle('success', isSuccess);
    registerMsg.style.display = message ? 'block' : 'none';
}

function setButtonLoading(isLoading) {
    submitBtn.disabled = isLoading || !setupAllowed;
    submitBtn.classList.toggle('loading', isLoading);
}

async function safeJson(response) {
    try {
        return await response.json();
    } catch (error) {
        return {};
    }
}
