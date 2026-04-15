package com.ssn.food.ui;

import java.awt.BorderLayout;
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
import java.awt.Image;
import java.awt.RenderingHints;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import com.ssn.food.model.ChatMsg;
import com.ssn.food.model.FoodItem;
import com.ssn.food.model.Order;
import com.ssn.food.model.Seller;
import com.ssn.food.service.AIService;
import com.ssn.food.service.AppStore;

public class SellerWindow extends JFrame {

    private Seller seller;
    private JLabel avatarLbl, nameLbl, subLbl, ratingLbl;
    private JPanel incomingPanel, ordersPanel, menuListPanel;
    private JTabbedPane tabs;

    public SellerWindow(Seller initial) {
        this.seller = initial;
        setTitle("🍪 Seller Dashboard  —  " + initial.getName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(700, 500));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        buildUI();
        AppStore.get().onOrder(o -> {
            if (o.getSellerId().equals(seller.getId()))
                SwingUtilities.invokeLater(() -> { refreshIncomingPanel(); refreshOrders(); });
        });
    }

    private void buildUI() {
        JPanel root = T.bg();
        root.setLayout(new BorderLayout());
        root.add(topBar(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerSize(4);
        split.setResizeWeight(0.3);
        split.setBorder(null);
        split.setOpaque(false);

        // LEFT incoming
        JPanel left = T.bg();
        left.setLayout(new BorderLayout());
        left.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 6));
        JLabel inTitle = new JLabel("📥 Pesanan Aktif");
        inTitle.setFont(T.FH);
        inTitle.setForeground(T.PINK_D);
        inTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        left.add(inTitle, BorderLayout.NORTH);

        incomingPanel = new JPanel();
        incomingPanel.setLayout(new BoxLayout(incomingPanel, BoxLayout.Y_AXIS));
        incomingPanel.setOpaque(false);
        
        refreshIncomingPanel();

        JPanel inWrap = new JPanel(new BorderLayout());
        inWrap.setOpaque(false);
        inWrap.add(incomingPanel, BorderLayout.NORTH);
        JScrollPane inScroll = new JScrollPane(inWrap);
        T.scrollFix(inScroll);
        left.add(inScroll, BorderLayout.CENTER);
        split.setLeftComponent(left);

        // RIGHT tabs
        tabs = new JTabbedPane();
        tabs.setFont(T.FB);
        tabs.setBackground(T.PINK_L);
        tabs.setForeground(T.PINK_D);
        tabs.addTab("💬 Chat", new ChatPanel(seller.getId(), ChatMsg.From.SELLER));
        tabs.addTab("📋 Orders", buildOrdersPane());
        tabs.addTab("🍽 Menu & Stok", buildMenuTab());
        tabs.addTab("🗺 Peta", buildMapTab());
        tabs.addTab("⚙ Settings", buildSettingsTab());

        JPanel rightP = new JPanel(new BorderLayout());
        rightP.setOpaque(false);
        rightP.setBorder(BorderFactory.createEmptyBorder(10, 6, 10, 10));
        rightP.add(tabs, BorderLayout.CENTER);
        split.setRightComponent(rightP);

        root.add(split, BorderLayout.CENTER);
        setContentPane(root);
        refreshOrders();
        
        AppStore.get().onOrder(o -> {
            if (o.getSellerId().equals(seller.getId())) {
                SwingUtilities.invokeLater(() -> refreshIncomingPanel());
            }
        });
    }

    // ── Top bar ───────────────────────────────────────────────────────────────
    private JPanel topBar() {
        JPanel bar = new JPanel(new BorderLayout(10,0));
        bar.setOpaque(false);
        bar.setBorder(new CompoundBorder(
            new MatteBorder(0,0,1,0,T.PINK_B),
            new EmptyBorder(8,14,8,14)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT,10,0)); left.setOpaque(false);
        avatarLbl = new JLabel(seller.getAvatarEmoji()); avatarLbl.setFont(T.f(30,Font.PLAIN));
        JPanel inf = new JPanel(new BorderLayout(0,2)); inf.setOpaque(false);
        nameLbl = new JLabel("🍪 "+seller.getName()+"   •   ID: "+seller.getId());
        nameLbl.setFont(T.FT); nameLbl.setForeground(T.PINK_D);
        subLbl = new JLabel("📱 "+seller.getPhone()+"   📍 "+seller.getAddress());
        subLbl.setFont(T.FS); subLbl.setForeground(T.GRAY);
        inf.add(nameLbl, BorderLayout.NORTH); inf.add(subLbl, BorderLayout.SOUTH);
        left.add(avatarLbl); left.add(inf);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); right.setOpaque(false);

