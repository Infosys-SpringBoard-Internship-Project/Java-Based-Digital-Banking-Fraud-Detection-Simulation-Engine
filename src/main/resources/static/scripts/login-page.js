        const form = document.getElementById('loginForm');
        const emailInput = document.getElementById('email');
        const passwordInput = document.getElementById('password');
        const toggleBtn = document.getElementById('togglePassword');
        const submitBtn = document.getElementById('submitBtn');
        const errorMsg = document.getElementById('errorMsg');

        toggleBtn.addEventListener('click', () => {
            const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
            passwordInput.setAttribute('type', type);
            toggleBtn.textContent = type === 'password' ? 'SHOW' : 'HIDE';
        });

        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const email = emailInput.value;
            const password = passwordInput.value;

            // Reset UI
            errorMsg.style.display = 'none';
            submitBtn.disabled = true;
            submitBtn.classList.add('loading');

            try {
                const response = await fetch('/auth/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email, password })
                });

                if (response.ok) {
                    const data = await response.json();
                    if(data.token) localStorage.setItem('fraud_token', data.token);
                    else localStorage.setItem('fraud_token', 'mock_token_for_now'); // Fallback if no token prop
                    window.location.href = '/pages/dashboard.html';
                } else if (response.status === 401) {
                    showError("Invalid credentials. Access denied.");
                } else {
                    showError("Authentication failed. (" + response.status + ")");
                }
            } catch (error) {
                showError("Cannot reach server. Is Spring Boot running?");
            } finally {
                submitBtn.disabled = false;
                submitBtn.classList.remove('loading');
            }
        });

        function showError(message) {
            errorMsg.textContent = message;
            errorMsg.style.display = 'block';
        }
