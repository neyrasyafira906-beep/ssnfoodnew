package com.ssn.food.model;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ChatMsg {
    public enum From { BUYER, SELLER, AI, SYSTEM }
    public final From   from;
    public final String text;
    public final String time;

    public ChatMsg(From from, String text) {
        this.from = from; this.text = text;
        this.time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