        List<Seller> sellers = AppStore.get().getSellers();
        String[] names = sellers.stream()
            .map(s -> s.getAvatarEmoji()+" "+s.getName()).toArray(String[]::new);
        JComboBox<String> dd = new JComboBox<>(names);
        dd.setFont(T.FBO); dd.setSelectedIndex(sellers.indexOf(seller));
        dd.setPreferredSize(new Dimension(200,32));
        dd.addActionListener(e -> {
            int idx = dd.getSelectedIndex();
            if (idx >= 0) switchSeller(sellers.get(idx));
        });

        ratingLbl = new JLabel(T.stars(seller.getRating())+"  "+seller.formatRating());
        ratingLbl.setFont(T.FBO); ratingLbl.setForeground(T.STAR);

        JButton rev = T.btn("💰 Revenue", new Color(16,185,129), new Color(5,150,105));
        rev.addActionListener(e -> showRevenue());

        JLabel swLbl = new JLabel("Ganti Seller:"); swLbl.setFont(T.FBO); swLbl.setForeground(T.PINK_D);
        right.add(swLbl); right.add(dd); right.add(ratingLbl); right.add(rev);
        bar.add(left, BorderLayout.WEST); bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void switchSeller(Seller s) {
        this.seller = s;
        nameLbl.setText("🍪 " + s.getName() + "   •   ID: " + s.getId());
        subLbl.setText("📱 " + s.getPhone() + "   📍 " + s.getAddress());
        avatarLbl.setText(s.getAvatarEmoji());
        ratingLbl.setText(T.stars(s.getRating()) + "  " + s.formatRating());
        setTitle("🍪 Seller Dashboard  —  " + s.getName());

        refreshIncomingPanel();
        tabs.setComponentAt(0, new ChatPanel(s.getId(), ChatMsg.From.SELLER));
        tabs.setComponentAt(2, buildMenuTab());
        tabs.setComponentAt(3, buildMapTab());
        tabs.setComponentAt(4, buildSettingsTab());
        refreshOrders();
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private int getCurrentStatusIndex(Order.Status status, Order.Status[] flow) {
        for (int i = 0; i < flow.length; i++) {
            if (status == flow[i]) return i;
        }
        return 0;
    }

    private void refreshIncomingPanel() {
        incomingPanel.removeAll();
        List<Order> orders = AppStore.get().getOrdersFor(seller.getId());
        
        List<Order> activeOrders = new ArrayList<>();
        for (Order o : orders) {
            if (o.getStatus() != Order.Status.DELIVERED && 
                o.getStatus() != Order.Status.CANCELLED) {
                activeOrders.add(o);
            }
        }
        
        if (activeOrders.isEmpty()) {
            JLabel hint = new JLabel("📭 Tidak ada pesanan aktif", SwingConstants.CENTER);
            hint.setFont(T.FS);
            hint.setForeground(T.GRAY);
            incomingPanel.add(hint);
        } else {
            for (Order o : activeOrders) {
                addIncomingCard(o);
            }
        }
        
        incomingPanel.revalidate();
        incomingPanel.repaint();
    }

    private void addIncomingCard(Order o) {
        JPanel card = T.card(14);
        card.setLayout(new BorderLayout(0, 4));
        card.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        Order.Status[] flow = {
            Order.Status.PENDING, Order.Status.CONFIRMED,
            Order.Status.PREPARING, Order.Status.READY, Order.Status.DELIVERED
        };
        String[] labels = { "PENDING", "CONFIRMED", "PREPARING", "READY", "DONE ✓" };
        int currentIdx = getCurrentStatusIndex(o.getStatus(), flow);

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        statusRow.setOpaque(false);
        JLabel badge = T.badge(labels[currentIdx], T.statusBg(o.getStatus()), T.statusColor(o.getStatus()));
        
        JButton next = T.btn("Next ▶", new Color(99, 102, 241), new Color(79, 70, 229));
        next.setPreferredSize(new Dimension(86, 26));
        
        if (currentIdx == flow.length - 1) {
            next.setEnabled(false);
        }
        
        final int[] idx = {currentIdx};
        next.addActionListener(e -> {
            if (idx[0] < flow.length - 1) {
                idx[0]++;
                o.setStatus(flow[idx[0]]);
                badge.setText(labels[idx[0]]);
                badge.setForeground(T.statusColor(flow[idx[0]]));
                AppStore.get().sendChat(seller.getId(), new ChatMsg(ChatMsg.From.SYSTEM,
                        "📦 Order " + o.getId() + " → " + labels[idx[0]]));
                if (idx[0] == flow.length - 1)
                    next.setEnabled(false);
                refreshOrders();
                refreshIncomingPanel();
            }
        });
        
        statusRow.add(badge);
        statusRow.add(next);
        card.add(statusRow, BorderLayout.NORTH);

        JPanel info = new JPanel(new GridLayout(4, 1, 0, 2));
        info.setOpaque(false);
        JLabel bLbl = new JLabel("👤 " + o.getBuyerName());
        bLbl.setFont(T.FBO);
        bLbl.setForeground(T.PINK_D);
        JLabel pLbl = new JLabel("📱 " + (o.getBuyerPhone().isEmpty() ? "—" : o.getBuyerPhone()));
        pLbl.setFont(T.FS);
        pLbl.setForeground(T.GRAY);
        JLabel aLbl = new JLabel("📍 " + (o.getBuyerAddress().isEmpty() ? "—" : o.getBuyerAddress()));
        aLbl.setFont(T.FS);
        aLbl.setForeground(T.GRAY);
        JPanel iRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        iRow.setOpaque(false);
        JLabel iLbl = new JLabel(o.getSummary());
        iLbl.setFont(T.FB);
        iLbl.setForeground(T.DARK);
        JLabel tLbl = new JLabel(o.formatTotal());
        tLbl.setFont(T.FBO);
        tLbl.setForeground(T.PINK_D);
        JLabel pyLbl = T.badge(o.getPayment().name(), T.BLUE_BG, T.BLUE);
        iRow.add(iLbl);
        iRow.add(tLbl);
        iRow.add(pyLbl);
        info.add(bLbl);
        info.add(pLbl);
        info.add(aLbl);
        info.add(iRow);
        card.add(info, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btns.setOpaque(false);
        
        JButton wa = T.btn("💬 WA", new Color(37, 211, 102), new Color(18, 140, 62));
        wa.addActionListener(e -> {
            String ph = o.getBuyerPhone().replaceAll("[^0-9]", "");
            if (ph.startsWith("0"))
                ph = "62" + ph.substring(1);
            try {
                Desktop.getDesktop().browse(new URI("https://wa.me/" + ph));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        
        btns.add(wa);
        card.add(btns, BorderLayout.SOUTH);
        
        incomingPanel.add(card);
        incomingPanel.add(Box.createVerticalStrut(6));
    }

    // ── Orders pane ───────────────────────────────────────────────────────────
    private JScrollPane buildOrdersPane() {
        ordersPanel = new JPanel();
        ordersPanel.setLayout(new BoxLayout(ordersPanel, BoxLayout.Y_AXIS));
        ordersPanel.setOpaque(false);
        ordersPanel.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        JPanel wrap = new JPanel(new BorderLayout()); 
        wrap.setOpaque(false);
        wrap.add(ordersPanel, BorderLayout.NORTH);
        JScrollPane sc = new JScrollPane(wrap); 
        T.scrollFix(sc);
        return sc;
    }

    private void refreshOrders() {
        if (ordersPanel == null) return;
        ordersPanel.removeAll();
        List<Order> list = AppStore.get().getOrdersFor(seller.getId());
        if (list.isEmpty()) {
            JLabel e = new JLabel("📦 Belum ada order", SwingConstants.CENTER);
            e.setFont(T.FB); 
            e.setForeground(T.GRAY); 
            ordersPanel.add(e);
        } else {
            for (Order o : list) {
                JPanel row = T.card(10); 
                row.setLayout(new GridLayout(1, 5, 8, 0));
                row.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                JLabel id = new JLabel(o.getId()); 
                id.setFont(T.FS); 
                id.setForeground(T.PINK_D);
                JLabel byr = new JLabel("👤 " + o.getBuyerName()); 
                byr.setFont(T.FS);
                JLabel itm = new JLabel(o.getSummary()); 
                itm.setFont(T.FS);
                JLabel tot = new JLabel(o.formatTotal()); 
                tot.setFont(T.FBO); 
                tot.setForeground(T.PINK_D);
                JLabel st = T.badge(o.getStatus().name(), T.statusBg(o.getStatus()), T.statusColor(o.getStatus()));
                row.add(id); 
                row.add(byr); 
                row.add(itm); 
                row.add(tot); 
                row.add(st);
                ordersPanel.add(row); 
                ordersPanel.add(Box.createVerticalStrut(4));
            }
        }
        ordersPanel.revalidate(); 
        ordersPanel.repaint();
    }

    // ── Menu tab ──────────────────────────────────────────────────────────────
    private JPanel buildMenuTab() {
        JPanel p = T.bg(); 
        p.setLayout(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JLabel ttl = new JLabel("🍽 Menu & Stok"); 
        ttl.setFont(T.FT); 
        ttl.setForeground(T.PINK_D);
        ttl.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        p.add(ttl, BorderLayout.NORTH);

        menuListPanel = new JPanel();
        menuListPanel.setLayout(new BoxLayout(menuListPanel, BoxLayout.Y_AXIS));
        menuListPanel.setOpaque(false);

        for (FoodItem item : seller.getMenu()) {
            menuListPanel.add(menuItemCard(item));
            menuListPanel.add(Box.createVerticalStrut(8));
        }
        menuListPanel.add(Box.createVerticalStrut(8));
        JLabel addTtl = new JLabel("➕ Tambah Menu Baru"); 
        addTtl.setFont(T.FBO); 
        addTtl.setForeground(T.PINK_D);
        addTtl.setAlignmentX(Component.LEFT_ALIGNMENT);
        menuListPanel.add(addTtl); 
        menuListPanel.add(Box.createVerticalStrut(4));
        menuListPanel.add(addItemRow());

        JPanel wrap = new JPanel(new BorderLayout()); 
        wrap.setOpaque(false);
        wrap.add(menuListPanel, BorderLayout.NORTH);
        JScrollPane sc = new JScrollPane(wrap); 
        T.scrollFix(sc);
        p.add(sc, BorderLayout.CENTER);

        JPanel vp = T.card(10); 
        vp.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JLabel vl = new JLabel("🎬 Video URL:"); 
        vl.setFont(T.FBO); 
        vl.setForeground(T.PINK_D);
        JTextField vf = T.field("https://youtube.com/..."); 
        vf.setPreferredSize(new Dimension(280, 28));
        vf.setText(seller.getVideoUrl());
        JButton vs = T.btn("Simpan");
        vs.addActionListener(e -> { 
            seller.setVideoUrl(vf.getText().trim());
            JOptionPane.showMessageDialog(this, "Video URL disimpan!", "OK", JOptionPane.INFORMATION_MESSAGE); 
        });
        vp.add(vl); 
        vp.add(vf); 
        vp.add(vs);
        p.add(vp, BorderLayout.SOUTH);
        return p;
    }

    private JPanel menuItemCard(FoodItem item) {
        JPanel card = T.card(12);
        card.setLayout(new BorderLayout(10, 0));
        card.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel imgBox = new JPanel() {
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (item.getImageIcon() != null) {
                    g2.drawImage(item.getImageIcon().getImage(), 0, 0, 70, 70, null);
                } else {
                    g2.setColor(T.PINK_P); 
                    g2.fillRoundRect(0, 0, 70, 70, 10, 10);
                    g2.setFont(T.f(28, Font.PLAIN));
                    FontMetrics fm = g2.getFontMetrics();
                    String em = item.getEmoji();
                    g2.drawString(em, (70 - fm.stringWidth(em)) / 2, (70 + fm.getAscent() - fm.getDescent()) / 2);
                }
                g2.dispose();
            }
        };
        imgBox.setPreferredSize(new Dimension(70, 70)); 
        imgBox.setOpaque(false);
        imgBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        imgBox.setToolTipText("Klik untuk upload foto");
        imgBox.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                JFileChooser fc = new JFileChooser();
                fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Gambar", "jpg", "jpeg", "png", "gif"));
                if (fc.showOpenDialog(SellerWindow.this) == JFileChooser.APPROVE_OPTION) {
                    try {
                        ImageIcon icon = new ImageIcon(fc.getSelectedFile().getAbsolutePath());
                        Image scaled = icon.getImage().getScaledInstance(70, 70, Image.SCALE_SMOOTH);
                        item.setImageIcon(new ImageIcon(scaled));
                        imgBox.repaint();
                        AppStore.get().fireMenu(seller);
                    } catch (Exception ex) { 
                        ex.printStackTrace(); 
                    }
                }
            }
        });
        card.add(imgBox, BorderLayout.WEST);

        JPanel fields = new JPanel(new GridLayout(2, 4, 6, 4)); 
        fields.setOpaque(false);
        JLabel nm = new JLabel(item.getEmoji() + " " + item.getName()); 
        nm.setFont(T.FBO); 
        nm.setForeground(T.DARK);
        JTextField priceF = T.field("Harga"); 
        priceF.setText(String.valueOf(item.getPrice()));
        JLabel stk = new JLabel("Stok: " + item.getStock()); 
        stk.setFont(T.FBO);
        stk.setForeground(item.getStock() > 0 ? T.GREEN : T.RED);
        JSpinner sp = new JSpinner(new SpinnerNumberModel(item.getStock(), 0, 999, 1)); 
        sp.setFont(T.FS);
        JLabel prLbl = new JLabel(item.formatPrice()); 
        prLbl.setFont(T.FS); 
        prLbl.setForeground(T.PINK_D);
        JTextField emojiF = T.field("Emoji"); 
        emojiF.setText(item.getEmoji()); 
        emojiF.setPreferredSize(new Dimension(48, 26));
        JLabel hint2 = new JLabel("🖼 Klik kotak kiri = ganti foto"); 
        hint2.setFont(T.FX); 
        hint2.setForeground(T.GRAY);
        JButton sv = T.btn("💾 Simpan");
        sv.addActionListener(e -> {
            try {
                item.setPrice(Long.parseLong(priceF.getText().trim()));
                item.setStock((int) sp.getValue());
                if (!emojiF.getText().trim().isEmpty()) 
                    item.setEmoji(emojiF.getText().trim());
                nm.setText(item.getEmoji() + " " + item.getName());
                stk.setText("Stok: " + item.getStock());
                stk.setForeground(item.getStock() > 0 ? T.GREEN : T.RED);
                prLbl.setText(item.formatPrice());
                imgBox.repaint();
                AppStore.get().fireMenu(seller);
                JOptionPane.showMessageDialog(this, item.getName() + " diperbarui!", "OK", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Harga harus angka!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        fields.add(nm); 
        fields.add(priceF); 
        fields.add(stk); 
        fields.add(sp);
        fields.add(prLbl); 
        fields.add(emojiF); 
        fields.add(hint2); 
        fields.add(sv);
        card.add(fields, BorderLayout.CENTER);
        return card;
    }

    private JPanel addItemRow() {
        JPanel row = T.card(10); 
        row.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56)); 
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField ef = T.field("🍚"); 
        ef.setPreferredSize(new Dimension(44, 28)); 
        ef.setText("🍚");
        JTextField nf = T.field("Nama menu"); 
        nf.setPreferredSize(new Dimension(120, 28));
        JTextField pf = T.field("Harga"); 
        pf.setPreferredSize(new Dimension(80, 28));
        JTextField sf = T.field("Stok"); 
        sf.setPreferredSize(new Dimension(55, 28));
        JButton add = T.btn("➕ Tambah");
        add.addActionListener(e -> {
            String nm = nf.getText().trim();
            if (nm.isEmpty()) { 
                JOptionPane.showMessageDialog(this, "Nama kosong!", "Error", JOptionPane.ERROR_MESSAGE); 
                return; 
            }
            try {
                long price = Long.parseLong(pf.getText().trim());
                int stock = Integer.parseInt(sf.getText().trim());
                String emoji = ef.getText().trim().isEmpty() ? "🍚" : ef.getText().trim();
                FoodItem fi = new FoodItem("x" + System.currentTimeMillis(), nm, price, stock, emoji);
                seller.addItem(fi);
                int at = menuListPanel.getComponentCount() - 4;
                menuListPanel.add(menuItemCard(fi), at);
                menuListPanel.add(Box.createVerticalStrut(8), at + 1);
                menuListPanel.revalidate(); 
                menuListPanel.repaint();
                AppStore.get().fireMenu(seller);
                nf.setText(""); 
                pf.setText(""); 
                sf.setText("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Harga/stok harus angka!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        row.add(ef); 
        row.add(nf); 
        row.add(pf); 
        row.add(sf); 
        row.add(add);
        return row;
    }

    // ── Map tab ───────────────────────────────────────────────────────────────
    private JPanel buildMapTab() {
        JPanel p = T.bg(); 
        p.setLayout(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JLabel ttl = new JLabel("🗺 Lokasi Seller"); 
        ttl.setFont(T.FT); 
        ttl.setForeground(T.PINK_D);
        ttl.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        p.add(ttl, BorderLayout.NORTH);
        JPanel card = T.card(14); 
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        JLabel al = new JLabel("📍 " + seller.getAddress()); 
        al.setFont(T.FB); 
        al.setForeground(T.DARK); 
        al.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel cl = new JLabel(String.format("🌏 %.4f, %.4f", seller.getLat(), seller.getLng())); 
        cl.setFont(T.FS); 
        cl.setForeground(T.GRAY); 
        cl.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton mb = T.btn("🗺 Buka Google Maps"); 
        mb.setAlignmentX(Component.LEFT_ALIGNMENT);
        mb.addActionListener(e -> {
            try { 
                Desktop.getDesktop().browse(new URI("https://maps.google.com/?q=" + seller.getLat() + "," + seller.getLng())); 
            } catch (Exception ex) { 
                ex.printStackTrace(); 
            }
        });
        card.add(al); 
        card.add(Box.createVerticalStrut(8)); 
        card.add(cl); 
        card.add(Box.createVerticalStrut(16)); 
        card.add(mb);
        p.add(card, BorderLayout.CENTER);
        return p;
    }

    // ── Settings tab ─────────────────────────────────────────────────────────
    private JPanel buildSettingsTab() {
        JPanel p = T.bg(); 
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        
        JLabel ttl = new JLabel("⚙ Settings"); 
        ttl.setFont(T.FT); 
        ttl.setForeground(T.PINK_D);
        ttl.setAlignmentX(Component.LEFT_ALIGNMENT); 
        ttl.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        p.add(ttl);

        // PROFIL SECTION
        JPanel pc = T.card(12); 
        pc.setLayout(new BoxLayout(pc, BoxLayout.Y_AXIS));
        pc.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        pc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200)); 
        pc.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel ptl = new JLabel("👤 Edit Profil"); 
        ptl.setFont(T.FBO); 
        ptl.setForeground(T.PINK_D); 
        ptl.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel pf = new JPanel(new GridLayout(3, 2, 10, 7)); 
        pf.setOpaque(false); 
        pf.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JTextField nf = T.field("Nama"); 
        nf.setText(seller.getName());
        JTextField phf = T.field("Telepon"); 
        phf.setText(seller.getPhone());
        JTextField af = T.field("Alamat"); 
        af.setText(seller.getAddress());
        
        addPairF(pf, "Nama:", nf); 
        addPairF(pf, "Telepon:", phf); 
        addPairF(pf, "Alamat:", af);
        
        JButton sv = T.btn("💾 Simpan"); 
        sv.setAlignmentX(Component.LEFT_ALIGNMENT);
        sv.addActionListener(e -> {
            seller.setName(nf.getText().trim()); 
            seller.setPhone(phf.getText().trim()); 
            seller.setAddress(af.getText().trim());
            nameLbl.setText("🍪 " + seller.getName() + "   •   ID: " + seller.getId());
            subLbl.setText("📱 " + seller.getPhone() + "   📍 " + seller.getAddress());
            setTitle("🍪 Seller Dashboard  —  " + seller.getName());
            JOptionPane.showMessageDialog(this, "Profil disimpan!", "OK", JOptionPane.INFORMATION_MESSAGE);
        });
        
        pc.add(ptl); 
        pc.add(Box.createVerticalStrut(8)); 
        pc.add(pf); 
        pc.add(Box.createVerticalStrut(10)); 
        pc.add(sv);
        p.add(pc); 
        p.add(Box.createVerticalStrut(14));

        // GEMINI AI SECTION (GRATIS)
        JPanel geminiPanel = T.card(12);
        geminiPanel.setLayout(new BoxLayout(geminiPanel, BoxLayout.Y_AXIS));
        geminiPanel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        geminiPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        geminiPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel geminiTitle = new JLabel("🤖 Google Gemini AI (GRATIS)");
        geminiTitle.setFont(T.FBO);
        geminiTitle.setForeground(new Color(16, 185, 129));
        geminiTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel geminiInfo = new JLabel("AI gratis tanpa batas. Klik tombol di bawah untuk mendapatkan API Key!");
        geminiInfo.setFont(T.FS);
        geminiInfo.setForeground(T.GRAY);
        geminiInfo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton getGeminiKey = T.btn("🔑 Dapatkan API Key di Google AI Studio", new Color(16, 185, 129), new Color(5, 150, 105));
        getGeminiKey.setAlignmentX(Component.LEFT_ALIGNMENT);
        getGeminiKey.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://aistudio.google.com/app/apikey"));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Gagal membuka browser: " + ex.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JTextField geminiKeyField = T.field("Masukkan Gemini API Key...");
        String currentKey = AIService.getKey();
        geminiKeyField.setText(currentKey != null ? currentKey : "");
        geminiKeyField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        geminiKeyField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton saveGeminiKey = T.btn("💾 Simpan Gemini API Key");
        saveGeminiKey.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveGeminiKey.addActionListener(e -> {
            AIService.setKey(geminiKeyField.getText().trim());
            JOptionPane.showMessageDialog(this, 
                "✅ Gemini API Key disimpan!\nSekarang AI akan menggunakan Gemini (gratis).", 
                "Sukses", JOptionPane.INFORMATION_MESSAGE);
        });

        geminiPanel.add(geminiTitle);
        geminiPanel.add(Box.createVerticalStrut(4));
        geminiPanel.add(geminiInfo);
        geminiPanel.add(Box.createVerticalStrut(8));
        geminiPanel.add(getGeminiKey);
        geminiPanel.add(Box.createVerticalStrut(8));
        geminiPanel.add(geminiKeyField);
        geminiPanel.add(Box.createVerticalStrut(8));
        geminiPanel.add(saveGeminiKey);

        p.add(geminiPanel);
        p.add(Box.createVerticalStrut(14));

        // ANTHROPIC SECTION (BERBAYAR)
        JPanel ac = T.card(12); 
        ac.setLayout(new BoxLayout(ac, BoxLayout.Y_AXIS));
        ac.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        ac.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170)); 
        ac.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel atl = new JLabel("🧠 Anthropic API Key (Berbayar)"); 
        atl.setFont(T.FBO); 
        atl.setForeground(T.PINK_D); 
        atl.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel info = new JLabel("Dapatkan di: console.anthropic.com  →  API Keys  →  Create Key"); 
        info.setFont(T.FS); 
        info.setForeground(T.GRAY); 
        info.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JTextField apif = T.field("sk-ant-api03-..."); 
        apif.setText(AIService.getKey());
        apif.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30)); 
        apif.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton asv = T.btn("💾 Simpan API Key"); 
        asv.setAlignmentX(Component.LEFT_ALIGNMENT);
        asv.addActionListener(e -> { 
            AIService.setKey(apif.getText().trim());
            JOptionPane.showMessageDialog(this, "API Key disimpan!", "OK", JOptionPane.INFORMATION_MESSAGE); 
        });
        
        JButton open = T.obtn("🌐 Buka console.anthropic.com", T.PINK); 
        open.setAlignmentX(Component.LEFT_ALIGNMENT);
        open.addActionListener(e -> { 
            try { 
                Desktop.getDesktop().browse(new URI("https://console.anthropic.com/settings/keys")); 
            } catch(Exception ex){} 
        });
        
        ac.add(atl); 
        ac.add(Box.createVerticalStrut(4)); 
        ac.add(info);
        ac.add(Box.createVerticalStrut(8)); 
        ac.add(apif); 
        ac.add(Box.createVerticalStrut(8));
        ac.add(asv); 
        ac.add(Box.createVerticalStrut(6)); 
        ac.add(open);
        p.add(ac);
        
        return p;
    }

    private void addPairF(JPanel p, String k, JTextField f) {
        JLabel l = new JLabel(k); 
        l.setFont(T.FBO); 
        l.setForeground(T.GRAY); 
        p.add(l); 
        p.add(f);
    }

    private void showRevenue() {
        List<Order> orders = AppStore.get().getOrdersFor(seller.getId());
        long total = orders.stream().mapToLong(Order::getTotal).sum();
        JOptionPane.showMessageDialog(this,
            "💰 Total Revenue: " + AppStore.rp(total) +
            "\n📋 Jumlah Order: " + orders.size() +
            "\n⭐ Rating: " + seller.formatRating(),
            "Revenue - " + seller.getName(), JOptionPane.INFORMATION_MESSAGE);
    }
}