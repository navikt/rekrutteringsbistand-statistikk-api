CREATE TABLE stilling (
    id SERIAL PRIMARY KEY,
    uuid TEXT,
    publiseringsdato timestamp,
    inkluderingsmuligheter TEXT,
    prioritertemålgrupper TEXT,
    tiltakellervirkemidler TEXT,
    tidspunkt timestamp
);

/*
 TODO: Dumt at "stillingid" i kandidatutfalltabellen går mot UUID og ikke denne ID-en
 */
