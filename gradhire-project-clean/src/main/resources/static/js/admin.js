// GradHire Admin Dashboard JS
let charts = {};
let allStudents = [];

document.addEventListener('DOMContentLoaded', () => {
    if (!checkAuth('ADMIN')) return;
    document.getElementById('headerUser').textContent = getUser().fullName;
    loadDashboard();
    loadUnreadCount();
    loadBatchFilter();
    loadDeptFilter();
});

function showSection(name) {
    document.querySelectorAll('[id^="section-"]').forEach(s => s.classList.add('hidden'));
    const sec = document.getElementById('section-' + name);
    if (sec) { sec.classList.remove('hidden'); sec.classList.add('fade-in'); }
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));

    const titles = {
        dashboard: 'Dashboard', students: 'Students', placements: 'Placements', drives: 'Upcoming Companies',
        users: 'User Management', resume: 'Resume', upload: 'Excel Upload',
        notifications: 'Notifications', errorReports: 'Error Reports', uploadHistory: 'Upload History'
    };
    document.getElementById('pageTitle').textContent = titles[name] || name;

    if (name === 'students') loadStudents();
    else if (name === 'placements') loadPlacements();
    else if (name === 'drives') loadDrives();
    else if (name === 'users') loadUsers();
    else if (name === 'notifications') loadNotifications();
    else if (name === 'errorReports') loadErrorReports();
    else if (name === 'dashboard') loadDashboard();
    else if (name === 'uploadHistory') loadUploadHistory();
    else if (name === 'resume') loadResumeHistory();
}

async function loadDashboard() {
    const data = await apiFetch('/admin/dashboard/stats');
    if (!data?.success) return;
    const a = data.data;
    document.getElementById('s1').textContent = a.totalStudents || 0;
    document.getElementById('s2').textContent = Object.keys(a.companyWiseCount || {}).length || 0;
    document.getElementById('s3').textContent = a.selectedCount || 0;
    document.getElementById('s4').textContent = (a.placementPercentage || 0).toFixed(1) + '%';

    // 1. Placement Status
    const studentStatus = a.studentPlacementStatusCount || {};
    const statusColors = { 
        'Selected': '#10b981', 
        'Rejected': '#dc2626', 
        'Applied': '#3b82f6', 
        'Waiting': '#f59e0b', 
        'Pending': '#fcd34d', 
        'Unselected': '#94a3b8' 
    };
    const statusLabels = Object.keys(studentStatus).filter(k => k && k !== 'null' && k !== 'undefined' && studentStatus[k] > 0);
    if (statusLabels.length > 0) {
        const statusVals = statusLabels.map(k => studentStatus[k]);
        const statusCols = statusLabels.map(k => statusColors[k] || '#6b7280');
        makeChart('statusChart', 'doughnut', null, statusCols, statusLabels, [{ data: statusVals, backgroundColor: statusCols, borderWidth: 2, borderColor: '#fff' }]);
    } else clearChart('statusChart');

    // 2. Batch-wise Students — stacked bar showing Total vs Selected per year
    const batchTotal = a.batchWiseTotalCount || {};
    const batchSelected = a.batchWiseCount || {};
    const batchDetailed = a.batchWiseDetailedCount || {};
    const batchLabels = Object.keys(batchTotal).filter(k => k && k !== 'null' && k !== 'undefined');
    if (batchLabels.length > 0) {
        makeBatchChart('batchChart', batchLabels, batchTotal, batchSelected, batchDetailed);
    } else clearChart('batchChart');

    // 3. Department Distribution (keep same logic)
    const deptData = a.departmentTotalCount || a.departmentWiseCount || {};
    if (Object.keys(deptData).length > 0) {
        const deptLabels = Object.keys(deptData).filter(k => k && k !== 'null' && k !== 'undefined');
        const deptVals = deptLabels.map(k => deptData[k]);
        const getCode = (v) => v >= 50 ? '#10b981' : (v >= 20 ? '#eab308' : '#3b82f6');
        makeChart('deptChart', 'bar', null, deptVals.map(getCode), deptLabels, [{ data: deptVals, backgroundColor: deptVals.map(getCode), borderRadius: 5 }]);
    } else clearChart('deptChart');

    // 4. Year-wise Trends
    const trends = a.yearWiseTrends || [];
    if (trends.length > 0) {
        const tLabels = trends.map(t => String(t.year || ''));
        // Selection Rate line chart
        makeTrendChart('trendSelectionChart', tLabels, trends.map(t => t.selectionRate || 0), 'Selection Rate (%)', '#10b981', '#d1fae5');
        // Avg CGPA line chart
        makeTrendChart('trendCgpaChart', tLabels, trends.map(t => t.avgCgpa || 0), 'Avg CGPA', '#3b82f6', '#dbeafe');
    } else {
        clearChart('trendSelectionChart');
        clearChart('trendCgpaChart');
    }
}

function clearChart(id) {
    if (charts[id]) charts[id].destroy();
    const ctx = document.getElementById(id)?.getContext('2d');
    if (ctx) ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
}

function makeChart(id, type, dataMap, colors, labels, datasets) {
    const canvas = document.getElementById(id);
    if (!canvas) return;
    if (charts[id]) charts[id].destroy();
    // Support both old (dataMap) and new (labels+datasets) signatures
    if (!labels && dataMap) {
        labels = Object.keys(dataMap).filter(k => k && k !== 'null' && k !== 'undefined');
        const values = labels.map(k => dataMap[k]);
        if (labels.length === 0) return;
        datasets = [{ data: values, backgroundColor: labels.map((_, i) => colors[i % colors.length]), borderRadius: type === 'bar' ? 5 : 0, borderWidth: type === 'doughnut' ? 2 : 0, borderColor: '#fff' }];
    }
    if (!labels || labels.length === 0) return;
    charts[id] = new Chart(canvas, {
        type,
        data: { labels, datasets },
        options: { responsive: true, plugins: { legend: { position: type === 'doughnut' ? 'bottom' : 'top', labels: { boxWidth: 12, font: { size: 11 } } } }, scales: type === 'bar' ? { y: { beginAtZero: true, ticks: { stepSize: 1 } } } : {} }
    });
}

function makeBatchChart(id, labels, totalData, selectedData, detailedData) {
    const canvas = document.getElementById(id);
    if (!canvas) return;
    if (charts[id]) charts[id].destroy();
    charts[id] = new Chart(canvas, {
        type: 'bar',
        data: {
            labels,
            datasets: [
                { label: 'Total Students', data: labels.map(k => totalData[k] || 0), backgroundColor: '#93c5fd', borderRadius: 5 },
                { label: 'Selected', data: labels.map(k => selectedData[k] || 0), backgroundColor: '#10b981', borderRadius: 5 }
            ]
        },
        options: {
            responsive: true,
            plugins: { 
                legend: { position: 'top', labels: { boxWidth: 12, font: { size: 11 } } },
                tooltip: {
                    callbacks: {
                        afterBody: function(context) {
                            if (!detailedData) return '';
                            let labelStr = context[0].label;
                            let details = detailedData[labelStr];
                            if (!details) return '';
                            return [
                                'Selected: ' + (details.Selected || 0),
                                'Pending: ' + (details.Pending || 0),
                                'Applied: ' + (details.Applied || 0),
                                'Rejected: ' + (details.Rejected || 0),
                                'Waiting: ' + (details.Waiting || 0),
                                'Unselected: ' + (details.Unselected || 0)
                            ];
                        }
                    }
                }
            },
            scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } }
        }
    });
}

function makeTrendChart(id, labels, data, label, lineColor, fillColor) {
    const canvas = document.getElementById(id);
    if (!canvas) return;
    if (charts[id]) charts[id].destroy();
    charts[id] = new Chart(canvas, {
        type: 'line',
        data: {
            labels,
            datasets: [{
                label,
                data,
                borderColor: lineColor,
                backgroundColor: fillColor,
                fill: true,
                tension: 0.3,
                pointBackgroundColor: lineColor,
                pointRadius: 5,
                pointHoverRadius: 7,
                borderWidth: 2.5
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { display: false } },
            scales: {
                y: { beginAtZero: true, ticks: { font: { size: 11 } } },
                x: { ticks: { font: { size: 11 } } }
            }
        }
    });
}


async function loadStudents() {
    const data = await apiFetch('/student');
    if (!data?.success) {
        document.getElementById('stuBody').innerHTML = '<tr><td colspan="7" class="text-center py-10 text-red-500">Failed to load students</td></tr>';
        return;
    }
    allStudents = data.data;
    renderStudents(allStudents);
}

function renderStudents(list) {
    const tbody = document.getElementById('stuBody');
    if (!list.length) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center py-10 text-gray-400">No students found</td></tr>';
        return;
    }
    tbody.innerHTML = list.map(s => `
        <tr ondblclick="viewStudentDetail(${s.id})" style="cursor:pointer;">
          <td class="font-mono text-xs text-blue-700 dark:text-blue-400 font-semibold">${s.registerNumber || 'NULL'}</td>
          <td><div class="font-medium dark:text-white text-sm"><span onclick="viewStudentDetail(${s.id});event.stopPropagation();" class="cursor-pointer hover:text-blue-600 hover:underline transition-colors">${s.fullName}</span></div><div class="text-xs text-gray-400">${s.email || 'NULL'}</div></td>
          <td class="text-sm text-gray-600 dark:text-gray-400">${s.department || 'NULL'}</td>
          <td><span class="font-semibold text-sm ${s.cgpa && s.cgpa >= 7 ? 'text-green-600' : (s.cgpa ? 'text-amber-600' : 'text-gray-400')}">${s.cgpa || 'NULL'}</span></td>
          <td class="text-sm">${s.batchYear || s.batch || 'NULL'}</td>
          <td class="text-sm">${s.placementStatus || 'NULL'}</td>
          <td>
            <div class="flex gap-1" onclick="event.stopPropagation()">
              <button onclick="openEditModal(${s.id})" class="btn-secondary btn-sm" title="Edit"><i class="fas fa-edit text-blue-500"></i></button>
              <button onclick="downloadAuthFile('/api/reports/student/${s.id}/pdf', 'resume.pdf')" class="btn-secondary btn-sm" title="Download Resume"><i class="fas fa-file-pdf text-red-500"></i></button>
              <button onclick="deleteStudent(${s.id})" class="btn-secondary btn-sm" title="Delete"><i class="fas fa-trash text-red-400"></i></button>
            </div>
          </td>
        </tr>`).join('');
}

