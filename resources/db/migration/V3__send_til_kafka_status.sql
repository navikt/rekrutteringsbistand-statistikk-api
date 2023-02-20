ALTER TABLE kandidatutfall ADD sendt_status TEXT DEFAULT 'IKKE_SENDT';
ALTER TABLE kandidatutfall ADD antall_sendt_forsok SMALLINT DEFAULT 0;
ALTER TABLE kandidatutfall ADD siste_sendt_forsok TIMESTAMP;

UPDATE kandidatutfall SET sendt_status = 'IKKE_SENDT';
UPDATE kandidatutfall SET antall_sendt_forsok = 0;

ALTER TABLE kandidatutfall ALTER COLUMN sendt_status SET NOT NULL;
ALTER TABLE kandidatutfall ALTER COLUMN antall_sendt_forsok SET NOT NULL;


ALTER TABLE kandidatlisteutfall ADD sendt_status TEXT DEFAULT 'IKKE_SENDT';
ALTER TABLE kandidatlisteutfall ADD antall_sendt_forsok SMALLINT DEFAULT 0;
ALTER TABLE kandidatlisteutfall ADD siste_sendt_forsok TIMESTAMP;

UPDATE kandidatlisteutfall SET sendt_status = 'IKKE_SENDT';
UPDATE kandidatlisteutfall SET antall_sendt_forsok = 0;

ALTER TABLE kandidatlisteutfall ALTER COLUMN sendt_status SET NOT NULL;
ALTER TABLE kandidatlisteutfall ALTER COLUMN antall_sendt_forsok SET NOT NULL;
