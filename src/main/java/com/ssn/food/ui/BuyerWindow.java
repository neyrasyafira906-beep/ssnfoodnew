package com.ssn.food.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
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

public class BuyerWindow extends JFrame {

    private final Buyer buyer = new Buyer();
    private JTextField nameF, phoneF, addrF;
    private final JPanel sellerList;
    private final JLabel locLbl;
    private String activeSellerId = null;
    private final JPanel chatHolder;

    // Poin 5: Cart system
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
        setTitle("\uD83D\uDED2 Buyer Dashboard  \u2014  SSN FoodApp");
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
                "<html><center>\uD83D\uDC46 Klik Chat pada seller<br>untuk mulai obrolan</center></html>",
                SwingConstants.CENTER);
        chatHint.setFont(T.FB);
        chatHint.setForeground(T.GRAY);
        chatHolder.add(chatHint, BorderLayout.CENTER);

        // RIGHT — seller list
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setOpaque(false);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 6, 10, 10));

        JPanel rhdr = new JPanel(new BorderLayout(10, 0));
        rhdr.setOpaque(false);
        rhdr.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        locLbl = new JLabel("\uD83D\uDCCD Jakarta  \u2022  5 Seller Ditemukan");
        locLbl.setFont(T.FS);
        locLbl.setForeground(T.GRAY);

        JPanel sortRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        sortRow.setOpaque(false);
        JButton nearBtn = T.btn("\uD83D\uDCCD Terdekat", new Color(16, 185, 129), new Color(5, 150, 105));
        nearBtn.addActionListener(e -> sortByDist());
        JButton cheapBtn = T.btn("\uD83D\uDCB0 Termurah", new Color(234, 88, 12), new Color(194, 65, 12));
        cheapBtn.addActionListener(e -> sortByCheap());
        JButton topBtn = T.btn("\u2B50 Terpopuler", new Color(139, 92, 246), new Color(109, 40, 217));
        topBtn.addActionListener(e -> sortByRating());
        JButton histBtn = T.obtn("\uD83D\uDCB3 Riwayat", T.PINK_D);
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

        setContentPane(root);
        rebuildSellerList(AppStore.get().getSellers());
        AppStore.get().onMenu(s -> SwingUtilities.invokeLater(() -> rebuildSellerList(AppStore.get().getSellers())));
    }

    // ── Top bar ───────────────────────────────────────────────────────────────
    private JPanel topBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setOpaque(false);
        bar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, T.PINK_B),
                new EmptyBorder(8, 14, 8, 14)));

        // Logo + title
        JPanel title = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        title.setOpaque(false);
        JLabel logo = new JLabel("\uD83D\uDED2");
        logo.setFont(T.f(26, Font.PLAIN));
        JLabel name = new JLabel("Buyer Dashboard");
        name.setFont(T.FT);
        name.setForeground(T.PINK_D);
        title.add(logo);
        title.add(name);

        // Profile fields
        JPanel pf = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pf.setOpaque(false);

        nameF = T.field("Nama");
        nameF.setPreferredSize(new Dimension(110, 30));
        phoneF = T.field("08xx...");
        phoneF.setPreferredSize(new Dimension(120, 30));
        addrF = T.field("Alamat");
        addrF.setPreferredSize(new Dimension(200, 30));

        JButton save = T.btn("\uD83D\uDCBE Simpan");
        save.addActionListener(e -> {
            buyer.setName(nameF.getText().trim());
            buyer.setPhone(phoneF.getText().trim());
            buyer.setAddress(addrF.getText().trim());
            locLbl.setText("\uD83D\uDCCD " + (addrF.getText().isEmpty() ? "Jakarta" : addrF.getText()));
            JOptionPane.showMessageDialog(this, "Profil disimpan!", "OK", JOptionPane.INFORMATION_MESSAGE);
        });

        JButton aiBtn = T.btn("\uD83E\uDD16 Tanya AI", new Color(124, 58, 237), new Color(91, 33, 182));
        aiBtn.addActionListener(e -> openAIDialog());

        JButton mapBtn = T.obtn("\uD83D\uDDFA Peta Saya", T.PINK);
        mapBtn.addActionListener(e -> openMap(buyer.getLat(), buyer.getLng()));

        // Poin 5: Tombol keranjang
        cartBtn = T.btn("🛒 Keranjang (0)", new Color(234, 88, 12), new Color(194, 65, 12));
        cartBtn.addActionListener(e -> showCart());

        addLbl(pf, "\uD83D\uDC64");
        pf.add(nameF);
        addLbl(pf, "\uD83D\uDCF1");
        pf.add(phoneF);
        addLbl(pf, "\uD83D\uDCCD");
        pf.add(addrF);
        pf.add(save);
        pf.add(aiBtn);
        pf.add(mapBtn);
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
            cartBtn.setText("🛒 Keranjang (" + cart.size() + ")");
        }
    }

    // Poin 5: Show cart dialog
    private void showCart() {
        if (cartDialog == null || !cartDialog.isVisible()) {
            cartDialog = new JDialog(this, "🛒 Keranjang Belanja", true);
            cartDialog.setSize(400, 500);
            cartDialog.setLocationRelativeTo(this);
        }

        JPanel main = T.bg();
        main.setLayout(new BorderLayout());
        main.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("🛒 Keranjang Belanja");
        title.setFont(T.FT);
        title.setForeground(T.PINK_D);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        main.add(title, BorderLayout.NORTH);

        JPanel itemsPanel = new JPanel();
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
        itemsPanel.setOpaque(false);

        long grandTotal = 0;
        for (CartItem ci : cart) {
            JPanel row = T.card(8);
            row.setLayout(new BorderLayout(8, 0));
            row.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

            JLabel name = new JLabel(ci.item.getEmoji() + " " + ci.item.getName() + " x" + ci.qty);
            name.setFont(T.FB);

            JLabel price = new JLabel(ci.item.formatPrice() + " = " + AppStore.rp(ci.getSubtotal()));
            price.setFont(T.FBO);
            price.setForeground(T.PINK_D);

            JButton remove = T.obtn("✖", T.GRAY);
            remove.addActionListener(e -> {
                cart.remove(ci);
                updateCartButton();
                showCart();
            });

            row.add(remove, BorderLayout.WEST);
            row.add(name, BorderLayout.CENTER);
            row.add(price, BorderLayout.EAST);
            itemsPanel.add(row);
            itemsPanel.add(Box.createVerticalStrut(5));
            grandTotal += ci.getSubtotal();
        }

        if (cart.isEmpty()) {
            JLabel empty = new JLabel("🛒 Keranjang kosong", SwingConstants.CENTER);
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

        JButton closeBtn = T.obtn("Tutup", T.GRAY);
        closeBtn.addActionListener(e -> cartDialog.setVisible(false));

        JButton checkoutBtn = T.btn("✅ Checkout");
        checkoutBtn.addActionListener(e -> {
            if (cart.isEmpty()) {
                JOptionPane.showMessageDialog(cartDialog, "Keranjang kosong!", "Info", JOptionPane.WARNING_MESSAGE);
                return;
            }
            checkoutCart();
            cartDialog.setVisible(false);
        });

        btns.add(closeBtn);
        btns.add(checkoutBtn);
        footer.add(totalLabel, BorderLayout.WEST);
        footer.add(btns, BorderLayout.EAST);
        main.add(footer, BorderLayout.SOUTH);

        cartDialog.setContentPane(main);
        cartDialog.setVisible(true);
    }

    private void checkoutCart() {
        if (cart.isEmpty())
            return;

        Map<Seller, List<CartItem>> bySeller = new HashMap<>();
        for (CartItem ci : cart) {
            bySeller.computeIfAbsent(ci.seller, k -> new ArrayList<>()).add(ci);
        }

        for (Map.Entry<Seller, List<CartItem>> entry : bySeller.entrySet()) {
            Seller seller = entry.getKey();
            List<CartItem> items = entry.getValue();

            Order.Payment pay = Order.Payment.CASH;
            Order order = new Order(buyer.getDisplayName(), buyer.getPhone(),
                    buyer.getAddress(), seller.getId(), pay);

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
                    "🛒 Order baru dari " + buyer.getDisplayName() + ": " + summary.toString() +
                            " | Total: " + AppStore.rp(total)));
            AppStore.get().fireMenu(seller);
        }

        JOptionPane.showMessageDialog(this,
                "✅ Pesanan berhasil!\n" + cart.size() + " item dipesan.",
                "Sukses!", JOptionPane.INFORMATION_MESSAGE);
        cart.clear();
        updateCartButton();
    }

    // ── Seller list ───────────────────────────────────────────────────────────
    private void rebuildSellerList(List<Seller> list) {
        sellerList.removeAll();
        list.forEach(s -> {
            sellerList.add(sellerCard(s));
            sellerList.add(Box.createVerticalStrut(10));
        });
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
        JLabel dl = new JLabel(String.format("\uD83D\uDCCD %.1f km", dist));
        dl.setFont(T.FS);
        dl.setForeground(T.GRAY);
        JLabel rl = new JLabel(T.stars(s.getRating()) + " " + s.formatRating());
        rl.setFont(T.FS);
        rl.setForeground(T.STAR);
        JLabel pl = new JLabel("\uD83D\uDCF1 " + s.getPhone());
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
        JButton chatBtn = T.btn("\uD83D\uDCAC Chat");
        chatBtn.addActionListener(e -> openChat(s));

        JButton vidBtn = T.btn("\uD83C\uDFAC Video", new Color(124, 58, 237), new Color(91, 33, 182));
        vidBtn.addActionListener(e -> openVideo(s));

        JButton mapBtn2 = T.obtn("\uD83D\uDDFA Peta", T.PINK);
        mapBtn2.addActionListener(e -> openMap(s.getLat(), s.getLng()));

        btns.add(mapBtn2);
        btns.add(vidBtn);
        btns.add(chatBtn);
        hdr.add(av, BorderLayout.WEST);
        hdr.add(info, BorderLayout.CENTER);
        hdr.add(btns, BorderLayout.EAST);
        card.add(hdr, BorderLayout.NORTH);

        // Menu grid
        if (!s.getMenu().isEmpty()) {
            JPanel grid = new JPanel(new GridLayout(0, 1, 0, 4));
            grid.setOpaque(false);
            for (FoodItem item : s.getMenu())
                grid.add(menuRow(s, item));
            card.add(grid, BorderLayout.CENTER);
        }
        return card;
    }

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
                    g2.setColor(T.PINK_P);
                    g2.fillRoundRect(0, 0, 48, 48, 8, 8);
                    g2.setFont(T.f(20, Font.PLAIN));
                    FontMetrics fm = g2.getFontMetrics();
                    String em = item.getEmoji();
                    g2.drawString(em, (48 - fm.stringWidth(em)) / 2,
                            (48 + fm.getAscent() - fm.getDescent()) / 2);
                }
                g2.dispose();
            }
        };
        thumb.setPreferredSize(new Dimension(48, 48));
        thumb.setOpaque(false);

        JLabel nm = new JLabel(item.getEmoji() + " " + item.getName());
        nm.setFont(T.FB);
        nm.setForeground(T.DARK);
        JLabel pr = new JLabel(item.formatPrice());
        pr.setFont(T.FBO);
        pr.setForeground(T.PINK_D);
        JLabel stk = new JLabel("Stok: " + item.getStock());
        stk.setFont(T.FS);
        stk.setForeground(item.getStock() > 0 ? T.GREEN : T.RED);
        SpinnerNumberModel sm = new SpinnerNumberModel(0, 0, Math.max(item.getStock(), 1), 1);
        JSpinner sp = new JSpinner(sm);
        sp.setFont(T.FS);
        JButton ob = T.btn("Order");

        // Poin 5: Order langsung masuk keranjang
        ob.addActionListener(e -> {
            int qty = (int) sp.getValue();
            if (qty <= 0) {
                JOptionPane.showMessageDialog(this, "Pilih jumlah dulu!", "Info", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Tambah ke keranjang
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
                    "🛒 " + item.getName() + " x" + qty + " ditambahkan ke keranjang!\n" +
                            "Klik tombol 🛒 Keranjang untuk checkout.",
                    "Ditambahkan", JOptionPane.INFORMATION_MESSAGE);
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

    // ── Chat ─────────────────────────────────────────────────────────────────
    private void openChat(Seller s) {
        activeSellerId = s.getId();
        chatHolder.removeAll();

        JPanel chatWrap = new JPanel(new BorderLayout());
        chatWrap.setOpaque(false);
        chatWrap.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 6));

        JPanel chatHead = new JPanel(new BorderLayout());
        chatHead.setOpaque(false);
        chatHead.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        JLabel tl = new JLabel(s.getAvatarEmoji() + " Chat: " + s.getName());
        tl.setFont(T.FH);
        tl.setForeground(T.PINK_D);
        JButton aiToggle = T.btn("\uD83E\uDD16 Aktifkan AI", new Color(124, 58, 237), new Color(91, 33, 182));
        chatHead.add(tl, BorderLayout.WEST);
        chatHead.add(aiToggle, BorderLayout.EAST);

        ChatPanel cp = new ChatPanel(s.getId(), ChatMsg.From.BUYER);
        final boolean[] aiActive = { false };
        aiToggle.addActionListener(e -> {
            aiActive[0] = !aiActive[0];
            cp.enableAI(aiActive[0]);
            aiToggle.setText(aiActive[0] ? "\uD83E\uDD16 AI Aktif \u2714" : "\uD83E\uDD16 Aktifkan AI");
        });

        chatWrap.add(chatHead, BorderLayout.NORTH);
        chatWrap.add(cp, BorderLayout.CENTER);
        chatHolder.add(chatWrap, BorderLayout.CENTER);
        chatHolder.revalidate();
        chatHolder.repaint();
    }

    // ── AI dialog ────────────────────────────────────────────────────────────
    private void openAIDialog() {
        List<Seller> sellers = AppStore.get().getSellers();
        if (sellers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Belum ada seller terdaftar.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String[] names = sellers.stream().map(Seller::getName).toArray(String[]::new);
        String chosen = (String) JOptionPane.showInputDialog(
                this, "Pilih seller untuk tanya AI:", "Tanya AI",
                JOptionPane.QUESTION_MESSAGE, null, names, names[0]);
        if (chosen == null)
            return;
        Seller s = sellers.stream().filter(x -> x.getName().equals(chosen)).findFirst().orElse(null);
        if (s == null)
            return;
        String q = JOptionPane.showInputDialog(this, "Pertanyaan kamu:");
        if (q == null || q.trim().isEmpty())
            return;
        AIService.ask(s.getId(), q,
                r -> SwingUtilities.invokeLater(
                        () -> JOptionPane.showMessageDialog(this, "🤖 AI: " + r, "Jawaban AI", JOptionPane.INFORMATION_MESSAGE)),
                e -> SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, e, "Error",
                        JOptionPane.ERROR_MESSAGE)));
    }

    // ── Sort ─────────────────────────────────────────────────────────────────
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

    // ── History ───────────────────────────────────────────────────────────────
    private void showHistory() {
        JDialog dlg = new JDialog(this, "\uD83D\uDCB3 Riwayat Pembayaran", true);
        dlg.setSize(520, 420);
        dlg.setLocationRelativeTo(this);
        JPanel p = T.bg();
        p.setLayout(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JLabel ttl = new JLabel("\uD83D\uDCB3 Riwayat Pembayaran");
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
            JLabel e = new JLabel("Belum ada pesanan", SwingConstants.CENTER);
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
                JLabel id = new JLabel(o.getId() + " \u2022 " + o.getTimestamp());
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
        JButton close = T.btn("Tutup");
        close.addActionListener(e -> dlg.dispose());
        JPanel bp = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bp.setOpaque(false);
        bp.add(close);
        p.add(bp, BorderLayout.SOUTH);
        dlg.setContentPane(p);
        dlg.setVisible(true);
    }

    // Poin 3: Video langsung ke YouTube
    private void openVideo(Seller s) {
        String url = s.getVideoUrl();
        if (url == null || url.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "📹 Seller belum menyediakan video promo.\nSilakan cek kembali nanti.",
                    "Video Tidak Tersedia",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        url = convertToYouTubeWatchUrl(url);

        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "❌ Gagal membuka video: " + ex.getMessage(),
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

    private void openMap(double lat, double lng) {
        try {
            Desktop.getDesktop().browse(new URI("https://maps.google.com/?q=" + lat + "," + lng));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}