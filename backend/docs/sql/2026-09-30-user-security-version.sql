-- Required before starting the production profile with Hibernate ddl-auto=validate.
-- Existing access tokens without this claim behave as security version 0.
ALTER TABLE "Users"
    ADD COLUMN IF NOT EXISTS "SecurityVersion" integer NOT NULL DEFAULT 0;
