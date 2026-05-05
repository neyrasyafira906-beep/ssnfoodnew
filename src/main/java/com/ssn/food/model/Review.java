package com.ssn.food.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Review {
    private String id;
    private String sellerId;
    private String buyerName;
    private int rating;
    private String comment;
    private String timestamp;

    public Review(String sellerId, String buyerName, int rating, String comment) {
        this.id = "REV-" + System.currentTimeMillis();
        this.sellerId = sellerId;
        this.buyerName = buyerName;
        this.rating = rating;
        this.comment = comment;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSellerId() { return sellerId; }
    public String getBuyerName() { return buyerName; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public String getTimestamp() { return timestamp; }
}
