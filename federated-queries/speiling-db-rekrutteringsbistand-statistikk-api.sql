SET @@location = 'europe-north1';

FOR t IN (
  SELECT * FROM EXTERNAL_QUERY("toi-prod-324e.europe-north1.rekrutteringsbistand-statistikk-api", '''
    SELECT table_name,
           string_agg(
             CASE WHEN data_type IN ('uuid','json','jsonb','USER-DEFINED')
                  THEN quote_ident(column_name) || '::text'
                  ELSE quote_ident(column_name) END
             || ' AS ' ||
             replace(replace(replace(column_name,'æ','ae'),'ø','o'),'å','aa'),
             ', ' ORDER BY ordinal_position) AS kolonner
    FROM information_schema.columns
    WHERE table_name IN ('kandidatliste','kandidatutfall','stilling','visning_kontaktinfo')
    GROUP BY table_name''')
)

DO
EXECUTE IMMEDIATE FORMAT("""
    CREATE OR REPLACE TABLE `toi-prod-324e.rekrutteringsbistand_statistikk_api.%s` AS
    SELECT * FROM EXTERNAL_QUERY("toi-prod-324e.europe-north1.rekrutteringsbistand-statistikk-api",
      '''SELECT %s FROM %s''')""",
    t.table_name, t.kolonner, t.table_name);
END FOR;
