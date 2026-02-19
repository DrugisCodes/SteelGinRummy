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

    @Autowired private GameSetupService setupService;
    @Autowired private RuleEngine ruleEngine;
    @Autowired private TurnManager turnManager;

    private final List<Player> players = new ArrayList<>();
    private int currentPlayerIndex = 0;
    private final Deck deck = new Deck();
    ArrayList<Card> discardPile = new ArrayList<>();
    private int currentRound = 0;
    private final List<List<Card>> tableMelds = new ArrayList<>();

    public void startGame() {
        setupService.prepareNewRound(players, deck, discardPile, currentRound);

        System.out.println("Round " + currentRound);
        System.out.println("kort på bordet " + (discardPile.size()-1));


    }


    /**
     * @param player
     * @param currentRound
     */
    public void executeLayDown(Player player, int currentRound) {
        if (ruleEngine.canLayDown(player, currentRound)) {
            player.hasLaidDown = true;

            // Henter de spesifikke kortene som utgjør kravene
            List<List<Card>> meldsToPlace = ruleEngine.identifyRequiredMelds(player.getHand(), currentRound);

            // Fjerner disse kortene fra spillerens hånd
            for (List<Card> meld : meldsToPlace) {
                player.getHand().removeAll(meld);
                // TODO: Legg 'meld' til en liste over melds på bordet (table)
            }

            System.out.println(player.getName() + " la ned " + meldsToPlace.size() + " kombinasjoner!");
        }
    }



    public void performLayOff(UUID playerId, Card card, int meldIndex) {

        Player player = players.stream()
                .filter(p -> p.getName().equals(playerId.toString()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Player " + playerId.toString() + " not found!"));

        if(!player.hasLaidDown) {
            System.out.println("Handling avvist: Du må legge ned egne krav først");
            return;
        }

        if (meldIndex < 0 || meldIndex >= tableMelds.size()) {
            throw new IllegalArgumentException("Ugyldig indeks for kombinasjon på bordet.");
        }

        List<Card> targetMeld = tableMelds.get(meldIndex);

        if(ruleEngine.canLayOff(card, targetMeld)) {

            player.getHand().remove(card);
            targetMeld.add(card);

            System.out.println(player.getName() + " la ned " + card.getValue() + " på bordet");

            if (player.getHand().isEmpty()) {
                handleRoundEnd(player);

            } else {
                System.out.println("Kortet " + card.getValue() + " passer ikke på denne kombinasjonen");

            }

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
        this.tableMelds.clear();
        this.discardPile.clear();

        if (currentRound <= 6) {
            setupService.prepareNewRound(players, deck, discardPile, currentRound);
            players.forEach(p -> p.hasLaidDown = false);
            System.out.println("\n--- GJØR KLAR FOR RUNDE " + currentRound + " ---");
        } else {
            // Spillet er over - vi kaller den nye metoden vår
            gameEnd();
        }
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


    }



