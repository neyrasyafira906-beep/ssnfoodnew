package com.ssn.food.service;

import com.ssn.food.model.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class AppStore {
    private static final AppStore INST = new AppStore();
    public  static AppStore get() { return INST; }

    private final List<Seller>   sellers = new ArrayList<>();
    private final List<Order>    orders  = new CopyOnWriteArrayList<>();
    private final Map<String, List<ChatMsg>> chats = new HashMap<>();

    private final List<Consumer<Order>>   orderCbs = new CopyOnWriteArrayList<>();
    private final List<Consumer<ChatMsg>> chatCbs  = new CopyOnWriteArrayList<>();
    private final List<Consumer<Seller>>  menuCbs  = new CopyOnWriteArrayList<>();

    private AppStore() { seed(); }

    // ── Seed ─────────────────────────────────────────────────────────────────
    private void seed() {
        Seller s1 = new Seller("Warung Bu Sari",  "08962693748", "Jl. Mawar No.1, Jakarta",    -6.2088, 106.8456);
        s1.setAvatarEmoji("\uD83C\uDF5B");
        s1.addItem(new FoodItem("s1a","Nasi Goreng", 15000,20,"\uD83C\uDF73"));
        s1.addItem(new FoodItem("s1b","Ayam Bakar",  20000,15,"\uD83C\uDF57"));
        s1.addItem(new FoodItem("s1c","Es Teh Manis", 5000,50,"\uD83E\uDD64"));
        s1.addItem(new FoodItem("s1d","Fruit Tea",    9000,30,"\uD83C\uDF75"));
        s1.addRating(5); s1.addRating(4); s1.addRating(5);

        Seller s2 = new Seller("Depot Pak Budi",  "08131330251", "Jl. Melati No.5, Jakarta",   -6.2150, 106.8400);
        s2.setAvatarEmoji("\uD83C\uDF72");
        s2.addItem(new FoodItem("s2a","Nasi Padang",18000,10,"\uD83C\uDF5B"));
        s2.addItem(new FoodItem("s2b","Rendang",    25000, 8,"\uD83E\uDD69"));
        s2.addItem(new FoodItem("s2c","Hot Tea",     6000,40,"\u2615"));
        s2.addItem(new FoodItem("s2d","Air Mineral", 3000,60,"\uD83D\uDCA7"));
        s2.addRating(4); s2.addRating(4);

        Seller s3 = new Seller("Mie Ayam Joss",   "08155585353", "Jl. Kenanga No.12, Jakarta", -6.2000, 106.8500);
        s3.setAvatarEmoji("\uD83C\uDF5C");
        s3.addItem(new FoodItem("s3a","Mie Ayam",  12000,25,"\uD83C\uDF5C"));
        s3.addItem(new FoodItem("s3b","Mie Goreng",13000,20,"\uD83C\uDF5D"));
        s3.addItem(new FoodItem("s3c","Bakso",     15000,18,"\uD83C\uDF5F"));
        s3.addItem(new FoodItem("s3d","Es Jeruk",   7000,35,"\uD83C\uDF4A"));
        s3.addRating(5); s3.addRating(3); s3.addRating(4);

        Seller s4 = new Seller("Burger Corner",   "08953655300", "Jl. Sudirman No.88, Jakarta",-6.2200, 106.8600);
        s4.setAvatarEmoji("\uD83C\uDF54");
        s4.addItem(new FoodItem("s4a","Burger Spesial",25000,12,"\uD83C\uDF54"));
        s4.addItem(new FoodItem("s4b","French Fries",  12000,20,"\uD83C\uDF5F"));
        s4.addItem(new FoodItem("s4c","Chicken Strip", 20000,15,"\uD83C\uDF57"));
        s4.addItem(new FoodItem("s4d","Cola",          10000,40,"\uD83E\uDD64"));
        s4.addRating(5); s4.addRating(5); s4.addRating(4);

        Seller s5 = new Seller("Sushi Hana",      "08139989317", "Jl. Thamrin No.20, Jakarta", -6.1950, 106.8230);
        s5.setAvatarEmoji("\uD83C\uDF63");
        s5.addItem(new FoodItem("s5a","Salmon Roll",35000, 8,"\uD83C\uDF63"));
        s5.addItem(new FoodItem("s5b","Gyoza",      20000,12,"\uD83E\uDD5F"));
        s5.addItem(new FoodItem("s5c","Ramen",      28000,10,"\uD83C\uDF5C"));
        s5.addItem(new FoodItem("s5d","Green Tea",  12000,25,"\uD83C\uDF75"));
        s5.addRating(5); s5.addRating(5);

        sellers.add(s1); sellers.add(s2); sellers.add(s3); sellers.add(s4); sellers.add(s5);
        sellers.forEach(s -> chats.put(s.getId(), new CopyOnWriteArrayList<>()));
    }

    // ── Sellers ───────────────────────────────────────────────────────────────
    public List<Seller> getSellers() { return sellers; }
    public Seller findSeller(String id) {
        return sellers.stream().filter(s->s.getId().equals(id)).findFirst().orElse(null);
    }

    // ── Orders ────────────────────────────────────────────────────────────────
    public void placeOrder(Order o) {
        orders.add(o);
        orderCbs.forEach(cb -> cb.accept(o));
    }
    public List<Order> getOrdersFor(String sellerId) {
        List<Order> r = new ArrayList<>();
        orders.stream().filter(o->o.getSellerId().equals(sellerId)).forEach(r::add);
        return r;
    }
    public List<Order> getAllOrders() { return orders; }
    public void onOrder(Consumer<Order> cb)  { orderCbs.add(cb); }

    // ── Chat ──────────────────────────────────────────────────────────────────
    public List<ChatMsg> getChat(String sid) {
        return chats.computeIfAbsent(sid, k -> new CopyOnWriteArrayList<>());
    }
    public void sendChat(String sid, ChatMsg m) {
        getChat(sid).add(m);
        chatCbs.forEach(cb -> cb.accept(m));
    }
    public void onChat(Consumer<ChatMsg> cb) { chatCbs.add(cb); }

    // ── Menu ──────────────────────────────────────────────────────────────────
    public void onMenu(Consumer<Seller> cb)  { menuCbs.add(cb); }
    public void fireMenu(Seller s)           { menuCbs.forEach(cb -> cb.accept(s)); }

    // ── Util ──────────────────────────────────────────────────────────────────
    public static String rp(long v) {
        return "Rp " + String.format("%,d", v).replace(',','.');
    }
}
