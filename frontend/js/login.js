import { auth } from './auth.js';

export function initLogin() {
    // Hide sidebar and header
    const appLayout = document.querySelector('.app-layout');
    if (appLayout) appLayout.classList.add('auth-mode');
    const sidebar = document.querySelector('.app-sidebar');
    if (sidebar) sidebar.style.display = 'none';
    const topHeader = document.querySelector('.top-header');
    if (topHeader) topHeader.style.display = 'none';

    const loginForm = document.getElementById('login-form');
    const errorDiv = document.getElementById('login-error');
    const loginBtn = document.getElementById('login-btn');

    if (!loginForm) return;

    // Password visibility toggle
    const togglePwd = document.getElementById('toggle-pwd');
    const pwdInput = document.getElementById('login-password');
    if (togglePwd && pwdInput) {
        togglePwd.addEventListener('click', () => {
            const type = pwdInput.getAttribute('type') === 'password' ? 'text' : 'password';
            pwdInput.setAttribute('type', type);
            togglePwd.className = type === 'password' ? 'fas fa-eye' : 'fas fa-eye-slash';
        });
    }

    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const email = document.getElementById('login-email').value.trim();
        const password = document.getElementById('login-password').value;

        if (!email || !password) {
            errorDiv.textContent = "Email and password are required.";
            errorDiv.style.display = "block";
            return;
        }

        try {
            errorDiv.style.display = "none";
            loginBtn.textContent = "Logging in...";
            loginBtn.disabled = true;
            loginBtn.style.opacity = "0.7";

            await auth.loginFarmer(email, password);
            
            // Redirect to home
            window.location.hash = 'home';
            
        } catch (err) {
            console.error("Login error:", err);
            errorDiv.textContent = "Invalid email or password.";
            errorDiv.style.display = "block";
        } finally {
            loginBtn.textContent = "Login";
            loginBtn.disabled = false;
            loginBtn.style.opacity = "1";
        }
    });
}
