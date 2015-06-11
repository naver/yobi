# --- !Ups

ALTER TABLE posting_comment ADD COLUMN parent_id bigint;

# --- !Downs

