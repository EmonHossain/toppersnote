-- 1. Create Core Roles
INSERT INTO roles (name) VALUES 
  ('USER_DEFAULT'),
  ('USER'),
  ('ADMIN'),
  ('SUPER_ADMIN')
ON CONFLICT (name) DO NOTHING;

-- 2. Define and Seed the Route Pattern Rules
-- A clean compilation of the API space grouped by functional boundaries
WITH dynamic_permissions AS (
    VALUES
      -- Global public / framework endpoints
      ('/v3/api-docs', 'GET', 'permit_all', 1),
      ('/v3/api-docs/**', 'GET', 'permit_all', 1),
      ('/swagger-ui.html', 'GET', 'permit_all', 1),
      ('/swagger-ui/**', 'GET', 'permit_all', 1),
      ('/api/v1/auth/**', 'ANY', 'permit_all', 5),
      ('/api/v1/user/register', 'POST', 'permit_all', 5),

      -- Read-only student permissions (Assigned to ROLE_USER_DEFAULT & up)
      ('/api/v1/user/me/**', 'GET', 'student:read', 10),
      ('/api/v1/notes', 'GET', 'student:read', 10),
      ('/api/v1/notes/recent', 'GET', 'student:read', 10),
      ('/api/v1/notes/search', 'GET', 'student:read', 10),
      ('/api/v1/notes/*/download', 'GET', 'student:read', 10),
      ('/api/v1/notes/*/preview', 'GET', 'student:read', 10),
      ('/api/v1/notes/downloads/all', 'GET', 'student:read', 10),
      ('/api/v1/notes/*/comments', 'GET', 'student:read', 10),
      ('/api/v1/notes/take-a-look', 'GET', 'student:read', 10),
      ('/api/v1/notes/*/versions', 'GET', 'student:read', 10),
      ('/api/v1/notes/*/proposals', 'GET', 'student:read', 10),
      ('/api/v1/notes/reports/me', 'GET', 'student:read', 10),
      ('/api/v1/notes/*/quality-score', 'GET', 'student:read', 10),
      ('/api/v1/notes/quality-scores/top', 'GET', 'student:read', 10),
      ('/api/v1/ai/providers', 'GET', 'student:read', 10),
      ('/api/v1/academic/**', 'GET', 'student:read', 10),
      ('/api/v1/lifecycle/exam-reminders', 'GET', 'student:read', 10),
      ('/api/v1/lifecycle/archived-classes', 'GET', 'student:read', 10),
      ('/api/v1/dashboard/me', 'GET', 'student:read', 10),
      ('/api/v1/notifications', 'GET', 'student:read', 10),
      ('/api/v1/notifications/summary', 'GET', 'student:read', 10),
      ('/api/v1/study-groups', 'GET', 'student:read', 10),
      ('/api/v1/study-groups/*/activity', 'GET', 'student:read', 10),
      ('/api/v1/study-groups/*/notebooks', 'GET', 'student:read', 10),
      ('/api/v1/collections', 'GET', 'student:read', 10),
      ('/api/v1/collections/*', 'GET', 'student:read', 10),
      ('/api/v1/analytics/me/**', 'GET', 'student:read', 10),

      -- Functional interaction permissions (Assigned to ROLE_USER & up)
      ('/api/v1/user/me/profile-picture', 'POST', 'student:write', 15),
      ('/api/v1/user/me/preferences', 'PATCH', 'student:write', 15),
      ('/api/v1/notes/upload', 'POST', 'student:write', 15),
      ('/api/v1/notes/downloads/selected', 'POST', 'student:write', 15),
      ('/api/v1/notes/*/comments', 'POST', 'student:write', 15),
      ('/api/v1/notes/*/comments/*/replies', 'POST', 'student:write', 15),
      ('/api/v1/notes/*/upvotes', 'POST', 'student:write', 15),
      ('/api/v1/notes/*/take-a-look', 'POST', 'student:write', 15),
      ('/api/v1/notes/*/versions', 'POST', 'student:write', 15),
      ('/api/v1/notes/*/proposals', 'POST', 'student:write', 15),
      ('/api/v1/notes/*/proposals/*/approve', 'POST', 'student:write', 15),
      ('/api/v1/notes/*/proposals/*/reject', 'POST', 'student:write', 15),
      ('/api/v1/notes/*/reports', 'POST', 'student:write', 15),
      ('/api/v1/notes/*/ai', 'POST', 'student:write', 15),
      ('/api/v1/lifecycle/exam-reminders', 'POST', 'student:write', 15),
      ('/api/v1/lifecycle/exam-reminders/*', 'PATCH', 'student:write', 15),
      ('/api/v1/lifecycle/exam-reminders/*', 'DELETE', 'student:write', 15),
      ('/api/v1/lifecycle/archived-classes', 'POST', 'student:write', 15),
      ('/api/v1/lifecycle/archived-classes/*', 'DELETE', 'student:write', 15),
      ('/api/v1/notifications/*', 'PATCH', 'student:write', 15),
      ('/api/v1/notifications/read-all', 'PATCH', 'student:write', 15),
      ('/api/v1/study-groups', 'POST', 'student:write', 15),
      ('/api/v1/study-groups/*/join', 'POST', 'student:write', 15),
      ('/api/v1/study-groups/*/leave', 'POST', 'student:write', 15),
      ('/api/v1/study-groups/*', 'DELETE', 'student:write', 15),
      ('/api/v1/study-groups/*/notebooks', 'POST', 'student:write', 15),
      ('/api/v1/study-groups/*/notebooks/*', 'DELETE', 'student:write', 15),
      ('/api/v1/study-groups/*/notebooks/*/notes/*', 'POST', 'student:write', 15),
      ('/api/v1/study-groups/*/notebooks/*/notes/*', 'DELETE', 'student:write', 15),
      ('/api/v1/collections', 'POST', 'student:write', 15),
      ('/api/v1/collections/*', 'PATCH', 'student:write', 15),
      ('/api/v1/collections/*', 'DELETE', 'student:write', 15),
      ('/api/v1/collections/*/notes/*', 'POST', 'student:write', 15),
      ('/api/v1/collections/*/notes/*', 'DELETE', 'student:write', 15),

      -- Standard Admin Management operations (Assigned to ROLE_ADMIN & up)
      ('/api/v1/admin/users', 'GET', 'admin:manage', 50),
      ('/api/v1/admin/users/*/ban-temporary', 'PATCH', 'admin:manage', 50),
      ('/api/v1/admin/users/*/unban', 'PATCH', 'admin:manage', 50),
      ('/api/v1/admin/note-reports', 'GET', 'admin:manage', 50),
      ('/api/v1/admin/note-reports/*', 'PATCH', 'admin:manage', 50),
      ('/api/v1/admin/analytics/**', 'GET', 'admin:analytics', 50),

      -- High-Tier Destructive & System Administrative operations (Assigned only to ROLE_SUPER_ADMIN)
      ('/api/v1/admin/users/*/ban-permanent', 'PATCH', 'admin:super', 90),
      ('/api/v1/admin/audit-events', 'GET', 'admin:super', 90),
      ('/api/v1/admin/permissions/refresh', 'POST', 'admin:super', 90),
      ('/api/v1/admin/note-retention/**', 'ANY', 'admin:super', 90)
),
inserted_permissions AS (
    -- Write permissions to the main lookup table safely
    INSERT INTO permissions (url_pattern, http_method, required_permission, sort_order)
    SELECT column1, column2, column3, column4 FROM dynamic_permissions
    ON CONFLICT (url_pattern, http_method) DO UPDATE 
    SET required_permission = EXCLUDED.required_permission, sort_order = EXCLUDED.sort_order
    RETURNING id, required_permission
)
-- 3. Bulk Link Permissions to Roles safely without creating duplicates
INSERT INTO roles_permissions (role_id, permission_id)

