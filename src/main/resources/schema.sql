create table if not exists customer (
  customer_id bigint primary key, first_name varchar(80) not null, last_name varchar(80) not null,
  email varchar(160), phone varchar(30), customer_status varchar(20) not null, created_at timestamp default current_timestamp
);
create table if not exists card_product (
  product_id bigint primary key, product_name varchar(100) not null, card_type varchar(20) not null,
  annual_fee decimal(12,2), interest_rate decimal(5,2)
);
create table if not exists card (
  card_id bigint primary key, customer_id bigint not null, product_id bigint, card_number varchar(19) not null unique,
  card_type varchar(20) not null, card_status varchar(20) not null, issue_date date, expiry_date date,
  activation_date timestamp, credit_limit decimal(12,2), available_limit decimal(12,2),
  constraint fk_card_customer foreign key(customer_id) references customer(customer_id),
  constraint fk_card_product foreign key(product_id) references card_product(product_id)
);
create table if not exists card_transaction (
  transaction_id bigint primary key, card_id bigint not null, transaction_date timestamp not null, amount decimal(12,2) not null,
  currency varchar(3) not null, merchant_name varchar(120), transaction_type varchar(30), transaction_status varchar(20),
  constraint fk_transaction_card foreign key(card_id) references card(card_id)
);
create table if not exists card_block (
  block_id bigint auto_increment primary key, card_id bigint not null, block_reason varchar(30) not null,
  blocked_at timestamp default current_timestamp, blocked_by varchar(80) not null,
  constraint fk_block_card foreign key(card_id) references card(card_id)
);
create table if not exists card_replacement (
  replacement_id bigint auto_increment primary key, old_card_id bigint not null, new_card_id bigint,
  reason varchar(30) not null, requested_at timestamp default current_timestamp, replacement_status varchar(20) not null,
  constraint fk_replacement_old foreign key(old_card_id) references card(card_id)
);
create table if not exists card_limit_history (
  history_id bigint auto_increment primary key, card_id bigint not null, old_limit decimal(12,2), new_limit decimal(12,2),
  changed_at timestamp default current_timestamp, changed_by varchar(80) not null,
  constraint fk_history_card foreign key(card_id) references card(card_id)
);
