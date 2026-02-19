package com.example.steelginrummy.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
    List<Card> cards = new ArrayList<Card>();


    public void resetAndShuffle() {
        cards.clear();

        String[] values = {"Ace", "2", "3", "4", "5", "6", "7", "8", "9", "10", "Jack", "Queen", "King"};
        String[] types = {"Hearts", "Diamonds", "Spades", "Clubs"};

        for(String type : types ) {
            for(String value : values ) {
                Card card = new Card(type, value);
                cards.add(card);
            }
        }

        Collections.shuffle(cards);

        System.out.println("Kortstokken er stokket!");

    }
    public List<Card> getCards() {
        return cards;
    }
    public Card drawCard() {
        if(cards.isEmpty()) {
            return null;

        }
        return cards.remove(cards.size()-1);
    }

    public void addCard(Card card) {
        if(card == null)
    {
        return;
    }
        cards.add(card);
    }

}
