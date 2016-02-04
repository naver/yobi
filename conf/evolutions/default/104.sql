# --- !Ups
ALTER TABLE milestone ADD COLUMN pull_request_id BIGINT;

ALTER TABLE pull_request ADD COLUMN milestone_id BIGINT;

# --- !Downs
ALTER TABLE milestone DROP COLUMN pull_request_id;

ALTER TABLE pull_request DROP COLUMN milestone_id;