CREATE TABLE kandidatutfall (
    id SERIAL PRIMARY KEY,
    aktorid TEXT,
    utfall TEXT,
    navident TEXT,
    navkontor TEXT,
    kandidatlisteid TEXT,
    stillingsid TEXT,
    tidspunkt timestamp
);
