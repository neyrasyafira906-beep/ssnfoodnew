package com.ssn.food.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.*;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Timer;

public class T {
    // Palette
    public static final Color PINK        = new Color(236, 72,153);
    public static final Color PINK_D      = new Color(190, 24, 93);
    public static final Color PINK_L      = new Color(253,242,248);
    public static final Color PINK_P      = new Color(252,231,243);
    public static final Color PINK_B      = new Color(249,168,212);
    public static final Color GREEN       = new Color(  4,120, 87);
    public static final Color GREEN_BG    = new Color(209,250,229);
    public static final Color YELLOW      = new Color(133, 77, 14);
    public static final Color YELLOW_BG   = new Color(254,249,195);
    public static final Color BLUE        = new Color( 29, 78,216);
    public static final Color BLUE_BG     = new Color(219,234,254);
    public static final Color RED         = new Color(185, 28, 28);
    public static final Color RED_BG      = new Color(254,226,226);
    public static final Color GRAY        = new Color(107,114,128);
    public static final Color DARK        = new Color( 31, 41, 55);
    public static final Color WHITE       = Color.WHITE;
    public static final Color STAR        = new Color(234,179,8);

    // Fonts
    public static Font f(int s, int w) { return new Font("Segoe UI", w, s); }
    public static final Font FT = f(20,Font.BOLD);   // title
    public static final Font FH = f(14,Font.BOLD);   // heading
    public static final Font FB = f(13,Font.PLAIN);  // body
    public static final Font FBO= f(13,Font.BOLD);   // body bold
    public static final Font FS = f(11,Font.PLAIN);  // small
    public static final Font FX = f(10,Font.PLAIN);  // tiny

