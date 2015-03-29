# --- !Ups

create table user_ssh_key (
  public_key                 varchar(588) not null,
  user_id                    bigint,
  comment                    varchar(255),
  finger_print               varchar(255),
  register_date              timestamp,
  last_used_date             timestamp,
  constraint pk_user_ssh_key primary key (public_key))
;

create sequence user_ssh_key_seq;
alter table user_ssh_key add constraint fk_user_ssh_key_user_1 foreign key (user_id) references n4user (id) on delete restrict on update restrict;
create index ix_user_ssh_key_user_1 on user_ssh_key (user_id);

# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;
drop table if exists user_ssh_key;
SET REFERENTIAL_INTEGRITY TRUE;
drop sequence if exists user_ssh_key_seq;
