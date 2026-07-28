let adminToken = null;

async function adminLogin() {
    const username = document.getElementById('admin-login-username').value.trim();
    const password = document.getElementById('admin-login-password').value.trim();
    const statusEl = document.getElementById('admin-login-status');

    if (!username || !password) {
        statusEl.innerText = 'Enter both fields.';
        return;
    }

    try {
        const res = await fetch('/api/admin/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        if (!res.ok) {
            statusEl.innerText = 'Invalid credentials or not an admin account.';
            return;
        }

        const data = await res.json();
        adminToken = data.token;

        document.getElementById('admin-login-overlay').classList.add('hidden');
        loadGames();
        refreshClients();
        loadConfig();
        loadLogs();
    } catch (err) {
        statusEl.innerText = 'Could not reach server.';
    }
}
