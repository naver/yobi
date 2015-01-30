# --- !Ups

create table pending_data (
  id                        bigint not null,
  data_type                 varchar(255),
  data_operation            varchar(255),
  data_id                   varchar(255),
  constraint pk_pending_data primary key (id))
;

create sequence pending_data_seq;

# --- !Downs

drop table if exists pending_data;

drop sequence if exists pending_data_seq;
