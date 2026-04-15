package com.ssn.food;

import com.ssn.food.service.AppStore;
import com.ssn.food.ui.BuyerWindow;
import com.ssn.food.ui.SellerWindow;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            BuyerWindow buyer = new BuyerWindow();
            buyer.setVisible(true);

            SellerWindow seller = new SellerWindow(AppStore.get().getSellers().get(0));
            seller.setVisible(true);
        });
    }
}
