package com.example.steelginrummy.service;

import com.example.steelginrummy.model.Card;
import com.example.steelginrummy.model.Player;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RuleEngine {

    // Hjelpeklasse for å holde styr på resultater fra serier (Runs)
    private static class RunResult {
        int cardCount = 0;
        int groupCount = 0;
    }

    // --- GRUNNLEGGENDE LOGIKK ---

    public int mapRankToValue(Card card, boolean aceIs14) {
        // Bruker toUpperCase for å unngå feil ved små/store bokstaver
        String val = card.getValue().toUpperCase();
        return switch (val) {
            case "ACE" -> aceIs14 ? 14 : 1;
            case "KING" -> 13;
            case "QUEEN" -> 12;
            case "JACK" -> 11;
            default -> Integer.parseInt(val);
        };
    }

    private HashMap<String, Integer> countRankFrequencies(List<Card> hand) {
        HashMap<String, Integer> frequencyMap = new HashMap<>();
        for (Card card : hand) {
            String value = card.getValue();
            frequencyMap.put(value, frequencyMap.getOrDefault(value, 0) + 1);
        }
        return frequencyMap;
    }

    // --- SJEKK AV KRAV (SETS & SERIES) ---

    private boolean hasRequiredSetsOfThree(int requiredAmount, List<Card> hand) {
        HashMap<String, Integer> counts = countRankFrequencies(hand);
        int setsFound = 0;
        for (int count : counts.values()) {
            if (count >= 3) setsFound++;
        }
        return setsFound >= requiredAmount;
    }

    public boolean hasRequiredSeries(List<Card> hand, int requiredAmount, int runSize) {
        Map<String, List<Card>> typeGroup = new HashMap<>();
        for (Card card : hand) {
            typeGroup.computeIfAbsent(card.getType(), k -> new ArrayList<>()).add(card);
        }
        int totalSeriesFound = 0;
        for (List<Card> cardsInSuit : typeGroup.values()) {
            totalSeriesFound += countRunsInSingleSuit(cardsInSuit, runSize);
        }
        return totalSeriesFound >= requiredAmount;
    }

    private int countRunsInSingleSuit(List<Card> cards, int runSize) {
        if (cards.size() < runSize) return 0;

        List<Integer> values = new ArrayList<>();
        for (Card card : cards) values.add(mapRankToValue(card, false));
        Collections.sort(values);

        int foundInThisSuit = 0;
        int count = 1;

        for (int i = 0; i < values.size() - 1; i++) {
            if (values.get(i + 1) == values.get(i) + 1) {
                count++;
                if (count == runSize) {
                    foundInThisSuit++;
                    count = 0;
                }
            } else if (values.get(i + 1) != values.get(i)) {
                count = 1;
            }
        }
        return foundInThisSuit;
    }

    // --- AI LOGIKK ---

    public boolean isCloseToSeries(List<Card> hand, Card card) {
        int targetValue = mapRankToValue(card, false);
        String targetType = card.getType();

        for (Card handCard : hand) {
            if (handCard.getType().equalsIgnoreCase(targetType)) {
                int handValue = mapRankToValue(handCard, false);
                if (Math.abs(handValue - targetValue) == 1) return true;
            }
        }
        return false;
    }

    // --- LAYDOWN & PERFECT HAND LOGIKK ---

    public boolean canLayDown(Player player, int currentRound) {
        List<Card> hand = player.getHand();

        return switch (currentRound) {
            case 1 -> hasRequiredSetsOfThree(2, hand);
            case 2 -> hasRequiredSetsOfThree(1, hand) && hasRequiredSeries(hand, 1, 4);
            case 3 -> hasRequiredSeries(hand, 2, 4);
            case 4 -> hasRequiredSetsOfThree(3, hand);
            case 5 -> hasRequiredSeries(hand, 1, 4) && hasRequiredSetsOfThree(2, hand);
            case 6 -> isPerfectHand(hand, 1, 2, 4); // Bruker hjelpemetoden her!
            default -> false;
        };
    }

    //hjelpe metode
    private boolean isPerfectHand(List<Card> hand, int minSets, int minRuns, int runMinSize) {
        int cardsInSets = 0;
        int setGroups = 0;

        // Tell kort i Tres
        Map<String, Integer> frequencies = countRankFrequencies(hand);
        for (int count : frequencies.values()) {
            if (count >= 3) {
                cardsInSets += count;
                setGroups++;
            }
        }

        // Tell kort i Serier
        int cardsInRuns = 0;
        int runGroups = 0;
        Map<String, List<Card>> typeGroup = new HashMap<>();
        for (Card card : hand) {
            typeGroup.computeIfAbsent(card.getType(), k -> new ArrayList<>()).add(card);
        }

        for (List<Card> suitCards : typeGroup.values()) {
            RunResult res = calculateRunsInSuit(suitCards, runMinSize);
            cardsInRuns += res.cardCount;
            runGroups += res.groupCount;
        }

        // Sjekk om alle krav er møtt OG alle kort er brukt
        boolean hasEnoughGroups = (setGroups >= minSets) && (runGroups >= minRuns);
        boolean allCardsUsed = (cardsInSets + cardsInRuns) == hand.size();

        return hasEnoughGroups && allCardsUsed;
    }

    public List<List<Card>> identifyRequiredMelds(List<Card> hand, int currentRound) {
        List<List<Card>> resultMelds = new ArrayList<>();
        // Vi jobber på en kopi slik at vi kan fjerne kort etter hvert som de "brukes"
        List<Card> remainingCards = new ArrayList<>(hand);

        switch (currentRound) {
            case 1 -> // 2 tres
                    resultMelds.addAll(extractSets(remainingCards, 2));

            case 2 -> { // 1 tres, 1 serie (4 kort)
                resultMelds.addAll(extractSets(remainingCards, 1));
                resultMelds.addAll(extractSeries(remainingCards, 1, 4));
            }

            case 3 -> // 2 serier (4 kort hver)
                    resultMelds.addAll(extractSeries(remainingCards, 2, 4));

            case 4 -> // 3 tres
                    resultMelds.addAll(extractSets(remainingCards, 3));

            case 5 -> { // 2 tres, 1 serie (4 kort)
                resultMelds.addAll(extractSets(remainingCards, 2));
                resultMelds.addAll(extractSeries(remainingCards, 1, 4));
            }

            case 6 -> { // Perfect Hand: 1 tres, 2 serier
                resultMelds.addAll(extractSets(remainingCards, 1));
                resultMelds.addAll(extractSeries(remainingCards, 2, 4));
                // I en perfect hand skal resten av kortene (hvis noen) også inkluderes
                if (!remainingCards.isEmpty()) {
                    resultMelds.add(new ArrayList<>(remainingCards));
                }
            }
        }
        return resultMelds;
    }


    private List<List<Card>> extractSets(List<Card> cards, int amountNeeded) {
        List<List<Card>> foundSets = new ArrayList<>();

        for (int i = 0; i < amountNeeded; i++) {
            Map<String, Integer> counts = countRankFrequencies(cards);
            String rankToExtract = null;

            // Finn en verdi som har 3 eller flere kort
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                if (entry.getValue() >= 3) {
                    rankToExtract = entry.getKey();
                    break;
                }
            }

            if (rankToExtract != null) {
                List<Card> newSet = new ArrayList<>();
                Iterator<Card> it = cards.iterator();
                while (it.hasNext() && newSet.size() < 3) {
                    Card c = it.next();
                    if (c.getValue().equalsIgnoreCase(rankToExtract)) {
                        newSet.add(c);
                        it.remove(); // Fjerner kortet fra 'remainingCards'
                    }
                }
                foundSets.add(newSet);
            }
        }
        return foundSets;
    }

    private List<List<Card>> extractSeries(List<Card> cards, int amountNeeded, int runSize) {
        List<List<Card>> foundRuns = new ArrayList<>();

        for (int a = 0; a < amountNeeded; a++) {
            // Grupper etter farge (Type)
            Map<String, List<Card>> suitGroups = new HashMap<>();
            for (Card c : cards) {
                suitGroups.computeIfAbsent(c.getType(), k -> new ArrayList<>()).add(c);
            }

            List<Card> seriesToRemove = null;

            for (List<Card> suitCards : suitGroups.values()) {
                if (suitCards.size() < runSize) continue;

                // Sorter kortene i fargen for å finne sammenhengende rekke
                suitCards.sort(Comparator.comparingInt(c -> mapRankToValue(c, false)));

                List<Card> potentialRun = new ArrayList<>();
                potentialRun.add(suitCards.get(0));

                for (int i = 0; i < suitCards.size() - 1; i++) {
                    int currentVal = mapRankToValue(suitCards.get(i), false);
                    int nextVal = mapRankToValue(suitCards.get(i + 1), false);

                    if (nextVal == currentVal + 1) {
                        potentialRun.add(suitCards.get(i + 1));
                        if (potentialRun.size() == runSize) {
                            seriesToRemove = new ArrayList<>(potentialRun);
                            break;
                        }
                    } else if (nextVal != currentVal) {
                        potentialRun.clear();
                        potentialRun.add(suitCards.get(i + 1));
                    }
                }
                if (seriesToRemove != null) break;
            }

            if (seriesToRemove != null) {
                // Fjern kortene fra hovedlisten slik at de ikke brukes på nytt
                for (Card r : seriesToRemove) {
                    cards.remove(r);
                }
                foundRuns.add(seriesToRemove);
            }
        }
        return foundRuns;
    }


    private RunResult calculateRunsInSuit(List<Card> cards, int minSize) {
        RunResult result = new RunResult();
        if (cards.size() < minSize) return result;

        List<Integer> values = new ArrayList<>();
        for (Card c : cards) values.add(mapRankToValue(c, false));
        Collections.sort(values);

        int currentRun = 1;
        for (int i = 0; i < values.size() - 1; i++) {
            if (values.get(i + 1) == values.get(i) + 1) {
                currentRun++;
            } else if (values.get(i + 1) != values.get(i)) {
                if (currentRun >= minSize) {
                    result.cardCount += currentRun;
                    result.groupCount++;
                }
                currentRun = 1;
            }
        }
        if (currentRun >= minSize) {
            result.cardCount += currentRun;
            result.groupCount++;
        }
        return result;
    }

    // --- LAYOFF LOGIKK (SKALERES VIDERE) ---

    public boolean canAttachToSet(Card card, List<Card> existingSet) {
        if (existingSet == null || existingSet.isEmpty()) {
            return false;
        }
        String representativeValue = existingSet.get(0).getValue();

        if(card.getValue().equalsIgnoreCase(representativeValue)){
            return true;

        } else {
            return false;
        }
    }

    public boolean canAttachToSeries(Card card, List<Card> existingSeries) {
        if (existingSeries == null || existingSeries.isEmpty()) return false;

        // 1. Sjekk farge - bruker equalsIgnoreCase for sikkerhets skyld
        if (!card.getType().equalsIgnoreCase(existingSeries.get(0).getType())) {
            return false;
        }

        // 2. Bruk hjelpemetoden for å sjekke om vi skal tolke Ess som 1 eller 14
        boolean aceIsHigh = hasKing(existingSeries);

        // 3. Konverter og sorter eksisterende verdier
        List<Integer> values = getSortedNumericValues(existingSeries, aceIsHigh);
        int low = values.get(0);
        int high = values.get(values.size() - 1);

        // 4. Sjekk om det nye kortet passer som enten 1 eller 14
        int val1 = mapRankToValue(card, false); // Ess som 1
        int val14 = mapRankToValue(card, true); // Ess som 14

        return (val1 == low - 1 || val1 == high + 1) ||
                (val14 == low - 1 || val14 == high + 1);
    }

    /**
     * Hjelpemetode: Sjekker om en samling kort inneholder en Konge.
     * Brukes for å avgjøre om Ess i samme rekke skal være 1 eller 14.
     */
    private boolean hasKing(List<Card> cards) {
        for (Card c : cards) {
            if (c.getValue().equalsIgnoreCase("KING")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Hjelpemetode: Mapper kort til tall og sorterer listen.
     */
    private List<Integer> getSortedNumericValues(List<Card> cards, boolean aceIsHigh) {
        List<Integer> values = new ArrayList<>();
        for (Card c : cards) {
            values.add(mapRankToValue(c, aceIsHigh));
        }
        Collections.sort(values);
        return values;
    }

    public int calculateDeadwood(Player player, List<List<Card>> allMeldsOnTable) {
        List<Card> remainingCards = new ArrayList<>(player.getHand());

        //fjerne kort som vi har lagt ned
        removeOwnMelds(remainingCards);

        // 3. FJERN LAY-OFFS (Kort som passer på andres stakker)
        // Gå gjennom hvert kort som er igjen og se om det kan legges på bordet
        Iterator<Card> iterator = remainingCards.iterator();
        while (iterator.hasNext()) {
            Card card = iterator.next();
            for (List<Card> tableMeld : allMeldsOnTable) {
                if (canLayOff(card, tableMeld)) {
                    iterator.remove(); // Kortet passet på bordet! 0 poeng.
                    break;
                }
            }
        }

        // 4. TABELLER POENGSUMMEN
        // Nå inneholder remainingCards kun de kortene som faktisk gir minuspoeng
        int totalScore = 0;
        for (Card card : remainingCards) {
            totalScore += getCardScoringValue(card);
        }

        return totalScore;
    }

    private void removeOwnMelds(List<Card> hand) {

        //lag en kopi av handen
        List<Card> remaingCards = new ArrayList<>(hand);


        //grupper etter type
        Map<String, List<Card>> type = new HashMap<>();
        for (Card card : remaingCards) {
            type.computeIfAbsent(card.getType(), k -> new ArrayList<>()).add(card);
        }

    }

    private int getCardScoringValue(Card card) {
        String val = card.getValue().toUpperCase();
        return switch (val) {
            case "KING", "QUEEN", "JACK", "10" -> 10;
            case "ACE" -> 25; // I dine regler: Ess er dyrt!
            default -> 5; // 2-10 er verdt tallet sitt
        };
    }

    public int calculateRoundScore(Player player) {

        List<Card> hand = player.getHand();

            int totalPenalty = 0;
            for (Card card : hand) {
                totalPenalty += getCardScoringValue(card);

            }
            return totalPenalty;

    }

    public boolean canLayOff(Card card, List<Card> existingMeld) {
        // Vi må vite om stabelen på bordet er en TRES eller en SERIE
        if (isSet(existingMeld)) {
            return canAttachToSet(card, existingMeld);
        } else {
            return canAttachToSeries(card, existingMeld);
        }
    }

    private boolean isSet(List<Card> meld) {
        if (meld.size() < 2) return false;
        // Hvis de to første kortene har samme verdi, er det en Tres (Set)
        return meld.get(0).getValue().equalsIgnoreCase(meld.get(1).getValue());
    }

    }
