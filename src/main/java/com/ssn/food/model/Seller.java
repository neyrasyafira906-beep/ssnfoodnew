package com.ssn.food.model;

import java.util.ArrayList;
import java.util.List;

public class Seller {
    private static int counter = 1;
    private final String id;
    private String name, phone, address, avatarEmoji = "\uD83C\uDFAA", videoUrl = "";
    private double lat, lng;
    private final List<FoodItem> menu = new ArrayList<>();
    private double rating = 0;
    private int ratingCount = 0;

    public Seller(String name, String phone, String address, double lat, double lng) {
        this.id = "S" + (counter++);
        this.name = name; this.phone = phone;
        this.address = address; this.lat = lat; this.lng = lng;
    }

    public String getId()              { return id; }
    public String getName()            { return name; }
    public void   setName(String n)    { this.name = n; }
    public String getPhone()           { return phone; }
    public void   setPhone(String p)   { this.phone = p; }
    public String getAddress()         { return address; }
    public void   setAddress(String a) { this.address = a; }
    public double getLat()             { return lat; }
    public void   setLat(double l)     { this.lat = l; }
    public double getLng()             { return lng; }
    public void   setLng(double l)     { this.lng = l; }
    public String getAvatarEmoji()     { return avatarEmoji; }
    public void   setAvatarEmoji(String e){ this.avatarEmoji = e; }
    public String getVideoUrl()        { return videoUrl; }
    public void   setVideoUrl(String v){ this.videoUrl = v; }
    public List<FoodItem> getMenu()    { return menu; }
    public void addItem(FoodItem f)    { menu.add(f); }

    public double getRating()          { return rating; }
    public int    getRatingCount()     { return ratingCount; }
    public void   addRating(int stars) {
        rating = (rating * ratingCount + stars) / (++ratingCount);
    }
    public String formatRating() {
        return ratingCount == 0 ? "Belum ada rating"
            : String.format("%.1f / 5.0  (%d ulasan)", rating, ratingCount);
    }

    public double distanceTo(double bLat, double bLng) {
        double dLat = Math.toRadians(bLat - lat), dLng = Math.toRadians(bLng - lng);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                 + Math.cos(Math.toRadians(lat))*Math.cos(Math.toRadians(bLat))
                 * Math.sin(dLng/2)*Math.sin(dLng/2);
        return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }

}
