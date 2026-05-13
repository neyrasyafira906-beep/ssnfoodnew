package com.ssn.food.ui;

import java.awt.BorderLayout;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import com.ssn.food.model.Buyer;
import com.ssn.food.model.ChatMsg;
import com.ssn.food.model.FoodItem;
import com.ssn.food.model.Order;
import com.ssn.food.model.Seller;
import com.ssn.food.service.AIService;
import com.ssn.food.service.AppStore;
import com.ssn.food.service.DatabaseManager;

public class BuyerWindow extends JFrame {

    private final Buyer buyer = new Buyer();
    private JTextField nameF, phoneF, addrF;
    private final JPanel sellerList;
    private final JLabel locLbl;
    private String activeSellerId = null;
    private final JPanel chatHolder;
    private int currentMaxPrice = -1;
    private int currentMinQty = -1;

    // Cart system
    private static class CartItem {
        FoodItem item;
        int qty;
        Seller seller;

        CartItem(FoodItem item, int qty, Seller seller) {
            this.item = item;
            this.qty = qty;
            this.seller = seller;
        }

        long getSubtotal() {
            return item.getPrice() * qty;
        }
    }

    private List<CartItem> cart = new ArrayList<>();
    private JDialog cartDialog = null;
    private JButton cartBtn;

    public BuyerWindow() {
        setTitle("Buyer Dashboard — SSN FoodApp");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(700, 500));
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        JPanel root = T.bg();
        root.setLayout(new BorderLayout());
        root.add(topBar(), BorderLayout.NORTH);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setDividerSize(4);
        mainSplit.setResizeWeight(0.3);
        mainSplit.setBorder(null);
        mainSplit.setOpaque(false);

