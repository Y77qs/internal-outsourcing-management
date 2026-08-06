const TOKEN_KEY = "pta_auth_token";

let currentUser = null;
let availableRoles = [];
let availableDepartments = [];
let availableProjects = [];
let latestUsers = [];
let editingRoleUserId = null;

const page = document.body.dataset.page;
const publicPages = new Set(["login", "register"]);

document.addEventListener("DOMContentLoaded", () => {
    bindLogout();
    if (page === "login") {
        initLogin();
        return;
    }
    if (page === "register") {
        initRegister();
        return;
    }
    initSecuredPage();
});

async function initSecuredPage() {
    try {
        currentUser = await requireLogin();
        renderCurrentUser();
        applyPermissions();
        if (page === "dashboard") {
            await initDashboard();
        } else if (page === "applications") {
            await initApplications();
        } else if (page === "approvals") {
            await initApprovals();
        } else if (page === "users") {
            await initUsers();
        } else if (page === "notifications") {
            await initNotifications();
        } else if (page === "operation-logs") {
            await initOperationLogs();
        }
    } catch (error) {
        showAlert(error.message, "danger");
    }
}

function bindLogout() {
    const button = document.querySelector("#logoutButton");
    if (!button) {
        return;
    }
    button.addEventListener("click", async () => {
        try {
            await api("/api/auth/logout", {method: "POST"});
        } finally {
            sessionStorage.removeItem(TOKEN_KEY);
            window.location.href = "/ui/login";
        }
    });
}

function initLogin() {
    document.querySelector("#loginForm").addEventListener("submit", async event => {
        event.preventDefault();
        const formData = new FormData(event.target);
        try {
            const data = await api("/api/auth/login", {
                method: "POST",
                body: Object.fromEntries(formData.entries())
            });
            saveLoginAndRedirect(data);
        } catch (error) {
            showAlert(error.message, "danger");
        }
    });
}

function initRegister() {
    document.querySelector("#registerForm").addEventListener("submit", async event => {
        event.preventDefault();
        const payload = compactObject(Object.fromEntries(new FormData(event.target).entries()));
        try {
            await api("/api/auth/register", {method: "POST", body: payload});
            // 注册成功后复用登录接口建立 JWT 登录态，保证公开注册流程一次完成。
            const data = await api("/api/auth/login", {
                method: "POST",
                body: {
                    username: payload.username,
                    password: payload.password
                }
            });
            saveLoginAndRedirect(data);
        } catch (error) {
            showAlert(error.message, "danger");
        }
    });
}

async function requireLogin() {
    if (!sessionStorage.getItem(TOKEN_KEY)) {
        window.location.href = "/ui/login";
        throw new Error("请先登录");
    }
    return api("/api/auth/me");
}

async function initDashboard() {
    document.querySelector("#roleLine").textContent = Array.from(currentUser.roles || []).join(", ");
    try {
        const health = await api("/api/health");
        document.querySelector("#healthStatus").textContent = `${health.status} / ${health.service}`;
    } catch (error) {
        document.querySelector("#healthStatus").textContent = "检查失败";
    }
}

async function initApplications() {
    await loadBasicOptions();
    renderDepartmentOptions("#departmentId", false);
    const firstProject = availableProjects[0];
    if (firstProject) {
        document.querySelector("#departmentId").value = String(firstProject.departmentId);
    }
    renderProjectOptions();
    document.querySelector("#departmentId")?.addEventListener("change", renderProjectOptions);
    document.querySelector("#applicationForm")?.addEventListener("submit", async event => {
        event.preventDefault();
        const payload = Object.fromEntries(new FormData(event.target).entries());
        payload.departmentId = Number(payload.departmentId);
        payload.projectId = Number(payload.projectId);
        try {
            await api("/api/onboarding/applications", {method: "POST", body: payload});
            closeModal("#applicationModal");
            showAlert("申请已提交。", "success");
            await loadApplications();
        } catch (error) {
            showAlert(error.message, "danger");
        }
    });
    document.querySelector("#reloadApplications").addEventListener("click", loadApplications);
    document.querySelector("#applicationRows").addEventListener("click", async event => {
        const button = event.target.closest("button[data-action='withdraw']");
        if (!button) {
            return;
        }
        try {
            await api(`/api/onboarding/applications/${button.dataset.id}/withdraw`, {method: "POST"});
            showAlert("申请已撤回。", "success");
            await loadApplications();
        } catch (error) {
            showAlert(error.message, "danger");
        }
    });
    await loadApplications();
}

