// GradHire - Shared Utilities

function getToken() { return localStorage.getItem('token'); }
function getUser() { try { return JSON.parse(localStorage.getItem('user')); } catch(e) { return null; } }
function authHeaders() { return { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + getToken() }; }

function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = '/index.html';
}

async function apiFetch(url, options = {}) {
    if (!options.headers) {
        options.headers = authHeaders();
    } else if (!options.headers['Authorization']) {
        options.headers['Authorization'] = 'Bearer ' + getToken();
    }
    try {
        const res = await fetch('/api' + url, options);
        if (res.status === 401) { logout(); return null; }
        if (res.status === 403) { showToast('Access denied', 'error'); return null; }
        const text = await res.text();
        try { return JSON.parse(text); } catch(e) { return null; }
    } catch(e) {
        showToast('Network error', 'error');
        return null;
    }
}

async function downloadAuthFile(url, fallbackName = 'download.pdf') {
    try {
        const res = await fetch(url, { headers: { 'Authorization': 'Bearer ' + getToken() } });
        if (!res.ok) { showToast('Download failed', 'error'); return; }
        let filename = fallbackName;
        const disp = res.headers.get('Content-Disposition');
        if (disp && disp.indexOf('attachment') !== -1) {
            const matches = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/.exec(disp);
            if (matches != null && matches[1]) filename = matches[1].replace(/['"]/g, '');
        }
        const blob = await res.blob();
        const blobUrl = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = blobUrl;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        a.remove();
        window.URL.revokeObjectURL(blobUrl);
    } catch(e) {
        showToast('Download error', 'error');
    }
}

function showToast(msg, type = 'info') {
    let t = document.getElementById('toast');
    if (!t) { t = document.createElement('div'); t.id = 'toast'; document.body.appendChild(t); }
    t.textContent = msg;
    t.className = 'show ' + type;
    clearTimeout(t._timer);
    t._timer = setTimeout(() => { t.className = ''; }, 3500);
}

function toggleTheme() {
    document.documentElement.classList.toggle('dark');
    localStorage.setItem('theme', document.documentElement.classList.contains('dark') ? 'dark' : 'light');
}

function initTheme() {
    if (localStorage.getItem('theme') === 'dark') document.documentElement.classList.add('dark');
}

function checkAuth(requiredRole) {
    const token = getToken();
    const user = getUser();
    if (!token || !user) { window.location.href = '/index.html'; return false; }
    if (requiredRole && user.role !== requiredRole) {
        const r = user.role;
        if (r === 'ADMIN') window.location.href = '/pages/admin-dashboard.html';
        else if (r === 'STAFF') window.location.href = '/pages/staff-dashboard.html';
        else window.location.href = '/pages/student-dashboard.html';
        return false;
    }
    return true;
}

function renderUserInfo() {
    const user = getUser();
    if (!user) return;
    const el = document.getElementById('sidebarUser');
    if (el) {
        el.innerHTML = `<div class="px-4 py-3 border-t border-white/10 mt-2">
            <div class="flex items-center gap-3">
                <div class="w-9 h-9 rounded-full bg-blue-400/30 flex items-center justify-center text-white font-bold text-sm flex-shrink-0">
                    ${user.fullName.charAt(0).toUpperCase()}
                </div>
                <div class="overflow-hidden">
                    <p class="text-white text-sm font-medium truncate">${user.fullName}</p>
                    <p class="text-blue-300 text-xs">${user.role.replace('ROLE_', '')}</p>
                </div>
            </div>
        </div>`;
    }
}

function toggleSidebar() { document.querySelector('.sidebar')?.classList.toggle('open'); }

function formatDate(str) {
    if (!str) return 'N/A';
    return new Date(str).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
}

function debounce(fn, ms) {
    let t;
    return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), ms); };
}

document.addEventListener('DOMContentLoaded', () => { initTheme(); renderUserInfo(); });
