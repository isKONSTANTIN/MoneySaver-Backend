create table if not exists users
(
    id            serial primary key,
    email         text not null,
    password      text not null,
    receipt_token text not null
);

alter table users
    owner to money_saver;

create unique index if not exists users_email_uindex
    on users (email);

create table if not exists transactions
(
    id          serial primary key,
    "user"      integer                                                 not null,
    delta       double precision                                        not null,
    tag         integer                                                 not null,
    date        timestamp                                               not null,
    account     integer                                                 not null,
    description text
);

alter table transactions
    owner to money_saver;

create table if not exists tags
(
    id      serial primary key,
    "user"  integer          not null,
    name    text             not null,
    kind    integer          not null,
    "limit" double precision not null
);

alter table tags
    owner to money_saver;

create table if not exists repeat_transactions
(
    id          serial primary key,
    "user"      integer          not null,
    tag         integer          not null,
    delta       double precision not null,
    account     integer          not null,
    arg         integer          not null,
    last_repeat timestamp,
    next_repeat timestamp,
    repeat_func integer          not null,
    description text             not null
);

alter table repeat_transactions
    owner to money_saver;

create table if not exists plans
(
    id          serial primary key,
    "user"      integer          not null,
    delta       double precision not null,
    tag         integer          not null,
    date        timestamp        not null,
    account     integer          not null,
    description text             not null,
    state       integer          not null
);

alter table plans
    owner to money_saver;

create table if not exists accounts
(
    id     serial primary key,
    "user" integer          not null,
    name   text             not null,
    amount double precision not null
);

alter table accounts
    owner to money_saver;

create table if not exists services_data
(
    service  text not null,
    var_name text not null,
    data     text not null
);

alter table services_data
    owner to money_saver;

create table if not exists users_notifications
(
    id       serial primary key,
    user_id  integer not null,
    endpoint text    not null,
    auth     text    not null,
    p256dh   text    not null
);

alter table users_notifications
    owner to money_saver;

create table if not exists users_sessions
(
    id         serial primary key,
    "user"     integer                                                   not null,
    session    uuid                                                      not null,
    expired_at timestamp                                                 not null
);

alter table users_sessions
    owner to money_saver;

create unique index if not exists user_sessions_id_uindex
    on users_sessions (id);

create unique index if not exists user_sessions_session_uindex
    on users_sessions (session);

