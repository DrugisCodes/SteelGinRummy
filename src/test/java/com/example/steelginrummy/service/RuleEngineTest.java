package com.example.steelginrummy.service;

import com.example.steelginrummy.model.Card;
import com.example.steelginrummy.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleEngineTest {

    private RuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        ruleEngine = new RuleEngine();
    }

    // Hjelpemetode for å gjenkjenne Jokere i test-lambdas
    private boolean isJoker(Card card) {
        return ruleEngine.isJoker(card);
    }

    @Test
    void mapRankToValue() {
        Card ace = new Card("Spades", "ACE");
        // Sjekker at Ess tolkes riktig basert på om det er høy eller lav
        assertEquals(1, ruleEngine.mapRankToValue(ace, false));
        assertEquals(14, ruleEngine.mapRankToValue(ace, true));

        Card king = new Card("Hearts", "KING");
        assertEquals(13, ruleEngine.mapRankToValue(king, false));

        Card joker = new Card("WILD", "JOKER");
        assertEquals(0, ruleEngine.mapRankToValue(joker, false));
    }

    @Test
    void hasRequiredSeries() {
        // En gyldig serie i dine regler må ha 4 kort
        List<Card> validSeries = new ArrayList<>(Arrays.asList(
                new Card("Diamonds", "7"), new Card("Diamonds", "8"),
                new Card("Diamonds", "9"), new Card("Diamonds", "10")
        ));
        assertTrue(ruleEngine.hasRequiredSeries(validSeries, 1, 4));

        // En serie på kun 3 skal bli avvist når kravet er 4
        List<Card> invalidSeries = new ArrayList<>(Arrays.asList(
                new Card("Clubs", "2"), new Card("Clubs", "3"), new Card("Clubs", "4")
        ));
        assertFalse(ruleEngine.hasRequiredSeries(invalidSeries, 1, 4));
    }

    @Test
    void testHighSeriesWithAce() {
        // J-Q-K-A (Ess som 14)
        List<Card> highSeries = new ArrayList<>(Arrays.asList(
                new Card("Spades", "JACK"), new Card("Spades", "QUEEN"),
                new Card("Spades", "KING"), new Card("Spades", "ACE")
        ));
        assertTrue(ruleEngine.hasRequiredSeries(highSeries, 1, 4));
    }

    @Test
    void isCloseToSeriesWithJoker() {
        List<Card> hand = List.of(new Card("Diamonds", "7"));
        // Sjekker om 9-eren er nær 7-eren (avstand 2 er OK pga Joker)
        assertTrue(ruleEngine.isCloseToSeries(hand, new Card("Diamonds", "9")));
        assertTrue(ruleEngine.isCloseToSeries(hand, new Card("WILD", "JOKER")));
    }

    @Test
    void canLayDown() {
        Player player = new Player("Sigurd");

        // Runde 1: Krever 2 tres (6 kort totalt)
        player.addCard(new Card("Hearts", "5"));
        player.addCard(new Card("Spades", "5"));
        player.addCard(new Card("Clubs", "5"));

        player.addCard(new Card("Diamonds", "JACK"));
        player.addCard(new Card("Clubs", "JACK"));
        player.addCard(new Card("Hearts", "JACK"));

        assertTrue(ruleEngine.canLayDown(player, 1), "Skal kunne legge ned med to rene sett");
    }

    @Test
    void identifyRequiredMelds() {
        List<Card> hand = new ArrayList<>(Arrays.asList(
                new Card("Hearts", "JACK"), new Card("Spades", "JACK"), new Card("Clubs", "JACK"),
                new Card("Diamonds", "2"), new Card("Diamonds", "3"), new Card("Diamonds", "4"), new Card("Diamonds", "5")
        ));
        // Runde 2: 1 tres og 1 serie (4 kort)
        List<List<Card>> melds = ruleEngine.identifyRequiredMelds(hand, 2);

        assertEquals(2, melds.size());
        // Sjekker at vi har både et sett på 3 og en serie på 4 (rekkefølgen kan variere pga tryRound2)
        assertTrue(melds.stream().anyMatch(m -> m.size() == 3), "Mangler settet på 3");
        assertTrue(melds.stream().anyMatch(m -> m.size() == 4), "Mangler serien på 4");
    }

    @Test
    void testJokerScoring() {
        Player player = new Player("Sigurd");
        player.addCard(new Card("WILD", "JOKER")); // 50p
        player.addCard(new Card("Spades", "2"));   // 5p

        // Forventer 55 poeng totalt
        assertEquals(55, ruleEngine.calculateRoundScore(player));
    }

    @Test
    void hasRequiredSeriesWithJokers() {
        // 7, Joker (8), 9, 10
        List<Card> hand = new ArrayList<>(Arrays.asList(
                new Card("Hearts", "7"),
                new Card("WILD", "JOKER"),
                new Card("Hearts", "9"),
                new Card("Hearts", "10")
        ));
        assertTrue(ruleEngine.hasRequiredSeries(hand, 1, 4), "Joker skal fylle gapet i serien");
    }

    @Test
    void testJokerInSet() {
        // Runde 1 krever 2 tres. Her bruker vi Jokere i begge
        List<Card> hand = new ArrayList<>(Arrays.asList(
                new Card("Hearts", "5"), new Card("Spades", "5"), new Card("WILD", "JOKER"),
                new Card("Clubs", "2"), new Card("Diamonds", "2"), new Card("WILD", "JOKER")
        ));
        List<List<Card>> melds = ruleEngine.identifyRequiredMelds(hand, 1);
        assertEquals(2, melds.size(), "Joker skal fullføre begge settene");
    }

    @Test
    void canAttachJokerToTable() {
        List<Card> existingSet = Arrays.asList(new Card("Hearts", "4"), new Card("Spades", "4"), new Card("Clubs", "4"));
        // En Joker kan alltid legges til på eksisterende melds
        assertTrue(ruleEngine.canLayOff(new Card("WILD", "JOKER"), existingSet));
    }

    @Test
    void testIdentifyMeldsUsesJokersCorrectly() {
        // Scenario: 10-10-Joker og 2-3-Joker-5.
        // Motoren må fordele 1 Joker til hver i stedet for å stjele begge til serien.
        List<Card> hand = new ArrayList<>(Arrays.asList(
                new Card("Hearts", "10"), new Card("Diamonds", "10"), new Card("WILD", "JOKER"),
                new Card("Clubs", "2"), new Card("Clubs", "3"), new Card("WILD", "JOKER"), new Card("Clubs", "5")
        ));
        List<List<Card>> melds = ruleEngine.identifyRequiredMelds(hand, 2);

        assertEquals(2, melds.size(), "Skal finne både settet og serien ved hjelp av smart joker-fordeling");
        assertTrue(melds.get(0).stream().anyMatch(this::isJoker), "Første kombinasjon skal ha en joker");
        assertTrue(melds.get(1).stream().anyMatch(this::isJoker), "Andre kombinasjon skal ha en joker");
    }

    @Test
    void testPerfectHandRunde6() {
        Player player = new Player("Vinner");
        // Runde 6: 1 tres (3 kort) + 2 serier (8 kort) = 11 kort totalt
        player.addCard(new Card("Clubs", "2")); player.addCard(new Card("Diamonds", "2")); player.addCard(new Card("WILD", "JOKER")); // Tres

        player.addCard(new Card("Hearts", "4")); player.addCard(new Card("Hearts", "5"));
        player.addCard(new Card("Hearts", "6")); player.addCard(new Card("Hearts", "7")); // Serie 1

        player.addCard(new Card("Spades", "JACK")); player.addCard(new Card("Spades", "QUEEN"));
        player.addCard(new Card("Spades", "KING")); player.addCard(new Card("Spades", "ACE")); // Serie 2

        assertTrue(ruleEngine.canLayDown(player, 6), "Runde 6 skal godkjennes når alle 11 kortene brukes");
    }
}