const searchStudents = debounce(async (q) => {
    if (!q.trim()) { renderStudents(allStudents); return; }
    const data = await apiFetch('/student/search?q=' + encodeURIComponent(q));
    if (data?.success) renderStudents(data.data);
}, 300);

async function filterBatch() {
    const bid = document.getElementById('batchFilter').value;
    if (!bid) { renderStudents(allStudents); return; }
    const data = await apiFetch('/student/batch/' + bid);
    if (data?.success) renderStudents(data.data);
}

async function filterDept() {
    const dept = document.getElementById('deptFilter').value;
    if (!dept) { renderStudents(allStudents); return; }
    const data = await apiFetch('/student/department/' + encodeURIComponent(dept));
    if (data?.success) renderStudents(data.data);
}

async function loadBatchFilter() {
    const data = await apiFetch('/student/batches');
    if (!data?.success) return;
    const sel = document.getElementById('batchFilter');
    data.data.forEach(b => {
        const o = document.createElement('option');
        o.value = b.id; o.textContent = 'Batch ' + b.year;
        sel.appendChild(o);
    });
}

async function loadDeptFilter() {
    const data = await apiFetch('/student/departments');
    if (!data?.success) return;
    const sel = document.getElementById('deptFilter');
    if (!sel) return;
    data.data.forEach(d => {
        const o = document.createElement('option');
        o.value = d; o.textContent = d;
        sel.appendChild(o);
    });
}

async function deleteStudent(id) {
    if (!confirm('Delete this student permanently?')) return;
    const data = await apiFetch('/student/' + id, { method: 'DELETE', headers: authHeaders() });
    if (data?.success) { showToast('Student deleted', 'success'); loadStudents(); }
    else showToast(data?.message || 'Failed', 'error');
}

async function loadPlacements() {
    const data = await apiFetch('/placements');
    const tbody = document.getElementById('placBody');
    if (!data?.success) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center py-10 text-red-500">Failed to load placements</td></tr>';
        return;
    }
    if (!data.data.length) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center py-10 text-gray-400">No placements yet</td></tr>';
        return;
    }
    tbody.innerHTML = data.data.map(p => `
        <tr ondblclick='viewPlacementFullDetail(${JSON.stringify(p).replace(/'/g, "&#39;")})' class="cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-800/50">
          <td><div class="font-medium dark:text-white text-sm">${p.studentName}</div><div class="text-xs text-blue-500 cursor-pointer" onclick="viewPlacementHistory(${p.studentId}, '${(p.studentName||'').replace(/'/g,"\\'")}', '${(p.registerNumber||'').replace(/'/g,"\\'")}')"><i class="fas fa-history mr-1"></i>${p.registerNumber}</div></td>
          <td class="font-medium text-sm dark:text-white">${p.companyName}</td>
          <td class="text-sm text-gray-600 dark:text-gray-400">${p.jobRole || '—'}</td>
          <td class="text-sm">${p.packageOffered ? p.packageOffered + ' LPA' : '—'}</td>
          <td><span class="badge badge-${(p.overallStatus||'').toLowerCase()}">${p.overallStatus}</span></td>
          <td class="text-sm text-gray-600 dark:text-gray-400">${p.department || '—'}</td>
          <td>
            <div class="flex gap-1">
              <button onclick='editPlacement(${JSON.stringify(p).replace(/'/g,"&#39;")})' class="btn-secondary btn-sm" title="Edit"><i class="fas fa-edit text-blue-500"></i></button>
              <button onclick="deletePlacement(${p.id})" class="btn-secondary btn-sm" title="Delete"><i class="fas fa-trash text-red-400"></i></button>
            </div>
          </td>
        </tr>`).join('');
}

const searchPlacements = debounce(async (q) => {
    if (!q.trim()) { loadPlacements(); return; }
    const data = await apiFetch('/placements/search?q=' + encodeURIComponent(q));
    if (!data?.success) return;
    const tbody = document.getElementById('placBody');
    if (!data.data.length) { tbody.innerHTML = '<tr><td colspan="7" class="text-center py-10 text-gray-400">No results</td></tr>'; return; }
    tbody.innerHTML = data.data.map(p => `
        <tr ondblclick='viewPlacementFullDetail(${JSON.stringify(p).replace(/'/g, "&#39;")})' class="cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-800/50">
          <td><div class="font-medium dark:text-white text-sm">${p.studentName}</div><div class="text-xs text-blue-500 cursor-pointer" onclick="viewPlacementHistory(${p.studentId}, '${(p.studentName||'').replace(/'/g,"\\'")}', '${(p.registerNumber||'').replace(/'/g,"\\'")}')"><i class="fas fa-history mr-1"></i>${p.registerNumber}</div></td>
          <td class="font-medium text-sm dark:text-white">${p.companyName}</td>
          <td class="text-sm text-gray-600 dark:text-gray-400">${p.jobRole || '—'}</td>
          <td class="text-sm">${p.packageOffered ? p.packageOffered + ' LPA' : '—'}</td>
          <td><span class="badge badge-${(p.overallStatus||'').toLowerCase()}">${p.overallStatus}</span></td>
          <td class="text-sm text-gray-600 dark:text-gray-400">${p.department || '—'}</td>
          <td>
            <div class="flex gap-1">
              <button onclick='editPlacement(${JSON.stringify(p).replace(/'/g,"&#39;")})' class="btn-secondary btn-sm"><i class="fas fa-edit text-blue-500"></i></button>
              <button onclick="deletePlacement(${p.id})" class="btn-secondary btn-sm"><i class="fas fa-trash text-red-400"></i></button>
            </div>
          </td>
        </tr>`).join('');
}, 300);

async function deletePlacement(id) {
    if (!confirm('Delete this placement?')) return;
    const data = await apiFetch('/placements/' + id, { method: 'DELETE', headers: authHeaders() });
    if (data?.success) { showToast('Placement deleted', 'success'); loadPlacements(); }
    else showToast(data?.message || 'Failed', 'error');
}

function openPlacementModal() {
    document.getElementById('placEditTitle').textContent = 'Add Placement';
    document.getElementById('peId').value = '';
    ['peStudentId','peCompany','peRole','peLocation','pePackage','peAppDate','peIntDate','peOffDate','peNotes','peRemarks'].forEach(id => document.getElementById(id).value = '');
    document.getElementById('peStatus').value = 'PENDING';
    document.getElementById('peStudentId').disabled = false;
    document.getElementById('placementEditModal').classList.remove('hidden');
}

function editPlacement(p) {
    document.getElementById('placEditTitle').textContent = 'Edit Placement';
    document.getElementById('peId').value = p.id;
    document.getElementById('peStudentId').value = p.studentId;
    document.getElementById('peStudentId').disabled = true;
    document.getElementById('peCompany').value = p.companyName || '';
    document.getElementById('peRole').value = p.jobRole || '';
    document.getElementById('peLocation').value = p.jobLocation || '';
    document.getElementById('pePackage').value = p.packageOffered || '';
    document.getElementById('peStatus').value = p.overallStatus || 'PENDING';
    document.getElementById('peAppDate').value = p.applicationDate || '';
    document.getElementById('peIntDate').value = p.interviewDate || '';
    document.getElementById('peOffDate').value = p.offerDate || '';
    document.getElementById('peNotes').value = p.notes || '';
    document.getElementById('peRemarks').value = p.remarks || '';
    document.getElementById('placementEditModal').classList.remove('hidden');
}

