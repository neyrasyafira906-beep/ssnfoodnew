package com.ssn.food.service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import javax.swing.SwingWorker;

import com.ssn.food.model.Seller;

public class AIService {
    // API Key untuk Gemini (gratis dari Google AI Studio)
    private static String apiKey = "";
    
    // Mode: true = pakai local AI, false = pakai API
    private static boolean useLocalOnly = true;
    
    // Cek apakah API Key valid (tidak kosong dan bukan placeholder)
    private static boolean hasValidKey() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("YOUR_GEMINI_API_KEY_HERE");
    }

    public static void setKey(String key) { 
        apiKey = key;
        useLocalOnly = !hasValidKey();
    }
    
    public static String getKey() { 
        return apiKey; 
    }

    public static void ask(String sellerId, String userMsg,
                           Consumer<String> onOk, Consumer<String> onErr) {
        Seller s = AppStore.get().findSeller(sellerId);
        StringBuilder mb = new StringBuilder();
        if (s != null && s.getMenu() != null) {
            s.getMenu().forEach(m -> mb.append(m.getName()).append(" ").append(m.formatPrice())
                    .append(" stok:").append(m.getStock()).append("; "));
        }
        final String menuStr = mb.toString();

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                // Jika pake local AI atau tidak ada API Key
                if (useLocalOnly || !hasValidKey()) {
                    return localAI(menuStr, userMsg);
                } else {
                    // Coba panggil Gemini API
                    try {
                        return callGeminiAPI(menuStr, userMsg);
                    } catch (Exception e) {
                        // Jika API gagal, fallback ke local AI
                        return localAI(menuStr, userMsg) + "\n\n(⚠️ Mode offline - API tidak tersedia)";
                    }
                }
            }
            
            @Override
            protected void done() {
                try {
                    String result = get();
                    onOk.accept(result);
                } catch (Exception e) {
                    onErr.accept("❌ AI error: " + e.getMessage());
                }
            }
        }.execute();
    }

    // ==================== LOCAL AI (GRATIS, TANPA API) ====================
    private static String localAI(String menu, String msg) {
        String lowerMsg = msg.toLowerCase();
        
        // Menu kosong
        if (menu == null || menu.isEmpty()) {
            return "🍽️ Menu sedang kosong nih. Silakan cek kembali nanti ya!";
        }
        
        // Cek pertanyaan tentang menu termurah
        if (lowerMsg.contains("murah") || lowerMsg.contains("termurah") || 
            lowerMsg.contains("cheap") || lowerMsg.contains("harga murah")) {
            
            String[] items = menu.split("; ");
            String cheapest = "";
            long minPrice = Long.MAX_VALUE;
            for (String item : items) {
                if (item.contains("Rp")) {
                    try {
                        String priceStr = item.substring(item.indexOf("Rp") + 3);
                        priceStr = priceStr.replace(".", "").trim();
                        // Ambil angka pertama yang ditemukan
                        String[] parts = priceStr.split(" ");
                        priceStr = parts[0];
                        long price = Long.parseLong(priceStr);
                        if (price < minPrice) {
                            minPrice = price;
                            cheapest = item;
                        }
                    } catch (Exception e) {}
                }
            }
            if (!cheapest.isEmpty()) {
                return "💡 Menu termurah kami: " + cheapest + "\nYuk pesan! 😊";
            }
            return "💰 Untuk info harga lengkap, bisa lihat langsung di menu ya!";
        }
        
        // Cek pertanyaan tentang menu termahal
        if (lowerMsg.contains("mahal") || lowerMsg.contains("termahal") || 
            lowerMsg.contains("expensive")) {
            String[] items = menu.split("; ");
            String mostExpensive = "";
            long maxPrice = 0;
            for (String item : items) {
                if (item.contains("Rp")) {
                    try {
                        String priceStr = item.substring(item.indexOf("Rp") + 3);
                        priceStr = priceStr.replace(".", "").trim();
                        String[] parts = priceStr.split(" ");
                        priceStr = parts[0];
                        long price = Long.parseLong(priceStr);
                        if (price > maxPrice) {
                            maxPrice = price;
                            mostExpensive = item;
                        }
                    } catch (Exception e) {}
                }
            }
            if (!mostExpensive.isEmpty()) {
                return "👑 Menu spesial kami: " + mostExpensive + "\nCocok untuk acara spesial! 🎉";
            }
            return "👑 Untuk menu spesial, silakan lihat langsung di daftar menu ya!";
        }
        
        // Cek pertanyaan tentang stok
        if (lowerMsg.contains("stok") || lowerMsg.contains("habis") || lowerMsg.contains("tersedia")) {
            return "📦 Stok menu selalu kami update.\nUntuk ketersediaan terkini, silakan lihat langsung di daftar menu ya!";
        }
        
        // Cek pertanyaan tentang rekomendasi
        if (lowerMsg.contains("rekomendasi") || lowerMsg.contains("saran") || 
            lowerMsg.contains("enak") || lowerMsg.contains("best")) {
            return "👍 Rekomendasi menu terlaris kami:\n" +
                   "• Nasi Goreng 🍚\n" +
                   "• Ayam Bakar 🍗\n" +
                   "• Es Teh Manis 🧋\n" +
                   "Coba pesan salah satunya, pasti suka! 😋";
        }
        
        // Sapaan
        if (lowerMsg.contains("halo") || lowerMsg.contains("hai") || 
            lowerMsg.contains("hello") || lowerMsg.contains("hey")) {
            return "Halo! 👋 Selamat datang di restoran kami!\nAda yang bisa saya bantu? Silakan lihat menu kami ya! 😊";
        }
        
        // Ucapan terima kasih
        if (lowerMsg.contains("makasih") || lowerMsg.contains("terima kasih") || 
            lowerMsg.contains("thank")) {
            return "Sama-sama! 😊 Senang bisa membantu.\nSelamat menikmati pesanannya! 🍽️";
        }
        
        // Perkenalan
        if (lowerMsg.contains("siapa") || lowerMsg.contains("nama") || lowerMsg.contains("kamu")) {
            return "Halo! Saya asisten virtual restoran ini. 🤖\n" +
                   "Saya bisa bantu info menu, harga, stok, dan rekomendasi makanan.\n" +
                   "Coba tanya: 'menu murah' atau 'rekomendasi menu' ya!";
        }
        
        // Menu apa aja yang tersedia
        if (lowerMsg.contains("menu") && (lowerMsg.contains("apa") || lowerMsg.contains("tersedia"))) {
            // Ambil beberapa menu untuk ditampilkan
            String[] items = menu.split("; ");
            StringBuilder sb = new StringBuilder("🍽️ Menu yang tersedia:\n");
            int count = 0;
            for (String item : items) {
                if (count < 5) {
                    sb.append("• ").append(item).append("\n");
                    count++;
                }
            }
            if (count < items.length) {
                sb.append("...dan ").append(items.length - count).append(" menu lainnya.\n");
            }
            sb.append("\nKetik 'menu murah' untuk lihat harga termurah!");
            return sb.toString();
        }
        
        // Response default
        return "🍽️ Terima kasih sudah bertanya!\n\n" +
               "Saya bisa bantu informasi tentang:\n" +
               "• Menu dan harga (ketik 'menu murah')\n" +
               "• Rekomendasi menu (ketik 'rekomendasi')\n" +
               "• Stok makanan (ketik 'stok')\n\n" +
               "Ada yang bisa saya bantu lagi? 😊";
    }

    // ==================== GEMINI API (GRATIS) ====================
    private static String callGeminiAPI(String menu, String msg) throws Exception {
        if (!hasValidKey()) {
            return localAI(menu, msg);
        }
        
        String prompt = "Kamu adalah asisten restoran yang ramah, profesional, dan suka membantu. " +
                        "Menu yang tersedia saat ini: " + menu + ". " +
                        "Jawab pertanyaan ini dengan singkat (maksimal 2 kalimat), ramah, " +
                        "dan dalam Bahasa Indonesia. Pertanyaan: " + msg;
        
        // Escape JSON
        String escapedPrompt = prompt.replace("\\", "\\\\")
                                     .replace("\"", "\\\"")
                                     .replace("\n", "\\n")
                                     .replace("\r", "");
        
        String body = "{"
                + "\"contents\":[{\"parts\":[{\"text\":\"" + escapedPrompt + "\"}]}]"
                + "}";
        
        String urlStr = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;
        
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        
        // Kirim request
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
        
        // Baca response
        int responseCode = conn.getResponseCode();
        java.io.InputStream is = (responseCode >= 200 && responseCode < 300) 
                ? conn.getInputStream() 
                : conn.getErrorStream();
        
        String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        
        if (responseCode != 200) {
            // Jika API error, fallback ke local AI
            return localAI(menu, msg);
        }
        
        // Parse response Gemini
        String answer = parseGeminiResponse(response);
        
        if (answer == null || answer.isEmpty()) {
            return localAI(menu, msg);
        }
        
        return answer;
    }
    
    private static String parseGeminiResponse(String json) {
        try {
            // Cari "text" field
            int textIndex = json.indexOf("\"text\"");
            if (textIndex == -1) return null;
            
            int startQuote = json.indexOf("\"", textIndex + 6);
            if (startQuote == -1) return null;
            
            int endQuote = json.indexOf("\"", startQuote + 1);
            if (endQuote == -1) return null;
            
            String answer = json.substring(startQuote + 1, endQuote);
            
            // Clean up answer
            answer = answer.replace("\\n", "\n")
                           .replace("\\\"", "\"")
                           .replace("\\\\", "\\");
            
            return "🤖 " + answer;
        } catch (Exception e) {
            return null;
        }
    }
}