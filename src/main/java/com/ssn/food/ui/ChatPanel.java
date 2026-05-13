package com.ssn.food.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.ssn.food.model.ChatMsg;
import com.ssn.food.model.FoodItem;
import com.ssn.food.model.Seller;
import com.ssn.food.service.AIService;
import com.ssn.food.service.AppStore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.BiConsumer;

public class ChatPanel extends JPanel {
    private String sellerId;
    private ChatMsg.From myRole;
    private final JPanel msgs;
    private final JScrollPane scroll;
    private BiConsumer<Integer, Integer> filterCallback;
    private boolean aiOn = false;
    private String buyerName = "Buyer";

    public void setFilterCallback(BiConsumer<Integer, Integer> cb) {
        this.filterCallback = cb;
    }

    public void setBuyerName(String name) {
        this.buyerName = (name == null || name.trim().isEmpty()) ? "Buyer" : name;
    }

    public ChatPanel(String sellerId, ChatMsg.From myRole, String buyerName) {
        this.sellerId = sellerId;
        this.myRole = myRole;
        setBuyerName(buyerName);
        setLayout(new BorderLayout());
        setOpaque(false);

        msgs = new JPanel();
        msgs.setLayout(new BoxLayout(msgs, BoxLayout.Y_AXIS));
        msgs.setOpaque(false);
        msgs.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(msgs, BorderLayout.NORTH);
        scroll = new JScrollPane(wrap);
        T.scrollFix(scroll);
        add(scroll, BorderLayout.CENTER);

        // Input panel
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JTextField inp = T.field("Type a message...");

        JButton sendBtn = new JButton("\u25B6") {
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, T.PINK, getWidth(), getHeight(), T.PINK_D));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                T.drawIcon(g2, "send", 10, 10, 15);
                g2.dispose();
            }
        };
        sendBtn.setPreferredSize(new Dimension(35, 35));
        sendBtn.setBorder(null);
        sendBtn.setContentAreaFilled(false);
        sendBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        Runnable send = () -> {
            String t = inp.getText().trim();
            if (t.isEmpty())
                return;
            inp.setText("");

            String lowerT = t.toLowerCase();

            // REGEX FILTER (Photo 1 Logic)
            // Handle qty x price patterns (e.g., 3x21k, 3x21000)
            Pattern p1 = Pattern.compile("(\\d+)x(\\d+)(k?)");
            Matcher m1 = p1.matcher(lowerT);
            if (m1.find() && filterCallback != null) {
                int qty = Integer.parseInt(m1.group(1));
                int price = Integer.parseInt(m1.group(2));
                if (m1.group(3).equals("k") || price < 1000) price *= 1000;
                
                final int fPrice = price;
                final int fQty = qty;

                filterCallback.accept(price, qty);
                
                // Smart Tender Logic: Find best matching seller
                List<Seller> matches = AppStore.get().getSellers().stream()
                    .filter(s -> s.getMenu().stream().anyMatch(i -> i.getPrice() <= fPrice && i.getStock() >= fQty))
                    .toList();
                
                String matchResult = matches.isEmpty() ? "No direct matches found." : "Found " + matches.size() + " smart matches!";
                AppStore.get().sendChat(sellerId, new ChatMsg(ChatMsg.From.SYSTEM, 
                    "Filter aktif: Maks " + AppStore.rp(price) + " | Min Stok: " + qty + "\n🤖 " + matchResult));
                return;
            }

            // Handle budget patterns (e.g., 21k, 21000, Rp 21.000)
            String budgetOnly = lowerT.replaceAll("[^\\dk]", ""); 
            Pattern p2 = Pattern.compile("(\\d+)(k?)");
            Matcher m2 = p2.matcher(budgetOnly);
            if (m2.find() && filterCallback != null) {
                int price = Integer.parseInt(m2.group(1));
                if (m2.group(2).equals("k") || price < 1000) price *= 1000;
                
                filterCallback.accept(price, 0);
                AppStore.get().sendChat(sellerId, new ChatMsg(ChatMsg.From.SYSTEM, 
                    "Filter aktif: Budget Maks " + AppStore.rp(price)));
                return;
            }

            // Reset filter if "reset" or "clear"
            if (lowerT.equals("reset") || lowerT.equals("clear")) {
                if (filterCallback != null) filterCallback.accept(-1, -1);
                AppStore.get().sendChat(sellerId, new ChatMsg(ChatMsg.From.SYSTEM, "Filter cleared."));
                return;
            }

            // Clear chat command
            if (lowerT.equals("clear chat")) {
                AppStore.get().getChat(sellerId).clear();
                refresh();
                return;
            }

            // Filter cheapest menu
            if (lowerT.contains("cheap") || lowerT.contains("cheapest") || lowerT.contains("murah")) {
                List<FoodItem> allItems = new ArrayList<>();
                if (AppStore.BROADCAST_ID.equals(sellerId)) {
                    AppStore.get().getSellers().forEach(s -> allItems.addAll(s.getMenu()));
                } else {
                    Seller seller = AppStore.get().findSeller(sellerId);
                    if (seller != null) allItems.addAll(seller.getMenu());
                }

                if (!allItems.isEmpty()) {
                    allItems.sort(Comparator.comparingLong(FoodItem::getPrice));
                    StringBuilder sb = new StringBuilder("Cheapest menu:\n");
                    for (int i = 0; i < Math.min(5, allItems.size()); i++) {
                        FoodItem item = allItems.get(i);
                        sb.append("  • ").append(item.getEmoji()).append(" ")
                                .append(item.getName()).append(" - ")
                                .append(item.formatPrice()).append("\n");
                    }
                    AppStore.get().sendChat(sellerId, new ChatMsg(ChatMsg.From.SYSTEM, sb.toString()));
                } else {
                    AppStore.get().sendChat(sellerId, new ChatMsg(ChatMsg.From.SYSTEM, "No menu available."));
                }
                return;
            }

            // Filter most expensive menu
            if (lowerT.contains("expensive") || lowerT.contains("most expensive") || lowerT.contains("mahal")) {
                List<FoodItem> allItems = new ArrayList<>();
                if (AppStore.BROADCAST_ID.equals(sellerId)) {
                    AppStore.get().getSellers().forEach(s -> allItems.addAll(s.getMenu()));
                } else {
                    Seller seller = AppStore.get().findSeller(sellerId);
                    if (seller != null) allItems.addAll(seller.getMenu());
                }

                if (!allItems.isEmpty()) {
                    allItems.sort((a, b) -> Long.compare(b.getPrice(), a.getPrice()));
                    StringBuilder sb = new StringBuilder("Most expensive menu:\n");
                    for (int i = 0; i < Math.min(3, allItems.size()); i++) {
                        FoodItem item = allItems.get(i);
                        sb.append("  • ").append(item.getEmoji()).append(" ")
                                .append(item.getName()).append(" - ")
                                .append(item.formatPrice()).append("\n");
                    }
                    AppStore.get().sendChat(sellerId, new ChatMsg(ChatMsg.From.SYSTEM, sb.toString()));
                } else {
                    AppStore.get().sendChat(sellerId, new ChatMsg(ChatMsg.From.SYSTEM, "No menu available."));
                }
                return;
            }

            // Advanced AI Flow (Concierge triggers)
            String low = t.toLowerCase();

            // 1. Send the original user message FIRST
            ChatMsg userM = new ChatMsg(myRole, t);
            AppStore.get().sendChat(sellerId, userM);

            // 2. AI Triggers (Quick Replies)
            if (low.matches(".*h[ae]ll+o.*") || low.contains("hi") || low.contains("halo") || low.contains("hey")) {
                String nameToUse = (buyerName == null || buyerName.equals("Buyer")) ? "" : buyerName;
                String greeting = "Hai " + (nameToUse.isEmpty() ? "Buyer" : nameToUse) + "! what u want to buy today? 🍱";
                AppStore.get().sendChat(sellerId, new ChatMsg(ChatMsg.From.SYSTEM, "🤖 AI: " + greeting));
            } else if (low.contains("spicy") || low.contains("pedas") || low.contains("hot") || low.contains("sambal")) {
                // Message for Everyone (mainly Buyer)
                AppStore.get().sendChat(sellerId, new ChatMsg(ChatMsg.From.SYSTEM, 
                    "🤖 AI: OK " + (buyerName.equals("Buyer") ? "" : buyerName) + ", I'll find some spicy food for you!"));
                
                // Message for Sellers Only
                AppStore.get().sendChat(sellerId, new ChatMsg(ChatMsg.From.SYSTEM, 
                    "📢 ATTENTION SELLERS: Buyer wants SPICY food!"));
            }

            // 3. Original AI Logic (LLM Integration) - RESTORED & PRESERVED
            if (aiOn && myRole == ChatMsg.From.BUYER) {
                AppStore.get().sendChat(sellerId, new ChatMsg(ChatMsg.From.SYSTEM, "AI is typing..."));
                AIService.ask(sellerId, t,
                        r -> AppStore.get().sendChat(sellerId, new ChatMsg(ChatMsg.From.AI, r)),
                        e -> AppStore.get().sendChat(sellerId, new ChatMsg(ChatMsg.From.AI, "Error: " + e)));
            }
        };

        sendBtn.addActionListener(e -> send.run());
        inp.addActionListener(e -> send.run());
        
        JButton clearBtn = T.obtn("Clear", T.PINK);
        clearBtn.setPreferredSize(new Dimension(65, 35));
        clearBtn.addActionListener(e -> {
            AppStore.get().clearChat(sellerId);
            refresh();
        });
        
        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        btnWrap.setOpaque(false);
        btnWrap.add(clearBtn);
        btnWrap.add(sendBtn);

        row.add(inp, BorderLayout.CENTER);
        row.add(btnWrap, BorderLayout.EAST);
        add(row, BorderLayout.SOUTH);

        refresh();
        AppStore.get().onChat(m -> SwingUtilities.invokeLater(this::refresh));
    }

    public void enableAI(boolean b) {
        aiOn = b;
    }

    private void refresh() {
        List<ChatMsg> list = AppStore.get().getChat(sellerId);
        msgs.removeAll();
        for (ChatMsg m : list) {
            // Hide specific system alerts from Buyer
            if (myRole == ChatMsg.From.BUYER && m.text.startsWith("📢 ATTENTION SELLERS")) {
                continue;
            }
            msgs.add(bubble(m));
        }
        msgs.revalidate();
        msgs.repaint();
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar()
                .setValue(scroll.getVerticalScrollBar().getMaximum()));
    }

    private JPanel bubble(ChatMsg m) {
        boolean mine = m.from == myRole;
        boolean isAI = m.from == ChatMsg.From.AI;
        boolean isSys = m.from == ChatMsg.From.SYSTEM;

        int align = isSys ? FlowLayout.CENTER : (mine ? FlowLayout.RIGHT : FlowLayout.LEFT);
        JPanel row = new JPanel(new FlowLayout(align, 5, 2));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1000));

        final Color bg = mine ? T.PINK : (isAI ? new Color(245, 243, 255) : Color.WHITE);
        Color fg = mine ? Color.WHITE : T.DARK;
        if (isSys) { fg = T.GRAY; }
        final Color fBg = isSys ? new Color(243, 244, 246) : bg;

        JLabel lbl = new JLabel("<html><body style='width: 180px; padding: 2px;'>" + 
            (isAI ? "<b>🤖 AI:</b><br>" : "") + m.text + "</body></html>");
        lbl.setFont(T.FS);
        lbl.setForeground(fg);

        JPanel bub = new JPanel(new BorderLayout()) {
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0,0,0,10)); // Shadow
                g2.fillRoundRect(1, 1, getWidth()-1, getHeight()-1, 15, 15);
                g2.setColor(fBg);
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 15, 15);
                
                if (!isSys) {
                    int[] tx, ty;
                    if (mine) {
                        tx = new int[]{getWidth()-1, getWidth()-1, getWidth()+4};
                        ty = new int[]{5, 15, 5};
                    } else {
                        tx = new int[]{0, 0, -4};
                        ty = new int[]{5, 15, 5};
                    }
                    g2.fillPolygon(tx, ty, 3);
                }
                g2.dispose();
            }
        };
        bub.setOpaque(false);
        bub.setBorder(new EmptyBorder(5, 10, 5, 10));
        bub.add(lbl, BorderLayout.CENTER);

        JLabel timeLbl = new JLabel(m.time);
        timeLbl.setFont(T.FX);
        timeLbl.setForeground(mine ? new Color(255,255,255,160) : Color.LIGHT_GRAY);
        JPanel tp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        tp.setOpaque(false);
        tp.add(timeLbl);
        bub.add(tp, BorderLayout.SOUTH);

        row.add(bub);
        return row;
    }
}