async function savePlacementEdit() {
    const peId = document.getElementById('peId').value;
    const studentId = parseInt(document.getElementById('peStudentId').value);
    const companyName = document.getElementById('peCompany').value.trim();
    
    if (!companyName) { showToast('Company name is required', 'error'); return; }
    if (!peId && !studentId) { showToast('Student ID is required', 'error'); return; }
    
    const body = {
        studentId: studentId || null,
        companyName: companyName,
        jobRole: document.getElementById('peRole').value.trim() || null,
        jobLocation: document.getElementById('peLocation').value.trim() || null,
        packageOffered: parseFloat(document.getElementById('pePackage').value) || null,
        overallStatus: document.getElementById('peStatus').value,
        applicationDate: document.getElementById('peAppDate').value || null,
        interviewDate: document.getElementById('peIntDate').value || null,
        offerDate: document.getElementById('peOffDate').value || null,
        notes: document.getElementById('peNotes').value.trim() || null,
        remarks: document.getElementById('peRemarks').value.trim() || null
    };
    
    let res;
    if (peId) {
        res = await apiFetch('/placements/' + peId, { method: 'PUT', headers: authHeaders(), body: JSON.stringify(body) });
    } else {
        res = await apiFetch('/placements', { method: 'POST', headers: authHeaders(), body: JSON.stringify(body) });
    }
    
    if (res?.success) {
        showToast(peId ? 'Placement updated' : 'Placement created', 'success');
        document.getElementById('placementEditModal').classList.add('hidden');
        loadPlacements();
    } else {
        showToast(res?.message || 'Failed to save placement', 'error');
    }
}

async function loadUsers() {
    const data = await apiFetch('/admin/users');
    if (!data?.success) {
        document.getElementById('usrBody').innerHTML = '<tr><td colspan="6" class="text-center py-10 text-red-500">Failed to load users</td></tr>';
        return;
    }
    renderUsers(data.data);
}

function renderUsers(list) {
    const tbody = document.getElementById('usrBody');
    tbody.innerHTML = list.map(u => `
        <tr>
          <td class="font-medium dark:text-white text-sm">${u.fullName}</td>
          <td class="text-sm"><div>${u.username}</div><div class="text-xs text-gray-400">${u.email || '—'}</div></td>
          <td><span class="badge badge-${u.role.toLowerCase()}">${u.role}</span></td>
          <td><span class="badge ${u.enabled ? 'badge-selected' : 'badge-failed'}">${u.enabled ? 'Active' : 'Disabled'}</span></td>
          <td class="text-xs text-gray-400">${u.forcePasswordChange ? '⚠️ Must reset' : (u.passwordChanged ? '✓ Changed' : '⏳ Default')}</td>
          <td>
            <div class="flex gap-1">
              <button onclick="toggleUser(${u.id})" class="btn-secondary btn-sm" title="${u.enabled?'Disable':'Enable'}">
                <i class="fas fa-${u.enabled ? 'ban text-red-500' : 'check text-green-500'}"></i>
              </button>
              <button onclick="resetPwd(${u.id})" class="btn-secondary btn-sm" title="Reset Password">
                <i class="fas fa-key text-amber-500"></i>
              </button>
            </div>
          </td>
        </tr>`).join('');
}

const searchUsers = debounce(async (q) => {
    if (!q.trim()) { loadUsers(); return; }
    const data = await apiFetch('/admin/users/search?q=' + encodeURIComponent(q));
    if (data?.success) renderUsers(data.data);
}, 300);

async function loadNotifications() {
    const data = await apiFetch('/admin/notifications');
    if (!data?.success) return;
    const div = document.getElementById('notifList');
    if (!data.data || !data.data.length) { div.innerHTML = '<p class="text-gray-400 text-center py-6">No notifications</p>'; return; }

    const formatNotifDate = (iso) => {
        if (!iso) return '';
        const d = new Date(iso);
        const day = String(d.getDate()).padStart(2, '0');
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const year = d.getFullYear();
        let h = d.getHours();
        const m = String(d.getMinutes()).padStart(2, '0');
        const ampm = h >= 12 ? 'PM' : 'AM';
        h = h % 12; h = h ? h : 12;
        const hr = String(h).padStart(2, '0');
        return `${day}-${month}-${year} ${hr}:${m} ${ampm}`;
    };

    div.innerHTML = data.data.map(n => {
        let msg = n.message || 'Notification';
        if (n.actionType === 'EXCEL_UPLOAD') {
            const roleStr = n.role === 'ADMIN' ? 'Admin' : (n.role ? n.role.charAt(0).toUpperCase() + n.role.slice(1).toLowerCase() : 'Staff');
            msg = `${roleStr} uploaded Excel on ${formatNotifDate(n.time)}`;
        } else if (n.actionType === 'RESUME_GENERATE') {
            const roleStr = n.role === 'ADMIN' ? 'Admin' : (n.role ? n.role.charAt(0).toUpperCase() + n.role.slice(1).toLowerCase() : 'Staff');
            msg = `${roleStr} generated Resumes on ${formatNotifDate(n.time)}`;
        }
        return `
        <div class="flex gap-3 p-3 rounded-xl mb-2 cursor-pointer ${n.readStatus ? 'bg-gray-50 dark:bg-gray-700/30' : 'bg-blue-50 dark:bg-blue-900/20 border border-blue-100 dark:border-blue-800'}"
             onclick="markRead(${n.id},this)">
          <div class="w-2 h-2 rounded-full mt-1.5 flex-shrink-0 ${n.readStatus ? 'bg-gray-300' : 'bg-blue-500'}"></div>
          <div class="flex-1 min-w-0">
            <p class="font-medium text-sm dark:text-white">${msg}</p>
            <p class="text-xs text-gray-500 dark:text-gray-400">By: ${n.createdBy||'System'}</p>
            <p class="text-xs text-gray-400 mt-0.5">${n.time ? new Date(n.time).toLocaleString('en-IN') : ''}</p>
          </div>
          <span class="text-xs px-2 py-0.5 rounded-full self-start bg-blue-100 text-blue-600">${n.role||'SYSTEM'}</span>
        </div>`;
    }).join('');

    // Remove badge and mark all read after opening notifications tab
    await apiFetch('/admin/notifications/read-all', { method: 'PATCH', headers: authHeaders() });
    const badge = document.getElementById('notifBadge');
    const dot = document.getElementById('notifDot');
    if (badge) badge.classList.add('hidden');
    if (dot) dot.classList.add('hidden');
}

async function markRead(id, el) {
    if (!el.classList.contains('bg-gray-50')) {
        await apiFetch('/admin/notifications/' + id + '/read', { method: 'PATCH', headers: authHeaders() });
        loadUnreadCount(); loadNotifications();
    }
}

async function loadUnreadCount() {
    const data = await apiFetch('/admin/notifications/unread-count');
    if (!data?.success) return;
    const c = data.data;
    const badge = document.getElementById('notifBadge');
    const dot = document.getElementById('notifDot');
    if (c > 0) {
        badge?.classList.remove('hidden'); if (badge) badge.textContent = c;
        dot?.classList.remove('hidden');
    } else {
        badge?.classList.add('hidden'); dot?.classList.add('hidden');
    }
}




async function loadUploadHistory() {
    const data = await apiFetch('/admin/upload-history');
    if (!data?.success) return;
    const div = document.getElementById('uploadHistList');
    if (!data.data.length) { div.innerHTML = '<p class="text-gray-400 text-center py-6">No upload history</p>'; return; }
    div.innerHTML = `<div class="table-wrapper"><table class="data-table">
      <thead><tr><th>File</th><th>Uploaded By</th><th>Date</th><th>Total</th><th>New</th><th>Updated</th><th>Failed</th><th>Status</th></tr></thead>
      <tbody>${data.data.map(h=>`
        <tr>
          <td class="text-sm font-medium dark:text-white">${h.fileName}</td>
          <td class="text-sm text-gray-600 dark:text-gray-400">${h.uploadedBy}</td>
          <td class="text-xs text-gray-400">${h.uploadDate?new Date(h.uploadDate).toLocaleString('en-IN'):''}</td>
          <td class="text-sm">${h.totalRecords}</td>
          <td class="text-sm text-green-600">${h.successCount}</td>
          <td class="text-sm text-blue-600">${h.updateCount}</td>
          <td class="text-sm text-red-600">${h.failedCount}</td>
          <td><span class="badge ${h.status==='SUCCESS'?'badge-selected':h.status==='PARTIAL'?'badge-pending':'badge-failed'}">${h.status}</span></td>
          <td>
            <div class="flex gap-1">
              <button onclick="editUpload(${h.id})" class="btn-secondary btn-sm" title="Edit Data"><i class="fas fa-edit text-blue-500"></i></button>
              <button onclick="deleteUpload(${h.id})" class="btn-secondary btn-sm" title="Delete"><i class="fas fa-trash text-red-400"></i></button>
            </div>
          </td>
        </tr>`).join('')}
      </tbody></table></div>`;
}

async function deleteUpload(id) {
    if (!confirm('Delete this upload history record?')) return;
    const data = await apiFetch('/admin/upload-history/' + id, { method: 'DELETE', headers: authHeaders() });
    if (data?.success) { showToast('Upload history deleted', 'success'); loadUploadHistory(); }
    else showToast(data?.message || 'Failed', 'error');
}