async function loadApplications() {
    const pageData = await api("/api/onboarding/applications/mine?pageNo=1&pageSize=20");
    const rows = document.querySelector("#applicationRows");
    if (pageData.records.length === 0) {
        rows.innerHTML = emptyRow(7, "暂无上岗申请", "bi-inbox");
        return;
    }
    rows.innerHTML = pageData.records.map(application => `
        <tr>
            <td>${application.id}</td>
            <td>${escapeHtml(application.projectName || "")}</td>
            <td>${escapeHtml(application.positionType || "")}</td>
            <td>${badge(application.status)}</td>
            <td>${formatTime(application.submittedAt || application.createdAt)}</td>
            <td class="text-truncate-cell" title="${escapeHtml(application.approvalOpinion || "")}">
                ${escapeHtml(application.approvalOpinion || "")}
            </td>
            <td>
                <span class="table-actions">
                    ${application.status === "PENDING"
                        ? `<button class="btn btn-outline-danger btn-sm btn-icon" data-action="withdraw"
                               data-id="${application.id}" type="button">
                               <i class="bi bi-arrow-counterclockwise"></i><span>撤回</span>
                           </button>`
                        : ""}
                </span>
            </td>
        </tr>
    `).join("");
}

async function initApprovals() {
    document.querySelector("#reloadApprovals").addEventListener("click", loadApprovals);
    document.querySelector("#checkAllApprovals").addEventListener("change", event => {
        document.querySelectorAll(".approval-check").forEach(input => {
            input.checked = event.target.checked;
        });
        updateBatchBar();
    });
    document.querySelector("#approvalRows").addEventListener("change", event => {
        if (event.target.classList.contains("approval-check")) {
            updateBatchBar();
        }
    });
    document.querySelector("#batchSubmit").addEventListener("click", batchApprove);
    document.querySelector("#confirmReject").addEventListener("click", confirmReject);
    document.querySelector("#approvalRows").addEventListener("click", async event => {
        const button = event.target.closest("button[data-action]");
        if (!button) {
            return;
        }
        if (button.dataset.action === "approve") {
            await processApproval(button.dataset.id, "approve", "同意");
        } else if (button.dataset.action === "reject") {
            openRejectModal(button.dataset.id);
        }
    });
    await loadApprovals();
}

async function loadApprovals() {
    const pageData = await api("/api/approvals/pending?pageNo=1&pageSize=20");
    document.querySelector("#pendingCount").textContent = `共 ${pageData.total} 条`;
    const rows = document.querySelector("#approvalRows");
    if (pageData.records.length === 0) {
        rows.innerHTML = emptyRow(8, "暂无待审批申请", "bi-check2-circle");
        document.querySelector("#checkAllApprovals").checked = false;
        updateBatchBar();
        return;
    }
    rows.innerHTML = pageData.records.map(application => `
        <tr>
            <td><input class="form-check-input approval-check" type="checkbox" value="${application.id}"></td>
            <td>${application.id}</td>
            <td>${escapeHtml(application.applicantName || "")}</td>
            <td>${escapeHtml(application.departmentName || "")}</td>
            <td>${escapeHtml(application.projectName || "")}</td>
            <td>${escapeHtml(application.positionType || "")}</td>
            <td>${formatTime(application.submittedAt || application.createdAt)}</td>
            <td>
                <span class="table-actions">
                    <button class="btn btn-success btn-sm btn-icon" data-action="approve" data-id="${application.id}"
                            type="button"><i class="bi bi-check2"></i><span>通过</span></button>
                    <button class="btn btn-outline-danger btn-sm btn-icon" data-action="reject"
                            data-id="${application.id}" type="button">
                            <i class="bi bi-x-circle"></i><span>驳回</span>
                    </button>
                </span>
            </td>
        </tr>
    `).join("");
    document.querySelector("#checkAllApprovals").checked = false;
    updateBatchBar();
}

