package com.example.steelginrummy.service;

import com.example.steelginrummy.model.Card;

import java.util.List;

public class Meld {
    private List<Card> cards;
    private boolean isSet;

    public Meld(List<Card> cards, boolean isSet) {
        this.cards = cards;
        this.isSet = isSet;
    }

    public boolean isSet() {
        return isSet;
    }

    public void setSet(boolean set) {
        isSet = set;
    }

    public void setCards(List<Card> cards) {
        this.cards = cards;
    }

    public List<Card> getCards() {
        return cards;
    }
//godkjente former av serie og tres
}
