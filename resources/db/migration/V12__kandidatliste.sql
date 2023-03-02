create table kandidatliste (
    id serial primary key,
    stillings_id text,
    kandidatliste_id text,
    er_direktemeldt boolean,
    antall_stillinger int,
    antall_kandidater int,
    stilling_opprettet_tidspunkt timestamp with time zone not null,
    stillingens_publiseringstidspunkt timestamp with time zone not null,
    organisasjonsnummer text,
    utført_av_nav_ident text,
    tidspunkt_for_hendelsen timestamp with time zone not null,
    event_name text
);
