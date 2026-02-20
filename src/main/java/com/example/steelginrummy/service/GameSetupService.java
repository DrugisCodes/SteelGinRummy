package com.example.steelginrummy.service;

import com.example.steelginrummy.model.Card;
import com.example.steelginrummy.model.Deck;
import com.example.steelginrummy.model.Player;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GameSetupService {

    public void prepareNewRound(List<Player> players, Deck deck, List<Card> discardPile, int cardsToDeal) {
        resetGame(players, deck, discardPile);
        dealStartingCards(players, deck, cardsToDeal);
        startDiscardPile(deck, discardPile);
    }

    private void resetGame(List<Player> players, Deck deck, List<Card> discardPile) {
        discardPile.clear();
        for (Player player : players) {
            player.getHand().clear();
        }
        // Viktig: Deck-klassen din må ha en metode som fyller den opp igjen
        deck.resetAndShuffle();
    }

    private void dealStartingCards(List<Player> players, Deck deck, int count) {
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < players.size(); j++) {
                Card card = deck.drawCard();
                if (card != null) {
                    players.get(j).addCard(card);
                }
            }
        }
    }

    private void startDiscardPile(Deck deck, List<Card> discardPile) {
        Card card = deck.drawCard();
        if (card != null) {
            discardPile.add(card);
        }
    }




}