let currentUploadStudents = [];
async function editUpload(id) {
    const data = await apiFetch('/student/upload/' + id);
    if (!data?.success) return showToast('Failed to load students for this upload', 'error');
    currentUploadStudents = data.data;
    if (!currentUploadStudents.length) return showToast('No students found for this upload', 'error');
    
    const tbody = document.getElementById('excelEditBody');
    tbody.innerHTML = currentUploadStudents.map((s, idx) => `
        <tr data-idx="${idx}" data-id="${s.id}">
          <td class="text-xs font-mono">${s.registerNumber || 'N/A'}</td>
          <td><input type="text" class="form-input text-sm p-1" value="${s.fullName || ''}" data-field="fullName"/></td>
          <td><input type="email" class="form-input text-sm p-1" value="${s.email || ''}" data-field="email"/></td>
          <td><input type="text" class="form-input text-sm p-1" value="${s.department || ''}" data-field="department"/></td>
          <td><input type="number" step="0.1" class="form-input text-sm p-1 w-20" value="${s.cgpa || ''}" data-field="cgpa"/></td>
          <td><input type="number" class="form-input text-sm p-1 w-20" value="${s.batchYear || ''}" data-field="batchYear"/></td>
          <td>
            <select class="form-input text-sm p-1 w-28" data-field="placementStatus">
              <option value="" ${!s.placementStatus?'selected':''}>N/A</option>
              <option value="SELECTED" ${s.placementStatus==='SELECTED'?'selected':''}>Selected</option>
              <option value="PENDING" ${s.placementStatus==='PENDING'?'selected':''}>Pending</option>
              <option value="NOT_ATTENDED" ${s.placementStatus==='NOT_ATTENDED'?'selected':''}>Not Attended</option>
            </select>
          </td>
        </tr>
    `).join('');
    document.getElementById('excelEditModal').classList.remove('hidden');
}

async function saveExcelEdits() {
    const rows = document.querySelectorAll('#excelEditBody tr');
    const btn = document.querySelector('#excelEditModal .btn-primary');
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Saving...';
    btn.disabled = true;

    let successCount = 0, failCount = 0;

    for (const row of rows) {
        const id = row.getAttribute('data-id');
        if (!id) { failCount++; continue; }

        const cgpaVal = row.querySelector('[data-field="cgpa"]').value;
        const batchVal = row.querySelector('[data-field="batchYear"]').value;
        const body = {
            fullName: row.querySelector('[data-field="fullName"]').value.trim(),
            email: row.querySelector('[data-field="email"]').value.trim(),
            department: row.querySelector('[data-field="department"]').value.trim(),
            cgpa: cgpaVal ? parseFloat(cgpaVal) : null,
            batchYear: batchVal ? parseInt(batchVal) : null,
            placementStatus: row.querySelector('[data-field="placementStatus"]').value || null
        };

        try {
            const res = await apiFetch('/student/' + id, { method: 'PUT', headers: authHeaders(), body: JSON.stringify(body) });
            if (res?.success) {
                successCount++;
                row.style.backgroundColor = 'rgba(16, 185, 129, 0.1)';
            } else {
                failCount++;
                row.style.backgroundColor = 'rgba(239, 68, 68, 0.1)';
                console.error('Failed to update student ' + id + ':', res?.message);
            }
        } catch (e) {
            failCount++;
            row.style.backgroundColor = 'rgba(239, 68, 68, 0.1)';
            console.error('Error updating student ' + id + ':', e);
        }
    }
    
    btn.innerHTML = '<i class="fas fa-save"></i> Save All Changes';
    btn.disabled = false;

    if (failCount === 0) {
        showToast(`All ${successCount} students updated successfully`, 'success');
        document.getElementById('excelEditModal').classList.add('hidden');
    } else {
        showToast(`Updated ${successCount}, failed ${failCount}. Check highlighted rows.`, failCount > 0 && successCount > 0 ? 'info' : 'error');
    }
    loadStudents();
}

let editingStudentId = null;
function openEditModal(sid) {
    const s = allStudents.find(x => x.id === sid);
    if (!s) return;
    editingStudentId = sid;
    
    document.getElementById('uRole').value = 'STUDENT';
    document.getElementById('uRole').disabled = true;
    toggleStudentF();
    
    document.getElementById('uName').value = s.fullName || '';
    document.getElementById('uEmail').value = s.email || '';
    document.getElementById('uPhone').value = s.phone || '';
    document.getElementById('uDept').value = s.department || '';
    document.getElementById('uReg').value = s.registerNumber || '';
    document.getElementById('uReg').disabled = true;
    document.getElementById('uBatch').value = s.batchYear || '';
    
    // Add extra student fields for admin editing
    document.getElementById('uCgpa').value = s.cgpa || '';
    document.getElementById('u10th').value = s.tenthPercent || '';
    document.getElementById('u12th').value = s.twelfthPercent || '';
    document.getElementById('uSkills').value = s.skills || '';
    document.getElementById('uStatus').value = s.placementStatus || '';
    document.getElementById('uUg').value = s.ugPercentage || '';
    document.getElementById('uGap').value = s.careerGap || '';
    document.getElementById('uCb').value = s.currentBacklogs != null ? s.currentBacklogs : '';
    document.getElementById('uHb').value = s.historyOfBacklogs != null ? s.historyOfBacklogs : '';
    document.getElementById('uLang').value = s.languages || '';
    document.getElementById('uInt').value = Array.isArray(s.internships) ? s.internships.join(', ') : (s.internships || '');
    document.getElementById('uProj').value = Array.isArray(s.projects) ? s.projects.join(', ') : (s.projects || '');
    document.getElementById('uCert').value = Array.isArray(s.certifications) ? s.certifications.join(', ') : (s.certifications || '');
    document.getElementById('uLin').value = s.linkedinUrl || '';
    document.getElementById('uGit').value = s.githubUrl || '';
    document.getElementById('uDob').value = s.dob || '';
    
    document.getElementById('userModal').classList.remove('hidden');
}

// Modal helpers
function openUserModal(role) {
    editingStudentId = null;
    document.getElementById('uRole').disabled = false;
    document.getElementById('uReg').disabled = false;
    if (role) document.getElementById('uRole').value = role;
    toggleStudentF();
    document.getElementById('userModal').classList.remove('hidden');
    // Clear previous values
    ['uName','uEmail','uPhone','uDept','uReg','uBatch','uCgpa','u10th','u12th','uSkills','uStatus','uUg','uGap','uCb','uHb','uLang','uInt','uProj','uCert','uLin','uGit','uDob'].forEach(id => { const el = document.getElementById(id); if(el) el.value = ''; });
}
function openPlacementModal() { document.getElementById('placModal').classList.remove('hidden'); }

function toggleStudentF() {
    const r = document.getElementById('uRole').value;
    document.getElementById('stuFields').classList.toggle('hidden', r !== 'STUDENT');
}

async function createUser() {
    const body = {
        fullName: document.getElementById('uName').value.trim(),
        email: document.getElementById('uEmail').value.trim(),
        role: document.getElementById('uRole').value,
        phone: document.getElementById('uPhone').value.trim(),
        department: document.getElementById('uDept').value.trim(),
        registerNumber: document.getElementById('uReg').value.trim(),
        batchYear: document.getElementById('uBatch').value ? parseInt(document.getElementById('uBatch').value) : null,
        cgpa: document.getElementById('uCgpa') ? parseFloat(document.getElementById('uCgpa').value) || null : null,
        tenthPercent: document.getElementById('u10th') ? document.getElementById('u10th').value.trim() : null,
        twelfthPercent: document.getElementById('u12th') ? document.getElementById('u12th').value.trim() : null,
        skills: document.getElementById('uSkills') ? document.getElementById('uSkills').value.trim() : null,
        placementStatus: document.getElementById('uStatus') ? document.getElementById('uStatus').value : null,
        ugPercentage: document.getElementById('uUg') ? document.getElementById('uUg').value.trim() : null,
        careerGap: document.getElementById('uGap') ? document.getElementById('uGap').value.trim() : null,
        currentBacklogs: document.getElementById('uCb') && document.getElementById('uCb').value ? parseInt(document.getElementById('uCb').value) : 0,
        historyOfBacklogs: document.getElementById('uHb') && document.getElementById('uHb').value ? parseInt(document.getElementById('uHb').value) : 0,
        languages: document.getElementById('uLang') ? document.getElementById('uLang').value.trim() : null,
        internships: document.getElementById('uInt') ? document.getElementById('uInt').value.trim() : null,
        projects: document.getElementById('uProj') ? document.getElementById('uProj').value.trim() : null,
        certifications: document.getElementById('uCert') ? document.getElementById('uCert').value.trim() : null,
        linkedinUrl: document.getElementById('uLin') ? document.getElementById('uLin').value.trim() : null,
        githubUrl: document.getElementById('uGit') ? document.getElementById('uGit').value.trim() : null,
        dob: document.getElementById('uDob') ? document.getElementById('uDob').value : null
    };
    if (!body.fullName) { showToast('Full name is required', 'error'); return; }
    if (body.role === 'STUDENT' && !body.registerNumber) { showToast('Register number is required', 'error'); return; }
    if (editingStudentId) {
        // Edit mode (only updates some user fields via AdminController /users/:id or /student/:id)
        // The easiest is to use the user update API if implemented, or student update route.
        const res = await apiFetch('/student/' + editingStudentId, { method:'PUT', headers:authHeaders(), body:JSON.stringify(body) });
        if (res?.success) {
            showToast('Student updated', 'success');
            document.getElementById('userModal').classList.add('hidden');
            editingStudentId = null;
            document.getElementById('uRole').disabled = false;
            document.getElementById('uReg').disabled = false;
            ['uName','uEmail','uPhone','uDept','uReg','uBatch','uCgpa','u10th','u12th','uSkills','uStatus','uUg','uGap','uCb','uHb','uLang','uInt','uProj','uCert','uLin','uGit','uDob'].forEach(id => { const el = document.getElementById(id); if(el) el.value=''; });
            loadStudents();
        } else {
            showToast(res?.message || 'Failed to update', 'error');
        }
        return;
    }

    const data = await apiFetch('/admin/users', { method:'POST', headers:authHeaders(), body:JSON.stringify(body) });
    if (data?.success) {
        showToast('User created! Password: Admin@1234', 'success');
        document.getElementById('userModal').classList.add('hidden');
        document.getElementById('uRole').disabled = false;
        document.getElementById('uReg').disabled = false;
        ['uName','uEmail','uPhone','uDept','uReg','uBatch','uCgpa','u10th','u12th','uSkills','uStatus','uUg','uGap','uCb','uHb','uLang','uInt','uProj','uCert','uLin','uGit','uDob'].forEach(id => { const el = document.getElementById(id); if(el) el.value=''; });
        loadStudents(); loadUsers();
    } else {
        showToast(data?.message || 'Failed to create user', 'error');
    }
}

