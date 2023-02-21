CREATE TABLE kandidatliste (
    id SERIAL PRIMARY KEY,
    stillingsid TEXT,
    navident TEXT,
    kandidatlisteid TEXT,
    tidspunkt timestamp,
    er_direktemeldt BOOLEAN,
    antall_stillinger INT,
    stilling_opprettet_tidspunkt timestamp
);
