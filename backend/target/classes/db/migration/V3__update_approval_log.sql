ALTER TABLE approval_log
  DROP COLUMN approver_id,
  DROP COLUMN action_time,
  DROP COLUMN comments,
  ADD COLUMN actor VARCHAR(255),
  ADD COLUMN confidence DOUBLE,
  ADD COLUMN rationale TEXT,
  ADD COLUMN source VARCHAR(50),
  RENAME COLUMN action_time TO created_at;