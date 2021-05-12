CREATE TABLE stilling (
    uuid VARCHAR(36),
    opprettet timestamp,
    publisert timestamp,
    inkluderingsmuligheter TEXT,
    prioritertemålgrupper TEXT,
    tiltakellervirkemidler TEXT,
    tidspunkt timestamp,
    PRIMARY KEY (uuid, tidspunkt)
);