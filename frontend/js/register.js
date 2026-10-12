import { auth } from './auth.js';

export function initRegister() {
    // Hide sidebar and header
    const appLayout = document.querySelector('.app-layout');
    if (appLayout) appLayout.classList.add('auth-mode');
    const sidebar = document.querySelector('.app-sidebar');
    if (sidebar) sidebar.style.display = 'none';
    const topHeader = document.querySelector('.top-header');
    if (topHeader) topHeader.style.display = 'none';

    const registerForm = document.getElementById('register-form');
    const errorDiv = document.getElementById('register-error');
    const registerBtn = document.getElementById('register-btn');

    if (!registerForm) return;

    // Reset error div style
    if (errorDiv) {
        errorDiv.style.display = "none";
        errorDiv.style.backgroundColor = "#fef2f2";
        errorDiv.style.borderColor = "#fecaca";
        errorDiv.style.color = "#dc2626";
    }

    // Password visibility toggles
    document.querySelectorAll('.toggle-reg-pwd').forEach(icon => {
        icon.addEventListener('click', (e) => {
            const targetId = e.target.getAttribute('data-target');
            const pwdInput = document.getElementById(targetId);
            if (pwdInput) {
                const type = pwdInput.getAttribute('type') === 'password' ? 'text' : 'password';
                pwdInput.setAttribute('type', type);
                e.target.className = type === 'password' ? 'fas fa-eye toggle-reg-pwd' : 'fas fa-eye-slash toggle-reg-pwd';
            }
        });
    });

    registerForm.addEventListener('submit', async function (event) {
        event.preventDefault();

        console.log("[REGISTER] Submit triggered");

        const fullName = document.getElementById('reg-name')?.value.trim();
        const email = document.getElementById('reg-email')?.value.trim();
        const password = document.getElementById('reg-password')?.value;
        const confirmPassword = document.getElementById('reg-confirm')?.value;
        const phone = document.getElementById('reg-phone')?.value.trim();
        const location = document.getElementById('reg-location')?.value.trim();
        const farmType = document.getElementById('reg-farm')?.value;
        const landHolding = document.getElementById('reg-land')?.value.trim();
        const preferredLanguage = document.getElementById('reg-lang')?.value || 'en';

        if (!fullName || !email || !password || !confirmPassword || !location || !farmType || !landHolding) {
            showError("Please fill all required fields.");
            return;
        }

        const parsedLand = parseFloat(landHolding);
        if (isNaN(parsedLand) || parsedLand <= 0) {
            showError("Please enter a valid farm land area in acres (e.g. 5.0).");
            return;
        }

        if (password.length < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }

        if (password !== confirmPassword) {
            showError("Passwords do not match.");
            return;
        }

        try {
            if (errorDiv) errorDiv.style.display = "none";
            registerBtn.textContent = "Creating Account...";
            registerBtn.disabled = true;

            const firstName = fullName.split(' ')[0];
            const lastName = fullName.split(' ').slice(1).join(' ');

            const userData = {
                firstName,
                lastName,
                fullName,
                email,
                password,
                phone,
                location,
                farmType,
                landHolding,
                preferredLanguage
            };

            const result = await auth.registerFarmer(userData);

            if (result && result.confirmationRequired) {
                // Display confirmation pending state
                console.log("[REGISTER] Email confirmation pending for:", email);
                if (errorDiv) {
                    errorDiv.style.display = "block";
                    errorDiv.style.backgroundColor = "#ecfdf5";
                    errorDiv.style.borderColor = "#6ee7b7";
                    errorDiv.style.color = "#065f46";
                    errorDiv.innerHTML = `
                        <i class="fas fa-envelope-circle-check" style="font-size: 20px; margin-bottom: 6px; display: block;"></i>
                        <strong>Verification Email Sent!</strong><br>
                        We have sent a confirmation link to <strong>${escapeHtml(email)}</strong>.<br>
                        Please check your inbox and verify your email before logging in.
                        <div style="margin-top: 12px;">
                            <a href="#login" class="auth-btn" style="text-decoration: none; display: inline-block; padding: 8px 16px; font-size: 13px;">Go to Login</a>
                        </div>
                    `;
                }
                registerBtn.style.display = "none";
            } else {
                console.log("[REGISTER] Registration complete and authenticated");
                registerBtn.textContent = "Success!";
                setTimeout(() => {
                    window.location.hash = 'home';
                }, 1000);
            }

        } catch (error) {
            console.error("[REGISTER] Error:", error);
            showError(error.message || "Registration failed. Please try again.");
            registerBtn.disabled = false;
            registerBtn.textContent = "Create Farmer Account";
        }
    });

    function showError(msg) {
        if (!errorDiv) return;
        errorDiv.textContent = msg;
        errorDiv.style.display = "block";
        errorDiv.style.backgroundColor = "#fef2f2";
        errorDiv.style.borderColor = "#fecaca";
        errorDiv.style.color = "#dc2626";
    }

    function escapeHtml(str) {
        return String(str || "").replace(/[&<>"']/g, m => ({
            '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
        })[m]);
    }
}
