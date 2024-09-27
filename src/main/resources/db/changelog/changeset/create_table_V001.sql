CREATE TABLE IF NOT EXISTS client
(
    id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR NOT NULL
);