package com.example.steelginrummy.service;

import com.example.steelginrummy.model.Card;
import com.example.steelginrummy.model.Deck;
import com.example.steelginrummy.model.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class GameService {

    private enum TurnPhase { DRAWING, PLAYING, DISCARDING }
    private TurnPhase currentPhase = TurnPhase.DRAWING;

    @Autowired private GameSetupService setupService;
    @Autowired private RuleEngine ruleEngine;

    private final List<Player> players = new ArrayList<>();
    private int currentPlayerIndex = 0;
    private final Deck deck = new Deck();
    private final List<Card> discardPile = new ArrayList<>();
    private int currentRound = 1; // Starter på runde 1
    private final List<List<Card>> tableMelds = new ArrayList<>();


    public void startGame() {
        this.currentRound = 1; // Sikre at vi starter på 1
        int cardsToDeal = 5 + currentRound;
        setupService.prepareNewRound(players, deck, discardPile, cardsToDeal);

        System.out.println("Round " + currentRound + " started with " + cardsToDeal + " cards.");


    }

    public void drawFromDeck(UUID playerId) {
        validateTurn(playerId, TurnPhase.DRAWING);

        if (deck.getCards().isEmpty()) {
            deck.refillFromDiscard(discardPile);
        }

        Card drawn = deck.drawCard();
        getCurrentPlayer().addCard(drawn);
        currentPhase = TurnPhase.PLAYING; // Nå kan spilleren legge ned kort
    }

    public void drawFromDiscard(UUID playerId) {
        validateTurn(playerId, TurnPhase.DRAWING);
        if (discardPile.isEmpty()) throw new IllegalStateException("Kastebunken er tom!");

        Card drawn = discardPile.remove(discardPile.size() - 1);
        getCurrentPlayer().addCard(drawn);
        currentPhase = TurnPhase.PLAYING;
    }


    public void executeLayDown(UUID playerId) {
        validateTurn(playerId, TurnPhase.PLAYING);
        Player player = getCurrentPlayer();

        // I siste runde (6) må ALT på hånden brukes for å få lov til å legge ned
        if (currentRound == 6) {
            if (!ruleEngine.canLayDown(player, 6)) {
                System.out.println("Siste runde: Du må kunne gå ut med alle kortene dine!");
                return;
            }
        }

        if (ruleEngine.canLayDown(player, currentRound)) {
            List<List<Card>> melds = ruleEngine.identifyRequiredMelds(player.getHand(), currentRound);
            for (List<Card> meld : melds) {
                player.getHand().removeAll(meld);
                tableMelds.add(meld);
            }
            player.hasLaidDown = true;
            checkWin(player);
        }
    }



    public void performLayOff(UUID playerId, Card card, int meldIndex) {
        validateTurn(playerId, TurnPhase.PLAYING);
        Player player = getCurrentPlayer();

        if (!player.hasLaidDown) {
            throw new IllegalStateException("Du må legge ned dine egne krav før du kan bygge på andre!");
        }

        List<Card> targetMeld = tableMelds.get(meldIndex);
        if (ruleEngine.canLayOff(card, targetMeld)) {
            player.getHand().remove(card);
            targetMeld.add(card);
            checkWin(player);
        } else {
            System.out.println("Kortet passer ikke!");
        }
    }



    private void handleRoundEnd(Player winner) {
        System.out.println(winner.getName() + " har vunnet runden!");

        for (Player p : players) {
            int roundScore = p.equals(winner) ? 0 : ruleEngine.calculateRoundScore(p);

            p.addScore(roundScore);
            System.out.println(p.getName() + " Fikk " + roundScore + " antall straffpoeng denne runden");
        }

        prepareNextRound();
    }

    private void prepareNextRound() {
        this.currentRound++;
        if (currentRound > 6) {
            gameEnd();
            return;
        }

        this.tableMelds.clear();
        this.discardPile.clear();

        // Logikk: Runde 1 = 6 kort, Runde 2 = 7 kort ... Runde 6 = 11 kort
        int cardsToDeal = 5 + currentRound;

        setupService.prepareNewRound(players, deck, discardPile, cardsToDeal);
        players.forEach(p -> p.hasLaidDown = false);

        this.currentPlayerIndex = 0;
        this.currentPhase = TurnPhase.DRAWING;
        System.out.println("\n--- RUNDE " + currentRound + " (" + cardsToDeal + " kort) ---");
    }

    private void gameEnd() {
        System.out.println("\n========================");
        System.out.println("   SPILLET ER FERDIG!   ");
        System.out.println("========================");

        // 1. Skriv ut poengsummen for alle spillere
        for (Player p : players) {
            System.out.println(p.getName() + ": " + p.getTotalGameScore());
        }

        // 2. Finn vinneren (den med færrest poeng)
        Player finalWinner = players.stream()
                .min(Comparator.comparingInt(Player::getTotalGameScore))
                .orElse(null);

        // 3. Annonser vinneren med stil
        if (finalWinner != null) {
            System.out.println("------------------------");
            System.out.println(finalWinner.getName().toLowerCase() + " vant spillet!");
        }
        System.out.println("========================\n");
    }

    public void discardCard(UUID playerId, Card card) {
        validateTurn(playerId, TurnPhase.PLAYING);
        Player player = getCurrentPlayer();

        player.getHand().remove(card);
        discardPile.add(card);

        if (player.getHand().isEmpty()) {
            handleRoundEnd(player);
        } else {
            // Gå til neste spiller
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            currentPhase = TurnPhase.DRAWING;
            System.out.println("Turen går til neste spiller.");
        }
    }



    //=====hjelpe metoder ======
    private void checkWin(Player player) {
        if (player.getHand().isEmpty()) {
            handleRoundEnd(player);
        }
    }
    private Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    private void validateTurn(UUID playerId, TurnPhase requiredPhase) {
        Player player = getCurrentPlayer();
        if (!player.getName().equals(playerId.toString())) {
            throw new IllegalStateException("Det er ikke din tur!");
        }
        if (currentPhase != requiredPhase) {
            throw new IllegalStateException("Ugyldig handling i denne fasen. Forventet: " + requiredPhase);
        }
    }


    }



