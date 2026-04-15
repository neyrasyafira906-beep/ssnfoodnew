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
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.ssn.food.model.ChatMsg;
import com.ssn.food.model.FoodItem;
import com.ssn.food.model.Seller;
import com.ssn.food.service.AIService;
import com.ssn.food.service.AppStore;

public class ChatPanel extends JPanel {
    private final String sellerId;
    private final ChatMsg.From myRole;
    private final JPanel msgs;
    private final JScrollPane scroll;
    private boolean aiOn = false;

    public ChatPanel(String sellerId, ChatMsg.From role) {
        this.sellerId = sellerId;
        this.myRole = role;
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
        JTextField inp = T.field("Ketik pesan...");

        JButton sendBtn = new JButton("\u25B6") {
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, T.PINK, getWidth(), getHeight(), T.PINK_D));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(T.WHITE);
                g2.setFont(T.f(13, Font.BOLD));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2 + 1,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        sendBtn.setPreferredSize(new Dimension(38, 38));
        sendBtn.setOpaque(false);
        sendBtn.setContentAreaFilled(false);
        sendBtn.setBorderPainted(false);
        sendBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        Runnable send = () -> {
            String t = inp.getText().trim();
            if (t.isEmpty())
                return;
            inp.setText("");

            String lowerT = t.toLowerCase();

            // Filter menu dari termurah
            if (lowerT.contains("murah") || lowerT.contains("termurah") || lowerT.contains("cheap")
                    || lowerT.contains("harga murah")) {

                Seller seller = AppStore.get().findSeller(sellerId);
                if (seller != null && !seller.getMenu().isEmpty()) {
                    List<FoodItem> sorted = new ArrayList<>(seller.getMenu());
                    sorted.sort(Comparator.comparingLong(FoodItem::getPrice));

                    StringBuilder sb = new StringBuilder("🍽️ MENU DARI TERMURAH:\n\n");
                    for (int i = 0; i < sorted.size(); i++) {
                        FoodItem item = sorted.get(i);
                        sb.append(i + 1).append(". ")
                          .append(item.getEmoji()).append(" ")
                          .append(item.getName()).append("\n")
                          .append("   💰 Harga: ").append(item.formatPrice()).append("\n")
                          .append("   📦 Stok: ").append(item.getStock()).append("\n\n");
                    }
                    AppStore.get().sendChat(sellerId, new ChatMsg(ChatMsg.From.SYSTEM, sb.toString()));
                } else {
                    AppStore.get().sendChat(sellerId,
                            new ChatMsg(ChatMsg.From.SYSTEM, "⚠️ Belum ada menu tersedia."));
                }
                return;
            }

            // Filter menu termahal
            if (lowerT.contains("mahal") || lowerT.contains("termahal") || lowerT.contains("expensive")) {
                Seller seller = AppStore.get().findSeller(sellerId);
                if (seller != null && !seller.getMenu().isEmpty()) {
                    List<FoodItem> sorted = new ArrayList<>(seller.getMenu());
                    sorted.sort((a, b) -> Long.compare(b.getPrice(), a.getPrice()));

                    StringBuilder sb = new StringBuilder("🍽️ MENU TERMAHAL:\n\n");
                    for (int i = 0; i < Math.min(5, sorted.size()); i++) {
                        FoodItem item = sorted.get(i);
                        sb.append(i + 1).append(". ")
                          .append(item.getEmoji()).append(" ")
                          .append(item.getName()).append("\n")
                          .append("   💰 Harga: ").append(item.formatPrice()).append("\n\n");
                    }
                    AppStore.get().sendChat(sellerId, new ChatMsg(ChatMsg.From.SYSTEM, sb.toString()));
                }
                return;
            }

            // Chat normal
            ChatMsg m = new ChatMsg(myRole, t);
            AppStore.get().sendChat(sellerId, m);

            if (aiOn && myRole == ChatMsg.From.BUYER) {
                AppStore.get().sendChat(sellerId,
                        new ChatMsg(ChatMsg.From.SYSTEM, "🤖 AI sedang mengetik..."));
                AIService.ask(sellerId, t,
                        r -> AppStore.get().sendChat(sellerId, new ChatMsg(ChatMsg.From.AI, r)),
                        e -> AppStore.get().sendChat(sellerId, new ChatMsg(ChatMsg.From.AI, "❌ " + e)));
            }
        };

