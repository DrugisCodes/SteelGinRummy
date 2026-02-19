package com.example.steelginrummy.model;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private String name;
    private List<Card> hand;
    private boolean isHuman;
    public boolean hasLaidDown = false;
    private int totalGameScore = 0;

    public int getTotalGameScore() {
        return totalGameScore;
    }

    public Player(int totalGameScore) {
        this.totalGameScore = totalGameScore;
    }

    public void setHand(List<Card> hand) {
        this.hand = hand;
    }

    public boolean isHuman() {
        return isHuman;
    }

    public Player(String name) {
        this.name = name;
        this.hand = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Card> getHand() {
        return hand;
    }

    public void addCard(Card card) {

        this.hand.add(card);
    }

    public void addScore(int roundScore) {
        this.totalGameScore += roundScore;
    }
}