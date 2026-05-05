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
        setTitle("Seller Dashboard — " + initial.getName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(700, 500));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        buildUI();
        AppStore.get().onOrder(o -> {
            if (o.getSellerId().equals(seller.getId()))
                SwingUtilities.invokeLater(() -> { addIncoming(o); refreshOrders(); });
        });
        
        // Notification for incoming buyer messages
        AppStore.get().onChat(m -> {
            if (m.from == ChatMsg.From.BUYER && m.sellerId != null && m.sellerId.equals(seller.getId())) {
                SwingUtilities.invokeLater(() -> {
                    // Only notify if not in the chat tab or if chatting with someone else?
                    // Actually, let's just always show it if the window is active but tab is different
                    if (tabs.getSelectedIndex() != 1) { // Tab 1 is Personal Chat
                        JOptionPane.showMessageDialog(this, 
                            "<html><b>New Message from Buyer:</b><br>" + m.text + "</html>",
                            "New Message", JOptionPane.INFORMATION_MESSAGE);
                    }
                });
            }
        });
        
        // Smart Match Listener
        AppStore.get().onChat(m -> {
            if (m.sellerId != null && m.sellerId.equals(AppStore.BROADCAST_ID) && m.from == ChatMsg.From.BUYER) {
                // Check if any item in our menu matches the request (parsed simple way)
                String txt = m.text.toLowerCase();
                for (FoodItem item : seller.getMenu()) {
                    if (txt.contains(item.getName().toLowerCase()) || 
                        (txt.contains("k") && item.getPrice() <= 21000)) { // Simple demo logic
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this, "🤖 Smart Match! Request matches your: " + item.getName());
                        });
                        break;
                    }
                }
            }
        });
        
        AppStore.get().onReview(() -> SwingUtilities.invokeLater(() -> {
            ratingLbl.setText(T.stars(seller.getRating()) + "  " + seller.formatRating());
            if (tabs.getSelectedIndex() == 3) {
                tabs.setComponentAt(3, buildReviewsTab());
            }
        }));
    }

    private void buildUI() {
        JPanel root = T.bg(); root.setLayout(new BorderLayout());
        root.add(topBar(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerSize(4);
        split.setResizeWeight(0.3);
        split.setBorder(null); split.setOpaque(false);

        // LEFT incoming
        JPanel left = T.bg(); left.setLayout(new BorderLayout());
        left.setBorder(BorderFactory.createEmptyBorder(10,10,10,6));
        JLabel inTitle = new JLabel("Incoming Requests");
        inTitle.setFont(T.FH); inTitle.setForeground(T.PINK_D);
        inTitle.setBorder(BorderFactory.createEmptyBorder(0,0,8,0));
        left.add(inTitle, BorderLayout.NORTH);

        incomingPanel = new JPanel();
        incomingPanel.setLayout(new BoxLayout(incomingPanel, BoxLayout.Y_AXIS));
        incomingPanel.setOpaque(false);
        JLabel hint = new JLabel("Waiting for orders...", SwingConstants.CENTER);
        hint.setFont(T.FS); hint.setForeground(T.GRAY);
        incomingPanel.add(hint);

        JPanel inWrap = new JPanel(new BorderLayout()); inWrap.setOpaque(false);
        inWrap.add(incomingPanel, BorderLayout.NORTH);
        JScrollPane inScroll = new JScrollPane(inWrap); T.scrollFix(inScroll);
        left.add(inScroll, BorderLayout.CENTER);
        split.setLeftComponent(left);

        // RIGHT tabs
        tabs = new JTabbedPane();
        tabs.setFont(T.FB); tabs.setBackground(T.PINK_L); tabs.setForeground(T.PINK_D);
        tabs.addTab("Broadcast",   new ChatPanel(AppStore.BROADCAST_ID, ChatMsg.From.SELLER, "Seller"));
        tabs.addTab("Personal Chat", new ChatPanel(seller.getId(), ChatMsg.From.SELLER, "Seller"));
        tabs.addTab("Orders",      buildOrdersPane());
        tabs.addTab("Reviews",     buildReviewsTab());
        tabs.addTab("Menu & Stock", buildMenuTab());
        
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 3) { // Reviews Tab
                tabs.setComponentAt(3, buildReviewsTab());
            }
        });
        tabs.addTab("Map",        buildMapTab());
        tabs.addTab("Settings",          buildSettingsTab());

        JPanel rightP = new JPanel(new BorderLayout());
        rightP.setOpaque(false);
        rightP.setBorder(BorderFactory.createEmptyBorder(10,6,10,10));
        rightP.add(tabs, BorderLayout.CENTER);
        split.setRightComponent(rightP);

        root.add(split, BorderLayout.CENTER);
        setContentPane(root);
        refreshOrders();
    }

    // ── Top bar ───────────────────────────────────────────────────────────────
    private JPanel topBar() {
        JPanel bar = new JPanel(new BorderLayout(10,0));
        bar.setOpaque(false);
        bar.setBorder(new CompoundBorder(
            new MatteBorder(0,0,1,0,T.PINK_B),
            new EmptyBorder(8,14,8,14)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT,10,0)); left.setOpaque(false);
        JPanel inf = new JPanel(new BorderLayout(0,2)); inf.setOpaque(false);
        nameLbl = new JLabel(seller.getName()+"   •   ID: "+seller.getId());
        nameLbl.setFont(T.FT); nameLbl.setForeground(T.PINK_D);
        subLbl = new JLabel("Phone: "+seller.getPhone()+"   Addr: "+seller.getAddress());
        subLbl.setFont(T.FS); subLbl.setForeground(T.GRAY);
        inf.add(nameLbl, BorderLayout.NORTH); inf.add(subLbl, BorderLayout.SOUTH);
        left.add(inf);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); right.setOpaque(false);

        List<Seller> sellers = AppStore.get().getSellers();
        String[] names = sellers.stream()
            .map(s -> s.getName()).toArray(String[]::new);
        JComboBox<String> dd = new JComboBox<>(names);
        dd.setFont(T.FBO); dd.setSelectedIndex(sellers.indexOf(seller));
        dd.setPreferredSize(new Dimension(200,32));
        dd.addActionListener(e -> {
            int idx = dd.getSelectedIndex();
            if (idx >= 0) switchSeller(sellers.get(idx));
        });

        ratingLbl = new JLabel(T.stars(seller.getRating())+"  "+seller.formatRating());
        ratingLbl.setFont(T.FBO); ratingLbl.setForeground(T.STAR);

        JButton rev = T.btn("Revenue", new Color(16,185,129), new Color(5,150,105));
        rev.addActionListener(e -> showRevenue());

        JLabel swLbl = new JLabel("Switch Seller:"); swLbl.setFont(T.FBO); swLbl.setForeground(T.PINK_D);
        right.add(swLbl); right.add(dd); right.add(ratingLbl); right.add(rev);
        bar.add(left, BorderLayout.WEST); bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void switchSeller(Seller s) {
        this.seller = s;
        nameLbl.setText(s.getName()+"   •   ID: "+s.getId());
        subLbl.setText("Phone: "+s.getPhone()+"   Addr: "+s.getAddress());
        ratingLbl.setText(T.stars(s.getRating())+"  "+s.formatRating());
        setTitle("Seller Dashboard  —  "+s.getName());

        incomingPanel.removeAll();
        JLabel hint = new JLabel("Waiting for orders...", SwingConstants.CENTER);
        hint.setFont(T.FS); hint.setForeground(T.GRAY);
        incomingPanel.add(hint);
        AppStore.get().getOrdersFor(s.getId()).forEach(o -> addIncoming(o));
        incomingPanel.revalidate(); incomingPanel.repaint();

        tabs.setComponentAt(0, new ChatPanel(AppStore.BROADCAST_ID, ChatMsg.From.SELLER, "Seller"));
        tabs.setComponentAt(1, new ChatPanel(s.getId(), ChatMsg.From.SELLER, "Seller"));
        tabs.setComponentAt(3, buildReviewsTab());
        tabs.setComponentAt(4, buildMenuTab());
        tabs.setComponentAt(5, buildMapTab());
        tabs.setComponentAt(6, buildSettingsTab());
        refreshOrders();
    }

    // ── Incoming (RATING UNTUK CUSTOMER DIHAPUS) ──────────────────────────────────────────────
    private void addIncoming(Order o) {
        if (incomingPanel.getComponentCount()==1
                && incomingPanel.getComponent(0) instanceof JLabel)
            incomingPanel.removeAll();

        JPanel card = T.card(14);
        card.setLayout(new BorderLayout(0,4));
        card.setBorder(BorderFactory.createEmptyBorder(10,12,10,12));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE,200));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        Order.Status[] flow = {
            Order.Status.PENDING, Order.Status.CONFIRMED,
            Order.Status.PREPARING, Order.Status.READY, Order.Status.DELIVERED
        };
        String[] labels = {"PENDING","CONFIRMED","PREPARING","READY","DONE"};
        final int[] idx = {0};

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT,4,0));
        statusRow.setOpaque(false);
        JLabel badge = T.badge(labels[0], T.statusBg(o.getStatus()), T.statusColor(o.getStatus()));
        statusRow.add(badge);
        card.add(statusRow, BorderLayout.NORTH);

        JPanel info = new JPanel(new GridLayout(4,1,0,2)); info.setOpaque(false);
        JLabel bLbl = new JLabel("Buyer: "+o.getBuyerName()); bLbl.setFont(T.FBO); bLbl.setForeground(T.PINK_D);
        JLabel pLbl = new JLabel("Phone: "+(o.getBuyerPhone().isEmpty()?"—":o.getBuyerPhone())); pLbl.setFont(T.FS); pLbl.setForeground(T.GRAY);
        JLabel aLbl = new JLabel("Addr: "+(o.getBuyerAddress().isEmpty()?"—":o.getBuyerAddress())); aLbl.setFont(T.FS); aLbl.setForeground(T.GRAY);
        JPanel iRow = new JPanel(new FlowLayout(FlowLayout.LEFT,6,0)); iRow.setOpaque(false);
        JLabel iLbl = new JLabel(o.getSummary()); iLbl.setFont(T.FB); iLbl.setForeground(T.DARK);
        JLabel tLbl = new JLabel(o.formatTotal()); tLbl.setFont(T.FBO); tLbl.setForeground(T.PINK_D);
        JLabel pyLbl = T.badge(o.getPayment().name(), T.BLUE_BG, T.BLUE);
        iRow.add(iLbl); iRow.add(tLbl); iRow.add(pyLbl);
        info.add(bLbl); info.add(pLbl); info.add(aLbl); info.add(iRow);
        card.add(info, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT,4,0)); btns.setOpaque(false);
        JButton wa = T.btn("Chat WA", new Color(37,211,102), new Color(18,140,62));
        wa.addActionListener(e -> {
            String ph = o.getBuyerPhone().replaceAll("[^0-9]","");
            if (ph.startsWith("0")) ph="62"+ph.substring(1);
            try { Desktop.getDesktop().browse(new URI("https://wa.me/"+ph)); }
            catch (Exception ex) { ex.printStackTrace(); }
        });
        
        // RATING BUTTON UNTUK CUSTOMER DIHAPUS - HANYA WA SAJA
        
        btns.add(wa);
        
        JButton next = T.btn("Next Status", new Color(99,102,241), new Color(79,70,229));
        next.setPreferredSize(new Dimension(100,28));
        next.addActionListener(e -> {
            if (idx[0] < flow.length-1) {
                idx[0]++;
                o.setStatus(flow[idx[0]]);
                badge.setText(labels[idx[0]]);
                badge.setForeground(T.statusColor(flow[idx[0]]));
                
                String updateMsg = "Order " + o.getId() + " -> " + labels[idx[0]];
                // Send to personal chat
                AppStore.get().sendChat(seller.getId(), new ChatMsg(ChatMsg.From.SYSTEM, updateMsg));
                // ALSO send to BROADCAST chat so Buyer sees it without switching
                AppStore.get().sendChat(AppStore.BROADCAST_ID, new ChatMsg(ChatMsg.From.SYSTEM, 
                    "[" + seller.getName() + "] " + updateMsg));
                
                if (idx[0]==flow.length-1) next.setEnabled(false);
                refreshOrders();
            }
        });
        btns.add(next);
        
        card.add(btns, BorderLayout.SOUTH);
        
        incomingPanel.add(card); incomingPanel.add(Box.createVerticalStrut(6));
        incomingPanel.revalidate(); incomingPanel.repaint();
    }

    // ── Orders pane ───────────────────────────────────────────────────────────
    private JScrollPane buildOrdersPane() {
        ordersPanel = new JPanel();
        ordersPanel.setLayout(new BoxLayout(ordersPanel, BoxLayout.Y_AXIS));
        ordersPanel.setOpaque(false);
        ordersPanel.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        JPanel wrap = new JPanel(new BorderLayout()); wrap.setOpaque(false);
        wrap.add(ordersPanel, BorderLayout.NORTH);
        JScrollPane sc = new JScrollPane(wrap); T.scrollFix(sc);
        return sc;
    }

    private void refreshOrders() {
        if (ordersPanel==null) return;
        ordersPanel.removeAll();
        List<Order> list = AppStore.get().getOrdersFor(seller.getId());
        if (list.isEmpty()) {
            JLabel e = new JLabel("No orders yet",SwingConstants.CENTER);
            e.setFont(T.FB); e.setForeground(T.GRAY); ordersPanel.add(e);
        } else {
            for (Order o : list) {
                JPanel row = T.card(10); row.setLayout(new GridLayout(1,5,8,0));
                row.setBorder(BorderFactory.createEmptyBorder(7,12,7,12));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE,48));
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                JLabel id  = new JLabel(o.getId()); id.setFont(T.FS); id.setForeground(T.PINK_D);
                JLabel byr = new JLabel(o.getBuyerName()); byr.setFont(T.FS);
                JLabel itm = new JLabel(o.getSummary()); itm.setFont(T.FS);
                JLabel tot = new JLabel(o.formatTotal()); tot.setFont(T.FBO); tot.setForeground(T.PINK_D);
                JLabel st  = T.badge(o.getStatus().name(),T.statusBg(o.getStatus()),T.statusColor(o.getStatus()));
                row.add(id); row.add(byr); row.add(itm); row.add(tot); row.add(st);
                ordersPanel.add(row); ordersPanel.add(Box.createVerticalStrut(4));
                T.fade(row);
            }
        }
        ordersPanel.revalidate(); ordersPanel.repaint();
    }

    // ── Menu tab ──────────────────────────────────────────────────────────────
    private JPanel buildMenuTab() {
        JPanel p = T.bg(); p.setLayout(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        JLabel ttl = new JLabel("Menu & Stock"); ttl.setFont(T.FT); ttl.setForeground(T.PINK_D);
        ttl.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));
        p.add(ttl, BorderLayout.NORTH);

        menuListPanel = new JPanel();
        menuListPanel.setLayout(new BoxLayout(menuListPanel, BoxLayout.Y_AXIS));
        menuListPanel.setOpaque(false);

        for (FoodItem item : seller.getMenu()) {
            menuListPanel.add(menuItemCard(item));
            menuListPanel.add(Box.createVerticalStrut(8));
        }
        menuListPanel.add(Box.createVerticalStrut(8));
        JLabel addTtl = new JLabel("Add New Menu Item"); addTtl.setFont(T.FBO); addTtl.setForeground(T.PINK_D);
        addTtl.setAlignmentX(Component.LEFT_ALIGNMENT);
        menuListPanel.add(addTtl); menuListPanel.add(Box.createVerticalStrut(4));
        menuListPanel.add(addItemRow());

        JPanel wrap = new JPanel(new BorderLayout()); wrap.setOpaque(false);
        wrap.add(menuListPanel, BorderLayout.NORTH);
        JScrollPane sc = new JScrollPane(wrap); T.scrollFix(sc);
        p.add(sc, BorderLayout.CENTER);

        JPanel vp = T.card(10); vp.setLayout(new FlowLayout(FlowLayout.LEFT,12,10));
        JLabel vl = new JLabel("🎬 Promo Video:"); vl.setFont(T.FBO); vl.setForeground(T.PINK_D);
        JButton vBtn = T.obtn("Set Video URL (Manual)", T.PINK_D);
        vBtn.addActionListener(e -> {
            String res = JOptionPane.showInputDialog(this, "Enter YouTube Video URL:", seller.getVideoUrl());
            if (res != null) {
                seller.setVideoUrl(res.trim());
                JOptionPane.showMessageDialog(this, "Video URL updated!", "OK", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        vp.add(vl); vp.add(vBtn);
        p.add(vp, BorderLayout.SOUTH);
        return p;
    }

    private JPanel menuItemCard(FoodItem item) {
        JPanel card = T.card(12);
        card.setLayout(new BorderLayout(10,0));
        card.setBorder(BorderFactory.createEmptyBorder(8,10,8,10));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE,90));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel imgBox = new JPanel() {
            public void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                if (item.getImageIcon()!=null) {
                    g2.drawImage(item.getImageIcon().getImage(),0,0,70,70,null);
                } else {
                    g2.setColor(T.PINK_L); 
                    g2.fillRoundRect(0,0,70,70,10,10);
                    g2.setColor(T.PINK_B);
                    T.drawIcon(g2, "food", 15, 15, 40);
                }
                g2.dispose();
            }
        };
        imgBox.setPreferredSize(new Dimension(70,70)); imgBox.setOpaque(false);
        imgBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        imgBox.setToolTipText("Click to upload photo");
        imgBox.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                JFileChooser fc = new JFileChooser();
                fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Images","jpg","jpeg","png","gif"));
                if (fc.showOpenDialog(SellerWindow.this)==JFileChooser.APPROVE_OPTION) {
                    try {
                        ImageIcon icon = new ImageIcon(fc.getSelectedFile().getAbsolutePath());
                        Image scaled = icon.getImage().getScaledInstance(70,70,Image.SCALE_SMOOTH);
                        item.setImageIcon(new ImageIcon(scaled));
                        imgBox.repaint();
                        AppStore.get().fireMenu(seller);
                    } catch (Exception ex) { ex.printStackTrace(); }
                }
            }
        });
        card.add(imgBox, BorderLayout.WEST);

        JPanel fields = new JPanel(new GridLayout(2,4,6,4)); fields.setOpaque(false);
        JLabel nm  = new JLabel(item.getName()); nm.setFont(T.FBO); nm.setForeground(T.DARK);
        JTextField priceF = T.field("Price"); priceF.setText(String.valueOf(item.getPrice()));
        priceF.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel stk = new JLabel("Stock: "+item.getStock()); stk.setFont(T.FBO);
        stk.setForeground(item.getStock()>0?T.GREEN:T.RED);
        JSpinner sp = new JSpinner(new SpinnerNumberModel(item.getStock(),0,999,1)); sp.setFont(T.FS);
        JLabel prLbl = new JLabel(item.formatPrice()); prLbl.setFont(T.FS); prLbl.setForeground(T.PINK_D);
        JLabel hint2 = new JLabel("🖼 Click left box = change photo"); hint2.setFont(T.FX); hint2.setForeground(T.GRAY);
        JButton sv = T.btn("Save");
        sv.addActionListener(e -> {
            try {
                item.setPrice(Long.parseLong(priceF.getText().trim()));
                item.setStock((int)sp.getValue());
                nm.setText(item.getName());
                stk.setText("Stock: "+item.getStock());
                stk.setForeground(item.getStock()>0?T.GREEN:T.RED);
                prLbl.setText(item.formatPrice());
                imgBox.repaint();
                AppStore.get().fireMenu(seller);
                JOptionPane.showMessageDialog(this,item.getName()+" updated!","OK",JOptionPane.INFORMATION_MESSAGE);
            } catch(NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,"Price must be a number!","Error",JOptionPane.ERROR_MESSAGE);
            }
        });
        fields.add(nm); fields.add(priceF); fields.add(stk); fields.add(sp);
        fields.add(prLbl); fields.add(new JLabel()); fields.add(hint2); fields.add(sv);
        card.add(fields, BorderLayout.CENTER);
        return card;
    }

    private JPanel addItemRow() {
        JPanel row = T.card(10); row.setLayout(new FlowLayout(FlowLayout.LEFT,8,6));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE,56)); row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField nf = T.field("Menu name"); nf.setPreferredSize(new Dimension(160,28));
        JTextField pf = T.field("Price"); pf.setPreferredSize(new Dimension(100,28));
        JTextField sf = T.field("Stock"); sf.setPreferredSize(new Dimension(70,28));
        JButton add = T.btn("Add");
        add.addActionListener(e -> {
            String nm = nf.getText().trim();
            if (nm.isEmpty()) { JOptionPane.showMessageDialog(this,"Name is empty!","Error",JOptionPane.ERROR_MESSAGE); return; }
            try {
                long price = Long.parseLong(pf.getText().trim());
                int stock = Integer.parseInt(sf.getText().trim());
                FoodItem fi = new FoodItem("it"+System.currentTimeMillis(),nm,price,stock,"🍲");
                seller.addItem(fi);
                int at = menuListPanel.getComponentCount()-3;
                menuListPanel.add(menuItemCard(fi),at);
                menuListPanel.add(Box.createVerticalStrut(8),at+1);
                menuListPanel.revalidate(); menuListPanel.repaint();
                AppStore.get().fireMenu(seller);
                nf.setText(""); pf.setText(""); sf.setText("");
            } catch(NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,"Price/stock must be a number!","Error",JOptionPane.ERROR_MESSAGE);
            }
        });
        row.add(nf); row.add(pf); row.add(sf); row.add(add);
        return row;
    }

    // ── Map tab with Google Maps API ───────────────────────────────────────────────
    private JPanel buildMapTab() {
        JPanel p = T.bg(); p.setLayout(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        JLabel ttl = new JLabel("Seller Location"); ttl.setFont(T.FT); ttl.setForeground(T.PINK_D);
        ttl.setBorder(BorderFactory.createEmptyBorder(0,0,14,0));
        p.add(ttl, BorderLayout.NORTH);
        
        JPanel card = T.card(14); card.setLayout(new BoxLayout(card,BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(16,20,16,20));
        
        JLabel al = new JLabel("Addr: "+seller.getAddress()); al.setFont(T.FB); al.setForeground(T.DARK);
        al.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel cl = new JLabel(String.format("Coord: %.4f, %.4f",seller.getLat(),seller.getLng())); 
        cl.setFont(T.FS); cl.setForeground(T.GRAY); cl.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Tombol untuk membuka Google Maps dengan navigasi
        JButton mb = T.btn("Open Google Maps", new Color(66, 133, 244), new Color(52, 105, 195));
        mb.setAlignmentX(Component.LEFT_ALIGNMENT);
        mb.addActionListener(e -> {
            try {
                // Google Maps URL dengan navigasi langsung
                String mapsUrl = "https://www.google.com/maps/search/?api=1&query=" 
                        + seller.getLat() + "," + seller.getLng();
                Desktop.getDesktop().browse(new URI(mapsUrl));
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, 
                    "Failed to open Google Maps: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // Tombol untuk mendapatkan arah/rute
        JButton dirBtn = T.obtn("Get Directions", T.PINK);
        dirBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        dirBtn.addActionListener(e -> {
            try {
                // Google Maps Directions URL
                String dirUrl = "https://www.google.com/maps/dir/?api=1&destination=" 
                        + seller.getLat() + "," + seller.getLng();
                Desktop.getDesktop().browse(new URI(dirUrl));
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, 
                    "Failed to open directions: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // Static map image from Google Maps Static API (optional - requires API key)
        // Tampilkan preview peta kecil
        JPanel previewPanel = new JPanel() {
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(240, 240, 245));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(T.GRAY);
                g2.setFont(T.f(10, Font.PLAIN));
                String previewText = "Loc: " + seller.getLat() + ", " + seller.getLng();
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(previewText, (getWidth() - fm.stringWidth(previewText)) / 2, 
                        (getHeight() + fm.getAscent()) / 2);
                g2.dispose();
            }
        };
        previewPanel.setPreferredSize(new Dimension(300, 150));
        previewPanel.setMaximumSize(new Dimension(300, 150));
        previewPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        previewPanel.setOpaque(false);
        
        card.add(al);
        card.add(Box.createVerticalStrut(8));
        card.add(cl);
        card.add(Box.createVerticalStrut(16));
        card.add(previewPanel);
        card.add(Box.createVerticalStrut(12));
        card.add(mb);
        card.add(Box.createVerticalStrut(8));
        card.add(dirBtn);
        
        p.add(card, BorderLayout.CENTER);
        return p;
    }

    // ── Settings tab ─────────────────────────────────────────────────────────
    private JPanel buildSettingsTab() {
        JPanel p = T.bg(); p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(18,18,18,18));
        JLabel ttl = new JLabel("Settings"); ttl.setFont(T.FT); ttl.setForeground(T.PINK_D);
        ttl.setAlignmentX(Component.LEFT_ALIGNMENT); ttl.setBorder(BorderFactory.createEmptyBorder(0,0,14,0));
        p.add(ttl);

        JPanel pc = T.card(12); pc.setLayout(new BoxLayout(pc,BoxLayout.Y_AXIS));
        pc.setBorder(BorderFactory.createEmptyBorder(14,16,14,16));
        pc.setMaximumSize(new Dimension(Integer.MAX_VALUE,200)); pc.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel ptl = new JLabel("Edit Profile"); ptl.setFont(T.FBO); ptl.setForeground(T.PINK_D); ptl.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel pf = new JPanel(new GridLayout(3,2,10,7)); pf.setOpaque(false); pf.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField nf = T.field("Name"); nf.setText(seller.getName());
        JTextField phf = T.field("Phone"); phf.setText(seller.getPhone());
        JTextField af = T.field("Address"); af.setText(seller.getAddress());
        addPairF(pf,"Name:",nf); addPairF(pf,"Phone:",phf); addPairF(pf,"Address:",af);
        JButton sv = T.btn("Save"); sv.setAlignmentX(Component.LEFT_ALIGNMENT);
        sv.addActionListener(e -> {
            seller.setName(nf.getText().trim()); seller.setPhone(phf.getText().trim()); seller.setAddress(af.getText().trim());
            nameLbl.setText("🎪 "+seller.getName()+"   •   ID: "+seller.getId());
            subLbl.setText("📱 "+seller.getPhone()+"   📍 "+seller.getAddress());
            setTitle("🎪 Seller Dashboard  —  "+seller.getName());
            JOptionPane.showMessageDialog(this,"Profile saved!","OK",JOptionPane.INFORMATION_MESSAGE);
        });
        pc.add(ptl); pc.add(Box.createVerticalStrut(8)); pc.add(pf); pc.add(Box.createVerticalStrut(10)); pc.add(sv);
        p.add(pc); p.add(Box.createVerticalStrut(14));

        JPanel ac = T.card(12); ac.setLayout(new BoxLayout(ac,BoxLayout.Y_AXIS));
        ac.setBorder(BorderFactory.createEmptyBorder(14,16,14,16));
        ac.setMaximumSize(new Dimension(Integer.MAX_VALUE,170)); ac.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel atl = new JLabel("Google Gemini API Key");
        JLabel info = new JLabel("Get your FREE API key at: aistudio.google.com  →  Get API Key");
        JTextField apif = T.field("AIzaSy...");
        apif.setMaximumSize(new Dimension(Integer.MAX_VALUE,30)); apif.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton asv = T.btn("Save API Key"); asv.setAlignmentX(Component.LEFT_ALIGNMENT);
        asv.addActionListener(e -> { 
        AIService.setKey(apif.getText().trim());
        JOptionPane.showMessageDialog(this, 
        "Gemini API Key saved! AI chat is now active (free tier).", 
        "OK", JOptionPane.INFORMATION_MESSAGE); 
        });
        JButton open = T.obtn("🌐 Open aistudio.google.com", T.PINK); open.setAlignmentX(Component.LEFT_ALIGNMENT);
        open.addActionListener(e -> { try { Desktop.getDesktop().browse(new URI("https://aistudio.google.com/")); } catch(Exception ex){} });
        ac.add(atl); ac.add(Box.createVerticalStrut(4)); ac.add(info);
        ac.add(Box.createVerticalStrut(8)); ac.add(apif); ac.add(Box.createVerticalStrut(8));
        ac.add(asv); ac.add(Box.createVerticalStrut(6)); ac.add(open);
        p.add(ac);
        return p;
    }

    private void addPairF(JPanel p, String k, JTextField f) {
        JLabel l = new JLabel(k); l.setFont(T.FBO); l.setForeground(T.GRAY); p.add(l); p.add(f);
    }

    private JPanel buildReviewsTab() {
        JPanel p = T.bg(); p.setLayout(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        JLabel ttl = new JLabel("Buyer Reviews & Ratings"); ttl.setFont(T.FT); ttl.setForeground(T.PINK_D);
        ttl.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));
        p.add(ttl, BorderLayout.NORTH);

        JPanel listP = new JPanel();
        listP.setLayout(new BoxLayout(listP, BoxLayout.Y_AXIS));
        listP.setOpaque(false);

        List<com.ssn.food.model.Review> reviews = com.ssn.food.service.DatabaseManager.get().loadReviews(seller.getId());
        if (reviews.isEmpty()) {
            JLabel empty = new JLabel("No reviews yet.", SwingConstants.CENTER);
            empty.setFont(T.FB); empty.setForeground(T.GRAY);
            listP.add(empty);
        } else {
            for (com.ssn.food.model.Review r : reviews) {
                JPanel card = T.card(12);
                card.setLayout(new BorderLayout(10,5));
                card.setBorder(BorderFactory.createEmptyBorder(10,12,10,12));
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
                
                JLabel name = new JLabel(r.getBuyerName() + "  •  " + r.getTimestamp());
                name.setFont(T.FBO); name.setForeground(T.PINK_D);
                
                JLabel stars = new JLabel(T.stars(r.getRating()) + " (" + r.getRating() + "/5)");
                stars.setFont(T.FS); stars.setForeground(T.STAR);
                
                JLabel comment = new JLabel("<html><i>\"" + r.getComment() + "\"</i></html>");
                comment.setFont(T.FS); comment.setForeground(T.DARK);
                
                JPanel head = new JPanel(new BorderLayout()); head.setOpaque(false);
                head.add(name, BorderLayout.WEST);
                head.add(stars, BorderLayout.EAST);
                
                card.add(head, BorderLayout.NORTH);
                card.add(comment, BorderLayout.CENTER);
                
                listP.add(card);
                listP.add(Box.createVerticalStrut(8));
            }
        }

        JPanel wrap = new JPanel(new BorderLayout()); wrap.setOpaque(false);
        wrap.add(listP, BorderLayout.NORTH);
        JScrollPane sc = new JScrollPane(wrap); T.scrollFix(sc);
        p.add(sc, BorderLayout.CENTER);
        return p;
    }

    private void showRevenue() {
        List<Order> orders = AppStore.get().getOrdersFor(seller.getId());
        long total = orders.stream().mapToLong(Order::getTotal).sum();
        JOptionPane.showMessageDialog(this,
            "💰 Total Revenue: "+AppStore.rp(total)
            +"\n📋 Total Orders: "+orders.size()
            +"\n⭐ Rating: "+seller.formatRating(),
            "Revenue - "+seller.getName(),JOptionPane.INFORMATION_MESSAGE);
    }
}