package com.ssn.food.ui;

import java.awt.Font;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

public class IconLoader {
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Map<String, ImageIcon> cache = new HashMap<>();
    private static final String ICONIFY_BASE = "https://api.iconify.design/";
    
    public static void loadIcon(String iconName, int size, JLabel targetLabel, String fallbackEmoji) {
        String cacheKey = iconName + ":" + size;
        
        if (cache.containsKey(cacheKey)) {
            SwingUtilities.invokeLater(() -> {
                targetLabel.setIcon(cache.get(cacheKey));
                targetLabel.setText("");
            });
            return;
        }
        
        SwingUtilities.invokeLater(() -> {
            targetLabel.setText(fallbackEmoji);
            targetLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, size));
        });
        
        String pngUrl = ICONIFY_BASE + "mdi/" + iconName + ".png?width=" + size + "&height=" + size;
        
        executor.submit(() -> {
            try {
                URL imageUrl = new URL(pngUrl);
                ImageIcon icon = new ImageIcon(imageUrl);
                Thread.sleep(100);
                
                if (icon.getIconWidth() > 0) {
                    cache.put(cacheKey, icon);
                    SwingUtilities.invokeLater(() -> {
                        targetLabel.setIcon(icon);
                        targetLabel.setText("");
                    });
                }
            } catch (Exception e) {
                System.err.println("Gagal load icon: " + iconName);
            }
        });
    }
}