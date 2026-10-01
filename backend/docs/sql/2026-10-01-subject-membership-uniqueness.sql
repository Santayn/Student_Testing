-- Production migration for the SubjectMembership invariant used by idempotent
-- teacher reassignment. Suspended-but-not-removed memberships deliberately
-- participate in the uniqueness rule so they can be reactivated instead of
-- duplicated.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM "SubjectMemberships"
        WHERE "RemovedAtUtc" IS NULL
        GROUP BY "SubjectId", "PersonId", "Role"
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION
            'Cannot create IX_SubjectMemberships_Subject_Person_Role_NotRemoved: duplicate non-removed SubjectMembership rows exist';
    END IF;
END
$$;

CREATE UNIQUE INDEX IF NOT EXISTS "IX_SubjectMemberships_Subject_Person_Role_NotRemoved"
    ON "SubjectMemberships" ("SubjectId", "PersonId", "Role")
    WHERE "RemovedAtUtc" IS NULL;
