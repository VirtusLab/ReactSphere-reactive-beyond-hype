-- CREATE  KEYSPACE microservices
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
    auction_id timeuuid,
    owner text,
    title text,
    description text,
    details text,
    minimum_price decimal,
    PRIMARY KEY (auction_id)
);

CREATE TABLE microservices.bids (
    auction_id timeuuid,
    bid_id timeuuid,
    bidder text,
    amount decimal,
    PRIMARY KEY ((auction_id, bid_id), bidder)
) WITH CLUSTERING ORDER BY (bidder DESC);