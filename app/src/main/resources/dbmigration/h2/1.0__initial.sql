create table url (
  id                            bigint PRIMARY KEY generated by default as identity not null,
  name                          varchar(255),
  created_at                    timestamp not null,
);