async function processApproval(id, action, opinion) {
    try {
        await api(`/api/approvals/${id}/${action}`, {method: "POST", body: {opinion}});
        showAlert("审批已处理。", "success");
        await loadApprovals();
    } catch (error) {
        showAlert(error.message, "danger");
    }
}

async function batchApprove() {
    const applicationIds = selectedApprovalIds();
    const result = document.querySelector("#batchResult").value;
    const opinion = document.querySelector("#batchOpinion").value;
    if (applicationIds.length === 0) {
        showAlert("请选择申请。", "warning");
        return;
    }
    if (result === "REJECTED" && !opinion) {
        showAlert("批量驳回必须填写审批意见。", "warning");
        return;
    }
    try {
        await api("/api/approvals/batch", {method: "POST", body: {applicationIds, result, opinion}});
        showAlert("批量处理完成。", "success");
        await loadApprovals();
    } catch (error) {
        showAlert(error.message, "danger");
    }
}

function openRejectModal(applicationId) {
    document.querySelector("#rejectApplicationId").value = applicationId;
    document.querySelector("#rejectOpinion").value = "";
    bootstrap.Modal.getOrCreateInstance(document.querySelector("#rejectModal")).show();
}

async function confirmReject() {
    const applicationId = document.querySelector("#rejectApplicationId").value;
    const opinion = document.querySelector("#rejectOpinion").value.trim();
    if (!opinion) {
        showAlert("驳回必须填写审批意见。", "warning");
        return;
    }
    await processApproval(applicationId, "reject", opinion);
    closeModal("#rejectModal");
}

function updateBatchBar() {
    const count = selectedApprovalIds().length;
    document.querySelector("#selectedApprovalCount").textContent = count;
    document.querySelector("#batchBar").classList.toggle("d-none", count === 0);
}

function selectedApprovalIds() {
    return Array.from(document.querySelectorAll(".approval-check:checked"))
            .map(input => Number(input.value));
}

async function initUsers() {
    availableRoles = await api("/api/roles");
    await loadBasicOptions();
    renderCreateRoleCheckboxes();
    renderDepartmentOptions("#createDepartmentId", true);
    document.querySelector("#createUserForm")?.addEventListener("submit", createUser);
    document.querySelector("#searchUsers").addEventListener("click", loadUsers);
    document.querySelector("#cancelRoleEdit")?.addEventListener("click", closeRoleEditor);
    document.querySelector("#saveRoles").addEventListener("click", saveRoles);
    document.querySelector("#userRows").addEventListener("click", async event => {
        const button = event.target.closest("button[data-action]");
        if (!button) {
            return;
        }
        const user = latestUsers.find(item => String(item.id) === button.dataset.id);
        if (!user) {
            return;
        }
        if (button.dataset.action === "status") {
            await updateUserStatus(user);
        } else if (button.dataset.action === "roles") {
            openRoleEditor(user);
        }
    });
    await loadUsers();
}

async function createUser(event) {
    event.preventDefault();
    const payload = compactObject(Object.fromEntries(new FormData(event.target).entries()));
    if (payload.departmentId) {
        payload.departmentId = Number(payload.departmentId);
    }
    payload.roleIds = Array.from(document.querySelectorAll(".create-role-check:checked"))
            .map(input => Number(input.value));
    if (payload.roleIds.length === 0) {
        showAlert("创建内部账号必须至少选择一个角色。", "warning");
        return;
    }
    try {
        await api("/api/users", {method: "POST", body: payload});
        closeModal("#createUserModal");
        showAlert("内部账号已创建。", "success");
        event.target.reset();
        renderDepartmentOptions("#createDepartmentId", true);
        await loadUsers();
    } catch (error) {
        showAlert(error.message, "danger");
    }
}

