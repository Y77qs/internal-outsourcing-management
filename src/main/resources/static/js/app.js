const TOKEN_KEY = "pta_auth_token";

let currentUser = null;
let availableRoles = [];
let availableDepartments = [];
let availableProjects = [];
let latestUsers = [];
let latestWorkLogs = [];
let latestPerformances = [];
let performanceFilterUserOptions = [];
let performanceModalUserOptions = [];
let performanceFilterUserTimer = null;
let performanceModalUserTimer = null;
let editingRoleUserId = null;
let editingWorkLogId = null;
let editingPerformanceId = null;

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
        } else if (page === "work-logs") {
            await initWorkLogs();
        } else if (page === "performances") {
            await initPerformances();
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
    setText("#dashboardUserName", currentUser.realName || currentUser.username || "系统用户");
    setText("#roleLine", Array.from(currentUser.roles || []).join(", ") || "-");
    try {
        const health = await api("/api/health");
        setText("#healthStatus", `${health.status} / ${health.service}`);
    } catch (error) {
        setText("#healthStatus", "检查失败");
    }
    // 工作台统计只读取当前账号具备权限的数据，避免为了展示数字触发无权限请求。
    await Promise.all([
        loadDashboardCount("#dashboardApplicationCount", "/api/onboarding/applications/mine?pageNo=1&pageSize=1",
                "application:read:self"),
        loadDashboardCount("#dashboardPendingCount", "/api/approvals/pending?pageNo=1&pageSize=1", "approval:read"),
        loadDashboardCount("#dashboardNotificationCount", "/api/notifications?pageNo=1&pageSize=1", "notification:read"),
        loadDashboardAnyCount("#dashboardWorkLogCount", [
            ["worklog:read:all", "/api/work-logs?pageNo=1&pageSize=1"],
            ["worklog:read:self", "/api/work-logs/mine?pageNo=1&pageSize=1"]
        ]),
        loadDashboardAnyCount("#dashboardPerformanceCount", [
            ["performance:read", "/api/performances?current=true&pageNo=1&pageSize=1"],
            ["performance:read:self", "/api/performances/mine?current=true&pageNo=1&pageSize=1"]
        ])
    ]);
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
    const rows = document.querySelector("#applicationRows");
    rows.innerHTML = loadingRow(7);
    const pageData = await api("/api/onboarding/applications/mine?pageNo=1&pageSize=20");
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
            <td class="text-end">
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

async function initWorkLogs() {
    await loadBasicOptions();
    renderProjectSelect("#workLogProjectFilter", true);
    renderProjectSelect("#workLogProjectId", false);
    document.querySelector("#workLogDate").value = new Date().toISOString().slice(0, 10);
    document.querySelector("#searchWorkLogs").addEventListener("click", loadWorkLogs);
    document.querySelector("#reloadWorkLogs").addEventListener("click", loadWorkLogs);
    document.querySelectorAll("[data-action='new-work-log']").forEach(button => {
        button.addEventListener("click", openNewWorkLogModal);
    });
    document.querySelector("#workLogForm")?.addEventListener("submit", saveWorkLog);
    document.querySelector("#workLogRows").addEventListener("click", event => {
        const button = event.target.closest("button[data-action='edit-work-log']");
        if (!button) {
            return;
        }
        const workLog = latestWorkLogs.find(item => String(item.id) === button.dataset.id);
        if (workLog) {
            openEditWorkLogModal(workLog);
        }
    });
    document.querySelector("#workLogAllFilters")?.classList.toggle("d-none", !hasClientPermission("worklog:read:all"));
    await loadWorkLogs();
}

async function loadWorkLogs() {
    const rows = document.querySelector("#workLogRows");
    rows.innerHTML = loadingRow(8);
    const params = new URLSearchParams();
    const projectId = document.querySelector("#workLogProjectFilter").value;
    const startDate = document.querySelector("#workLogStartDate").value;
    const endDate = document.querySelector("#workLogEndDate").value;
    const userId = document.querySelector("#workLogUserId")?.value;
    if (projectId) {
        params.set("projectId", projectId);
    }
    if (startDate) {
        params.set("startDate", startDate);
    }
    if (endDate) {
        params.set("endDate", endDate);
    }
    if (userId && hasClientPermission("worklog:read:all")) {
        params.set("userId", userId);
    }
    params.set("pageNo", "1");
    params.set("pageSize", "20");
    const endpoint = hasClientPermission("worklog:read:all") ? "/api/work-logs" : "/api/work-logs/mine";
    const pageData = await api(`${endpoint}?${params.toString()}`);
    latestWorkLogs = pageData.records;
    document.querySelector("#workLogCount").textContent = `共 ${pageData.total} 条`;
    if (latestWorkLogs.length === 0) {
        rows.innerHTML = emptyRow(8, "暂无工作日志", "bi-journal-plus");
        return;
    }
    rows.innerHTML = latestWorkLogs.map(workLog => `
        <tr>
            <td>${workLog.id}</td>
            <td>${escapeHtml(workLog.realName || workLog.username || "")}</td>
            <td>${escapeHtml(workLog.projectName || "")}</td>
            <td>${workLog.workDate || ""}</td>
            <td class="text-truncate-cell" title="${escapeHtml(workLog.workContent || "")}">
                ${escapeHtml(workLog.workContent || "")}
            </td>
            <td class="text-truncate-cell" title="${escapeHtml(workLog.issueRecord || "")}">
                ${escapeHtml(workLog.issueRecord || "")}
            </td>
            <td class="text-truncate-cell" title="${escapeHtml(workLog.completionStatus || "")}">
                ${escapeHtml(workLog.completionStatus || "")}
            </td>
            <td class="text-end">
                ${canEditWorkLog(workLog)
                    ? `<button class="btn btn-outline-primary btn-sm btn-icon" data-action="edit-work-log"
                               data-id="${workLog.id}" type="button">
                           <i class="bi bi-pencil-square"></i><span>修改</span>
                       </button>`
                    : ""}
            </td>
        </tr>
    `).join("");
}

function openNewWorkLogModal() {
    editingWorkLogId = null;
    document.querySelector("#workLogModalTitle").textContent = "提交工作日志";
    document.querySelector("#workLogForm").reset();
    renderProjectSelect("#workLogProjectId", false);
    document.querySelector("#workLogDate").value = new Date().toISOString().slice(0, 10);
    bootstrap.Modal.getOrCreateInstance(document.querySelector("#workLogModal")).show();
}

function openEditWorkLogModal(workLog) {
    editingWorkLogId = workLog.id;
    document.querySelector("#workLogModalTitle").textContent = `修改工作日志 #${workLog.id}`;
    renderProjectSelect("#workLogProjectId", false);
    document.querySelector("#workLogProjectId").value = String(workLog.projectId);
    document.querySelector("#workLogDate").value = workLog.workDate || "";
    document.querySelector("#workContent").value = workLog.workContent || "";
    document.querySelector("#issueRecord").value = workLog.issueRecord || "";
    document.querySelector("#completionStatus").value = workLog.completionStatus || "";
    bootstrap.Modal.getOrCreateInstance(document.querySelector("#workLogModal")).show();
}

async function saveWorkLog(event) {
    event.preventDefault();
    const payload = Object.fromEntries(new FormData(event.target).entries());
    payload.projectId = Number(payload.projectId);
    const path = editingWorkLogId ? `/api/work-logs/${editingWorkLogId}` : "/api/work-logs";
    const method = editingWorkLogId ? "PUT" : "POST";
    try {
        await api(path, {method, body: payload});
        closeModal("#workLogModal");
        showAlert("工作日志已保存。", "success");
        await loadWorkLogs();
    } catch (error) {
        showAlert(error.message, "danger");
    }
}

function canEditWorkLog(workLog) {
    return hasClientPermission("worklog:update:self") && currentUser && currentUser.id === workLog.userId;
}

async function initPerformances() {
    await loadBasicOptions();
    renderProjectSelect("#performanceProjectFilter", true);
    renderProjectSelect("#performanceProjectId", false);
    bindPerformanceUserSearch();
    document.querySelector("#searchPerformances").addEventListener("click", loadPerformances);
    document.querySelector("#reloadPerformances").addEventListener("click", loadPerformances);
    document.querySelectorAll("[data-action='new-performance']").forEach(button => {
        button.addEventListener("click", openNewPerformanceModal);
    });
    document.querySelector("#performanceForm")?.addEventListener("submit", savePerformance);
    document.querySelector("#periodType")?.addEventListener("change", () => updatePeriodValueState());
    document.querySelector("#performanceRows").addEventListener("click", event => {
        const button = event.target.closest("button[data-action='edit-performance']");
        if (!button) {
            return;
        }
        const performance = latestPerformances.find(item => String(item.id) === button.dataset.id);
        if (performance) {
            openEditPerformanceModal(performance);
        }
    });
    document.querySelector("#performanceAllFilters")?.classList.toggle("d-none",
            !hasClientPermission("performance:read"));
    await loadPerformances();
}

function bindPerformanceUserSearch() {
    const filterName = document.querySelector("#performanceUserName");
    const filterId = document.querySelector("#performanceUserId");
    const modalName = document.querySelector("#evaluatedUserName");
    const modalId = document.querySelector("#evaluatedUserIdInput");
    filterName?.addEventListener("input", () => schedulePerformanceUserSearch("filter", "name"));
    filterId?.addEventListener("change", () => schedulePerformanceUserSearch("filter", "id", 0));
    modalName?.addEventListener("input", () => schedulePerformanceUserSearch("modal", "name"));
    modalId?.addEventListener("change", () => schedulePerformanceUserSearch("modal", "id", 0));
    document.querySelector("#performanceUserMatches")?.addEventListener("click", handlePerformanceUserOptionClick);
    document.querySelector("#evaluatedUserMatches")?.addEventListener("click", handlePerformanceUserOptionClick);
}

function schedulePerformanceUserSearch(scope, mode, delay = 350) {
    const timerName = scope === "filter" ? "performanceFilterUserTimer" : "performanceModalUserTimer";
    clearTimeout(scope === "filter" ? performanceFilterUserTimer : performanceModalUserTimer);
    const timer = setTimeout(() => {
        refreshPerformanceUserOptions(scope, mode).catch(error => showAlert(error.message, "danger"));
    }, delay);
    if (timerName === "performanceFilterUserTimer") {
        performanceFilterUserTimer = timer;
    } else {
        performanceModalUserTimer = timer;
    }
}

async function refreshPerformanceUserOptions(scope, mode) {
    const controls = performanceUserControls(scope);
    if (!controls.nameInput || !controls.idInput) {
        return [];
    }
    if (mode === "name") {
        controls.idInput.value = "";
        if (controls.hiddenInput) {
            controls.hiddenInput.value = "";
        }
    }
    const userId = controls.idInput.value.trim();
    const name = controls.nameInput.value.trim();
    if (!userId && !name) {
        setPerformanceUserOptions(scope, []);
        return [];
    }
    const options = await searchPerformanceUserOptions(userId ? {userId} : {name});
    setPerformanceUserOptions(scope, options);
    if (userId && options.length === 1) {
        controls.nameInput.value = options[0].realName || options[0].username || "";
        if (controls.hiddenInput) {
            controls.hiddenInput.value = options[0].id;
        }
        renderPerformanceUserSelection(scope);
    }
    return options;
}

async function searchPerformanceUserOptions(query) {
    const params = new URLSearchParams();
    if (query.userId) {
        params.set("userId", query.userId);
    }
    if (query.name) {
        params.set("name", query.name);
    }
    return api(`/api/performances/user-options?${params.toString()}`);
}

function performanceUserControls(scope) {
    if (scope === "filter") {
        return {
            nameInput: document.querySelector("#performanceUserName"),
            idInput: document.querySelector("#performanceUserId"),
            optionList: document.querySelector("#performanceUserMatches"),
            hiddenInput: null
        };
    }
    return {
        nameInput: document.querySelector("#evaluatedUserName"),
        idInput: document.querySelector("#evaluatedUserIdInput"),
        optionList: document.querySelector("#evaluatedUserMatches"),
        hiddenInput: document.querySelector("#evaluatedUserId")
    };
}

function setPerformanceUserOptions(scope, options) {
    const controls = performanceUserControls(scope);
    if (scope === "filter") {
        performanceFilterUserOptions = options;
    } else {
        performanceModalUserOptions = options;
    }
    if (controls.optionList) {
        controls.optionList.innerHTML = performanceUserOptionsHtml(scope, options, controls);
    }
    if (scope === "modal" && options.length === 1 && controls.hiddenInput && !controls.idInput.value) {
        controls.idInput.value = options[0].id;
        controls.hiddenInput.value = options[0].id;
        renderPerformanceUserSelection(scope);
    }
}

function performanceUserOptionsHtml(scope, options, controls) {
    if (options.length === 0) {
        if (!controls.nameInput?.value.trim() && !controls.idInput?.value.trim()) {
            return "";
        }
        return `<div class="user-option-empty">未匹配到人员。</div>`;
    }
    const allOption = scope === "filter" && options.length > 1
            ? userOptionButtonHtml("all", `全部匹配人员：ID ${options.map(user => user.id).join("、")}`,
                    "默认查询全部匹配人员", !controls.idInput.value)
            : "";
    return allOption + options.map(user => userOptionButtonHtml(user.id, userOptionLabel(user), user.status,
            String(controls.idInput.value) === String(user.id))).join("");
}

function userOptionButtonHtml(value, label, meta, active) {
    return `
        <button class="user-option-button ${active ? "active" : ""}" type="button" data-user-option="${value}">
            <span class="user-option-main">${escapeHtml(label)}</span>
            <span class="user-option-meta">${escapeHtml(meta || "")}</span>
        </button>
    `;
}

function handlePerformanceUserOptionClick(event) {
    const button = event.target.closest("button[data-user-option]");
    if (!button) {
        return;
    }
    const scope = button.closest("#performanceUserMatches") ? "filter" : "modal";
    const value = button.dataset.userOption;
    if (value === "all") {
        selectAllPerformanceUsers(scope);
        return;
    }
    selectPerformanceUser(scope, Number(value));
}

function selectAllPerformanceUsers(scope) {
    const controls = performanceUserControls(scope);
    controls.idInput.value = "";
    if (controls.hiddenInput) {
        controls.hiddenInput.value = "";
    }
    renderPerformanceUserSelection(scope);
}

function selectPerformanceUser(scope, userId) {
    const options = scope === "filter" ? performanceFilterUserOptions : performanceModalUserOptions;
    const selected = options.find(user => Number(user.id) === Number(userId));
    const controls = performanceUserControls(scope);
    controls.idInput.value = String(userId);
    if (selected && controls.nameInput) {
        controls.nameInput.value = selected.realName || selected.username || "";
    }
    if (controls.hiddenInput) {
        controls.hiddenInput.value = String(userId);
    }
    renderPerformanceUserSelection(scope);
}

function renderPerformanceUserSelection(scope) {
    const controls = performanceUserControls(scope);
    const buttons = controls.optionList?.querySelectorAll("button[data-user-option]") || [];
    buttons.forEach(button => {
        const value = button.dataset.userOption;
        button.classList.toggle("active", value === "all" ? !controls.idInput.value
                : String(controls.idInput.value) === value);
    });
}

function userOptionLabel(user) {
    return `ID ${user.id} - ${user.realName || "未填写姓名"} - ${user.username || ""}`;
}

async function appendPerformanceUserParams(params) {
    const nameInput = document.querySelector("#performanceUserName");
    const idInput = document.querySelector("#performanceUserId");
    const evaluatedUserId = idInput?.value.trim();
    const evaluatedUserName = nameInput?.value.trim();
    if (evaluatedUserId) {
        params.set("evaluatedUserId", evaluatedUserId);
        await refreshPerformanceUserOptions("filter", "id");
        return;
    }
    if (!evaluatedUserName) {
        setPerformanceUserOptions("filter", []);
        return;
    }
    const options = await refreshPerformanceUserOptions("filter", "name");
    if (options.length === 0) {
        params.set("evaluatedUserId", "-1");
        return;
    }
    options.forEach(user => params.append("evaluatedUserIds", user.id));
}

async function loadPerformances() {
    const rows = document.querySelector("#performanceRows");
    rows.innerHTML = loadingRow(9);
    const params = new URLSearchParams();
    const projectId = document.querySelector("#performanceProjectFilter").value;
    const periodType = document.querySelector("#performancePeriodFilter").value;
    const current = document.querySelector("#performanceCurrentOnly").checked;
    if (projectId) {
        params.set("projectId", projectId);
    }
    if (hasClientPermission("performance:read")) {
        await appendPerformanceUserParams(params);
    }
    if (periodType) {
        params.set("periodType", periodType);
    }
    if (current) {
        params.set("current", "true");
    }
    params.set("pageNo", "1");
    params.set("pageSize", "20");
    const endpoint = hasClientPermission("performance:read") ? "/api/performances" : "/api/performances/mine";
    const pageData = await api(`${endpoint}?${params.toString()}`);
    latestPerformances = pageData.records;
    document.querySelector("#performanceCount").textContent = `共 ${pageData.total} 条`;
    if (latestPerformances.length === 0) {
        rows.innerHTML = emptyRow(9, "暂无绩效记录", "bi-award");
        return;
    }
    rows.innerHTML = latestPerformances.map(performance => `
        <tr>
            <td>${performance.id}</td>
            <td>
                <div>${escapeHtml(performance.evaluatedRealName || performance.evaluatedUsername || "")}</div>
                <div class="small text-muted">ID ${performance.evaluatedUserId}</div>
            </td>
            <td>${escapeHtml(performance.projectName || "")}</td>
            <td>${escapeHtml(performance.periodType || "")}</td>
            <td>${escapeHtml(performance.periodValue || "")}</td>
            <td>${gradeBadge(performance.grade)}</td>
            <td>${performance.current ? badge("ENABLED") : badge("DISABLED")}</td>
            <td class="text-truncate-cell" title="${escapeHtml(performance.comment || "")}">
                ${escapeHtml(performance.comment || "")}
            </td>
            <td class="text-end">
                ${canEditPerformance(performance)
                    ? `<button class="btn btn-outline-primary btn-sm btn-icon" data-action="edit-performance"
                               data-id="${performance.id}" type="button">
                           <i class="bi bi-pencil-square"></i><span>修改</span>
                       </button>`
                    : ""}
            </td>
        </tr>
    `).join("");
}

function openNewPerformanceModal() {
    editingPerformanceId = null;
    document.querySelector("#performanceModalTitle").textContent = "新增绩效记录";
    document.querySelector("#performanceForm").reset();
    renderProjectSelect("#performanceProjectId", false);
    document.querySelector("#periodType").value = "MONTH";
    document.querySelector("#modificationReasonGroup").classList.add("d-none");
    document.querySelector("#modificationReason").required = false;
    document.querySelector("#evaluatedUserName").disabled = false;
    document.querySelector("#evaluatedUserIdInput").disabled = false;
    document.querySelector("#evaluatedUserId").value = "";
    document.querySelector("#performanceProjectId").disabled = false;
    document.querySelector("#periodType").disabled = false;
    document.querySelector("#periodValue").disabled = false;
    setPerformanceUserOptions("modal", []);
    updatePeriodValueState(currentMonthPeriodValue());
    bootstrap.Modal.getOrCreateInstance(document.querySelector("#performanceModal")).show();
}

function openEditPerformanceModal(performance) {
    editingPerformanceId = performance.id;
    document.querySelector("#performanceModalTitle").textContent = `修改绩效 #${performance.id}`;
    renderProjectSelect("#performanceProjectId", false);
    document.querySelector("#evaluatedUserId").value = performance.evaluatedUserId;
    document.querySelector("#evaluatedUserIdInput").value = performance.evaluatedUserId;
    document.querySelector("#evaluatedUserName").value = performance.evaluatedRealName || performance.evaluatedUsername || "";
    document.querySelector("#performanceProjectId").value = String(performance.projectId);
    document.querySelector("#periodType").value = performance.periodType;
    document.querySelector("#performanceGrade").value = performance.grade;
    document.querySelector("#performanceComment").value = performance.comment || "";
    document.querySelector("#modificationReason").value = "";
    document.querySelector("#modificationReasonGroup").classList.remove("d-none");
    document.querySelector("#modificationReason").required = true;
    document.querySelector("#evaluatedUserName").disabled = true;
    document.querySelector("#evaluatedUserIdInput").disabled = true;
    document.querySelector("#performanceProjectId").disabled = true;
    document.querySelector("#periodType").disabled = true;
    document.querySelector("#periodValue").disabled = true;
    setPerformanceUserOptions("modal", [{
        id: performance.evaluatedUserId,
        username: performance.evaluatedUsername,
        realName: performance.evaluatedRealName,
        status: "ENABLED"
    }]);
    updatePeriodValueState(performance.periodValue);
    bootstrap.Modal.getOrCreateInstance(document.querySelector("#performanceModal")).show();
}

async function savePerformance(event) {
    event.preventDefault();
    if (!editingPerformanceId && !(await resolvePerformanceModalUserId())) {
        return;
    }
    const formData = new FormData(event.target);
    const payload = compactObject(Object.fromEntries(formData.entries()));
    const method = editingPerformanceId ? "PUT" : "POST";
    const path = editingPerformanceId ? `/api/performances/${editingPerformanceId}` : "/api/performances";
    if (!editingPerformanceId) {
        payload.evaluatedUserId = Number(payload.evaluatedUserId);
        payload.projectId = Number(payload.projectId);
    }
    try {
        await api(path, {method, body: payload});
        closeModal("#performanceModal");
        showAlert("绩效记录已保存。", "success");
        await loadPerformances();
    } catch (error) {
        showAlert(error.message, "danger");
    }
}

async function resolvePerformanceModalUserId() {
    const controls = performanceUserControls("modal");
    const selectedUserId = controls.idInput?.value.trim();
    if (!selectedUserId) {
        if (controls.nameInput?.value.trim()) {
            const options = await refreshPerformanceUserOptions("modal", "name");
            if (options.length > 1) {
                showAlert("存在多个同名人员，请选择具体人员 ID。", "warning");
                return false;
            }
            if (options.length === 1) {
                controls.hiddenInput.value = options[0].id;
                return true;
            }
        }
        showAlert("请选择被评价用户 ID。", "warning");
        return false;
    }
    controls.hiddenInput.value = selectedUserId;
    return true;
}

function updatePeriodValueState(preferredValue) {
    const periodType = document.querySelector("#periodType")?.value;
    const periodValue = document.querySelector("#periodValue");
    if (!periodValue) {
        return;
    }
    const selectedValue = resolvePeriodValue(periodType, preferredValue ?? periodValue.value);
    periodValue.innerHTML = periodValueOptionsHtml(periodType, selectedValue);
    periodValue.value = selectedValue;
    periodValue.disabled = Boolean(editingPerformanceId) || periodType === "PROJECT";
    periodValue.required = periodType !== "PROJECT";
}

function periodValueOptionsHtml(periodType, selectedValue) {
    if (periodType === "PROJECT") {
        const label = selectedValue ? `${selectedValue}（由项目生成）` : "由项目自动生成";
        return `<option value="${escapeHtml(selectedValue)}">${escapeHtml(label)}</option>`;
    }
    const values = periodType === "QUARTER" ? currentYearQuarterValues() : currentYearMonthValues();
    if (selectedValue && !values.includes(selectedValue)) {
        values.unshift(selectedValue);
    }
    return values.map(value => `<option value="${escapeHtml(value)}">${escapeHtml(value)}</option>`).join("");
}

function resolvePeriodValue(periodType, value) {
    if (periodType === "PROJECT") {
        return editingPerformanceId && value ? value : "";
    }
    if (periodType === "QUARTER") {
        return monthToQuarterValue(value) || quarterPeriodValue(value) || currentQuarterPeriodValue();
    }
    return quarterToMonthValue(value) || monthPeriodValue(value) || currentMonthPeriodValue();
}

function currentYearMonthValues() {
    const year = new Date().getFullYear();
    return Array.from({length: 12}, (_, index) => `${year}-${padTwo(index + 1)}`);
}

function currentYearQuarterValues() {
    const year = new Date().getFullYear();
    return [1, 2, 3, 4].map(quarter => `${year}-Q${quarter}`);
}

function currentMonthPeriodValue() {
    const date = new Date();
    return `${date.getFullYear()}-${padTwo(date.getMonth() + 1)}`;
}

function currentQuarterPeriodValue() {
    const date = new Date();
    return `${date.getFullYear()}-Q${Math.floor(date.getMonth() / 3) + 1}`;
}

function monthToQuarterValue(value) {
    const month = monthPeriodValue(value);
    if (!month) {
        return "";
    }
    const [, year, monthNumber] = month.match(/^(\d{4})-(\d{2})$/);
    return `${year}-Q${Math.floor((Number(monthNumber) - 1) / 3) + 1}`;
}

function quarterToMonthValue(value) {
    const quarter = quarterPeriodValue(value);
    if (!quarter) {
        return "";
    }
    const [, year, quarterNumber] = quarter.match(/^(\d{4})-Q([1-4])$/);
    return `${year}-${padTwo((Number(quarterNumber) - 1) * 3 + 1)}`;
}

function monthPeriodValue(value) {
    const match = String(value || "").match(/^(\d{4})-(0[1-9]|1[0-2])$/);
    return match ? match[0] : "";
}

function quarterPeriodValue(value) {
    const match = String(value || "").toUpperCase().match(/^(\d{4})-Q([1-4])$/);
    return match ? match[0] : "";
}

function padTwo(value) {
    return String(value).padStart(2, "0");
}

function canEditPerformance(performance) {
    return hasClientPermission("performance:write") && performance.current;
}

async function loadApprovals() {
    const rows = document.querySelector("#approvalRows");
    rows.innerHTML = loadingRow(8);
    const pageData = await api("/api/approvals/pending?pageNo=1&pageSize=20");
    document.querySelector("#pendingCount").textContent = `共 ${pageData.total} 条`;
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
            <td class="text-end">
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
    const rows = document.querySelector("#userRows");
    rows.innerHTML = loadingRow(7);
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
            <td class="text-end">
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
    const rows = document.querySelector("#notificationRows");
    rows.innerHTML = loadingRow(7);
    const pageData = await api("/api/notifications?pageNo=1&pageSize=20");
    document.querySelector("#notificationCount").textContent = `共 ${pageData.total} 条`;
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
    const rows = document.querySelector("#operationLogRows");
    rows.innerHTML = loadingRow(8);
    const params = new URLSearchParams();
    const operatorId = document.querySelector("#logOperatorId").value;
    const moduleName = document.querySelector("#logModuleName").value;
    const keyword = document.querySelector("#logKeyword").value;
    const startTime = document.querySelector("#logStartTime").value;
    const endTime = document.querySelector("#logEndTime").value;
    if (operatorId) {
        params.set("operatorId", operatorId);
    }
    if (moduleName) {
        params.set("moduleName", moduleName);
    }
    if (keyword) {
        params.set("keyword", keyword);
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

function renderProjectSelect(selector, includeEmpty) {
    const select = document.querySelector(selector);
    if (!select) {
        return;
    }
    const empty = includeEmpty ? `<option value="">全部项目</option>` : "";
    select.innerHTML = empty + availableProjects.map(project => `
        <option value="${project.id}">${escapeHtml(project.projectName)}</option>
    `).join("");
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
        <label class="form-check-card">
            <input class="form-check-input ${inputClass}" type="checkbox" value="${role.id}"
                   ${selected(role) ? "checked" : ""}>
            <span class="form-check-label">${escapeHtml(role.roleName)} (${escapeHtml(role.roleCode)})</span>
        </label>
    `).join("");
}

function renderCurrentUser() {
    setText("#currentUser", currentUser ? `${currentUser.realName || currentUser.username}` : "");
}

function applyPermissions() {
    const permissions = new Set(currentUser?.permissions || []);
    document.querySelectorAll("[data-permission]").forEach(element => {
        if (!permissions.has(element.dataset.permission)) {
            element.classList.add("d-none");
        }
    });
    document.querySelectorAll("[data-permission-any]").forEach(element => {
        const anyPermission = element.dataset.permissionAny.split(",")
                .map(item => item.trim())
                .some(permission => permissions.has(permission));
        if (!anyPermission) {
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
                    <div class="empty-title">${escapeHtml(message)}</div>
                    <p class="empty-copy">当前没有符合条件的数据。</p>
                </div>
            </td>
        </tr>
    `;
}

function loadingRow(colspan) {
    return `
        <tr>
            <td colspan="${colspan}">
                <div class="empty-state">
                    <span class="spinner-border spinner-border-sm text-success" aria-hidden="true"></span>
                    <div class="empty-title">数据加载中</div>
                    <p class="empty-copy">正在读取最新业务数据。</p>
                </div>
            </td>
        </tr>
    `;
}

async function loadDashboardCount(selector, url, permission) {
    if (!hasClientPermission(permission)) {
        setText(selector, "-");
        return;
    }
    try {
        const pageData = await api(url);
        setText(selector, pageData.total ?? 0);
    } catch (error) {
        setText(selector, "-");
    }
}

async function loadDashboardAnyCount(selector, options) {
    const matched = options.find(([permission]) => hasClientPermission(permission));
    if (!matched) {
        setText(selector, "-");
        return;
    }
    try {
        const pageData = await api(matched[1]);
        setText(selector, pageData.total ?? 0);
    } catch (error) {
        setText(selector, "-");
    }
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

function gradeBadge(value) {
    const color = {
        A: "success",
        B: "info",
        C: "warning"
    }[value] || "secondary";
    return `<span class="badge text-bg-${color} status-badge">${escapeHtml(value || "")}</span>`;
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

function hasClientPermission(permission) {
    return new Set(currentUser?.permissions || []).has(permission);
}

function setText(selector, value) {
    const element = document.querySelector(selector);
    if (element) {
        element.textContent = value;
    }
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
