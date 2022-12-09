CREATE TABLE tiltak (
    id            SERIAL PRIMARY KEY,
    avtaleId      TEXT UNIQUE,
    deltakerAktørId TEXT,
    deltakerFnr   TEXT,
    enhetOppfolging     TEXT,
    tiltakstype    TEXT,
    avtaleInngått timestamp with time zone
);