async function loadUsers() {
    const params = new URLSearchParams();
    const username = document.querySelector("#userKeyword").value;
    const status = document.querySelector("#userStatus").value;
    if (username) {
        params.set("username", username);
    }
    if (status) {
        params.set("status", status);
    }
    params.set("pageNo", "1");
    params.set("pageSize", "20");
    const pageData = await api(`/api/users?${params.toString()}`);
    latestUsers = pageData.records;
    const rows = document.querySelector("#userRows");
    if (latestUsers.length === 0) {
        rows.innerHTML = emptyRow(7, "暂无用户数据", "bi-people");
        return;
    }
    rows.innerHTML = latestUsers.map(user => `
        <tr>
            <td>${user.id}</td>
            <td>${escapeHtml(user.username || "")}</td>
            <td>${escapeHtml(user.realName || "")}</td>
            <td>${escapeHtml(departmentName(user.departmentId))}</td>
            <td>${badge(user.status)}</td>
            <td>${escapeHtml(Array.from(user.roles || []).join(", "))}</td>
            <td>
                <span class="table-actions">
                    <button class="btn ${user.status === "ENABLED" ? "btn-outline-danger" : "btn-outline-success"}
                            btn-sm btn-icon" data-action="status" data-id="${user.id}" type="button">
                        <i class="bi ${user.status === "ENABLED" ? "bi-person-dash" : "bi-person-check"}"></i>
                        <span>${user.status === "ENABLED" ? "禁用" : "启用"}</span>
                    </button>
                    <button class="btn btn-outline-primary btn-sm btn-icon" data-action="roles" data-id="${user.id}"
                            type="button">
                        <i class="bi bi-shield-lock"></i><span>角色</span>
                    </button>
                </span>
            </td>
        </tr>
    `).join("");
}

async function updateUserStatus(user) {
    const nextStatus = user.status === "ENABLED" ? "DISABLED" : "ENABLED";
    try {
        await api(`/api/users/${user.id}/status`, {method: "PUT", body: {status: nextStatus}});
        showAlert("用户状态已更新。", "success");
        await loadUsers();
    } catch (error) {
        showAlert(error.message, "danger");
    }
}

function openRoleEditor(user) {
    editingRoleUserId = user.id;
    document.querySelector("#roleEditorTitle").textContent = `分配角色：${user.username}`;
    document.querySelector("#roleCheckboxes").innerHTML = buildRoleCheckboxes(
            "role-check",
            role => Array.from(user.roles || []).includes(role.roleCode)
    );
    bootstrap.Modal.getOrCreateInstance(document.querySelector("#roleEditor")).show();
}

function closeRoleEditor() {
    editingRoleUserId = null;
    closeModal("#roleEditor");
}

async function saveRoles() {
    if (!editingRoleUserId) {
        return;
    }
    const roleIds = Array.from(document.querySelectorAll(".role-check:checked"))
            .map(input => Number(input.value));
    try {
        await api(`/api/users/${editingRoleUserId}/roles`, {method: "PUT", body: {roleIds}});
        showAlert("角色已保存。", "success");
        closeRoleEditor();
        await loadUsers();
    } catch (error) {
        showAlert(error.message, "danger");
    }
}

async function initNotifications() {
    document.querySelector("#reloadNotifications").addEventListener("click", loadNotifications);
    await loadNotifications();
}

async function loadNotifications() {
    const pageData = await api("/api/notifications?pageNo=1&pageSize=20");
    document.querySelector("#notificationCount").textContent = `共 ${pageData.total} 条`;
    const rows = document.querySelector("#notificationRows");
    if (pageData.records.length === 0) {
        rows.innerHTML = emptyRow(7, "暂无通知消息", "bi-bell");
        return;
    }
    rows.innerHTML = pageData.records.map(message => `
        <tr>
            <td>${message.id}</td>
            <td>${message.applicationId}</td>
            <td>${escapeHtml(message.eventType || "")}</td>
            <td class="text-truncate-cell" title="${escapeHtml(message.title || "")}">
                ${escapeHtml(message.title || "")}
            </td>
            <td>${badge(message.status)}</td>
            <td>${message.retryCount}</td>
            <td>${formatTime(message.createdAt)}</td>
        </tr>
    `).join("");
}

