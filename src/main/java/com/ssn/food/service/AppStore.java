package com.ssn.food.service;

import java.io.*;
import java.nio.file.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import com.ssn.food.model.ChatMsg;
import com.ssn.food.model.FoodItem;
import com.ssn.food.model.Order;
import com.ssn.food.model.Seller;

public class AppStore {
    private static final AppStore INST = new AppStore();
    public  static AppStore get() { return INST; }
    public  static final String BROADCAST_ID = "BROADCAST";

    private final List<Seller>   sellers = new ArrayList<>();
    private final List<Order>    orders  = new CopyOnWriteArrayList<>();
    private final Map<String, List<ChatMsg>> chats = new HashMap<>();

    private final List<Consumer<Order>>   orderCbs = new CopyOnWriteArrayList<>();
    private final List<Consumer<ChatMsg>> chatCbs  = new CopyOnWriteArrayList<>();
    private final List<Consumer<Seller>>  menuCbs  = new CopyOnWriteArrayList<>();
    private final List<Runnable>          reviewCbs = new CopyOnWriteArrayList<>();

    private AppStore() { 
        seed(); 
        load();
    }
    
    public void save() {
        // Simple persistence logic (Simulating Database)
        // In a real server environment, this would be an SQL call
        try {
            // Logic to save sellers/orders to a file could go here
            // For now, we seed and maintain in-memory, 
            // but we'll prepare the 'save' hook for the user.
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void load() {
        if (DatabaseManager.get().isConnected()) {
            List<Seller> dbSellers = DatabaseManager.get().loadSellers();
            if (!dbSellers.isEmpty()) {
                sellers.clear();
                sellers.addAll(dbSellers);
                for (Seller s : sellers) {
                    s.getMenu().clear(); // Clear seed menu before loading DB menu
                    s.getMenu().addAll(DatabaseManager.get().loadMenu(s.getId()));
                }
                System.out.println("✅ Data loaded from MySQL.");
            } else {
                // If DB is empty, sync current seed sellers to DB
                System.out.println("🔄 Syncing seed data to empty MySQL database...");
                for (Seller s : sellers) {
                    DatabaseManager.get().saveSeller(s);
                    for (FoodItem item : s.getMenu()) {
                        DatabaseManager.get().saveMenuItem(s.getId(), item);
                    }
                }
            }
        }
    }

    private void seed() {
        Seller s1 = new Seller("Seller 1",  "08962693748", "Jl. Mawar No.1, Jakarta",    -6.2088, 106.8456);
        s1.setAvatarEmoji("");
        s1.addRating(5); s1.addRating(4); s1.addRating(5);

        Seller s2 = new Seller("Seller 2",  "08131330251", "Jl. Melati No.5, Jakarta",   -6.2150, 106.8400);
        s2.setAvatarEmoji("");
        s2.addRating(4); s2.addRating(4);

        Seller s3 = new Seller("Seller 3",   "08155585353", "Jl. Kenanga No.12, Jakarta", -6.2000, 106.8500);
        s3.setAvatarEmoji("");
        s3.addRating(5); s3.addRating(3); s3.addRating(4);

        Seller s4 = new Seller("Seller 4",   "08953655300", "Jl. Sudirman No.88, Jakarta",-6.2200, 106.8600);
        s4.setAvatarEmoji("");
        s4.addRating(5); s4.addRating(5); s4.addRating(4);

        Seller s5 = new Seller("Seller 5",      "08139989317", "Jl. Thamrin No.20, Jakarta", -6.1950, 106.8230);
        s5.setAvatarEmoji("");
        s5.addRating(5); s5.addRating(5);

        s1.addItem(new FoodItem("F1", "Nasi Goreng", 20000, 10, "🍛"));
        s2.addItem(new FoodItem("F2", "Burger Beef", 35000, 5, "🍔"));
        s3.addItem(new FoodItem("F3", "Pizza Cheese", 50000, 8, "🍕"));
        s4.addItem(new FoodItem("F4", "Sushi Set", 45000, 12, "🍣"));
        s5.addItem(new FoodItem("F5", "Ice Cream", 15000, 20, "🍦"));

        sellers.add(s1); sellers.add(s2); sellers.add(s3); sellers.add(s4); sellers.add(s5);
        sellers.forEach(s -> chats.put(s.getId(), new CopyOnWriteArrayList<>()));
        chats.put(BROADCAST_ID, new CopyOnWriteArrayList<>());
    }

    public List<Seller> getSellers() { return sellers; }
    public Seller findSeller(String id) {
        return sellers.stream().filter(s->s.getId().equals(id)).findFirst().orElse(null);
    }

    public void placeOrder(Order o) {
        orders.add(o);
        DatabaseManager.get().saveOrder(o);
        orderCbs.forEach(cb -> cb.accept(o));
    }
    public List<Order> getOrdersFor(String sellerId) {
        List<Order> r = new ArrayList<>();
        orders.stream().filter(o->o.getSellerId().equals(sellerId)).forEach(r::add);
        return r;
    }
    public List<Order> getAllOrders() { return orders; }
    public void onOrder(Consumer<Order> cb)  { orderCbs.add(cb); }

    public List<ChatMsg> getChat(String sid) {
        if (!chats.containsKey(sid)) {
            chats.put(sid, new CopyOnWriteArrayList<>(DatabaseManager.get().loadChats(sid)));
        }
        return chats.get(sid);
    }

    public void addMenuItem(String sellerId, FoodItem item) {
        Seller s = findSeller(sellerId);
        if (s != null) {
            s.addItem(item);
            DatabaseManager.get().saveMenuItem(sellerId, item);
            fireMenu(s);
        }
    }

    public void sendChat(String sid, ChatMsg m) {
        m.sellerId = sid;
        getChat(sid).add(m);
        DatabaseManager.get().saveChat(sid, m);
        chatCbs.forEach(cb -> cb.accept(m));
    }

    public void onChat(Consumer<ChatMsg> cb) { chatCbs.add(cb); }

    public void onMenu(Consumer<Seller> cb)  { menuCbs.add(cb); }
    public void fireMenu(Seller s)           { menuCbs.forEach(cb -> cb.accept(s)); }

    public void onReview(Runnable cb)        { reviewCbs.add(cb); }
    public void fireReview()                 { reviewCbs.forEach(Runnable::run); }

    public static String rp(long v) {
        return "Rp " + String.format("%,d", v).replace(',','.');
    }
}