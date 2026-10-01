ALTER TABLE "QuestionResponses"
    ADD COLUMN IF NOT EXISTS "GradingStatus" varchar(20) DEFAULT 'GRADED';
ALTER TABLE "QuestionResponses"
    ADD COLUMN IF NOT EXISTS "GradingVersion" integer NOT NULL DEFAULT 0;
ALTER TABLE "QuestionResponses"
    ADD COLUMN IF NOT EXISTS "GradingUpdatedAtUtc" timestamp with time zone;
ALTER TABLE "QuestionResponses"
    ADD COLUMN IF NOT EXISTS "GradingError" varchar(1000);

UPDATE "QuestionResponses"
SET "GradingStatus" = 'GRADED'
WHERE "GradingStatus" IS NULL;

ALTER TABLE "TestAttempts"
    ADD COLUMN IF NOT EXISTS "GradingStatus" varchar(20) DEFAULT 'GRADED';
ALTER TABLE "TestAttempts"
    ADD COLUMN IF NOT EXISTS "GradingUpdatedAtUtc" timestamp with time zone;

UPDATE "TestAttempts"
SET "GradingStatus" = 'GRADED'
WHERE "GradingStatus" IS NULL;

CREATE TABLE IF NOT EXISTS "TextAnswerGradingJobs" (
    "Id" bigserial PRIMARY KEY,
    "QuestionResponseId" bigint NOT NULL REFERENCES "QuestionResponses" ("Id") ON DELETE CASCADE,
    "ResponseVersion" integer NOT NULL,
    "Status" varchar(20) NOT NULL,
    "Attempts" integer NOT NULL DEFAULT 0,
    "NextAttemptAtUtc" timestamp with time zone,
    "LastError" varchar(1000),
    "CreatedAtUtc" timestamp with time zone NOT NULL,
    "UpdatedAtUtc" timestamp with time zone NOT NULL,
    CONSTRAINT "UQ_TextAnswerGradingJobs_Response" UNIQUE ("QuestionResponseId")
);

CREATE INDEX IF NOT EXISTS "IX_TextAnswerGradingJobs_Claim"
    ON "TextAnswerGradingJobs" ("Status", "NextAttemptAtUtc", "UpdatedAtUtc");
