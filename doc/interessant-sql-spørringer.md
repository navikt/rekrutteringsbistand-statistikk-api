# Interessante SQL-spørringer

Antall unike kandidater: `SELECT COUNT(DISTINCT aktorid) FROM kandidatutfall;`
Antall registrertinger totalt: `SELECT COUNT(*) FROM kandidatutfall;`

Hvilke kandidater er registrert med samme statuser flere ganger:
```
SELECT
    aktorid, utfall, COUNT(*)
FROM
    kandidatutfall 
GROUP BY
    aktorid, utfall
HAVING 
    COUNT(*) > 1;
```

Antall kandidater som er registrert med samme statuser flere ganger:
```
SELECT
    COUNT(*)
FROM (
    SELECT
        aktorid, utfall, COUNT(*)
    FROM
        kandidatutfall 
    GROUP BY
        aktorid, utfall
    HAVING 
        COUNT(*) > 1
    ) as bla;
```
