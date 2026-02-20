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

        // Vi legger til TO fulle kortstokker (104 kort)
        for (int i = 0; i < 2; i++) {
            for (String type : types) {
                for (String value : values) {
                    cards.add(new Card(type, value));
                }
            }
        }

        // Legger til 4 Jokere (2 per kortstokk er standard)
        for (int i = 0; i < 4; i++) {
            cards.add(new Card("WILD", "JOKER"));
        }

        Collections.shuffle(cards);
        System.out.println("Kortstokken (108 kort) er stokket med Jokere!");
    }

    public void refillFromDiscard(List<Card> discardPile) {
        if (discardPile.size() <= 1) return; // Må beholde det øverste kortet

        Card topCard = discardPile.remove(discardPile.size() - 1);
        this.cards.addAll(discardPile);
        discardPile.clear();
        discardPile.add(topCard);

        Collections.shuffle(this.cards);
        System.out.println("Stokken var tom! Har stokket om kastebunken.");
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
