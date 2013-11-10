# --- !Ups

ALTER TABLE project ADD COLUMN default_branch varchar(255);

# --- !Downs

ALTER TABLE project DROP COLUMN default_branch;
