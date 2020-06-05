ALTER TABLE kandidatutfall DROP COLUMN enhetsnr;
ALTER TABLE kandidatutfall ADD COLUMN navkontor TEXT;
ALTER TABLE kandidatutfall ADD COLUMN kandidatlisteid TEXT;
ALTER TABLE kandidatutfall ADD COLUMN stillingsid TEXT;
ALTER TABLE kandidatutfall ADD COLUMN tidspunkt timestamp;
