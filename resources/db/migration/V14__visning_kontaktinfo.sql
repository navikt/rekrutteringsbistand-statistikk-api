create table visning_kontaktinfo (
    id          bigserial primary key,
    aktør_id    varchar(13)              not null,
    stilling_id uuid                     not null,
    tidspunkt   timestamp with time zone not null default current_timestamp
)