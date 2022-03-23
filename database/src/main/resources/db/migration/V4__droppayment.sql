
drop table payment;

alter table balance
rename column telegram_id to chat_id;

alter table admin
rename column telegram_id to chat_id;