async function initOperationLogs() {
    document.querySelector("#searchLogs").addEventListener("click", loadOperationLogs);
    await loadOperationLogs();
}

async function loadOperationLogs() {
    const params = new URLSearchParams();
    const operatorId = document.querySelector("#logOperatorId").value;
    const moduleName = document.querySelector("#logModuleName").value;
    const startTime = document.querySelector("#logStartTime").value;
    const endTime = document.querySelector("#logEndTime").value;
    if (operatorId) {
        params.set("operatorId", operatorId);
    }
    if (moduleName) {
        params.set("moduleName", moduleName);
    }
    if (startTime) {
        params.set("startTime", startTime);
    }
    if (endTime) {
        params.set("endTime", endTime);
    }
    params.set("pageNo", "1");
    params.set("pageSize", "30");
    const pageData = await api(`/api/operation-logs?${params.toString()}`);
    const rows = document.querySelector("#operationLogRows");
    if (pageData.records.length === 0) {
        rows.innerHTML = emptyRow(8, "暂无操作日志", "bi-journal-text");
        return;
    }
    rows.innerHTML = pageData.records.map(log => `
        <tr>
            <td>${log.id}</td>
            <td>${escapeHtml(log.operatorName || "")}</td>
            <td>${escapeHtml(log.moduleName || "")}</td>
            <td>${escapeHtml(log.operationType || "")}</td>
            <td>${badge(log.result)}</td>
            <td class="text-truncate-cell" title="${escapeHtml(log.requestPath || "")}">
                ${escapeHtml(log.requestPath || "")}
            </td>
            <td class="text-truncate-cell" title="${escapeHtml(log.requestParams || "")}">
                ${escapeHtml(log.requestParams || "")}
            </td>
            <td>${formatTime(log.createdAt)}</td>
        </tr>
    `).join("");
}

async function loadBasicOptions() {
    if (availableDepartments.length === 0) {
        availableDepartments = await api("/api/departments");
    }
    if (availableProjects.length === 0) {
        availableProjects = await api("/api/projects");
    }
}

function renderDepartmentOptions(selector, includeEmpty) {
    const select = document.querySelector(selector);
    if (!select) {
        return;
    }
    const empty = includeEmpty ? `<option value="">未分配</option>` : "";
    select.innerHTML = empty + availableDepartments.map(department => `
        <option value="${department.id}">${escapeHtml(department.departmentName)}</option>
    `).join("");
}

function renderProjectOptions() {
    const departmentId = Number(document.querySelector("#departmentId")?.value || 0);
    const select = document.querySelector("#projectId");
    if (!select) {
        return;
    }
    const projects = availableProjects.filter(project => !departmentId || project.departmentId === departmentId);
    select.innerHTML = projects.map(project => `
        <option value="${project.id}">${escapeHtml(project.projectName)}</option>
    `).join("");
    select.disabled = projects.length === 0;
}

async function api(path, options = {}) {
    const request = {
        method: options.method || "GET",
        headers: {
            Accept: "application/json",
            ...(options.headers || {})
        }
    };
    const token = sessionStorage.getItem(TOKEN_KEY);
    if (token) {
        request.headers.Authorization = `Bearer ${token}`;
    }
    if (options.body !== undefined) {
        request.headers["Content-Type"] = "application/json";
        request.body = JSON.stringify(options.body);
    }
    const response = await fetch(path, request);
    const payload = await response.json().catch(() => null);
    if (response.status === 401 && !publicPages.has(page)) {
        sessionStorage.removeItem(TOKEN_KEY);
        window.location.href = "/ui/login";
        throw new Error("登录已失效");
    }
    if (!response.ok || !payload || payload.code !== "00000") {
        throw new Error(payload?.message || response.statusText || "请求失败");
    }
    return payload.data;
}

