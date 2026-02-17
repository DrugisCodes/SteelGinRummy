package com.example.steelginrummy.service;

import com.example.steelginrummy.model.Card;
import com.example.steelginrummy.model.Player;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RuleEngine {

    public int mapRankToValue(Card card, boolean aceIs14) {
        return switch (card.getValue()) {
            case "ACE" -> aceIs14 ? 14 : 1;
            case "KING" -> 13;
            case "QUEEN" -> 12;
            case "JACK" -> 11;
            default -> convertToInt(card.getValue());
        };


    }

    private int convertToInt(String value) {

        return Integer.parseInt(value);
    }

    //teller hvor mange av en type spilleren har

    private HashMap<String, Integer> countRankFrequencies(List<Card> hand) {
        HashMap<String, Integer> frequencyMap = new HashMap<>();

        for(Card card : hand) {
            String value = card.getValue();

            int currentCount = frequencyMap.getOrDefault(value, 0);

            frequencyMap.put(value, currentCount + 1);

        }
        return frequencyMap;
    }

    private boolean hasRequiredSetsOfThree(int requiredAmount, List<Card> hand) {

        HashMap<String, Integer> counts = countRankFrequencies(hand);

        int setsFound = 0;

        for(String value : counts.keySet()) {
            if(counts.get(value) >= 3) {
                setsFound = 1 + setsFound;
            }

        }
        return (setsFound >= requiredAmount);
    }

    public boolean hasRequiredSeries(List<Card> hand, int requiredAmount, int runSize) {
        // 1. Grupper kortene etter farge
        Map<String, List<Card>> typeGroup = new HashMap<>();
        for (Card card : hand) {
            typeGroup.computeIfAbsent(card.getType(), k -> new ArrayList<>()).add(card);
        }

        int totalSeriesFound = 0;

        // 2. Bruk hjelpemetoden for hver fargegruppe
        for (List<Card> cardsInSuit : typeGroup.values()) {
            totalSeriesFound += countRunsInSingleSuit(cardsInSuit, runSize);
        }

        return totalSeriesFound >= requiredAmount;
    }
    private int countRunsInSingleSuit(List<Card> cards, int runSize) {
        if (cards.size() < runSize) {
            return 0;
        }

        // Konverter til tallverdier og sorter
        List<Integer> values = new ArrayList<>();
        for (Card card : cards) {
            values.add(mapRankToValue(card, false));
        }
        Collections.sort(values);

        int foundInThisSuit = 0;
        int count = 1;

        for (int i = 0; i < values.size() - 1; i++) {
            int current = values.get(i);
            int next = values.get(i + 1);

            if (next == current + 1) {
                count++;
                if (count == runSize) {
                    foundInThisSuit++;
                    count = 0; // Nullstill for å finne neste potensielle serie
                }
            } else if (next != current) {
                count = 1; // Brudd i rekken
            }
        }

        return foundInThisSuit;
    }


    // Bot hjelpe metode
    public boolean isCloseToSeries(List<Card> hand, Card card) {
        int targetValue = mapRankToValue(card, false);
        String targetType = card.getType(); //kløver osv

        for(Card handCard : hand) {
            if(handCard.getType().equalsIgnoreCase(targetType)) {
                int handValue = mapRankToValue(handCard, false);

                if(Math.abs(handValue - targetValue) == 1) {
                    return true;
                }
            }
        }
        return false;

        }


    public boolean canAttachToSet(Card card, List<Card> existingSet) {
        return false;
    }

    public boolean canAttachToSeries(Card card, List<Card> existingRun) {
        return false;
    }

    public boolean canLayOff(Card card, List<Card> meld) {
        return false;
    }

    public boolean canLayDown(Player player, int currentRound) {
        return false;
    }
}