        sendBtn.addActionListener(e -> send.run());
        inp.addActionListener(e -> send.run());
        row.add(inp, BorderLayout.CENTER);
        row.add(sendBtn, BorderLayout.EAST);
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
        
        msgs.add(Box.createVerticalStrut(4));
        
        for (ChatMsg m : list) {
            JPanel bubblePanel = bubble(m);
            bubblePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            msgs.add(bubblePanel);
            msgs.add(Box.createVerticalStrut(8));
        }
        
        msgs.add(Box.createVerticalStrut(4));
        msgs.revalidate();
        msgs.repaint();
        
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = scroll.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    // Bubble chat yang rapi dan tidak kepotong
    private JPanel bubble(ChatMsg m) {
        boolean mine = m.from == myRole;
        boolean isAI = m.from == ChatMsg.From.AI;
        boolean isSys = m.from == ChatMsg.From.SYSTEM;

        // Container utama
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);
        
        // Warna berbeda untuk setiap role
        Color bgColor;
        Color textColor;
        String prefix = "";
        String roleName = "";
        
        if (isAI) {
            bgColor = new Color(235, 245, 255);
            textColor = new Color(25, 85, 150);
            prefix = "🤖 ";
            roleName = "AI Assistant";
        } else if (isSys) {
            bgColor = new Color(240, 248, 235);
            textColor = new Color(40, 100, 60);
            prefix = "ℹ️ ";
            roleName = "System";
        } else if (mine) {
            bgColor = new Color(255, 220, 230);
            textColor = new Color(180, 40, 90);
        } else {
            bgColor = new Color(245, 245, 250);
            textColor = new Color(50, 50, 70);
        }
        
        // Header untuk AI/System
        if (isAI || isSys) {
            JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            header.setOpaque(false);
            JLabel roleLabel = new JLabel(roleName);
            roleLabel.setFont(T.f(9, Font.BOLD));
            roleLabel.setForeground(textColor);
            roleLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 2, 0));
            header.add(roleLabel);
            container.add(header);
        }
        
        // Bubble panel
        JPanel bubblePanel = new JPanel() {
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(textColor);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g2.dispose();
            }
        };
        bubblePanel.setLayout(new BorderLayout());
        bubblePanel.setOpaque(false);
        
        // Text area untuk pesan
        JTextArea ta = new JTextArea();
        String displayText = prefix + m.text;
        ta.setText(displayText);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setOpaque(false);
        ta.setEditable(false);
        ta.setFocusable(false);
        ta.setForeground(textColor);
        
        // Atur font berdasarkan panjang teks
        if (displayText.length() > 150) {
            ta.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        } else if (isAI) {
            ta.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        } else {
            ta.setFont(T.FB);
        }
        
        // Hitung ukuran yang tepat
        int textWidth = ta.getPreferredSize().width;
        int maxWidth = 400;
        
        if (displayText.length() > 200) {
            maxWidth = 500;
        } else if (displayText.length() > 100) {
            maxWidth = 450;
        }
        
        int finalWidth = Math.min(Math.max(textWidth + 40, 120), maxWidth);
        int textHeight = ta.getPreferredSize().height;
        ta.setPreferredSize(new Dimension(finalWidth - 20, textHeight));
        
        bubblePanel.add(ta, BorderLayout.CENTER);
        bubblePanel.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        bubblePanel.setMaximumSize(new Dimension(maxWidth + 20, Integer.MAX_VALUE));
        
        // Time stamp
        JPanel footer = new JPanel(new FlowLayout(mine ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        footer.setOpaque(false);
        JLabel timeLabel = new JLabel(m.time);
        timeLabel.setFont(T.f(9, Font.PLAIN));
        timeLabel.setForeground(new Color(150, 150, 150));
        timeLabel.setBorder(BorderFactory.createEmptyBorder(2, 8, 0, 8));
        footer.add(timeLabel);
        
        container.add(bubblePanel);
        container.add(footer);
        
        // Alignment wrapper
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new FlowLayout(mine ? FlowLayout.RIGHT : FlowLayout.LEFT, 5, 2));
        wrapper.setOpaque(false);
        wrapper.add(container);
        
        return wrapper;
    }
}