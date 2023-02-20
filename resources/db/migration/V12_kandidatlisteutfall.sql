CREATE TABLE kandidatlisteutfall (
    id SERIAL PRIMARY KEY,
    stillingsid TEXT,
    utfall TEXT,
    navident TEXT,
    kandidatlisteid TEXT,
    tidspunkt timestamp,
    er_direktemeldt BOOLEAN,
    antall_stillinger INT,
    stilling_opprettet_tidspunkt timestamp
);
