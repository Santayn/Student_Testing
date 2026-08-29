CREATE EXTENSION IF NOT EXISTS citext;

CREATE TABLE IF NOT EXISTS "FacultySubjects" (
                                                 "Id" serial PRIMARY KEY,
                                                 "FacultyId" integer NOT NULL REFERENCES "Faculties" ("Id") ON DELETE CASCADE,
    "SubjectId" integer NOT NULL REFERENCES "Subjects" ("Id") ON DELETE CASCADE
    );

CREATE UNIQUE INDEX IF NOT EXISTS "IX_FacultySubjects_Faculty_Subject"
    ON "FacultySubjects" ("FacultyId", "SubjectId");

CREATE TABLE IF NOT EXISTS "LectureTopics" (
                                               "Id" serial PRIMARY KEY,
                                               "CourseLectureId" integer NOT NULL REFERENCES "CourseLectures" ("Id") ON DELETE CASCADE,
    "Ordinal" integer NOT NULL DEFAULT 1 CHECK ("Ordinal" >= 1),
    "Name" varchar(200) NOT NULL,
    "Description" varchar(2000)
    );

ALTER TABLE "LectureTopics"
    ADD COLUMN IF NOT EXISTS "SubjectId" integer;

ALTER TABLE "LectureTopics"
    ADD COLUMN IF NOT EXISTS "SubjectMembershipId" integer;

UPDATE "LectureTopics" topic
SET "SubjectId" = course_template."SubjectId"
    FROM "CourseLectures" lecture
JOIN "CourseVersions" course_version
ON course_version."Id" = lecture."CourseVersionId"
    JOIN "CourseTemplates" course_template
    ON course_template."Id" = course_version."CourseTemplateId"
WHERE topic."SubjectId" IS NULL
  AND topic."CourseLectureId" = lecture."Id";

WITH normalized_topic_ordinals AS (
    SELECT topic."Id",
           row_number() OVER (
               PARTITION BY topic."SubjectId"
               ORDER BY topic."Ordinal", topic."Id"
           ) AS new_ordinal
    FROM "LectureTopics" topic
    WHERE topic."SubjectId" IS NOT NULL
)
UPDATE "LectureTopics" topic
SET "Ordinal" = normalized_topic_ordinals.new_ordinal
    FROM normalized_topic_ordinals
WHERE topic."Id" = normalized_topic_ordinals."Id"
  AND topic."Ordinal" <> normalized_topic_ordinals.new_ordinal;

ALTER TABLE "LectureTopics"
    ALTER COLUMN "CourseLectureId" DROP NOT NULL;

ALTER TABLE "LectureTopics"
    ALTER COLUMN "SubjectId" SET NOT NULL;

ALTER TABLE "LectureTopics"
DROP CONSTRAINT IF EXISTS "FK_LectureTopics_Subjects";

ALTER TABLE "LectureTopics"
    ADD CONSTRAINT "FK_LectureTopics_Subjects"
        FOREIGN KEY ("SubjectId") REFERENCES "Subjects" ("Id") ON DELETE CASCADE;

ALTER TABLE "LectureTopics"
DROP CONSTRAINT IF EXISTS "FK_LectureTopics_SubjectMemberships";

ALTER TABLE "LectureTopics"
    ADD CONSTRAINT "FK_LectureTopics_SubjectMemberships"
        FOREIGN KEY ("SubjectMembershipId") REFERENCES "SubjectMemberships" ("Id") ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS "IX_LectureTopics_CourseLectureId"
    ON "LectureTopics" ("CourseLectureId");

CREATE INDEX IF NOT EXISTS "IX_LectureTopics_SubjectId"
    ON "LectureTopics" ("SubjectId");

CREATE INDEX IF NOT EXISTS "IX_LectureTopics_SubjectMembershipId"
    ON "LectureTopics" ("SubjectMembershipId");

DROP INDEX IF EXISTS "IX_LectureTopics_Lecture_Ordinal";
DROP INDEX IF EXISTS "IX_LectureTopics_Subject_Ordinal";

CREATE UNIQUE INDEX IF NOT EXISTS "IX_LectureTopics_Lecture_Ordinal"
    ON "LectureTopics" ("CourseLectureId", "Ordinal")
    WHERE "CourseLectureId" IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS "IX_LectureTopics_SubjectMembership_Ordinal"
    ON "LectureTopics" ("SubjectMembershipId", "Ordinal")
    WHERE "SubjectMembershipId" IS NOT NULL;

ALTER TABLE "TestQuestions"
    ADD COLUMN IF NOT EXISTS "CourseLectureId" integer;

ALTER TABLE "TestQuestions"
    ADD COLUMN IF NOT EXISTS "TopicId" integer;

ALTER TABLE "TestQuestions"
ALTER COLUMN "CorrectAnswer" TYPE varchar(4000);

ALTER TABLE "TestQuestions"
    ALTER COLUMN "TestId" DROP NOT NULL;

