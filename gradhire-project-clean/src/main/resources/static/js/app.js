// GradHire - Shared Utilities

function getToken() { return sessionStorage.getItem('token'); }
function getUser() { try { return JSON.parse(sessionStorage.getItem('user')); } catch(e) { return null; } }
function authHeaders() { return { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + getToken() }; }

function logout() {
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('user');
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

window.viewPlacementFullDetail = function(p) {
    let modal = document.getElementById('globalPlacementDetailModal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'globalPlacementDetailModal';
        modal.className = 'modal-backdrop hidden';
        modal.style.zIndex = '9999';
        modal.onclick = function(e) { if(e.target===this) this.classList.add('hidden'); };
        modal.innerHTML = `
        <div class="modal p-6 dark:bg-gray-800 max-w-xl w-full">
            <div class="flex justify-between items-center mb-4 border-b border-gray-100 dark:border-gray-700 pb-3">
                <h3 class="font-semibold dark:text-white text-lg"><i class="fas fa-briefcase text-blue-500 mr-2"></i>Placement Details</h3>
                <button onclick="document.getElementById('globalPlacementDetailModal').classList.add('hidden')" class="text-gray-400 hover:text-gray-600 text-xl leading-none">&times;</button>
            </div>
            <div class="space-y-4 max-h-[70vh] overflow-y-auto pr-1 text-sm text-gray-700 dark:text-gray-300" id="globalPlacementDetailContent">
            </div>
        </div>`;
        document.body.appendChild(modal);
    }
    
    document.getElementById('globalPlacementDetailContent').innerHTML = `
        <div class="grid grid-cols-2 gap-4">
            <div><span class="text-xs text-gray-500 dark:text-gray-400 font-semibold block mb-1">STUDENT NAME</span><span class="font-medium dark:text-white">${p.studentName || '—'}</span></div>
            <div><span class="text-xs text-gray-500 dark:text-gray-400 font-semibold block mb-1">REGISTER NO</span><span class="font-medium dark:text-white">${p.registerNumber || '—'}</span></div>
            <div><span class="text-xs text-gray-500 dark:text-gray-400 font-semibold block mb-1">COMPANY</span><span class="font-medium dark:text-white">${p.companyName || '—'}</span></div>
            <div><span class="text-xs text-gray-500 dark:text-gray-400 font-semibold block mb-1">JOB ROLE</span><span class="font-medium dark:text-white">${p.jobRole || '—'}</span></div>
            <div><span class="text-xs text-gray-500 dark:text-gray-400 font-semibold block mb-1">PACKAGE (LPA)</span><span class="font-medium dark:text-white">${p.packageOffered || '—'}</span></div>
            <div><span class="text-xs text-gray-500 dark:text-gray-400 font-semibold block mb-1">STATUS</span><span class="badge badge-${(p.overallStatus||'').toLowerCase()}">${p.overallStatus || '—'}</span></div>
            <div><span class="text-xs text-gray-500 dark:text-gray-400 font-semibold block mb-1">LOCATION</span><span class="font-medium dark:text-white">${p.jobLocation || '—'}</span></div>
            <div><span class="text-xs text-gray-500 dark:text-gray-400 font-semibold block mb-1">DEPARTMENT</span><span class="font-medium dark:text-white">${p.department || '—'}</span></div>
            <div><span class="text-xs text-gray-500 dark:text-gray-400 font-semibold block mb-1">APP DATE</span><span class="font-medium dark:text-white">${p.applicationDate || '—'}</span></div>
            <div><span class="text-xs text-gray-500 dark:text-gray-400 font-semibold block mb-1">INT DATE</span><span class="font-medium dark:text-white">${p.interviewDate || '—'}</span></div>
            <div><span class="text-xs text-gray-500 dark:text-gray-400 font-semibold block mb-1">OFFER DATE</span><span class="font-medium dark:text-white">${p.offerDate || '—'}</span></div>
        </div>
        <div class="mt-4 pt-4 border-t border-gray-100 dark:border-gray-700">
            <div class="mb-3"><span class="text-xs text-gray-500 dark:text-gray-400 font-semibold block mb-1">NOTES</span><div class="bg-gray-50 dark:bg-gray-700 p-3 rounded-lg min-h-[50px] whitespace-pre-wrap">${p.notes || '—'}</div></div>
            <div><span class="text-xs text-gray-500 dark:text-gray-400 font-semibold block mb-1">REMARKS</span><div class="bg-gray-50 dark:bg-gray-700 p-3 rounded-lg min-h-[50px] whitespace-pre-wrap">${p.remarks || '—'}</div></div>
        </div>
    `;
    modal.classList.remove('hidden');
};

window.viewDriveFullDetail = function(dStr, role) {
    let d = typeof dStr === 'string' ? JSON.parse(dStr) : dStr;
    let modal = document.getElementById('globalDriveDetailModal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'globalDriveDetailModal';
        modal.className = 'modal-backdrop hidden';
        modal.style.zIndex = '9999';
        modal.onclick = function(e) { if(e.target===this) this.classList.add('hidden'); };
        document.body.appendChild(modal);
    }
    
    let actionButtons = '';
    if (role === 'STUDENT') {
        actionButtons = `<button onclick="applyDrive(${d.id}); document.getElementById('globalDriveDetailModal').classList.add('hidden')" class="btn-primary w-full justify-center mt-4"><i class="fas fa-paper-plane mr-2"></i>Apply for Drive</button>`;
    } else if (role === 'ADMIN' || role === 'STAFF') {
        actionButtons = `<button onclick='editDrive(${JSON.stringify(d).replace(/'/g, "&#39;")}); document.getElementById("globalDriveDetailModal").classList.add("hidden")' class="btn-secondary w-full justify-center mt-4"><i class="fas fa-edit mr-2"></i>Edit Drive Details</button>`;
    }
    
    let content = `
        <div class="modal p-6 dark:bg-gray-800 max-w-xl w-full">
            <div class="flex justify-between items-center mb-4 border-b border-gray-100 dark:border-gray-700 pb-3">
                <h3 class="font-semibold dark:text-white text-lg"><i class="fas fa-building text-blue-500 mr-2"></i>Company Details</h3>
                <button onclick="document.getElementById('globalDriveDetailModal').classList.add('hidden')" class="text-gray-400 hover:text-gray-600 text-xl leading-none">&times;</button>
            </div>
            <div class="space-y-4 max-h-[70vh] overflow-y-auto pr-1 text-sm text-gray-700 dark:text-gray-300">
                <div class="grid grid-cols-2 gap-4">
                    <div><span class="text-xs text-gray-500 dark:text-gray-400 font-semibold block mb-1">COMPANY NAME</span><span class="font-medium dark:text-white text-base">${d.companyName || '—'}</span></div>
                    <div><span class="text-xs text-gray-500 dark:text-gray-400 font-semibold block mb-1">JOB ROLE</span><span class="font-medium dark:text-white">${d.jobRole || '—'}</span></div>
                    <div><span class="text-xs text-gray-500 dark:text-gray-400 font-semibold block mb-1">PACKAGE (LPA)</span><span class="font-medium dark:text-white">${d.packageOffered || '—'}</span></div>
                    <div><span class="text-xs text-gray-500 dark:text-gray-400 font-semibold block mb-1">MIN CGPA</span><span class="font-medium dark:text-white">${d.minCgpa || '—'}</span></div>
                    <div><span class="text-xs text-gray-500 dark:text-gray-400 font-semibold block mb-1">ELIGIBLE BATCH</span><span class="font-medium dark:text-white">${d.batch || '—'}</span></div>
                    <div><span class="text-xs text-gray-500 dark:text-gray-400 font-semibold block mb-1">DRIVE DATE</span><span class="font-medium dark:text-white">${d.driveDate ? formatDate(d.driveDate) : '—'}</span></div>
                    <div><span class="text-xs text-gray-500 dark:text-gray-400 font-semibold block mb-1">DEADLINE</span><span class="font-medium text-red-500">${d.applicationDeadline ? formatDate(d.applicationDeadline) : '—'}</span></div>
                    <div><span class="text-xs text-gray-500 dark:text-gray-400 font-semibold block mb-1">STATUS</span><span class="badge ${d.status==='ONGOING'?'badge-selected':'badge-failed'}">${d.status || '—'}</span></div>
                </div>
                <div class="mt-4 pt-4 border-t border-gray-100 dark:border-gray-700">
                    <div class="mb-3"><span class="text-xs text-gray-500 dark:text-gray-400 font-semibold block mb-1">REQUIREMENTS / SKILLS</span><div class="bg-gray-50 dark:bg-gray-700 p-3 rounded-lg min-h-[50px] whitespace-pre-wrap">${d.skills || '—'}</div></div>
                </div>
                ${actionButtons}
            </div>
        </div>`;
    modal.innerHTML = content;
    modal.classList.remove('hidden');
};
