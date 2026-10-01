ALTER TABLE "CourseLectures"
    ADD COLUMN IF NOT EXISTS "ContentSource" text;

ALTER TABLE "CourseLectures"
    ADD COLUMN IF NOT EXISTS "ContentFormat" varchar(32) NOT NULL DEFAULT 'markdown';

ALTER TABLE "CourseLectures"
    ADD COLUMN IF NOT EXISTS "ContentSchemaVersion" integer NOT NULL DEFAULT 1;

ALTER TABLE "CourseLectures"
DROP CONSTRAINT IF EXISTS "CK_CourseLectures_ContentSchemaVersion";

ALTER TABLE "CourseLectures"
    ADD CONSTRAINT "CK_CourseLectures_ContentSchemaVersion" CHECK ("ContentSchemaVersion" > 0);