async function createPlacement() {
    const dateVal = document.getElementById('pDate').value;
    let applicationDate = null;
    if (dateVal) {
        const parsed = new Date(dateVal);
        if (isNaN(parsed.getTime()) || parsed.getFullYear() < 1900 || parsed.getFullYear() > 2100) {
            showToast('Invalid application date. Use a valid date (YYYY-MM-DD)', 'error');
            return;
        }
        applicationDate = parsed.toISOString().split('T')[0];
    }
    const body = {
        studentId: parseInt(document.getElementById('pSid').value),
        companyName: document.getElementById('pCo').value.trim(),
        jobRole: document.getElementById('pRole').value.trim(),
        jobLocation: document.getElementById('pLoc').value.trim(),
        packageOffered: parseFloat(document.getElementById('pPkg').value) || null,
        applicationDate: applicationDate,
        overallStatus: document.getElementById('pStatus').value || 'PENDING',
        notes: document.getElementById('pNotes').value.trim()
    };
    if (!body.studentId || !body.companyName) { showToast('Student ID and Company are required', 'error'); return; }
    const data = await apiFetch('/placements', { method:'POST', headers:authHeaders(), body:JSON.stringify(body) });
    if (data?.success) {
        showToast('Placement added!', 'success');
        document.getElementById('placModal').classList.add('hidden');
        loadPlacements();
    } else {
        showToast(data?.message || 'Failed', 'error');
    }
}

async function toggleUser(id) {
    const data = await apiFetch('/admin/users/'+id+'/toggle', { method:'PATCH', headers:authHeaders() });
    if (data?.success) { showToast('User status updated', 'success'); loadUsers(); }
}

async function resetPwd(id) {
    if (!confirm('Reset password to Admin@1234 for this user? They will be forced to change it on next login.')) return;
    const data = await apiFetch('/admin/users/'+id+'/reset-password', { method:'POST', headers:authHeaders() });
    if (data?.success) showToast('Password reset to Admin@1234', 'success');
    else showToast('Failed to reset', 'error');
}

// Placement tracking logic
let currentHistoryStudentId = null;
async function viewPlacementHistory(studentId, sName, sReg) {
    currentHistoryStudentId = studentId;
    document.getElementById('phStudentName').textContent = sName + ' (' + sReg + ')';
    document.getElementById('placementHistoryModal').classList.remove('hidden');
    document.getElementById('phContent').innerHTML = '<p class="text-center text-gray-400 py-6">Loading...</p>';
    
    const res = await apiFetch('/placements/history/student/' + studentId);
    if (!res?.success) {
        document.getElementById('phContent').innerHTML = '<p class="text-red-500 text-center">Failed to load history</p>';
        return;
    }
    
    const { placements, logs } = res.data;
    let html = '';
    
    if (!placements || placements.length === 0) {
        html += '<p class="text-gray-500 dark:text-gray-400 text-center py-6">No placement records found.</p>';
    } else {
        for (const p of placements) {
            const roundsRes = await apiFetch('/placements/' + p.id + '/rounds');
            const rounds = roundsRes?.success ? roundsRes.data : [];
            
            html += `
            <div class="bg-gray-50 dark:bg-gray-700/30 rounded-xl p-5 border border-gray-100 dark:border-gray-700">
                <div class="flex justify-between items-start mb-3">
                    <div>
                        <h4 class="font-bold text-lg dark:text-white">${p.companyName}</h4>
                        <p class="text-sm text-gray-600 dark:text-gray-400">${p.jobRole || 'N/A'} • ${p.packageOffered ? p.packageOffered + ' LPA' : 'N/A'} • <span class="badge badge-${(p.overallStatus||'').toLowerCase()}">${p.overallStatus}</span></p>
                    </div>
                    <button onclick="openRoundModal(${p.id})" class="btn-primary btn-sm"><i class="fas fa-plus"></i> Add Round</button>
                </div>
                <div class="mt-4">
                    <h5 class="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Rounds</h5>
                    ${rounds.length === 0 ? '<p class="text-sm text-gray-400">No rounds tracked yet.</p>' : `
                    <div class="space-y-2">
                        ${rounds.map(r => `
                        <div class="flex items-center justify-between bg-white dark:bg-gray-800 p-3 rounded-lg border border-gray-200 dark:border-gray-700">
                            <div>
                                <div class="flex items-center gap-2">
                                    <span class="font-medium text-sm dark:text-white">${r.roundName}</span>
                                    <span class="badge badge-${(r.status||'').toLowerCase()}">${r.status}</span>
                                </div>
                                <div class="text-xs text-gray-500 mt-1">
                                    ${r.interviewDate ? 'Date: ' + r.interviewDate : ''} 
                                    ${r.notes ? '• Notes: ' + r.notes : ''} 
                                    ${r.feedback ? '• Feedback: ' + r.feedback : ''}
                                </div>
                            </div>
                            <div class="flex gap-1">
                                <button onclick="editRound(${r.id}, ${p.id}, '${r.roundName}', '${r.interviewDate||''}', '${r.status}', '${(r.feedback||'').replace(/'/g, "\\'")}', '${(r.notes||'').replace(/'/g, "\\'")}')" class="btn-secondary btn-sm"><i class="fas fa-edit text-blue-500"></i></button>
                                <button onclick="deleteRoundStatus(${r.id})" class="btn-secondary btn-sm"><i class="fas fa-trash text-red-400"></i></button>
                            </div>
                        </div>
                        `).join('')}
                    </div>
                    `}
                </div>
            </div>`;
        }
    }
    
    html += '<h4 class="font-bold text-gray-700 dark:text-gray-300 mt-6 mb-3 border-t border-gray-200 dark:border-gray-700 pt-4">Action History</h4>';
    if (!logs || logs.length === 0) {
        html += '<p class="text-sm text-gray-500">No action history recorded.</p>';
    } else {
        html += '<ul class="space-y-2 text-sm">';
        logs.forEach(log => {
            const time = new Date(log.timestamp).toLocaleString('en-IN');
            html += `<li class="flex gap-2 items-start"><i class="fas fa-history text-gray-400 mt-1"></i><div class="flex-1"><span class="font-medium text-gray-700 dark:text-gray-300">[${time}] ${log.actionType} ${log.entityType}:</span> <span class="text-gray-600 dark:text-gray-400">${log.details}</span> <span class="text-xs text-gray-400 ml-1">by ${log.performedBy}</span></div></li>`;
        });
        html += '</ul>';
    }
    
    document.getElementById('phContent').innerHTML = html;
}

function openRoundModal(placementId) {
    document.getElementById('roundModalTitle').textContent = 'Add Round';
    document.getElementById('rId').value = '';
    document.getElementById('rPlacementId').value = placementId;
    ['rName', 'rDate', 'rFeedback', 'rNotes'].forEach(id => document.getElementById(id).value = '');
    document.getElementById('rStatus').value = 'PENDING';
    document.getElementById('roundModal').classList.remove('hidden');
}

function editRound(id, pId, name, date, status, feedback, notes) {
    document.getElementById('roundModalTitle').textContent = 'Edit Round';
    document.getElementById('rId').value = id;
    document.getElementById('rPlacementId').value = pId;
    document.getElementById('rName').value = name;
    document.getElementById('rDate').value = date;
    document.getElementById('rStatus').value = status;
    document.getElementById('rFeedback').value = feedback;
    document.getElementById('rNotes').value = notes;
    document.getElementById('roundModal').classList.remove('hidden');
}

