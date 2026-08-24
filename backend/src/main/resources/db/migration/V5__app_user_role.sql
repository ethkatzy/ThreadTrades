-- Authorization foundation for the admin console: a role on the auth identity
-- table (app_user, not user_profile -- see AppUser.java javadoc). No column here
-- grants ADMIN; promotion is a manual SQL update against this column, not an
-- application code path (see ROADMAP.md notes).
ALTER TABLE app_user
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER'
        CHECK (role IN ('USER', 'ADMIN'));
