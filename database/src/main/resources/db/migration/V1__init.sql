
create table payment (
  id bigserial primary key,
  rubles bigint not null,
  telegram_id bigint not null,
  charge_time timestamp with time zone not null
);

create table balance (
  telegram_id bigint primary key,
  seconds bigint not null
);