    // ── Panels ──────────────────────────────────────────────────────────────
    public static JPanel bg() {
        return new JPanel() {
            public void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setPaint(new GradientPaint(0,0,PINK_L,getWidth(),getHeight(),new Color(255,240,248)));
                g2.fillRect(0,0,getWidth(),getHeight()); g2.dispose();
            }
        };
    }

    public static JPanel card(int arc) {
        return new JPanel() {
            public void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(WHITE); g2.fillRoundRect(0,0,getWidth(),getHeight(),arc,arc);
                g2.setColor(PINK_B); g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(1,1,getWidth()-2,getHeight()-2,arc,arc); g2.dispose();
            }
        };
    }

    // ── Buttons ─────────────────────────────────────────────────────────────
    public static JButton btn(String txt, Color c1, Color c2) {
        JButton b = new JButton(txt) {
            public void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0,0,c1,0,getHeight(),c2));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),16,16);
                g2.setColor(WHITE); g2.setFont(getFont());
                FontMetrics fm=g2.getFontMetrics();
                g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,
                    (getHeight()+fm.getAscent()-fm.getDescent())/2); g2.dispose();
            }
        };
        setup(b); return b;
    }

    public static JButton btn(String txt)  { return btn(txt,PINK,PINK_D); }

    public static JButton obtn(String txt, Color col) {
        JButton b = new JButton(txt) {
            public void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(WHITE); g2.fillRoundRect(0,0,getWidth(),getHeight(),16,16);
                g2.setColor(col); g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1,1,getWidth()-2,getHeight()-2,16,16);
                g2.setColor(col); g2.setFont(getFont());
                FontMetrics fm=g2.getFontMetrics();
                g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,
                    (getHeight()+fm.getAscent()-fm.getDescent())/2); g2.dispose();
            }
        };
        setup(b); return b;
    }

    private static void setup(JButton b) {
        b.setFont(f(12,Font.BOLD)); b.setOpaque(false);
        b.setContentAreaFilled(false); b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Dimension ps = b.getPreferredSize();
        b.setPreferredSize(new Dimension(ps.width+18,32));
    }

    // ── Fields ───────────────────────────────────────────────────────────────
    public static JTextField field(String ph) {
        JTextField tf = new JTextField() {
            public void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(WHITE); g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                g2.setColor(isFocusOwner()?PINK:PINK_B); g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1,1,getWidth()-2,getHeight()-2,12,12);
                super.paintComponent(g);
                if (getText().isEmpty()&&!isFocusOwner()&&!ph.isEmpty()) {
                    g2.setColor(new Color(180,180,180)); g2.setFont(getFont());
                    FontMetrics fm=g2.getFontMetrics();
                    g2.drawString(ph,8,(getHeight()+fm.getAscent()-fm.getDescent())/2);
                }
                g2.dispose();
            }
        };
        tf.setOpaque(false); tf.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
        tf.setFont(FB); return tf;
    }

    // ── Labels ───────────────────────────────────────────────────────────────
    public static void styleBtn(JButton b, Color bg, Color fg) {
        b.setBackground(bg); b.setForeground(fg);
        b.setFocusPainted(false); b.setBorderPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    public static JLabel badge(String txt, Color bg, Color fg) {
        JLabel l = new JLabel(txt) {
            public void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg); g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                super.paintComponent(g); g2.dispose();
            }
        };
        l.setFont(f(10,Font.BOLD)); l.setForeground(fg);
        l.setBorder(BorderFactory.createEmptyBorder(2,8,2,8));
        l.setOpaque(false); return l;
    }

    public static void scrollFix(JScrollPane s) {
        s.setOpaque(false);
        s.getViewport().setOpaque(false);
        s.setBorder(null);
        s.getVerticalScrollBar().setUnitIncrement(16);
    }

    public static void fade(Component c) {
        Timer timer = new Timer(20, null);
        final float[] alpha = {0f};
        timer.addActionListener(e -> {
            alpha[0] += 0.1f;
            if (alpha[0] >= 1.0f) {
                alpha[0] = 1.0f;
                timer.stop();
            }
            c.repaint();
        });
        timer.start();
    }

    public static void drawIcon(Graphics2D g, String type, int x, int y, int size) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        if (type.equals("chat")) {
            g.drawRoundRect(x, y, size, (int)(size * 0.8), 4, 4);
            int[] px = {x + 4, x + 8, x + 4};
            int[] py = {y + (int)(size * 0.8), y + (int)(size * 0.8), y + size};
            g.drawPolyline(px, py, 3);
        } else if (type.equals("cart")) {
            g.drawRect(x, y + 2, size - 2, size - 6);
            g.drawOval(x + 2, y + size - 4, 3, 3);
            g.drawOval(x + size - 6, y + size - 4, 3, 3);
            g.drawLine(x, y + 2, x - 2, y);
        } else if (type.equals("map")) {
            // Location Pin Icon
            g.setColor(g.getColor());
            g.drawOval(x + size/4, y, size/2, size/2);
            Path2D pin = new Path2D.Double();
            pin.moveTo(x + size/4, y + size/4);
            pin.curveTo(x + size/4, y + size/2, x + size/2, y + size, x + size/2, y + size);
            pin.curveTo(x + size/2, y + size, x + size*3/4, y + size/2, x + size*3/4, y + size/4);
            g.draw(pin);
            g.fillOval(x + size/2 - 2, y + size/4 - 2, 4, 4);
        } else if (type.equals("send")) {
            // Paper Plane Icon
            Path2D plane = new Path2D.Double();
            plane.moveTo(x, y + size/2);
            plane.lineTo(x + size, y);
            plane.lineTo(x + size/3, y + size*2/3);
            plane.lineTo(x + size/3, y + size);
            plane.lineTo(x + size/2, y + size*2/3);
            plane.lineTo(x + size, y);
            g.draw(plane);
        } else if (type.equals("video")) {
            // Play Button Icon
            Path2D play = new Path2D.Double();
            play.moveTo(x + 2, y);
            play.lineTo(x + size, y + size/2);
            play.lineTo(x + 2, y + size);
            play.closePath();
            g.draw(play);
        } else if (type.equals("food")) {
            g.drawOval(x, y + size/2, size, size/2); // Plate
            g.drawArc(x + 2, y, size - 4, size, 0, 180); // Dome
        } else if (type.equals("star")) {
            int midX = x + size/2, midY = y + size/2;
            Path2D star = new Path2D.Double();
            for (int i = 0; i < 5; i++) {
                double angle = Math.toRadians(-90 + i * 72);
                double outerX = midX + Math.cos(angle) * (size / 2.0);
                double outerY = midY + Math.sin(angle) * (size / 2.0);
                if (i == 0) star.moveTo(outerX, outerY); else star.lineTo(outerX, outerY);
                angle = Math.toRadians(-90 + i * 72 + 36);
                double innerX = midX + Math.cos(angle) * (size / 4.0);
                double innerY = midY + Math.sin(angle) * (size / 4.0);
                star.lineTo(innerX, innerY);
            }
            star.closePath();
            g.fill(star);
        }
    }

    public static String stars(double r) {
    int full = (int) Math.floor(r);
    boolean half = (r - full) >= 0.5;
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < full; i++) sb.append("*");
    if (half) sb.append("."); 
    for (int i = sb.length(); i < 5; i++) sb.append("-");
    return sb.toString();
}

    public static Color statusColor(com.ssn.food.model.Order.Status s) {
        switch (s) {
            case PENDING:   return YELLOW;
            case CONFIRMED: return BLUE;
            case PREPARING: return new Color(124,58,237);
            case READY:     return GREEN;
            case DELIVERED: return new Color(20,184,166);
            case CANCELLED: return RED;
            default:        return GRAY;
        }
    }
    public static Color statusBg(com.ssn.food.model.Order.Status s) {
        switch (s) {
            case PENDING:   return YELLOW_BG;
            case CONFIRMED: return BLUE_BG;
            case PREPARING: return new Color(237,233,254);
            case READY:     return GREEN_BG;
            case DELIVERED: return new Color(204,251,241);
            case CANCELLED: return RED_BG;
            default:        return new Color(243,244,246);
        }
    }
}