CREATE TABLE IF NOT EXISTS "TestQuestionSelectionRules" (
                                                            "Id" bigserial PRIMARY KEY,
                                                            "TestId" integer NOT NULL REFERENCES "Tests" ("Id") ON DELETE CASCADE,
    "CourseLectureId" integer REFERENCES "CourseLectures" ("Id") ON DELETE RESTRICT,
    "TopicId" integer,
    "QuestionCount" integer NOT NULL DEFAULT 1 CHECK ("QuestionCount" >= 1),
    "TextQuestionCount" integer NOT NULL DEFAULT 0,
    "SingleAnswerQuestionCount" integer NOT NULL DEFAULT 0,
    "MultipleAnswerQuestionCount" integer NOT NULL DEFAULT 0,
    "MatchingQuestionCount" integer NOT NULL DEFAULT 0,
    "Ordinal" integer NOT NULL DEFAULT 1 CHECK ("Ordinal" >= 1),
    CONSTRAINT "CK_TestQuestionSelectionRules_TypeCounts" CHECK (
                                                                    "TextQuestionCount" >= 0
                                                                    AND "SingleAnswerQuestionCount" >= 0
                                                                    AND "MultipleAnswerQuestionCount" >= 0
                                                                    AND "MatchingQuestionCount" >= 0
                                                                    AND ("TextQuestionCount" + "SingleAnswerQuestionCount" + "MultipleAnswerQuestionCount" + "MatchingQuestionCount") <= "QuestionCount"
    )
    );


ALTER TABLE "TestQuestionSelectionRules"
    ADD COLUMN IF NOT EXISTS "MatchingQuestionCount" integer NOT NULL DEFAULT 0;

ALTER TABLE "TestQuestionSelectionRules"
DROP CONSTRAINT IF EXISTS "CK_TestQuestionSelectionRules_TypeCounts";

ALTER TABLE "TestQuestionSelectionRules"
    ADD CONSTRAINT "CK_TestQuestionSelectionRules_TypeCounts" CHECK (
        "TextQuestionCount" >= 0
            AND "SingleAnswerQuestionCount" >= 0
            AND "MultipleAnswerQuestionCount" >= 0
            AND "MatchingQuestionCount" >= 0
            AND ("TextQuestionCount" + "SingleAnswerQuestionCount" + "MultipleAnswerQuestionCount" + "MatchingQuestionCount") <= "QuestionCount"
        );

ALTER TABLE "TestQuestionSelectionRules"
    ADD COLUMN IF NOT EXISTS "TopicId" integer;

ALTER TABLE "TestQuestionSelectionRules"
    ALTER COLUMN "CourseLectureId" DROP NOT NULL;

ALTER TABLE "CourseLectures"
    ADD COLUMN IF NOT EXISTS "SubjectId" integer;

ALTER TABLE "CourseLectures"
    ADD COLUMN IF NOT EXISTS "SubjectMembershipId" integer;

UPDATE "CourseLectures" lecture
SET "SubjectId" = course_template."SubjectId"
    FROM "CourseVersions" course_version
JOIN "CourseTemplates" course_template
ON course_template."Id" = course_version."CourseTemplateId"
WHERE lecture."SubjectId" IS NULL
  AND lecture."CourseVersionId" = course_version."Id";

UPDATE "CourseLectures" lecture
SET "SubjectMembershipId" = membership."Id"
    FROM "CourseVersions" course_version
JOIN "CourseTemplates" course_template
ON course_template."Id" = course_version."CourseTemplateId"
    JOIN "SubjectMemberships" membership
    ON membership."SubjectId" = course_template."SubjectId"
    AND membership."PersonId" = course_template."AuthorPersonId"
    AND membership."Role" = 1
    AND membership."RemovedAtUtc" IS NULL
WHERE lecture."SubjectMembershipId" IS NULL
  AND lecture."CourseVersionId" = course_version."Id";

ALTER TABLE "CourseLectures"
    ALTER COLUMN "CourseVersionId" DROP NOT NULL;

ALTER TABLE "CourseLectures"
DROP CONSTRAINT IF EXISTS "FK_CourseLectures_Subjects";

ALTER TABLE "CourseLectures"
    ADD CONSTRAINT "FK_CourseLectures_Subjects"
        FOREIGN KEY ("SubjectId") REFERENCES "Subjects" ("Id") ON DELETE CASCADE;

ALTER TABLE "CourseLectures"
DROP CONSTRAINT IF EXISTS "FK_CourseLectures_SubjectMemberships";

ALTER TABLE "CourseLectures"
    ADD CONSTRAINT "FK_CourseLectures_SubjectMemberships"
        FOREIGN KEY ("SubjectMembershipId") REFERENCES "SubjectMemberships" ("Id") ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS "IX_CourseLectures_SubjectId"
    ON "CourseLectures" ("SubjectId");

