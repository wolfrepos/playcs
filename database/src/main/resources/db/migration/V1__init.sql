
create table admin (
  chat_id bigint primary key
);

create table server (
  chat_id bigint primary key,
  pass varchar not null,
  map varchar not null
);

create table balance (
  chat_id bigint primary key,
  minutes bigint not null
);

create table will (
  user_id bigint not null,
  chat_id bigint not null,
  "start" timestamp with time zone not null,
  "end" timestamp with time zone not null
);
