package com.ssn.food.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import javax.swing.SwingWorker;

import com.ssn.food.model.Seller;

public class AIService {
    private static String key = System.getenv("GEMINI_API_KEY") != null
            ? System.getenv("GEMINI_API_KEY") : "";

    public static void setKey(String k) { key = k; }
    public static String getKey() { return key; }

    public static void ask(String sellerId, String userMsg,
                           Consumer<String> onOk, Consumer<String> onErr) {
        Seller s = AppStore.get().findSeller(sellerId);
        StringBuilder mb = new StringBuilder();
        if (s != null) {
            s.getMenu().forEach(m -> mb.append(m.getName()).append(" ").append(m.formatPrice())
                    .append(" stock:").append(m.getStock()).append("; "));
        }
        final String menuStr = mb.toString();

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return callGeminiAPI(menuStr, userMsg);
            }
            @Override
            protected void done() {
                try {
                    onOk.accept(get());
                } catch (Exception e) {
                    onErr.accept("AI error: " + e.getMessage());
                }
            }
        }.execute();
    }

    /**
     * Call Google Gemini API
     * Free tier: 50-100 requests per day (enough for testing)
     * Get API key from: https://aistudio.google.com/
     */
    private static String callGeminiAPI(String menu, String msg) {
        // Fallback mock AI response logic
        String mockResponse = "Halo! Saya asisten AI. ";
        if (msg.toLowerCase().contains("murah") || msg.toLowerCase().contains("cheap")) {
            mockResponse += "Berdasarkan menu, opsi termurah ada di sini: " + menu;
        } else if (msg.toLowerCase().contains("rekomendasi") || msg.toLowerCase().contains("recommend")) {
            mockResponse += "Saya merekomendasikan menu terbaik kami: " + menu;
        } else {
            mockResponse += "Ada yang bisa saya bantu terkait pesanan? Menu kami: " + menu;
        }
        
        if (key.isEmpty() || key.trim().isEmpty()) {
            return "🤖 " + mockResponse;
        }

        try {
            // System prompt to guide the AI
            String systemPrompt = "You are a friendly restaurant assistant. " +
                    "Available menu: " + menu + ". " +
                    "Answer briefly in English (max 2 sentences). Be helpful and polite. " +
                    "If asked about price, format as Rp X.XXX. " +
                    "If item not available, suggest similar alternatives. " +
                    "If user asks for cheap menu, list the cheapest items. " +
                    "If user asks for menu recommendation, suggest popular items.";

            // Build the prompt with system instruction + user message
            String fullPrompt = systemPrompt + "\n\nUser: " + msg + "\n\nAssistant:";

            // Gemini API JSON body
            String jsonBody = String.format(
                "{\n" +
                "  \"contents\": [\n" +
                "    {\n" +
                "      \"parts\": [\n" +
                "        {\"text\": \"%s\"}\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}",
                escapeJson(fullPrompt)
            );

            // Connect to Gemini API
            String urlString = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + key;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            // Send request
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            // Check response code
            int responseCode = conn.getResponseCode();
            
            if (responseCode == 429 || responseCode == 403 || responseCode >= 400) {
                return "🤖 " + mockResponse; // Fallback instead of limit error
            }

            // Read response
            BufferedReader br;
            if (responseCode >= 200 && responseCode < 300) {
                br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            } else {
                br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();

            if (responseCode != 200) {
                return "🤖 " + mockResponse;
            }

            // Parse Gemini response
            String reply = parseGeminiResponse(response.toString());
            return "🤖 " + reply;

        } catch (Exception e) {
            e.printStackTrace();
            return "🤖 " + mockResponse;
        }
    }

    /**
     * Parse JSON response from Gemini API
     * Response format: {"candidates":[{"content":{"parts":[{"text":"..."}]}}]}
     */
    private static String parseGeminiResponse(String json) {
        try {
            // Find "text" field in the response
            String searchText = "\"text\":";
            int textIndex = json.indexOf(searchText);
            if (textIndex == -1) {
                return "No response from AI. Please try again.";
            }
            
            // Find the start of the text value
            int startQuote = json.indexOf("\"", textIndex + searchText.length());
            if (startQuote == -1) {
                return "Invalid response format.";
            }
            
            // Find the end quote
            int endQuote = json.indexOf("\"", startQuote + 1);
            if (endQuote == -1) {
                return "Invalid response format.";
            }
            
            String answer = json.substring(startQuote + 1, endQuote);
            
            // Clean up the answer
            answer = answer.replace("\\n", "\n")
                           .replace("\\\"", "\"")
                           .replace("\\\\", "\\");
            
            // Remove the system prompt from response if it appears
            if (answer.contains("User:")) {
                int lastUserIndex = answer.lastIndexOf("User:");
                if (lastUserIndex > 0) {
                    answer = answer.substring(0, lastUserIndex).trim();
                }
            }
            
            if (answer.isEmpty()) {
                return "I'm here to help with your food orders! Ask me about menu, prices, or recommendations.";
            }
            
            return answer;
            
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to parse AI response: " + e.getMessage();
        }
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "\\t");
    }
}