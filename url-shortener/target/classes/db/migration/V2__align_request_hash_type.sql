-- Align request_hash with the JPA mapping.
-- Safe for existing local databases created by V1.
ALTER TABLE url_shortener.short_url
    ALTER COLUMN request_hash TYPE varchar(64);
