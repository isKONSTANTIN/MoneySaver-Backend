create table user_registrations
(
    id                serial primary key,
    "user"            int       not null,
    registration_time timestamp not null,
    expires_in        timestamp not null,
    demo_account      boolean   not null
);

create unique index user_registrations_user_uindex
    on user_registrations ("user");