async function saveRound() {
    const rId = document.getElementById('rId').value;
    const pId = document.getElementById('rPlacementId').value;
    const body = {
        roundName: document.getElementById('rName').value.trim(),
        interviewDate: document.getElementById('rDate').value || null,
        status: document.getElementById('rStatus').value,
        feedback: document.getElementById('rFeedback').value.trim(),
        notes: document.getElementById('rNotes').value.trim()
    };
    if (!body.roundName) return showToast('Round name is required', 'error');
    
    let res;
    if (rId) {
        res = await apiFetch('/placements/rounds/' + rId, { method: 'PUT', headers: authHeaders(), body: JSON.stringify(body) });
    } else {
        res = await apiFetch('/placements/' + pId + '/rounds', { method: 'POST', headers: authHeaders(), body: JSON.stringify(body) });
    }
    
    if (res?.success) {
        showToast('Round saved successfully', 'success');
        document.getElementById('roundModal').classList.add('hidden');
        if (currentHistoryStudentId) {
            const name = document.getElementById('phStudentName').textContent.split(' (')[0];
            const reg = document.getElementById('phStudentName').textContent.split(' (')[1].replace(')', '');
            viewPlacementHistory(currentHistoryStudentId, name, reg);
        }
    } else {
        showToast(res?.message || 'Failed to save round', 'error');
    }
}

async function deleteRoundStatus(id) {
    if (!confirm('Delete this round?')) return;
    const res = await apiFetch('/placements/rounds/' + id, { method: 'DELETE', headers: authHeaders() });
    if (res?.success) {
        showToast('Round deleted', 'success');
        if (currentHistoryStudentId) {
            const name = document.getElementById('phStudentName').textContent.split(' (')[0];
            const reg = document.getElementById('phStudentName').textContent.split(' (')[1].replace(')', '');
            viewPlacementHistory(currentHistoryStudentId, name, reg);
        }
    } else {
        showToast(res?.message || 'Failed to delete round', 'error');
    }
}

