package com.ssn.food.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Order {
    public enum Status  { PENDING, CONFIRMED, PREPARING, READY, DELIVERED, CANCELLED }
    public enum Payment { CASH, TRANSFER, CASHLESS }

    private static int counter = 1000;
    private final String id, buyerName, buyerPhone, buyerAddress, sellerId, timestamp;
    private final List<Line> lines = new ArrayList<>();
    private final Payment payment;
    private Status status = Status.PENDING;

    public static class Line {
        public final FoodItem item;
        public final int qty;
        public Line(FoodItem i, int q) { item = i; qty = q; }
        public long subtotal() { return item.getPrice() * qty; }
    }

    public Order(String buyerName, String buyerPhone, String buyerAddress,
                 String sellerId, Payment payment) {
        this.id = "ORD-" + (counter++);
        this.buyerName = buyerName; this.buyerPhone = buyerPhone;
        this.buyerAddress = buyerAddress; this.sellerId = sellerId;
        this.payment = payment;
        this.timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public void addLine(FoodItem item, int qty) { lines.add(new Line(item, qty)); }

    public String  getId()            { return id; }
    public String  getBuyerName()     { return buyerName; }
    public String  getBuyerPhone()    { return buyerPhone; }
    public String  getBuyerAddress()  { return buyerAddress; }
    public String  getSellerId()      { return sellerId; }
    public Payment getPayment()       { return payment; }
    public Status  getStatus()        { return status; }
    public void    setStatus(Status s){ this.status = s; }
    public String  getTimestamp()     { return timestamp; }
    public List<Line> getLines()      { return lines; }

    public long getTotal() {
        return lines.stream().mapToLong(Line::subtotal).sum();
    }
    public String formatTotal() {
        return "Rp " + String.format("%,d", getTotal()).replace(',','.');
    }
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        for (Line l : lines) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(l.item.getName()).append(" x").append(l.qty);
        }
        return sb.toString();
    }
}
