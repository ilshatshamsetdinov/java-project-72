-- apply changes
create table url (
  id                            bigint generated by default as identity not null,
  name                          varchar(255),
  created_at                    timestamptz not null,
  constraint pk_url primary key (id)
);
