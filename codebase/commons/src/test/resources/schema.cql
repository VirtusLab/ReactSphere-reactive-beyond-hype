-- for singular cassandra node use this:
-- CREATE KEYSPACE microservices
--   WITH REPLICATION = {
--      'class' : 'SimpleStrategy', 'replication_factor' : 1
--   };

-- for cluster with at least 2 nodes use this:
-- CREATE KEYSPACE microservices
--   WITH REPLICATION = {
--      'class' : 'SimpleStrategy', 'replication_factor' : 2
--   };

CREATE TABLE microservices.accounts (
    username text,
    password text,
    PRIMARY KEY (username)
);

CREATE TABLE microservices.tokens (
    bearer_token text,
    username text,
    expires_at timestamp,
    PRIMARY KEY (bearer_token)
);

CREATE TABLE microservices.auctions (
    category text,
    created_at timestamp,
    auction_id uuid,
    owner text,
    title text,
    description text,
    details text,
    minimum_price decimal,
    PRIMARY KEY (category, created_at, auction_id)
) WITH CLUSTERING ORDER BY (created_at DESC);

CREATE MATERIALIZED VIEW microservices.auctions_view AS
    SELECT * FROM microservices.auctions
    WHERE auction_id IS NOT NULL AND created_at IS NOT NULL AND category IS NOT NULL
    PRIMARY KEY (auction_id, created_at, category);

CREATE TABLE microservices.bids (
    auction_id uuid,
    bid_id timeuuid,
    bidder text,
    amount decimal,
    PRIMARY KEY (auction_id, bid_id)
);