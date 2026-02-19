package com.example.steelginrummy.service;

import com.example.steelginrummy.model.Card;
import com.example.steelginrummy.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuleEngineTest {

    private RuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        // Vi lager en ny instans før hver test for å ha blanke ark
        ruleEngine = new RuleEngine();
    }

    @Test
    void mapRankToValue() {
    }

    @Test
    void hasRequiredSeries() {
    }

    @Test
    void isCloseToSeries() {
    }

    @Test
    void canLayDown() {
    }

    @Test
    void identifyRequiredMelds() {
    }

    @Test
    void canAttachToSet() {
    }

    @Test
    void canAttachToSeries() {
    }

    //@Test
    //void calculateDeadwood() {
    //}

    @Test
    void calculateRoundScore() {
        Player player = new Player("Sigurd");

        player.addCard(new Card("5", "Diamond"));
        player.addCard(new Card("10", "Clover"));
        player.addCard(new Card("Ace", "Spade"));

        int expectedScore = 40;
        int actualScore = ruleEngine.calculateRoundScore(player);

        assertEquals(expectedScore, actualScore);
    }

    @Test
    void canLayOff() {
    }
}