CREATE INDEX IF NOT EXISTS "IX_CourseLectures_SubjectMembershipId"
    ON "CourseLectures" ("SubjectMembershipId");

CREATE UNIQUE INDEX IF NOT EXISTS "IX_CourseLectures_SubjectMembership_Ordinal"
    ON "CourseLectures" ("SubjectMembershipId", "Ordinal")
    WHERE "SubjectMembershipId" IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS "IX_CourseLectures_CourseVersion_Ordinal"
    ON "CourseLectures" ("CourseVersionId", "Ordinal")
    WHERE "CourseVersionId" IS NOT NULL;

ALTER TABLE "CourseLectures"
    ADD COLUMN IF NOT EXISTS "LinkedTestId" integer REFERENCES "Tests" ("Id") ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS "LectureTestLinks" (
                                                  "Id" serial PRIMARY KEY,
                                                  "CourseLectureId" integer NOT NULL REFERENCES "CourseLectures" ("Id") ON DELETE CASCADE,
    "TestId" integer NOT NULL REFERENCES "Tests" ("Id") ON DELETE CASCADE
    );

CREATE UNIQUE INDEX IF NOT EXISTS "IX_LectureTestLinks_Lecture_Test"
    ON "LectureTestLinks" ("CourseLectureId", "TestId");

CREATE INDEX IF NOT EXISTS "IX_LectureTestLinks_TestId"
    ON "LectureTestLinks" ("TestId");

INSERT INTO "LectureTestLinks" ("CourseLectureId", "TestId")
SELECT lecture."Id", lecture."LinkedTestId"
FROM "CourseLectures" lecture
WHERE lecture."LinkedTestId" IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM "LectureTestLinks" link
    WHERE link."CourseLectureId" = lecture."Id"
      AND link."TestId" = lecture."LinkedTestId"
);

INSERT INTO "LectureTestLinks" ("CourseLectureId", "TestId")
SELECT assignment."CourseLectureId", assignment."TestId"
FROM "TestAssignments" assignment
WHERE assignment."CourseLectureId" IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM "LectureTestLinks" link
    WHERE link."CourseLectureId" = assignment."CourseLectureId"
      AND link."TestId" = assignment."TestId"
);

CREATE TABLE IF NOT EXISTS "LectureMaterials" (
                                                  "Id" serial PRIMARY KEY,
                                                  "CourseLectureId" integer NOT NULL REFERENCES "CourseLectures" ("Id") ON DELETE CASCADE,
    "FileName" varchar(255) NOT NULL,
    "StoredPath" varchar(512) NOT NULL,
    "ContentType" varchar(255),
    "SizeBytes" bigint NOT NULL DEFAULT 0,
    "UploadedAtUtc" timestamptz NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS "IX_LectureMaterials_LectureId"
    ON "LectureMaterials" ("CourseLectureId");

ALTER TABLE "LectureAssignments"
    ALTER COLUMN "SnapshotCourseVersionId" DROP NOT NULL;

ALTER TABLE "TestAssignments"
    ADD COLUMN IF NOT EXISTS "TeachingAssignmentId" integer REFERENCES "TeachingAssignments" ("Id") ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS "IX_TestQuestions_CourseLectureId"
    ON "TestQuestions" ("CourseLectureId");

CREATE INDEX IF NOT EXISTS "IX_TestQuestions_TopicId"
    ON "TestQuestions" ("TopicId");

CREATE UNIQUE INDEX IF NOT EXISTS "IX_TestQuestions_Topic_Ordinal"
    ON "TestQuestions" ("TopicId", "Ordinal")
    WHERE "TopicId" IS NOT NULL AND "TestId" IS NULL;

CREATE INDEX IF NOT EXISTS "IX_TestQuestionSelectionRules_TestId"
    ON "TestQuestionSelectionRules" ("TestId");

CREATE INDEX IF NOT EXISTS "IX_TestQuestionSelectionRules_TopicId"
    ON "TestQuestionSelectionRules" ("TopicId");

CREATE INDEX IF NOT EXISTS "IX_TestAssignments_TeachingAssignmentId"
    ON "TestAssignments" ("TeachingAssignmentId");

ALTER TABLE "TeachingAssignments"
    ADD COLUMN IF NOT EXISTS "StudyCourse" integer;

CREATE INDEX IF NOT EXISTS "IX_TeachingAssignments_StudyCourse"
    ON "TeachingAssignments" ("StudyCourse");

DROP INDEX IF EXISTS "IX_TestQuestionSelectionRules_Test_Lecture";

CREATE UNIQUE INDEX IF NOT EXISTS "IX_TestQuestionSelectionRules_Test_Lecture"
    ON "TestQuestionSelectionRules" ("TestId", "CourseLectureId")
    WHERE "TopicId" IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS "IX_TestQuestionSelectionRules_Test_Topic"
    ON "TestQuestionSelectionRules" ("TestId", "TopicId")
    WHERE "TopicId" IS NOT NULL;