        // LEFT — chat
        chatHolder = new JPanel(new BorderLayout());
        chatHolder.setOpaque(false);
        chatHolder.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 6));
        JLabel chatHint = new JLabel(
                "<html><center>Broadcast your request here!<br>Sellers will respond with offers.</center></html>",
                SwingConstants.CENTER);
        chatHint.setFont(T.FB);
        chatHint.setForeground(T.GRAY);
        chatHolder.add(chatHint, BorderLayout.CENTER);

        // Auto-open Broadcast chat
        openChat(null); // special case for broadcast

        // RIGHT — seller list
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setOpaque(false);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 6, 10, 10));

        JPanel rhdr = new JPanel(new BorderLayout(10, 0));
        rhdr.setOpaque(false);
        rhdr.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        locLbl = new JLabel("📍 Detecting Location...");
        locLbl.setFont(T.FBO); locLbl.setForeground(T.PINK_D);
        
        AppStore.get().onReview(() -> SwingUtilities.invokeLater(() -> rebuildSellerList(AppStore.get().getSellers())));

        JPanel sortRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        sortRow.setOpaque(false);
        JButton nearBtn = T.btn("Nearest", new Color(16, 185, 129), new Color(5, 150, 105));
        nearBtn.addActionListener(e -> sortByDist());
        JButton cheapBtn = T.btn("Cheapest", new Color(234, 88, 12), new Color(194, 65, 12));
        cheapBtn.addActionListener(e -> sortByCheap());
        JButton topBtn = T.btn("Top Rated", new Color(139, 92, 246), new Color(109, 40, 217));
        topBtn.addActionListener(e -> sortByRating());
        JButton histBtn = T.obtn("History", T.PINK_D);
        histBtn.addActionListener(e -> showHistory());
        sortRow.add(nearBtn);
        sortRow.add(cheapBtn);
        sortRow.add(topBtn);
        sortRow.add(histBtn);
        rhdr.add(locLbl, BorderLayout.WEST);
        rhdr.add(sortRow, BorderLayout.EAST);
        rightPanel.add(rhdr, BorderLayout.NORTH);

        sellerList = new JPanel();
        sellerList.setLayout(new BoxLayout(sellerList, BoxLayout.Y_AXIS));
        sellerList.setOpaque(false);

        JPanel slWrap = new JPanel(new BorderLayout());
        slWrap.setOpaque(false);
        slWrap.add(sellerList, BorderLayout.NORTH);
        JScrollPane slScroll = new JScrollPane(slWrap);
        T.scrollFix(slScroll);
        rightPanel.add(slScroll, BorderLayout.CENTER);

        mainSplit.setLeftComponent(chatHolder);
        mainSplit.setRightComponent(rightPanel);
        root.add(mainSplit, BorderLayout.CENTER);

        // Database Status Bar
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 3));
        statusBar.setBackground(new Color(245, 245, 245));
        statusBar.setBorder(new MatteBorder(1, 0, 0, 0, new Color(220, 220, 220)));
        boolean connected = DatabaseManager.get().isConnected();
        JLabel dbStatus = new JLabel(connected ? "🟢 Database: Connected" : "🔴 Database: Disconnected (Check XAMPP)");
        dbStatus.setFont(T.f(10, Font.PLAIN));
        dbStatus.setForeground(connected ? new Color(21, 128, 61) : Color.RED);
        statusBar.add(dbStatus);
        root.add(statusBar, BorderLayout.SOUTH);

        setContentPane(root);
        
        // Initialize seller list after components are ready
        rebuildSellerList(AppStore.get().getSellers());
        AppStore.get().onMenu(s -> SwingUtilities.invokeLater(() -> 
            rebuildSellerList(AppStore.get().getSellers(), currentMaxPrice, currentMinQty)));
        
        // Removed old rebuild calls to avoid duplication with above onMenu listener
        
        // Notification for personal messages
        AppStore.get().onChat(m -> {
            if (m.from == ChatMsg.From.SELLER) {
                SwingUtilities.invokeLater(() -> {
                    Seller s = AppStore.get().findSeller(m.sellerId);
                    String name = (s != null) ? s.getName() : "Seller";
                    if (!m.sellerId.equals(activeSellerId)) {
                         JOptionPane.showMessageDialog(this, 
                             "<html><b>New Message from " + name + ":</b><br>" + m.text + "</html>",
                             "New Message", JOptionPane.INFORMATION_MESSAGE);
                    }
                });
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // TOP BAR
    // ─────────────────────────────────────────────────────────────────────
    private JPanel topBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setOpaque(false);
        bar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, T.PINK_B),
                new EmptyBorder(8, 14, 8, 14)));

        // Logo + title
        JPanel title = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        title.setOpaque(false);
        JLabel name = new JLabel("Buyer Dashboard");
        name.setFont(T.FT);
        name.setForeground(T.PINK_D);
        title.add(name);

        // Profile fields
        JPanel pf = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pf.setOpaque(false);

        nameF = T.field("Name");
        nameF.setPreferredSize(new Dimension(110, 30));
        phoneF = T.field("08xx...");
        phoneF.setPreferredSize(new Dimension(120, 30));
        addrF = T.field("Address");
        addrF.setPreferredSize(new Dimension(200, 30));

        JButton save = T.btn("Save");
        save.addActionListener(e -> {
            String nameVal = nameF.getText().trim();
            String addrVal = addrF.getText().trim();
            if (nameVal.isEmpty() || addrVal.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill both Name and Address to activate your profile!", "Profile Incomplete", JOptionPane.WARNING_MESSAGE);
                return;
            }
            buyer.setName(nameVal);
            buyer.setPhone(phoneF.getText().trim());
            buyer.setAddress(addrVal);
            locLbl.setText("📍 " + buyer.getAddress());
            
            // Refresh current chat name if open
            if (activeSellerId != null) {
                if (activeSellerId.equals(AppStore.BROADCAST_ID)) {
                    openChat(null);
                } else {
                    openChat(AppStore.get().findSeller(activeSellerId));
                }
            }

            JOptionPane.showMessageDialog(this, "Profile saved and activated!", "OK", JOptionPane.INFORMATION_MESSAGE);
        });

        JButton aiBtn = T.btn("Ask AI", new Color(124, 58, 237), new Color(91, 33, 182));
        aiBtn.addActionListener(e -> openAIDialog());

        // Cart button
        cartBtn = T.btn("Cart (0)", new Color(234, 88, 12), new Color(194, 65, 12));
        cartBtn.addActionListener(e -> showCart());

        addLbl(pf, "Name:");
        pf.add(nameF);
        addLbl(pf, "Phone:");
        pf.add(phoneF);
        addLbl(pf, "Addr:");
        pf.add(addrF);
        pf.add(save);
        pf.add(aiBtn);

        pf.add(cartBtn);

        bar.add(title, BorderLayout.WEST);
        bar.add(pf, BorderLayout.CENTER);
        return bar;
    }

    private void addLbl(JPanel p, String txt) {
        JLabel l = new JLabel(txt);
        l.setFont(T.f(14, Font.PLAIN));
        p.add(l);
    }

    private void updateCartButton() {
        if (cartBtn != null) {
            cartBtn.setText("Cart (" + cart.size() + ")");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // SHOPPING CART
    // ─────────────────────────────────────────────────────────────────────
    private void showCart() {
        if (cartDialog == null || !cartDialog.isVisible()) {
            cartDialog = new JDialog(this, "Shopping Cart", true);
            cartDialog.setSize(400, 500);
            cartDialog.setLocationRelativeTo(this);
        }

        JPanel main = T.bg();
        main.setLayout(new BorderLayout());
        main.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Header Invoice
        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setOpaque(false);
        JLabel title = new JLabel("OFFICIAL INVOICE", SwingConstants.CENTER);
        title.setFont(T.FT); title.setForeground(T.PINK_D);
        JLabel sub = new JLabel("SSN FoodApp Pro • Market Receipt", SwingConstants.CENTER);
        sub.setFont(T.FX); sub.setForeground(T.GRAY);
        header.add(title); header.add(sub);
        header.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 2, 0, T.PINK_D), 
            BorderFactory.createEmptyBorder(10, 0, 15, 0)));
        main.add(header, BorderLayout.NORTH);

        JPanel itemsPanel = new JPanel();
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
        itemsPanel.setOpaque(false);

        long grandTotal = 0;
        for (CartItem ci : cart) {
            JPanel row = new JPanel(new BorderLayout(15, 0));
            row.setOpaque(false);
            row.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, new Color(230, 230, 230)), 
                new EmptyBorder(12, 10, 12, 10)));
            
            JLabel icon = new JLabel() {
                public void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(245, 245, 245)); g2.fillOval(0, 0, 34, 34);
                    g2.setColor(T.PINK_B); T.drawIcon(g2, "food", 7, 7, 20);
                    g2.dispose();
                }
            };
            icon.setPreferredSize(new Dimension(34, 34));
            
            JLabel nameLabel = new JLabel("<html><b style='font-size:11px;'>" + ci.item.getName().toUpperCase() + "</b><br><font color='#777777'>Quantity: " + ci.qty + "</font></html>");
            nameLabel.setFont(T.FS);
            
            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            left.setOpaque(false);
            left.add(icon); left.add(nameLabel);

            JLabel priceLabel = new JLabel(AppStore.rp(ci.getSubtotal()));
            priceLabel.setFont(T.FBO); priceLabel.setForeground(T.DARK);

            JButton removeBtn = new JButton("✕");
            removeBtn.setFont(T.f(11, Font.BOLD)); removeBtn.setForeground(new Color(200, 200, 200));
            removeBtn.setBorder(null); removeBtn.setContentAreaFilled(false);
            removeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            removeBtn.addActionListener(e -> {
                cart.remove(ci);
                cartDialog.setVisible(false);
                showCart();
                updateCartButton();
            });

            row.add(removeBtn, BorderLayout.WEST);
            row.add(left, BorderLayout.CENTER);
            row.add(priceLabel, BorderLayout.EAST);
            itemsPanel.add(row);
            grandTotal += ci.getSubtotal();
        }

        // Receipt-style Footer Separator
        JPanel dashPanel = new JPanel() {
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(200, 200, 200));
                float[] dash = {5f, 5f};
                g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10, dash, 0));
                g2.drawLine(0, 5, getWidth(), 5);
                g2.dispose();
            }
        };
        dashPanel.setPreferredSize(new Dimension(100, 10));
        dashPanel.setOpaque(false);
        itemsPanel.add(dashPanel);

        if (cart.isEmpty()) {
            JLabel empty = new JLabel("Cart is empty", SwingConstants.CENTER);
            empty.setFont(T.FB);
            empty.setForeground(T.GRAY);
            itemsPanel.add(empty);
        }

        JScrollPane scroll = new JScrollPane(itemsPanel);
        T.scrollFix(scroll);
        main.add(scroll, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JLabel totalLabel = new JLabel("Total: " + AppStore.rp(grandTotal));
        totalLabel.setFont(T.FT);
        totalLabel.setForeground(T.PINK_D);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);

        JButton closeBtn = T.obtn("Close", T.GRAY);
        closeBtn.addActionListener(e -> cartDialog.setVisible(false));

        JButton checkoutBtn = T.btn("Checkout");
        
        btns.add(closeBtn);
        btns.add(checkoutBtn);
        footer.add(totalLabel, BorderLayout.WEST);
        footer.add(btns, BorderLayout.EAST);
        
        // Payment choice
        JPanel payRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        payRow.setOpaque(false);
        payRow.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
        JLabel payLbl = new JLabel("Payment Method:");
        payLbl.setFont(T.FBO);
        payRow.add(payLbl);
        javax.swing.JComboBox<Order.Payment> payCombo = new javax.swing.JComboBox<>(Order.Payment.values());
        payCombo.setFont(T.FBO);
        payCombo.setPreferredSize(new Dimension(150, 35));
        payRow.add(payCombo);
        
        JPanel footWrap = new JPanel(new BorderLayout());
        footWrap.setOpaque(false);
        footWrap.add(payRow, BorderLayout.NORTH);
        footWrap.add(footer, BorderLayout.CENTER);
        
        main.add(footWrap, BorderLayout.SOUTH);

        checkoutBtn.addActionListener(e -> {
            if (buyer.getName().isEmpty()) {
                JOptionPane.showMessageDialog(cartDialog, 
                    "Please fill and Save your Name in the Profile bar at the top first!", 
                    "Profile Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (cart.isEmpty()) {
                JOptionPane.showMessageDialog(cartDialog, "Cart is empty!", "Info", JOptionPane.WARNING_MESSAGE);
                return;
            }
            checkoutCart((Order.Payment)payCombo.getSelectedItem());
            cartDialog.setVisible(false);
        });

        cartDialog.setContentPane(main);
        cartDialog.setVisible(true);
    }

    private void checkoutCart(Order.Payment payment) {
        if (cart.isEmpty())
            return;

        Map<Seller, List<CartItem>> bySeller = new HashMap<>();
        for (CartItem ci : cart) {
            bySeller.computeIfAbsent(ci.seller, k -> new ArrayList<>()).add(ci);
        }

        for (Map.Entry<Seller, List<CartItem>> entry : bySeller.entrySet()) {
            Seller seller = entry.getKey();
            List<CartItem> items = entry.getValue();

            Order order = new Order(buyer.getDisplayName(), buyer.getPhone(),
                    buyer.getAddress(), seller.getId(), payment);

            StringBuilder summary = new StringBuilder();
            long total = 0;
            for (CartItem ci : items) {
                order.addLine(ci.item, ci.qty);
                ci.item.setStock(ci.item.getStock() - ci.qty);
                total += ci.getSubtotal();
                summary.append(ci.item.getName()).append(" x").append(ci.qty).append(", ");
            }

            AppStore.get().placeOrder(order);
            AppStore.get().sendChat(seller.getId(), new ChatMsg(ChatMsg.From.SYSTEM,
                    "New order from " + buyer.getDisplayName() + ": " + summary.toString() +
                            " | Total: " + AppStore.rp(total)));
            AppStore.get().fireMenu(seller);
        }

        JOptionPane.showMessageDialog(this,
                "✅ Order successful!\n" + cart.size() + " item(s) ordered.",
                "Success!", JOptionPane.INFORMATION_MESSAGE);
        cart.clear();
        updateCartButton();
    }

    // ─────────────────────────────────────────────────────────────────────
    // SELLER LIST
    // ─────────────────────────────────────────────────────────────────────
    private void rebuildSellerList(List<Seller> list) {
        rebuildSellerList(list, -1, -1);
    }

    private void rebuildSellerList(List<Seller> list, int maxPrice, int minQty) {
        this.currentMaxPrice = maxPrice;
        this.currentMinQty = minQty;
        sellerList.removeAll();
        
        List<Seller> activeSellers = new ArrayList<>();
        for (Seller s : list) {
            if (s.getMenu().isEmpty()) continue;
            
            // Filter by price/qty if active
            boolean match = true;
            if (maxPrice > 0) {
                match = s.getMenu().stream().anyMatch(item -> item.getPrice() <= maxPrice);
            }
            if (match && minQty > 0) {
                match = s.getMenu().stream().anyMatch(item -> item.getStock() >= minQty);
            }
            
            if (match) activeSellers.add(s);
        }

        locLbl.setText("Jakarta • " + activeSellers.size() + " Offers Found");

        if (activeSellers.isEmpty()) {
            JPanel empty = T.bg();
            empty.setLayout(new GridBagLayout());
            JLabel msg = new JLabel("<html><center>No offers yet.<br>Send a request on the left!</center></html>");
            msg.setFont(T.FH);
            msg.setForeground(T.GRAY);
            empty.add(msg);
            sellerList.add(empty);
        } else {
            for (Seller s : activeSellers) {
                JPanel card = sellerCard(s);
                sellerList.add(card);
                sellerList.add(Box.createVerticalStrut(15));
                T.fade(card); // Smooth entrance
            }
        }
        sellerList.revalidate();
        sellerList.repaint();
    }

    private JPanel sellerCard(Seller s) {
        JPanel card = T.card(16);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Header
        JPanel hdr = new JPanel(new BorderLayout(10, 0));
        hdr.setOpaque(false);

        JLabel av = new JLabel(s.getAvatarEmoji());
        av.setFont(T.f(30, Font.PLAIN));

        JPanel info = new JPanel(new BorderLayout(0, 3));
        info.setOpaque(false);
        JLabel nm = new JLabel(s.getName());
        nm.setFont(T.FH);
        nm.setForeground(T.PINK_D);
        JPanel meta = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        meta.setOpaque(false);

        double dist = s.distanceTo(buyer.getLat(), buyer.getLng());
        JLabel dl = new JLabel(String.format("Dist: %.1f km", dist));
        dl.setFont(T.FS);
        dl.setForeground(T.GRAY);
        JLabel rl = new JLabel(T.stars(s.getRating()) + " " + s.formatRating());
        rl.setFont(T.FS);
        rl.setForeground(T.STAR);
        JLabel pl = new JLabel("Phone: " + s.getPhone());
        pl.setFont(T.FS);
        pl.setForeground(T.GRAY);
        meta.add(dl);
        meta.add(rl);
        meta.add(pl);
        info.add(nm, BorderLayout.NORTH);
        info.add(meta, BorderLayout.CENTER);

        // Buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btns.setOpaque(false);
        
        btns.add(iconBtn(s, "map", T.PINK, "MAP", e -> openSellerMap(s.getLat(), s.getLng())));
        btns.add(iconBtn(s, "video", new Color(124, 58, 237), "VIDEO", e -> openVideo(s)));
        btns.add(iconBtn(s, "chat", T.PINK_D, "CHAT", e -> openChat(s)));
        
        // Show REVIEW button only if buyer has a DELIVERED order from this seller
        boolean hasDoneOrder = AppStore.get().getOrdersFor(s.getId()).stream()
            .anyMatch(o -> o.getBuyerName().equalsIgnoreCase(buyer.getName()) && o.getStatus() == Order.Status.DELIVERED);
        
        if (hasDoneOrder) {
            btns.add(iconBtn(s, "star", T.STAR, "REVIEW", e -> showReviewDialog(s)));
        }
        
        hdr.add(info, BorderLayout.CENTER);
        hdr.add(btns, BorderLayout.EAST);
        card.add(hdr, BorderLayout.NORTH);

        // Menu grid
        JPanel grid = new JPanel(new GridLayout(0, 1, 0, 4));
        grid.setOpaque(false);
        for (FoodItem item : s.getMenu()) {
            // Apply filter to individual items
            boolean match = true;
            if (currentMaxPrice > 0 && item.getPrice() > currentMaxPrice) match = false;
            if (match && currentMinQty > 0 && item.getStock() < currentMinQty) match = false;
            
            if (match) grid.add(menuRow(s, item));
        }
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    // REMOVED emptyMenuRow to comply with user request for no "strip-strip" placeholders
    

    private JPanel menuRow(Seller seller, FoodItem item) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));

        JPanel thumb = new JPanel() {
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (item.getImageIcon() != null) {
                    g2.drawImage(item.getImageIcon().getImage(), 0, 0, 48, 48, null);
                } else {
                    g2.setColor(T.PINK_L);
                    g2.fillRoundRect(0, 0, 48, 48, 8, 8);
                    g2.setColor(T.PINK_B);
                    T.drawIcon(g2, "food", 12, 12, 24);
                }
                g2.dispose();
            }
        };
        thumb.setPreferredSize(new Dimension(48, 48));
        thumb.setOpaque(false);

        JLabel nm = new JLabel(item.getName());
        nm.setFont(T.FB);
        nm.setForeground(T.DARK);
        JLabel pr = new JLabel(item.formatPrice());
        pr.setFont(T.FBO);
        pr.setForeground(T.PINK_D);
        JLabel stk = new JLabel("Stock: " + item.getStock());
        stk.setFont(T.FS);
        stk.setForeground(item.getStock() > 0 ? T.GREEN : T.RED);
        SpinnerNumberModel sm = new SpinnerNumberModel(0, 0, Math.max(item.getStock(), 1), 1);
        JSpinner sp = new JSpinner(sm);
        sp.setFont(T.FS);
        JButton ob = T.btn("Order");

        ob.addActionListener(e -> {
            if (buyer.getName().isEmpty() || buyer.getAddress().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill your Profile Name and Address first!", "Profile Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int qty = (int) sp.getValue();
            if (qty <= 0) {
                JOptionPane.showMessageDialog(this, "Please select quantity first!", "Info", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Add to cart
            boolean found = false;
            for (CartItem ci : cart) {
                if (ci.item == item && ci.seller == seller) {
                    ci.qty += qty;
                    found = true;
                    break;
                }
            }
            if (!found) {
                cart.add(new CartItem(item, qty, seller));
            }
            updateCartButton();

            JOptionPane.showMessageDialog(this,
                    item.getName() + " x" + qty + " added to cart!\n" +
                            "Click 🛒 Cart button to checkout.",
                    "Added", JOptionPane.INFORMATION_MESSAGE);
        });

        JPanel info = new JPanel(new GridLayout(1, 4, 8, 0));
        info.setOpaque(false);
        info.add(pr);
        info.add(stk);
        info.add(sp);
        info.add(ob);

        JPanel right = new JPanel(new BorderLayout(0, 2));
        right.setOpaque(false);
        right.add(nm, BorderLayout.NORTH);
        right.add(info, BorderLayout.CENTER);

        row.add(thumb, BorderLayout.WEST);
        row.add(right, BorderLayout.CENTER);
        return row;
    }

    // ─────────────────────────────────────────────────────────────────────
    // CHAT
    // ─────────────────────────────────────────────────────────────────────
    private void openChat(Seller s) {
        activeSellerId = (s == null) ? AppStore.BROADCAST_ID : s.getId();
        chatHolder.removeAll();

        JPanel chatWrap = new JPanel(new BorderLayout());
        chatWrap.setOpaque(false);

        JPanel chatHead = new JPanel(new BorderLayout());
        chatHead.setOpaque(false);
        chatHead.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        
        String titleText = (s == null) ? "Broadcast Request (To All Sellers)" : "Chat: " + s.getName();
        JLabel tl = new JLabel(titleText);
        tl.setFont(T.FH);
        tl.setForeground(T.PINK_D);
        
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        titlePanel.setOpaque(false);
        if (s != null) {
            JButton backBtn = T.obtn("◀ Back", T.PINK_D);
            backBtn.setPreferredSize(new Dimension(80, 25));
            backBtn.addActionListener(e -> openChat(null));
            titlePanel.add(backBtn);
        }
        titlePanel.add(tl);
        
        JButton aiToggle = T.btn("Enable AI", new Color(124, 58, 237), new Color(91, 33, 182));
        chatHead.add(titlePanel, BorderLayout.WEST);
        if (s != null) chatHead.add(aiToggle, BorderLayout.EAST);

        ChatPanel cp = new ChatPanel(activeSellerId, ChatMsg.From.BUYER, buyer.getName());
        if (s == null) {
            cp.setFilterCallback((maxPrice, minQty) -> {
                SwingUtilities.invokeLater(() -> rebuildSellerList(AppStore.get().getSellers(), maxPrice, minQty));
            });
        }
        final boolean[] aiActive = { false };
        aiToggle.addActionListener(e -> {
            aiActive[0] = !aiActive[0];
            cp.enableAI(aiActive[0]);
            aiToggle.setText(aiActive[0] ? "AI Active" : "Enable AI");
        });

        chatWrap.add(chatHead, BorderLayout.NORTH);
        chatWrap.add(cp, BorderLayout.CENTER);

        chatHolder.add(chatWrap, BorderLayout.CENTER);
        chatHolder.revalidate();
        chatHolder.repaint();
    }

    // ─────────────────────────────────────────────────────────────────────
    // REVIEWS
    // ─────────────────────────────────────────────────────────────────────
    private void showReviewDialog(Seller s) {
        if (buyer.getName().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please save your Name in profile first!", "Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JDialog dlg = new JDialog(this, "Review " + s.getName(), true);
        dlg.setSize(350, 300);
        dlg.setLocationRelativeTo(this);
        
        JPanel p = T.bg(); p.setLayout(new BorderLayout(10,10));
        p.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));
        
        JLabel ttl = new JLabel("Give your rating for " + s.getName());
        ttl.setFont(T.FBO); ttl.setForeground(T.PINK_D);
        p.add(ttl, BorderLayout.NORTH);
        
        JPanel mid = new JPanel(new GridLayout(3,1,5,5)); mid.setOpaque(false);
        
        JPanel starRow = new JPanel(new FlowLayout(FlowLayout.LEFT)); starRow.setOpaque(false);
        JSpinner starSpin = new JSpinner(new SpinnerNumberModel(5, 1, 5, 1));
        starSpin.setFont(T.FB);
        starRow.add(new JLabel("Rating (1-5 stars):"));
        starRow.add(starSpin);
        
        JTextArea comm = new JTextArea(3, 20);
        comm.setFont(T.FS);
        comm.setBorder(BorderFactory.createLineBorder(T.PINK_B));
        comm.setLineWrap(true);
        
        mid.add(starRow);
        mid.add(new JLabel("Comment:"));
        mid.add(new JScrollPane(comm));
        p.add(mid, BorderLayout.CENTER);
        
        JButton sub = T.btn("Submit Review");
        sub.addActionListener(e -> {
            int rVal = (int) starSpin.getValue();
            String cText = comm.getText().trim();
            if (cText.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Please enter a comment!");
                return;
            }
            
            com.ssn.food.model.Review rev = new com.ssn.food.model.Review(s.getId(), buyer.getName(), rVal, cText);
            DatabaseManager.get().saveReview(rev);
            
            // Refresh seller in memory
            s.addRating(rVal);
            AppStore.get().fireReview();
            
            JOptionPane.showMessageDialog(this, "Thank you for your review!");
            dlg.dispose();
        });
        p.add(sub, BorderLayout.SOUTH);
        
        dlg.setContentPane(p);
        dlg.setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────────────
    // AI DIALOG
    // ─────────────────────────────────────────────────────────────────────
    private void openAIDialog() {
        List<Seller> sellers = AppStore.get().getSellers();
        if (sellers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No sellers registered.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String[] names = sellers.stream().map(Seller::getName).toArray(String[]::new);
        String chosen = (String) JOptionPane.showInputDialog(
                this, "Select seller to ask AI:", "Ask AI",
                JOptionPane.QUESTION_MESSAGE, null, names, names[0]);
        if (chosen == null)
            return;
        Seller s = sellers.stream().filter(x -> x.getName().equals(chosen)).findFirst().orElse(null);
        if (s == null)
            return;

        JDialog aiRoom = new JDialog(this, "AI Roomchat - " + s.getName(), false);
        aiRoom.setSize(450, 600);
        aiRoom.setLocationRelativeTo(this);
        
        JPanel p = T.bg();
        p.setLayout(new BorderLayout());
        
        JLabel ttl = new JLabel("🤖 AI Online Roomchat: " + s.getName());
        ttl.setFont(T.FBO); ttl.setForeground(T.PINK_D);
        ttl.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p.add(ttl, BorderLayout.NORTH);

        ChatPanel cp = new ChatPanel(s.getId(), ChatMsg.From.BUYER, buyer.getName());
        cp.enableAI(true);
        p.add(cp, BorderLayout.CENTER);
        
        aiRoom.setContentPane(p);
        aiRoom.setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────────────
    // SORT METHODS
    // ─────────────────────────────────────────────────────────────────────
    private void sortByDist() {
        List<Seller> list = new ArrayList<>(AppStore.get().getSellers());
        list.sort(Comparator.comparingDouble(s -> s.distanceTo(buyer.getLat(), buyer.getLng())));
        rebuildSellerList(list);
    }

    private void sortByCheap() {
        List<Seller> list = new ArrayList<>(AppStore.get().getSellers());
        list.sort(Comparator.comparingLong(s -> s.getMenu().stream().mapToLong(FoodItem::getPrice).min()
                .orElse(Long.MAX_VALUE)));
        rebuildSellerList(list);
    }

    private void sortByRating() {
        List<Seller> list = new ArrayList<>(AppStore.get().getSellers());
        list.sort((a, b) -> Double.compare(b.getRating(), a.getRating()));
        rebuildSellerList(list);
    }

    // ─────────────────────────────────────────────────────────────────────
    // ORDER HISTORY
    // ─────────────────────────────────────────────────────────────────────
    private void showHistory() {
        JDialog dlg = new JDialog(this, "Order History", true);
        dlg.setSize(520, 420);
        dlg.setLocationRelativeTo(this);
        JPanel p = T.bg();
        p.setLayout(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JLabel ttl = new JLabel("Order History");
        ttl.setFont(T.FT);
        ttl.setForeground(T.PINK_D);
        ttl.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        p.add(ttl, BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);
        List<Order> mine = new ArrayList<>();
        AppStore.get().getAllOrders().stream()
                .filter(o -> o.getBuyerName().equals(buyer.getDisplayName()))
                .forEach(mine::add);

        if (mine.isEmpty()) {
            JLabel e = new JLabel("No orders yet", SwingConstants.CENTER);
            e.setFont(T.FB);
            e.setForeground(T.GRAY);
            list.add(e);
        } else {
            for (Order o : mine) {
                JPanel row = T.card(12);
                row.setLayout(new BorderLayout(8, 0));
                row.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                JPanel l2 = new JPanel(new BorderLayout(0, 3));
                l2.setOpaque(false);
                JLabel id = new JLabel(o.getId() + " • " + o.getTimestamp());
                id.setFont(T.FBO);
                id.setForeground(T.PINK_D);
                JLabel sm = new JLabel(o.getSummary());
                sm.setFont(T.FS);
                sm.setForeground(T.DARK);
                l2.add(id, BorderLayout.NORTH);
                l2.add(sm, BorderLayout.CENTER);
                JPanel r2 = new JPanel(new BorderLayout(0, 3));
                r2.setOpaque(false);
                JLabel tot = new JLabel(o.formatTotal());
                tot.setFont(T.FBO);
                tot.setForeground(T.PINK_D);
                JLabel pay = T.badge(o.getPayment().name(), T.BLUE_BG, T.BLUE);
                r2.add(tot, BorderLayout.NORTH);
                r2.add(pay, BorderLayout.CENTER);
                row.add(l2, BorderLayout.CENTER);
                row.add(r2, BorderLayout.EAST);
                list.add(row);
                list.add(Box.createVerticalStrut(6));
            }
        }
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(list, BorderLayout.NORTH);
        JScrollPane sc = new JScrollPane(wrap);
        T.scrollFix(sc);
        p.add(sc, BorderLayout.CENTER);
        JButton close = T.btn("Close");
        close.addActionListener(e -> dlg.dispose());
        JPanel bp = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bp.setOpaque(false);
        bp.add(close);
        p.add(bp, BorderLayout.SOUTH);
        dlg.setContentPane(p);
        dlg.setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────────────
    // VIDEO (YOUTUBE)
    // ─────────────────────────────────────────────────────────────────────
    private void openVideo(Seller s) {
        String url = s.getVideoUrl();
        if (url == null || url.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Seller hasn't uploaded a promo video.\nPlease check again later.",
                    "Video Not Available",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        url = convertToYouTubeWatchUrl(url);

        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "❌ Failed to open video: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String convertToYouTubeWatchUrl(String url) {
        if (url == null)
            return "";

        // youtu.be/xxxxx -> youtube.com/watch?v=xxxxx
        if (url.contains("youtu.be/")) {
            String id = url.substring(url.lastIndexOf("/") + 1);
            int qIdx = id.indexOf("?");
            if (qIdx > 0)
                id = id.substring(0, qIdx);
            return "https://www.youtube.com/watch?v=" + id;
        }

        // youtube.com/shorts/xxxxx -> youtube.com/watch?v=xxxxx
        if (url.contains("/shorts/")) {
            String id = url.substring(url.lastIndexOf("/shorts/") + 8);
            int qIdx = id.indexOf("?");
            if (qIdx > 0)
                id = id.substring(0, qIdx);
            return "https://www.youtube.com/watch?v=" + id;
        }

        // m.youtube.com -> www.youtube.com
        if (url.contains("m.youtube.com")) {
            url = url.replace("m.youtube.com", "www.youtube.com");
        }

        return url;
    }

    // ─────────────────────────────────────────────────────────────────────
    // GOOGLE MAPS (BUYER & SELLER)
    // ─────────────────────────────────────────────────────────────────────


    // Helper for circular icon buttons with labels
    private JPanel iconBtn(Seller s, String type, Color bg, String label, java.awt.event.ActionListener al) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        JButton b = new JButton() {
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg); g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Color.WHITE); T.drawIcon(g2, type, 8, 8, 14);
                g2.dispose();
            }
        };
        b.setPreferredSize(new Dimension(30, 30)); b.setBorder(null);
        b.setContentAreaFilled(false); b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addActionListener(al);
        b.setToolTipText(label);

        JLabel l = new JLabel(label, SwingConstants.CENTER);
        l.setFont(T.f(8, Font.BOLD)); l.setForeground(T.GRAY);
        p.add(b, BorderLayout.CENTER);
        p.add(l, BorderLayout.SOUTH);
        return p;
    }

    // Open map for Seller's location
    private void openSellerMap(double lat, double lng) {
        try {
            String mapsUrl = "https://www.google.com/maps/search/?api=1&query=" + lat + "," + lng;
            Desktop.getDesktop().browse(new URI(mapsUrl));
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Failed to open seller location: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}