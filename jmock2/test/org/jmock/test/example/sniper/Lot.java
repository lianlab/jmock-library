package org.jmock.test.example.sniper;

public interface Lot {
    public void bid(Bid amount) throws AuctionException;
}