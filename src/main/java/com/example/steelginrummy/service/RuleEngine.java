package com.example.steelginrummy.service;

import com.example.steelginrummy.model.Card;
import com.example.steelginrummy.model.Player;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class RuleEngine {

    // --- GRUNNLEGGENDE LOGIKK OG MAPPING ---

    public int mapRankToValue(Card card, boolean aceIs14) {
        if (isJoker(card)) return 0;
        String val = card.getValue().toUpperCase().trim();
        return switch (val) {
            case "ACE" -> aceIs14 ? 14 : 1;
            case "KING" -> 13;
            case "QUEEN" -> 12;
            case "JACK" -> 11;
            default -> {
                try {
                    yield Integer.parseInt(val); // Java 21 krever yield i blokk-switch
                } catch (NumberFormatException e) {
                    yield 0;
                }
            }
        };
    }

    public boolean isJoker(Card card) {
        if (card == null || card.getValue() == null) return false;
        String val = card.getValue().toUpperCase();
        return val.equals("JOKER") || val.equals("WILD");
    }

    private int countJokers(List<Card> cards) {
        return (int) cards.stream().filter(this::isJoker).count();
    }

    private HashMap<String, Integer> countRankFrequencies(List<Card> hand) {
        HashMap<String, Integer> map = new HashMap<>();
        for (Card c : hand) {
            if (!isJoker(c)) map.put(c.getValue(), map.getOrDefault(c.getValue(), 0) + 1);
        }
        return map;
    }

    // --- RUNDE-LOGIKK: IDENTIFISERING AV KOMBINASJONER ---

    public List<List<Card>> identifyRequiredMelds(List<Card> hand, int currentRound) {
        return switch (currentRound) {
            case 1 -> extractSets(new ArrayList<>(hand), 2);
            case 2 -> tryRound2(hand); // Håndterer bytte av jokere mellom sett/serie
            case 3 -> extractSeries(new ArrayList<>(hand), 2, 4);
            case 4 -> extractSets(new ArrayList<>(hand), 3);
            case 5 -> tryRound5(hand); // Håndterer bytte av jokere mellom 2 sett/1 serie
            case 6 -> isPerfectHand(hand) ? extractPerfect(hand) : new ArrayList<>();
            default -> new ArrayList<>();
        };
    }

    private List<List<Card>> tryRound2(List<Card> hand) {
        // Forsøk A: Serie først, deretter sett
        List<Card> cardsA = new ArrayList<>(hand);
        List<List<Card>> seriesA = extractSeries(cardsA, 1, 4);
        List<List<Card>> setsA = extractSets(cardsA, 1);
        if (seriesA.size() == 1 && setsA.size() == 1) {
            List<List<Card>> result = new ArrayList<>(seriesA);
            result.addAll(setsA);
            return result;
        }

        // Forsøk B: Sett først, deretter serie (Jokeren flyttes hvis nødvendig)
        List<Card> cardsB = new ArrayList<>(hand);
        List<List<Card>> setsB = extractSets(cardsB, 1);
        List<List<Card>> seriesB = extractSeries(cardsB, 1, 4);
        if (setsB.size() == 1 && seriesB.size() == 1) {
            List<List<Card>> result = new ArrayList<>(setsB);
            result.addAll(seriesB);
            return result;
        }
        return new ArrayList<>();
    }

    private List<List<Card>> tryRound5(List<Card> hand) {
        // Samme bytte-logikk for 2 sett og 1 serie
        for (boolean runFirst : new boolean[]{true, false}) {
            List<Card> rem = new ArrayList<>(hand);
            List<List<Card>> res = new ArrayList<>();
            if (runFirst) {
                res.addAll(extractSeries(rem, 1, 4));
                res.addAll(extractSets(rem, 2));
            } else {
                res.addAll(extractSets(rem, 2));
                res.addAll(extractSeries(rem, 1, 4));
            }
            if (res.size() == 3) return res;
        }
        return new ArrayList<>();
    }

    // --- EKSTRAKSJONSMETODER (SETT OG SERIER) ---

    public List<List<Card>> extractSets(List<Card> cards, int amountNeeded) {
        List<List<Card>> found = new ArrayList<>();
        for (int i = 0; i < amountNeeded; i++) {
            Map<String, Integer> counts = countRankFrequencies(cards);
            int jokersAvailable = countJokers(cards);
            String target = counts.entrySet().stream()
                    .filter(e -> e.getValue() + jokersAvailable >= 3)
                    .map(Map.Entry::getKey).findFirst().orElse(null);

            if (target != null) {
                List<Card> set = new ArrayList<>();
                Iterator<Card> it = cards.iterator();
                while (it.hasNext() && set.size() < 3) {
                    Card c = it.next();
                    if (c.getValue().equalsIgnoreCase(target)) { set.add(c); it.remove(); }
                }
                while (set.size() < 3) {
                    Card j = cards.stream().filter(this::isJoker).findFirst().orElse(null);
                    if (j != null) { set.add(j); cards.remove(j); } else break;
                }
                if (set.size() == 3) found.add(set);
            }
        }
        return found;
    }

    public List<List<Card>> extractSeries(List<Card> cards, int amountNeeded, int runSize) {
        List<List<Card>> found = new ArrayList<>();
        for (int i = 0; i < amountNeeded; i++) {
            List<Card> run = findBestSeries(cards, runSize); // Bruker gjerrig joker-logikk
            if (run != null) {
                for (Card c : run) cards.remove(c);
                found.add(run);
            }
        }
        return found;
    }

    private List<Card> findBestSeries(List<Card> cards, int size) {
        List<Card> best = null;
        int minJokers = Integer.MAX_VALUE;

        for (boolean high : new boolean[]{false, true}) {
            Map<String, List<Card>> suits = new HashMap<>();
            for (Card c : cards) if (!isJoker(c)) suits.computeIfAbsent(c.getType(), k -> new ArrayList<>()).add(c);

            int availableJokers = countJokers(cards);
            for (List<Card> suitCards : suits.values()) {
                for (int start = 1; start <= 15 - size; start++) {
                    List<Card> naturalCards = new ArrayList<>();
                    int jokersNeeded = 0;
                    for (int v = start; v < start + size; v++) {
                        final int val = v;
                        Optional<Card> m = suitCards.stream().filter(c -> mapRankToValue(c, high) == val).findFirst();
                        if (m.isPresent()) naturalCards.add(m.get());
                        else jokersNeeded++; // Teller manglende kort korrekt
                    }

                    if (jokersNeeded <= availableJokers && jokersNeeded < minJokers) {
                        minJokers = jokersNeeded;
                        List<Card> run = new ArrayList<>(naturalCards);
                        cards.stream().filter(this::isJoker).limit(jokersNeeded).forEach(run::add);
                        best = run;
                    }
                }
            }
        }
        return best;
    }

    // --- VALIDERING OG POENG ---

    public boolean canLayDown(Player player, int currentRound) {
        return !identifyRequiredMelds(player.getHand(), currentRound).isEmpty();
    }

    private boolean isPerfectHand(List<Card> hand) {
        List<Card> rem = new ArrayList<>(hand);
        extractSets(rem, 1);
        extractSeries(rem, 2, 4);
        return rem.isEmpty();
    }

    private List<List<Card>> extractPerfect(List<Card> hand) {
        List<Card> rem = new ArrayList<>(hand);
        List<List<Card>> res = extractSets(rem, 1);
        res.addAll(extractSeries(rem, 2, 4));
        return res;
    }

    public int calculateRoundScore(Player player) {
        return player.getHand().stream().mapToInt(c -> isJoker(c) ? 50 :
                switch(c.getValue().toUpperCase()) {
                    case "ACE" -> 25;
                    case "KING", "QUEEN", "JACK", "10" -> 10;
                    default -> 5;
                }).sum();
    }

    // --- LAYOFF OG AI LOGIKK ---

    public boolean canLayOff(Card card, List<Card> existingMeld) {
        if (isJoker(card)) return true;
        List<Card> nonJ = existingMeld.stream().filter(x -> !isJoker(x)).toList();
        if (nonJ.size() < 1) return true;

        // Sjekk om det er et sett (alle har samme verdi)
        if (nonJ.size() >= 2 && nonJ.get(0).getValue().equalsIgnoreCase(nonJ.get(1).getValue())) {
            return card.getValue().equalsIgnoreCase(nonJ.get(0).getValue());
        }

        // Ellers er det en serie
        if (!card.getType().equalsIgnoreCase(nonJ.get(0).getType())) return false;
        int low = mapRankToValue(card, false), high = mapRankToValue(card, true);
        List<Integer> vLow = nonJ.stream().map(x -> mapRankToValue(x, false)).sorted().toList();
        List<Integer> vHigh = nonJ.stream().map(x -> mapRankToValue(x, true)).sorted().toList();
        return (low == vLow.get(0)-1 || low == vLow.get(vLow.size()-1)+1) ||
                (high == vHigh.get(0)-1 || high == vHigh.get(vHigh.size()-1)+1);
    }

    public boolean isCloseToSeries(List<Card> hand, Card card) {
        if (isJoker(card)) return true;
        String type = card.getType();
        int val = mapRankToValue(card, false);
        for (Card h : hand) {
            if (h.getType().equalsIgnoreCase(type)) {
                int hVal = mapRankToValue(h, false);
                if (Math.abs(hVal - val) >= 1 && Math.abs(hVal - val) <= 2) return true;
            }
        }
        return false;
    }

    public boolean hasRequiredSeries(List<Card> hand, int req, int size) {
        return extractSeries(new ArrayList<>(hand), req, size).size() >= req;
    }
    public boolean isUsefulCard(Card card, List<Card> hand, int currentRound) {
        // 1. Sjekk om kortet hjelper oss å nå runden (identifyRequiredMelds)
        // 2. Sjekk om kortet kan legges på noe vi ALLEREDE har (lay-off logikk)
        List<List<Card>> currentMelds = identifyRequiredMelds(hand, currentRound);
        for (List<Card> meld : currentMelds) {
            if (canLayOff(card, meld)) return true; // Behold kortet!
        }
        return isCloseToSeries(hand, card); // Eller hvis det kan bli noe senere
    }
}