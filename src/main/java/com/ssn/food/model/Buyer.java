package com.ssn.food.model;

public class Buyer {
    private String name = "", phone = "", address = "";
    private double lat = -6.2088, lng = 106.8456;

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
    public String getDisplayName()     { return name.isEmpty() ? "Pembeli" : name; }
}
