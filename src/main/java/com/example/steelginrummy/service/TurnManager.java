package com.example.steelginrummy.service;

import com.example.steelginrummy.model.Card;
import com.example.steelginrummy.model.Deck;
import com.example.steelginrummy.model.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TurnManager {

    @Autowired
    private RuleEngine ruleEngine;

    public boolean handleBuyPriority(List<Card> discardPile, int activePlayerIndex, List<Player> players, Deck deck) {
        if (discardPile.isEmpty()) return false;

        // Henter det øverste kortet fra kastebunken
        Card cardToBuy = discardPile.get(discardPile.size() - 1);
        int nextPlayerIndex = (activePlayerIndex + 1) % players.size();

        for (int i = 1; i < players.size(); i++) {
            int targetIndex = (activePlayerIndex + i) % players.size();
            Player potentialBuyer = players.get(targetIndex);

            // Her bruker vi den nye determineChoice-metoden
            Choice choice = determineChoice(potentialBuyer, cardToBuy);

            if (choice == Choice.YES) {
                discardPile.remove(cardToBuy);
                potentialBuyer.addCard(cardToBuy);

                // Straffekort hvis det ikke er din tur
                if (targetIndex != nextPlayerIndex) {
                    Card penaltyCard = deck.drawCard();
                    if (penaltyCard != null) potentialBuyer.addCard(penaltyCard);
                    System.out.println(potentialBuyer.getName() + " kjøpte kortet med straff!");
                }
                return true;
            }
        }
        return false;
    }

    private Choice determineChoice(Player player, Card card) {
        if (player.isHuman()) return Choice.WAITING;
        return runAiLogic(player, card) ? Choice.YES : Choice.NO;
    }

    private boolean runAiLogic(Player player, Card card) {
        // REGEL 1: AI takker ALLTID ja til Joker (wildcard + 50 poeng straff for andre!)
        if (card.getValue().equalsIgnoreCase("JOKER")) return true;

        List<Card> currentHand = player.getHand();

        // Sjekk for sett
        long matchingRanks = currentHand.stream()
                .filter(c -> c.getValue().equalsIgnoreCase(card.getValue()))
                .count();

        if (matchingRanks >= 2) return true;

        // Sjekk om det hjelper på en serie
        if (ruleEngine.isCloseToSeries(currentHand, card)) return true;

        // Strategisk: Ta lave kort for å minske deadwood
        int cardValue = ruleEngine.mapRankToValue(card, false);
        return cardValue > 0 && cardValue <= 3;
    }
}



