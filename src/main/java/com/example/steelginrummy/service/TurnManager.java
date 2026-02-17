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

    public boolean handleBuyPriority(List<Card> discardCard, int activePlayerIndex, List<Player> players, Deck deck) {
        if(discardCard.isEmpty()) {
            return false;
        }

        Card cardToBuy = discardCard.get(discardCard.size()-1);

        //personen som står i tur etter spilleren
        int nextPlayerIndex = (activePlayerIndex+1) % players.size();

        for(int i = 1; i < players.size(); i++) {
            int targetIndex = (activePlayerIndex + i) % players.size();
            Player potentialBuyer = players.get(targetIndex);

            System.out.println("Asking  " + potentialBuyer.getName() + " Do you want to take " + discardCard + "?");

            if(simulatePlayerChoice(potentialBuyer)) {
                discardCard.remove(cardToBuy);
                potentialBuyer.getHand().add(cardToBuy);

                // 2. Sjekk om det skal gis straffekort
                if(targetIndex == nextPlayerIndex) {
                    System.out.println(potentialBuyer.getName() + " took  " + discardCard + "with no pentalty");

                } else {
                    Card penaltyCard = deck.drawCard();
                    if(penaltyCard != null) {
                        potentialBuyer.getHand().add(penaltyCard);
                        System.out.println(potentialBuyer.getName() + " took  " + discardCard + " with pentalty");
                    }
                }
                return true;
            }


            }
        return false;


        }
    private boolean simulatePlayerChoice(Player potentialBuyer) {
        return false;
    }

    private Choice determineChoice(Player player, Card card) {
        if(player.isHuman()) {
            return Choice.WAITING;
        } else {
            return runAiLogic(player, card) ? Choice.YES : Choice.NO;
        }
    }

    private boolean runAiLogic(Player player, Card card) {
        List<Card> currentHand = player.getHand();

        // 1. Fullfører dette kortet et sett (3 eller 4 like)?
        long matchingRanks = currentHand.stream().filter(c -> c.getValue()
                                    .equalsIgnoreCase(card.getValue())).count();

        if(matchingRanks >= 2) {
            System.out.println(player.getName() + " wants the card");
            return true;
        }
        // 2. Lager dette kortet et par (nær et sett)?
        if(matchingRanks == 1) {
            System.out.println(player.getName() + " wants the card");
        }
        // 3. Hjelper kortet på en serie (Run)?
        if(ruleEngine.isCloseToSeries(currentHand, card)) {
            return true;

        }
        // 4. Strategisk: Er det et veldig lavt kort?
        int cardValue = ruleEngine.mapRankToValue(card, false);
            if(cardValue <= 3) {
                return true;
        }
        return false;


    }


}
