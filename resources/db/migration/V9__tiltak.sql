CREATE TABLE tiltak (
    id            SERIAL PRIMARY KEY,
    avtaleid      TEXT UNIQUE,
    aktorid       TEXT,
    fnr           TEXT,
    navkontor     TEXT,
    tiltakstype    TEXT,
    avtaleInngått timestamp
);