async function uploadXL() {
    const f = document.getElementById('xlFile').files[0];
    const batchYearInput = document.getElementById('xlBatchYear');
    if (!f) { showToast('Please select a file', 'error'); return; }
    const fd = new FormData(); fd.append('file', f);
    if (batchYearInput && batchYearInput.value) {
        fd.append('batchYear', batchYearInput.value);
    }

    const uploadBtn = document.querySelector('#section-upload .btn-primary');
    if (uploadBtn) { uploadBtn.disabled = true; uploadBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Uploading...'; }

    try {
        const res = await fetch('/api/admin/upload-students', {
            method:'POST', headers:{'Authorization':'Bearer '+getToken()}, body:fd
        });
        const data = await res.json();
        const div = document.getElementById('xlResults');
        div.classList.remove('hidden');

        const results = data.data?.results || data.data || [];
        const summary = data.data?.totalRecords ? `
            <div class="bg-blue-50 dark:bg-blue-900/20 rounded-lg p-3 mb-3">
                <p class="text-sm font-semibold dark:text-white">Upload Summary</p>
                <div class="grid grid-cols-4 gap-2 mt-2">
                    <div class="text-center"><p class="text-lg font-bold text-gray-700 dark:text-white">${data.data.totalRecords}</p><p class="text-xs text-gray-500">Total</p></div>
                    <div class="text-center"><p class="text-lg font-bold text-green-600">${data.data.successCount}</p><p class="text-xs text-gray-500">New</p></div>
                    <div class="text-center"><p class="text-lg font-bold text-blue-600">${data.data.updateCount}</p><p class="text-xs text-gray-500">Updated</p></div>
                    <div class="text-center"><p class="text-lg font-bold text-red-600">${data.data.failedCount}</p><p class="text-xs text-gray-500">Failed</p></div>
                </div>
            </div>` : '';

        div.innerHTML = summary + (Array.isArray(results) ? results.map(r =>
            `<p class="text-xs ${r.includes('successfully') || r.includes('created') || r.includes('updated') ? 'text-green-600' : 'text-red-500'}">${r}</p>`
        ).join('') : '');
        showToast('Upload processed', 'success');
        loadUploadHistory();
    } catch(e) {
        showToast('Upload failed: ' + e.message, 'error');
    } finally {
        if (uploadBtn) { uploadBtn.disabled = false; uploadBtn.innerHTML = '<i class="fas fa-cloud-upload-alt"></i> Upload & Import'; }
    }
}

// Resume module JS
async function generateBulkResumes() {
    const body = {};
    const s = document.getElementById('resGenSkills').value.trim(); if(s) body.skills = s;
    const cg = document.getElementById('resGenCgpa').value; if(cg) body.cgpa = parseFloat(cg);
    const m10 = document.getElementById('resGen10th').value; if(m10) body.tenthPercent = parseFloat(m10);
    const m12 = document.getElementById('resGen12th').value; if(m12) body.twelfthPercent = parseFloat(m12);
    const dp = document.getElementById('resGenDept').value.trim(); if(dp) body.departments = [dp];
    const bt = document.getElementById('resGenBatch').value; if(bt) body.batchYears = [parseInt(bt)];
    const dm = document.getElementById('resGenDomain').value.trim(); if(dm) body.domain = dm;
    const cb = document.getElementById('resGenBacklogs'); if(cb && cb.value) body.currentBacklogs = parseInt(cb.value);

    // First preview matched count
    const previewRes = await apiFetch('/reports/preview-bulk', { method:'POST', headers:authHeaders(), body:JSON.stringify(body) });
    if (!previewRes?.success) {
        showToast('Failed to check matched count', 'error');
        return;
    }
    
    const count = previewRes.data;
    if (count === 0) {
        showToast('No students matched exactly with the selected criteria', 'info');
        return;
    }
    
    if (!confirm(`Matched ${count} student(s). Do you want to generate resumes for them?`)) {
        return;
    }

    showToast('Generating resumes... Please wait', 'info');
    const res = await apiFetch('/reports/bulk-resumes', { method:'POST', headers:authHeaders(), body:JSON.stringify(body) });
    if(res?.success) {
        if (!res.data) {
            showToast(res.message, 'info');
        } else {
            showToast(res.message || 'Resumes generated successfully', 'success');
            loadResumeHistory();
        }
    } else { 
        showToast(res?.message || 'Failed to generate resumes', 'error'); 
    }
}

async function loadResumeHistory() {
    const data = await apiFetch('/admin/resume-history');
    const div = document.getElementById('resumeHistoryList');
    if (!data?.success || !data.data || !data.data.length) {
        div.innerHTML = '<p class="text-gray-400 text-center py-6">No history found</p>';
        return;
    }
    div.innerHTML = `<div class="table-wrapper"><table class="data-table">
      <thead><tr><th>File</th><th>Generated By</th><th>Date</th><th>Expires</th><th>Action</th></tr></thead>
      <tbody>${data.data.map(h => `
        <tr>
          <td class="font-medium dark:text-white">${h.fileName}</td>
          <td class="text-sm text-gray-500">${h.generatedBy || '—'}</td>
          <td class="text-xs text-gray-400">${new Date(h.createdAt).toLocaleString('en-IN')}</td>
          <td class="text-xs text-red-400">${new Date(h.expirationDate).toLocaleString('en-IN')}</td>
          <td><button onclick="downloadAuthFile('/api/reports/download-bulk/${h.id}', '${h.fileName}')" class="btn-primary btn-sm"><i class="fas fa-download"></i> Download</button></td>
        </tr>`).join('')}
      </tbody></table></div>`;
}



// Drive logic
function openDriveModal() {
    window.currentDriveId = null;
    ['dCo','dRole','dPkg','dCgpa','dBatch','dSkills','dDate','dDeadline'].forEach(id => document.getElementById(id).value = '');
    document.getElementById('driveModal').classList.remove('hidden');
}

window.editDrive = function(d) {
    window.currentDriveId = d.id;
    document.getElementById('dCo').value = d.companyName || '';
    document.getElementById('dRole').value = d.jobRole || '';
    document.getElementById('dPkg').value = d.packageOffered || '';
    document.getElementById('dCgpa').value = d.minCgpa || '';
    document.getElementById('dBatch').value = d.batch || '';
    document.getElementById('dSkills').value = d.skills || '';
    document.getElementById('dDate').value = d.driveDate || '';
    document.getElementById('dDeadline').value = d.applicationDeadline || '';
    document.getElementById('driveModal').classList.remove('hidden');
};

async function loadDrives() {
    const data = await apiFetch('/placement-drives');
    if (!data?.success) return;
    const tbody = document.getElementById('drvBody');
    if (!data.data.length) { tbody.innerHTML = '<tr><td colspan="7" class="text-center py-10 text-gray-400">No upcoming drives</td></tr>'; return; }
    tbody.innerHTML = data.data.map(d => `
        <tr ondblclick='viewDriveFullDetail(${JSON.stringify(d).replace(/'/g, "&#39;")}, "ADMIN")' class="cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-800/50">
          <td class="font-medium dark:text-white text-sm">${d.companyName}</td>
          <td class="text-sm">${d.jobRole || '—'}</td>
          <td class="text-sm">${d.minCgpa || '—'}</td>
          <td class="text-sm text-red-500">${d.applicationDeadline || '—'}</td>
          <td><span class="badge ${d.status==='ONGOING'?'badge-selected':'badge-failed'}">${d.status}</span></td>
          <td class="text-sm cursor-pointer hover:text-blue-500" title="View Applied">${d.studentsAppliedCount || 0} applications</td>
          <td>
            <div class="flex gap-1">
              <button onclick='editDrive(${JSON.stringify(d).replace(/'/g, "&#39;")})' class="btn-secondary btn-sm"><i class="fas fa-edit text-blue-500"></i></button>
              <button onclick="deleteDrive(${d.id})" class="btn-secondary btn-sm"><i class="fas fa-trash text-red-400"></i></button>
            </div>
          </td>
        </tr>`).join('');
}

async function saveDrive() {
    const driveDate = document.getElementById('dDate').value;
    const deadline = document.getElementById('dDeadline').value;
    const body = {
        companyName: document.getElementById('dCo').value.trim(),
        jobRole: document.getElementById('dRole').value.trim(),
        packageOffered: parseFloat(document.getElementById('dPkg').value) || null,
        minCgpa: parseFloat(document.getElementById('dCgpa').value) || null,
        batch: parseInt(document.getElementById('dBatch').value) || null,
        skills: document.getElementById('dSkills').value.trim(),
        driveDate: driveDate ? new Date(driveDate).toISOString().split('T')[0] : null,
        applicationDeadline: deadline ? new Date(deadline).toISOString().split('T')[0] : null
    };
    if (!body.companyName) return showToast('Company name is required', 'error');
    let url = '/placement-drives';
    let method = 'POST';
    if (window.currentDriveId) {
        url = '/placement-drives/' + window.currentDriveId;
        method = 'PUT';
    }
    const res = await apiFetch(url, { method:method, headers:authHeaders(), body:JSON.stringify(body) });
    if(res?.success) {
        showToast(window.currentDriveId ? 'Drive updated' : 'Drive created', 'success');
        document.getElementById('driveModal').classList.add('hidden');
        loadDrives();
    } else showToast(res?.message || 'Failed', 'error');
}

async function deleteDrive(id) {
    if (!confirm('Delete this drive?')) return;
    const res = await apiFetch('/placement-drives/' + id, { method:'DELETE', headers:authHeaders() });
    if (res?.success) loadDrives(); else showToast(res?.message || 'Failed', 'error');
}

// ── Error Reports ──

async function loadErrorReports() {
    const div = document.getElementById('reportList');
    div.innerHTML = '<p class="text-gray-400 text-center py-8">Loading...</p>';

    // Try admin endpoint first, fall back to reports endpoint
    let data = await apiFetch('/admin/error-reports');
    if (!data || !data.success) {
        data = await apiFetch('/reports');
    }
    if (!data?.success || !data.data || !data.data.length) {
        div.innerHTML = '<p class="text-gray-400 text-center py-8"><i class="fas fa-check-circle text-green-400 text-2xl mb-2 block"></i>No reports found</p>';
        document.getElementById('reportCount').textContent = '0 reports';
        return;
    }

    const reports = data.data;
    document.getElementById('reportCount').textContent = reports.length + ' report' + (reports.length !== 1 ? 's' : '');

    div.innerHTML = `<div class="table-wrapper"><table class="data-table">
        <thead><tr>
            <th>User</th>
            <th>Issue</th>
            <th>Status</th>
            <th>Action / Details</th>
        </tr></thead>
        <tbody>${reports.map(r => `<tr>
            <td>
                <div class="flex flex-col">
                    <span class="font-medium text-sm dark:text-white">${r.user || 'Unknown'}</span>
                    <span class="text-xs text-gray-400">${r.role || ''}</span>
                    <span class="text-[10px] text-gray-500 mt-1"><i class="far fa-clock mr-1"></i>${r.timestamp ? new Date(r.timestamp).toLocaleString('en-IN') : '—'}</span>
                </div>
            </td>
            <td>
                <div class="text-sm font-medium text-gray-800 dark:text-gray-200">${r.subject || '—'}</div>
                <div class="text-xs text-gray-500 mt-1 max-w-sm whitespace-normal">${(r.message || '').replace(/"/g, '&quot;')}</div>
                ${r.severity ? `<span class="mt-2 inline-block px-2 py-0.5 rounded-md text-[10px] font-bold ${r.severity === 'HIGH' ? 'bg-red-100 text-red-600' : (r.severity === 'MEDIUM' ? 'bg-amber-100 text-amber-600' : 'bg-gray-100 text-gray-600')}">${r.severity}</span>` : ''}
            </td>
            <td>
                <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold ${r.status === 'RESOLVED' ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400' : 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400'}">
                    <i class="fas ${r.status === 'RESOLVED' ? 'fa-check-circle' : 'fa-clock'} mr-1 text-[10px]"></i>
                    ${r.status || 'PENDING'}
                </span>
            </td>
            <td>
                ${r.status !== 'RESOLVED'
                    ? `<button onclick="resolveReport(${r.id})" class="btn-primary btn-sm"><i class="fas fa-check mr-1"></i>Reply & Resolve</button>`
                    : `<div class="text-xs text-gray-500 bg-gray-50 dark:bg-gray-800 p-2 rounded-lg border dark:border-gray-700">
                         <div class="font-semibold text-green-600 mb-1"><i class="fas fa-check-double mr-1"></i>Solved by ${r.resolvedBy || 'Admin'}</div>
                         <div class="italic">"${r.adminReply || 'Resolved'}"</div>
                         <div class="text-[10px] text-gray-400 mt-1">${r.resolvedAt ? new Date(r.resolvedAt).toLocaleString('en-IN') : ''}</div>
                       </div>`}
            </td>
        </tr>`).join('')}</tbody>
    </table></div>`;
}

async function resolveReport(id) {
    const adminReply = prompt('Enter reply for the user (they will see this in their notifications):', 'Issue has been resolved.');
    if (adminReply === null) return; // User cancelled

    let res = await apiFetch('/admin/error-reports/' + id + '/resolve', { 
        method: 'PATCH', 
        headers: authHeaders(),
        body: JSON.stringify({ adminReply: adminReply.trim() || 'Resolved' })
    });
    
    if (!res || !res.success) {
        res = await apiFetch('/reports/' + id + '/resolve', { 
            method: 'PATCH', 
            headers: authHeaders(),
            body: JSON.stringify({ adminReply: adminReply.trim() || 'Resolved' })
        });
    }
    
    if (res?.success) {
        showToast('Report resolved! User has been notified.', 'success');
        loadErrorReports();
    } else {
        showToast(res?.message || 'Failed to resolve report', 'error');
    }
}

// ── Student Detail View & Edit ──

let currentDetailStudent = null;
let isDetailEditMode = false;

async function viewStudentDetail(studentId) {
    // Fetch fresh data from backend
    const res = await apiFetch('/student/' + studentId);
    if (!res?.success) {
        showToast('Failed to load student details', 'error');
        return;
    }
    currentDetailStudent = res.data;
    isDetailEditMode = false;
    renderStudentDetailView(currentDetailStudent);
    document.getElementById('studentDetailModal').classList.remove('hidden');
    document.getElementById('sdViewContent').classList.remove('hidden');
    document.getElementById('sdEditContent').classList.add('hidden');
    document.getElementById('sdEditActions').classList.add('hidden');
    document.getElementById('sdEditBtn').classList.remove('hidden');
}

function renderStudentDetailView(s) {
    const initial = (s.fullName || 'S').charAt(0).toUpperCase();
    document.getElementById('sdAvatar').textContent = initial;
    document.getElementById('sdTitle').textContent = s.fullName || 'Student';
    document.getElementById('sdSubtitle').textContent = (s.registerNumber || '') + (s.department ? ' • ' + s.department : '');

    const val = (v, fallback = '—') => (v !== null && v !== undefined && v !== '') ? v : fallback;
    const badge = (status) => {
        if (!status) return '<span class="text-gray-400">—</span>';
        const cls = status === 'SELECTED' ? 'badge-selected' : status === 'PENDING' ? 'badge-pending' : 'badge-failed';
        return `<span class="badge ${cls}">${status}</span>`;
    };
    const listVal = (v) => {
        if (!v) return '—';
        if (Array.isArray(v)) return v.length ? v.join(', ') : '—';
        return v;
    };
    const link = (url, label) => {
        if (!url) return '—';
        return `<a href="${url}" target="_blank" class="text-blue-600 hover:underline text-sm">${label || url}</a>`;
    };

    const section = (title, icon, rows) => `
        <div class="mb-5">
            <h4 class="flex items-center gap-2 text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3 pb-2 border-b border-gray-100 dark:border-gray-700">
                <i class="fas ${icon} text-blue-500 text-xs"></i>${title}
            </h4>
            <div class="grid grid-cols-2 md:grid-cols-3 gap-x-6 gap-y-3">
                ${rows.map(r => `
                    <div>
                        <p class="text-[11px] font-medium text-gray-400 uppercase tracking-wider">${r[0]}</p>
                        <p class="text-sm font-medium text-gray-800 dark:text-gray-200 mt-0.5">${r[1]}</p>
                    </div>
                `).join('')}
            </div>
        </div>`;

    let html = '';
    html += section('Basic Information', 'fa-user', [
        ['Register Number', val(s.registerNumber)],
        ['Full Name', val(s.fullName)],
        ['Email', val(s.email)],
        ['Phone', val(s.phone)],
        ['Department', val(s.department)],
        ['Gender', val(s.gender)],
        ['Date of Birth', s.dob ? formatDate(s.dob) : '—'],
        ['Batch / Year', val(s.batchYear || s.batch)],
        ['Placement Status', badge(s.placementStatus)],
    ]);
    html += section('Academics', 'fa-graduation-cap', [
        ['CGPA', val(s.cgpa)],
        ['10th %', val(s.tenthPercent)],
        ['12th %', val(s.twelfthPercent)],
        ['UG %', val(s.ugPercentage)],
        ['Current Backlogs', val(s.currentBacklogs)],
        ['History of Backlogs', val(s.historyOfBacklogs)],
        ['Career Gap', val(s.careerGap)],
    ]);
    html += section('Skills & Profile', 'fa-code', [
        ['Skills', val(s.skills)],
        ['Tools & Technologies', listVal(s.toolsAndTechnologies)],
        ['Domain', val(s.domain)],
        ['LinkedIn', link(s.linkedinUrl, 'View Profile')],
        ['GitHub', link(s.githubUrl, 'View Profile')],
    ]);
    html += section('Resume Details', 'fa-file-alt', [
        ['Career Objective', val(s.careerObjective)],
        ['Projects', listVal(s.projects)],
        ['Certifications', listVal(s.certifications)],
        ['Internships', listVal(s.internships)],
        ['Achievements', val(s.achievements)],
        ['Languages', val(s.languages)],
        ['Hobbies', val(s.hobbies)],
    ]);
    if (s.address) {
        html += section('Other', 'fa-info-circle', [
            ['Address', val(s.address)],
        ]);
    }
    // Resume download button
    html += `
        <div class="mt-4 pt-4 border-t border-gray-100 dark:border-gray-700">
            <button onclick="downloadAuthFile('/api/reports/student/${s.id}/pdf', 'resume_${(s.registerNumber||'student')}.pdf')" class="btn-primary btn-sm">
                <i class="fas fa-file-pdf"></i> Download Resume PDF
            </button>
        </div>`;

    document.getElementById('sdViewContent').innerHTML = html;
}

function toggleStudentEdit() {
    if (!currentDetailStudent) return;
    isDetailEditMode = true;
    document.getElementById('sdViewContent').classList.add('hidden');
    document.getElementById('sdEditContent').classList.remove('hidden');
    document.getElementById('sdEditActions').classList.remove('hidden');
    document.getElementById('sdEditActions').style.display = 'flex';
    document.getElementById('sdEditBtn').classList.add('hidden');
    renderStudentDetailEdit(currentDetailStudent);
}

function renderStudentDetailEdit(s) {
    const inp = (id, label, value, type = 'text', extra = '') => `
        <div>
            <label class="block text-[11px] font-medium text-gray-500 dark:text-gray-400 mb-1 uppercase tracking-wider">${label}</label>
            <input id="${id}" type="${type}" class="form-input text-sm" value="${value !== null && value !== undefined ? value : ''}" ${extra}/>
        </div>`;
    const sel = (id, label, value, options) => `
        <div>
            <label class="block text-[11px] font-medium text-gray-500 dark:text-gray-400 mb-1 uppercase tracking-wider">${label}</label>
            <select id="${id}" class="form-input text-sm">
                ${options.map(o => `<option value="${o[0]}" ${value === o[0] ? 'selected' : ''}>${o[1]}</option>`).join('')}
            </select>
        </div>`;
    const txtarea = (id, label, value) => `
        <div class="col-span-2 md:col-span-3">
            <label class="block text-[11px] font-medium text-gray-500 dark:text-gray-400 mb-1 uppercase tracking-wider">${label}</label>
            <textarea id="${id}" class="form-input text-sm" rows="2">${value || ''}</textarea>
        </div>`;

    const listToStr = (v) => Array.isArray(v) ? v.join(', ') : (v || '');

    let html = '<div class="grid grid-cols-2 md:grid-cols-3 gap-4">';
    html += inp('sd_regNo', 'Register Number', s.registerNumber, 'text', 'disabled class="form-input text-sm bg-gray-50 dark:bg-gray-700 cursor-not-allowed"');
    html += inp('sd_fullName', 'Full Name', s.fullName);
    html += inp('sd_email', 'Email', s.email, 'email');
    html += inp('sd_phone', 'Phone', s.phone);
    html += inp('sd_department', 'Department', s.department);
    html += sel('sd_gender', 'Gender', s.gender || '', [['','Select'],['Male','Male'],['Female','Female'],['Other','Other']]);
    html += inp('sd_dob', 'Date of Birth', s.dob || '', 'date');
    html += inp('sd_batchYear', 'Batch Year', s.batchYear || '', 'number');
    html += sel('sd_placementStatus', 'Placement Status', s.placementStatus || '', [['','N/A'],['SELECTED','Selected'],['PENDING','Pending'],['NOT_ATTENDED','Not Attended']]);
    html += inp('sd_cgpa', 'CGPA', s.cgpa || '', 'number', 'step="0.1"');
    html += inp('sd_tenthPercent', '10th %', s.tenthPercent || '');
    html += inp('sd_twelfthPercent', '12th %', s.twelfthPercent || '');
    html += inp('sd_ugPercentage', 'UG %', s.ugPercentage || '');
    html += inp('sd_currentBacklogs', 'Current Backlogs', s.currentBacklogs != null ? s.currentBacklogs : '', 'number');
    html += inp('sd_historyOfBacklogs', 'History Backlogs', s.historyOfBacklogs != null ? s.historyOfBacklogs : '', 'number');
    html += inp('sd_careerGap', 'Career Gap', s.careerGap || '');
    html += inp('sd_skills', 'Skills', s.skills || '');
    html += inp('sd_domain', 'Domain', s.domain || '');
    html += inp('sd_linkedinUrl', 'LinkedIn URL', s.linkedinUrl || '', 'url');
    html += inp('sd_githubUrl', 'GitHub URL', s.githubUrl || '', 'url');
    html += inp('sd_languages', 'Languages', s.languages || '');
    html += inp('sd_certifications', 'Certifications', listToStr(s.certifications));
    html += inp('sd_internships', 'Internships', listToStr(s.internships));
    html += inp('sd_projects', 'Projects', listToStr(s.projects));
    html += txtarea('sd_careerObjective', 'Career Objective', s.careerObjective);
    html += txtarea('sd_achievements', 'Achievements', s.achievements);
    html += txtarea('sd_hobbies', 'Hobbies', s.hobbies);
    html += txtarea('sd_address', 'Address', s.address);
    html += '</div>';

    document.getElementById('sdEditContent').innerHTML = html;
    // Fix the disabled register number input class
    const regInput = document.getElementById('sd_regNo');
    if (regInput) { regInput.className = 'form-input text-sm bg-gray-50 dark:bg-gray-700 cursor-not-allowed'; }
}

async function saveStudentDetail() {
    if (!currentDetailStudent) return;
    const v = (id) => { const el = document.getElementById(id); return el ? el.value.trim() : ''; };
    const body = {
        fullName: v('sd_fullName'),
        email: v('sd_email'),
        phone: v('sd_phone'),
        department: v('sd_department'),
        gender: v('sd_gender') || null,
        dob: v('sd_dob') || null,
        batchYear: v('sd_batchYear') ? parseInt(v('sd_batchYear')) : null,
        placementStatus: v('sd_placementStatus') || null,
        cgpa: v('sd_cgpa') ? parseFloat(v('sd_cgpa')) : null,
        tenthPercent: v('sd_tenthPercent') || null,
        twelfthPercent: v('sd_twelfthPercent') || null,
        ugPercentage: v('sd_ugPercentage') || null,
        currentBacklogs: v('sd_currentBacklogs') !== '' ? parseInt(v('sd_currentBacklogs')) : null,
        historyOfBacklogs: v('sd_historyOfBacklogs') !== '' ? parseInt(v('sd_historyOfBacklogs')) : null,
        careerGap: v('sd_careerGap') || null,
        skills: v('sd_skills') || null,
        domain: v('sd_domain') || null,
        linkedinUrl: v('sd_linkedinUrl') || null,
        githubUrl: v('sd_githubUrl') || null,
        languages: v('sd_languages') || null,
        certifications: v('sd_certifications') || null,
        internships: v('sd_internships') || null,
        projects: v('sd_projects') || null,
        careerObjective: v('sd_careerObjective') || null,
        achievements: v('sd_achievements') || null,
        hobbies: v('sd_hobbies') || null,
        address: v('sd_address') || null,
    };

    const res = await apiFetch('/student/' + currentDetailStudent.id, { method: 'PUT', headers: authHeaders(), body: JSON.stringify(body) });
    if (res?.success) {
        showToast('Student updated successfully', 'success');
        currentDetailStudent = res.data;
        // Switch back to view mode
        cancelStudentEdit();
        renderStudentDetailView(currentDetailStudent);
        loadStudents(); // refresh the table
    } else {
        showToast(res?.message || 'Failed to update student', 'error');
    }
}

function cancelStudentEdit() {
    isDetailEditMode = false;
    document.getElementById('sdViewContent').classList.remove('hidden');
    document.getElementById('sdEditContent').classList.add('hidden');
    document.getElementById('sdEditActions').classList.add('hidden');
    document.getElementById('sdEditBtn').classList.remove('hidden');
}

function closeStudentDetail() {
    document.getElementById('studentDetailModal').classList.add('hidden');
    isDetailEditMode = false;
    currentDetailStudent = null;
}
