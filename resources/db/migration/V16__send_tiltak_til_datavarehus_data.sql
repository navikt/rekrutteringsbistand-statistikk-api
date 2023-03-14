ALTER TABLE tiltak ADD sendt_status TEXT DEFAULT 'IKKE_SENDT' NOT NULL;
ALTER TABLE tiltak ADD antall_sendt_forsok SMALLINT DEFAULT 0 NOT NULL;
ALTER TABLE tiltak ADD siste_sendt_forsok TIMESTAMP WITH TIME ZONE;

UPDATE tiltak SET sendt_status = 'IKKE_SENDT';
UPDATE tiltak SET antall_sendt_forsok = 0;