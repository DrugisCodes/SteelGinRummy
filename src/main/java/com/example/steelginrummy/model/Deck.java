package com.example.steelginrummy.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
    List<Card> cards = new ArrayList<Card>();

    public Deck() {
        String[] cardsType = {"Heart", "Spade", "Diamond", "Clover"};

        for(String type : cardsType) {
            for(int value = 1; value <= 13; value++) {
                this.cards.add(new Card(value, type));
            }
        }

    }

    public void shuffle() {
        Collections.shuffle(cards);
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
        cards.add(card);
    }


    //funksjon for å handtere poengsum

}
