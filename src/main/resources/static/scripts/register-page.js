const registerForm = document.getElementById('registerForm');
const nameInput = document.getElementById('name');
const emailInput = document.getElementById('email');
const passwordInput = document.getElementById('password');
const toggleBtn = document.getElementById('togglePassword');
const submitBtn = document.getElementById('submitBtn');
const registerMsg = document.getElementById('registerMsg');

toggleBtn.addEventListener('click', () => {
    const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
    passwordInput.setAttribute('type', type);
    toggleBtn.textContent = type === 'password' ? 'SHOW' : 'HIDE';
});

registerForm.addEventListener('submit', async (event) => {
    event.preventDefault();

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
            showMessage('Account created successfully. Redirecting to login...', true);
            setTimeout(() => {
                window.location.href = '/pages/login.html';
            }, 1200);
            return;
        }

        showMessage(data.error || 'Unable to create account right now.');
    } catch (error) {
        showMessage('Cannot reach server. Is Spring Boot running?');
    } finally {
        setButtonLoading(false);
    }
});

function showMessage(message, isSuccess = false) {
    registerMsg.textContent = message;
    registerMsg.classList.toggle('success', isSuccess);
    registerMsg.style.display = message ? 'block' : 'none';
}

function setButtonLoading(isLoading) {
    submitBtn.disabled = isLoading;
    submitBtn.classList.toggle('loading', isLoading);
}

async function safeJson(response) {
    try {
        return await response.json();
    } catch (error) {
        return {};
    }
}