SELECT links.role_id, links.permission_id
FROM (
    -- Tier 1: ROLE_USER_DEFAULT gets only public and view-only permissions
    SELECT r.id AS role_id, p.id AS permission_id FROM roles r, inserted_permissions p 
    WHERE r.name = 'USER_DEFAULT' AND p.required_permission IN ('permit_all', 'user:read')

    UNION ALL

    -- Tier 2: ROLE_USER gets everything above + full write/interaction permissions
    SELECT r.id AS role_id, p.id AS permission_id FROM roles r, inserted_permissions p 
    WHERE r.name = 'USER' AND p.required_permission IN ('permit_all', 'user:read', 'user:write')

    UNION ALL

    -- Tier 3: ROLE_ADMIN gets everything above + baseline user bans, reporting review, and admin metrics
    SELECT r.id AS role_id, p.id AS permission_id FROM roles r, inserted_permissions p 
    WHERE r.name = 'ADMIN' AND p.required_permission IN ('permit_all', 'user:read', 'user:write', 'admin:manage', 'admin:analytics')

    UNION ALL

    -- Tier 4: ROLE_SUPER_ADMIN gets absolute full access to the platform (all rows mapped)
    SELECT r.id AS role_id, p.id AS permission_id FROM roles r, inserted_permissions p 
    WHERE r.name = 'SUPER_ADMIN'
) links
-- The Safety Guard: Only insert if this specific pairing does not already exist
WHERE NOT EXISTS (
    SELECT 1 
    FROM roles_permissions rp 
    WHERE rp.role_id = links.role_id 
      AND rp.permission_id = links.permission_id
);