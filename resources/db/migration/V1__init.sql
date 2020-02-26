CREATE TABLE kandidatutfall (
    id SERIAL PRIMARY KEY,
    aktorid TEXT,
    utfall TEXT,
    navident TEXT,
    enhetsnr TEXT,
    tidspunkt TIMESTAMP
);
