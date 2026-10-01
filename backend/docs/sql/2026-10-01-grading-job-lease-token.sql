ALTER TABLE "TextAnswerGradingJobs"
    ADD COLUMN IF NOT EXISTS "ClaimToken" varchar(36);

UPDATE "TextAnswerGradingJobs"
SET "ClaimToken" = NULL
WHERE "Status" <> 'PROCESSING';
