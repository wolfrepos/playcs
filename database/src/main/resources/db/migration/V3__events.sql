
alter table balance
rename column seconds to minutes;

update balance set minutes = minutes / 60 + 1;

create table server (
  chat_id bigint primary key,
  pass varchar not null,
  map varchar not null
);
