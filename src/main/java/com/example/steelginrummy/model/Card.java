package com.example.steelginrummy.model;

public class Card {
    private int value;
    private String type;


    public Card(int value, String type) {
        this.value = value;
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Card{" +
                "value=" + value +
                ", type='" + type +
                '}';
    }
}
