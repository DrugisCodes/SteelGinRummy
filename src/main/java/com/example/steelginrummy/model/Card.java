package com.example.steelginrummy.model;

public class Card {
    private final String value;
    private final String type;


    public Card(String type, String value) {
        this.value = value;
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value + "av " +  type;
    }
}
