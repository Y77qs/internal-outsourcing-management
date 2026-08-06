INSERT INTO sys_role (id, role_code, role_name, description, status)
VALUES
    (1, 'ADMIN', '系统管理员', '拥有系统管理、用户权限和审计日志查询权限', 'ENABLED'),
    (2, 'LEADER', '上级领导', '负责测试外包人员上岗申请审批', 'ENABLED'),
    (3, 'OUTSOURCER', '测试外包人员', '可提交和查看个人上岗申请', 'ENABLED')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), description = VALUES(description), status = VALUES(status);

INSERT INTO sys_permission (
    id,
    permission_code,
    permission_name,
    module_name,
    permission_type,
    api_path,
    http_method,
    request_method,
    path
)
VALUES
    (1, 'user:read', '查询用户', '用户管理', 'API', '/api/users/**', 'GET', 'GET', '/api/users/**'),
    (2, 'user:write', '维护用户', '用户管理', 'API', '/api/users/**', 'POST,PUT', 'POST,PUT', '/api/users/**'),
    (3, 'role:read', '查询角色权限', '权限管理', 'API', '/api/roles,/api/permissions', 'GET', 'GET',
        '/api/roles,/api/permissions'),
    (4, 'application:create', '提交上岗申请', '上岗申请', 'API', '/api/onboarding/applications', 'POST', 'POST',
        '/api/onboarding/applications'),
    (5, 'application:read:self', '查询个人申请', '上岗申请', 'API', '/api/onboarding/applications/**', 'GET', 'GET',
        '/api/onboarding/applications/**'),
    (6, 'application:withdraw', '撤回申请', '上岗申请', 'API', '/api/onboarding/applications/*/withdraw', 'POST',
        'POST', '/api/onboarding/applications/*/withdraw'),
    (7, 'approval:read', '查询待审批申请', '领导审批', 'API', '/api/approvals/**', 'GET', 'GET',
        '/api/approvals/**'),
    (8, 'approval:write', '处理审批', '领导审批', 'API', '/api/approvals/**', 'POST', 'POST',
        '/api/approvals/**'),
    (9, 'notification:read', '查询通知', '异步通知', 'API', '/api/notifications/**', 'GET', 'GET',
        '/api/notifications/**'),
    (10, 'operation:read', '查询操作日志', '操作日志', 'API', '/api/operation-logs/**', 'GET', 'GET',
        '/api/operation-logs/**'),
    (11, 'basic:read', '查询基础资料', '基础资料', 'API', '/api/departments,/api/projects', 'GET', 'GET',
        '/api/departments,/api/projects')
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    module_name = VALUES(module_name),
    permission_type = VALUES(permission_type),
    api_path = VALUES(api_path),
    http_method = VALUES(http_method),
    request_method = VALUES(request_method),
    path = VALUES(path);

INSERT IGNORE INTO sys_role_permission (id, role_id, permission_id)
VALUES
    (1, 1, 1), (2, 1, 2), (3, 1, 3), (4, 1, 4), (5, 1, 5),
    (6, 1, 6), (7, 1, 7), (8, 1, 8), (9, 1, 9), (10, 1, 10), (21, 1, 11),
    (11, 2, 5), (12, 2, 7), (13, 2, 8), (14, 2, 9), (19, 2, 11),
    (15, 3, 4), (16, 3, 5), (17, 3, 6), (18, 3, 9), (20, 3, 11);

INSERT INTO sys_user (id, username, password_hash, phone, email, real_name, department_id, status)
VALUES
    (1, 'admin', '$2b$10$AhtPkXS0J5D3VXKs7eOoVOGYEz3ZiD47jJxPvBCQPn0aDt1GwKtdO', '13800000000',
        'admin@example.com', '系统管理员', 1, 'ENABLED'),
    (2, 'leader', '$2b$10$W74tL2snl/6ggc9IN6lJ/ulhw5rf/ywloEmD9IY4DxrLpQnf1Yx3q', '13800000001',
        'leader@example.com', '审批领导', 2, 'ENABLED')
ON DUPLICATE KEY UPDATE
    real_name = VALUES(real_name),
    department_id = VALUES(department_id),
    status = VALUES(status);

INSERT IGNORE INTO sys_user_role (id, user_id, role_id)
VALUES (1, 1, 1), (2, 2, 2);

INSERT INTO sys_department (id, parent_id, department_code, department_name, leader_user_id, description, sort_order, status)
VALUES
    (1, NULL, 'QA_CENTER', '质量保障中心', 1, '内部测试管理归口部门', 1, 'ENABLED'),
    (2, 1, 'TEST_PLATFORM', '测试平台部', 2, '测试外包人员上岗与审批部门', 2, 'ENABLED')
ON DUPLICATE KEY UPDATE
    department_code = VALUES(department_code),
    department_name = VALUES(department_name),
    leader_user_id = VALUES(leader_user_id),
    description = VALUES(description),
    sort_order = VALUES(sort_order),
    status = VALUES(status);

INSERT INTO project (id, department_id, project_code, project_name, description, start_date, end_date, status)
VALUES
    (1, 2, 'PTA-OUTSOURCING', '内部测试外包人员管理系统', '用于系统验收的测试外包管理项目',
        '2026-08-03', '2026-08-07', 'ENABLED')
ON DUPLICATE KEY UPDATE
    project_code = VALUES(project_code),
    project_name = VALUES(project_name),
    description = VALUES(description),
    start_date = VALUES(start_date),
    end_date = VALUES(end_date),
    status = VALUES(status);