function saveLoginAndRedirect(data) {
    sessionStorage.setItem(TOKEN_KEY, data.token);
    window.location.href = "/ui/dashboard";
}

function renderCreateRoleCheckboxes() {
    const container = document.querySelector("#createRoleCheckboxes");
    if (!container) {
        return;
    }
    container.innerHTML = buildRoleCheckboxes("create-role-check", () => false);
}

function buildRoleCheckboxes(inputClass, selected) {
    return availableRoles.map(role => `
        <label class="form-check">
            <input class="form-check-input ${inputClass}" type="checkbox" value="${role.id}"
                   ${selected(role) ? "checked" : ""}>
            <span class="form-check-label">${escapeHtml(role.roleName)} (${escapeHtml(role.roleCode)})</span>
        </label>
    `).join("");
}

function renderCurrentUser() {
    const element = document.querySelector("#currentUser");
    if (element && currentUser) {
        element.textContent = `${currentUser.realName || currentUser.username}`;
    }
}

function applyPermissions() {
    const permissions = new Set(currentUser?.permissions || []);
    document.querySelectorAll("[data-permission]").forEach(element => {
        if (!permissions.has(element.dataset.permission)) {
            element.classList.add("d-none");
        }
    });
}

function showAlert(message, type) {
    const alert = document.querySelector("#alert");
    if (alert) {
        alert.textContent = message;
        alert.className = `alert alert-${type}`;
    }
    const container = document.querySelector("#toastContainer");
    if (!container || typeof bootstrap === "undefined") {
        return;
    }
    const toast = document.createElement("div");
    toast.className = `toast align-items-center text-bg-${toastType(type)} border-0`;
    toast.setAttribute("role", "status");
    toast.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">${escapeHtml(message)}</div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"
                    aria-label="关闭"></button>
        </div>
    `;
    container.appendChild(toast);
    const instance = new bootstrap.Toast(toast, {delay: 2600});
    toast.addEventListener("hidden.bs.toast", () => toast.remove());
    instance.show();
}

function closeModal(selector) {
    const modal = document.querySelector(selector);
    if (modal && typeof bootstrap !== "undefined") {
        bootstrap.Modal.getOrCreateInstance(modal).hide();
    }
}

function compactObject(payload) {
    Object.keys(payload).forEach(key => {
        if (payload[key] === "") {
            delete payload[key];
        }
    });
    return payload;
}

function emptyRow(colspan, message, icon) {
    return `
        <tr>
            <td colspan="${colspan}">
                <div class="empty-state">
                    <i class="bi ${icon}"></i>
                    <div>${escapeHtml(message)}</div>
                </div>
            </td>
        </tr>
    `;
}

function badge(value) {
    const color = {
        ENABLED: "success",
        DISABLED: "secondary",
        PENDING: "warning",
        APPROVED: "success",
        REJECTED: "danger",
        WITHDRAWN: "secondary",
        SUCCESS: "success",
        FAILED: "danger",
        SENT: "success"
    }[value] || "secondary";
    return `<span class="badge text-bg-${color} status-badge">${escapeHtml(statusLabel(value))}</span>`;
}

function statusLabel(value) {
    return {
        ENABLED: "启用",
        DISABLED: "禁用",
        PENDING: "待审批",
        APPROVED: "已通过",
        REJECTED: "已驳回",
        WITHDRAWN: "已撤回",
        SUCCESS: "成功",
        FAILED: "失败",
        SENT: "已发送"
    }[value] || value || "";
}

function toastType(type) {
    return {
        danger: "danger",
        warning: "warning",
        success: "success"
    }[type] || "secondary";
}

function departmentName(departmentId) {
    if (!departmentId) {
        return "";
    }
    const department = availableDepartments.find(item => item.id === departmentId);
    return department ? department.departmentName : String(departmentId);
}

function formatTime(value) {
    if (!value) {
        return "";
    }
    return String(value).replace("T", " ").slice(0, 19);
}

function escapeHtml(value) {
    return String(value)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&#039;");
}
