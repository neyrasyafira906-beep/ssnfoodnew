package com.ssn.food.model;

public class FoodItem {
    private final String id;  // ← ditambah final
    private String name, emoji, videoUrl = "", description = "";
    private long price;
    private int stock;
    private javax.swing.ImageIcon imageIcon = null;

    public FoodItem(String id, String name, long price, int stock, String emoji) {
        this.id = id; this.name = name; this.price = price;
        this.stock = stock; this.emoji = emoji;
    }

    public String getId()           { return id; }
    public String getName()         { return name; }
    public void   setName(String n) { this.name = n; }
    public long   getPrice()        { return price; }
    public void   setPrice(long p)  { this.price = p; }
    public int    getStock()        { return stock; }
    public void   setStock(int s)   { this.stock = s; }
    public String getEmoji()        { return emoji; }
    public void   setEmoji(String e){ this.emoji = e; }
    public String getVideoUrl()     { return videoUrl; }
    public void   setVideoUrl(String v){ this.videoUrl = v; }
    public String getDescription()  { return description; }
    public void   setDescription(String d){ this.description = d; }
    public javax.swing.ImageIcon getImageIcon()  { return imageIcon; }
    public void   setImageIcon(javax.swing.ImageIcon i){ this.imageIcon = i; }
    public String formatPrice()     { return "Rp " + String.format("%,d", price).replace(',','.'); }
    
    @Override  // ← ditambah @Override
    public String toString()        { return emoji + " " + name + " - " + formatPrice(); }
}