package com.example.steelginrummy.service;

import com.example.steelginrummy.model.Card;
import com.example.steelginrummy.model.Deck;
import com.example.steelginrummy.model.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class GameService {

    @Autowired private GameSetupService setupService;
    @Autowired private RuleEngine ruleEngine;
    @Autowired private TurnManager turnManager;

    private final List<Player> players = new ArrayList<>();
    private int currentPlayerIndex = 0;
    private final Deck deck = new Deck();
    ArrayList<Card> discardPile = new ArrayList<>();
    private int currentRound = 0;


    public void startGame() {
        setupService.prepareNewRound(players, deck, discardPile, currentRound);

        System.out.println("Round " + currentRound);
        System.out.println("kort på bordet " + (discardPile.size()-1));


    }



    public void executeLayDown(Player player, int currentRound, Card discardCard) {

    }



    public void performLayOff(UUID playerId, Card card, int meldIndex) {

    }

}

