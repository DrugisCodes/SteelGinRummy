package com.example.steelginrummy.service;

import com.example.steelginrummy.model.Card;
import com.example.steelginrummy.model.Deck;
import com.example.steelginrummy.model.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class GameService {

    private final List<Player> players = new ArrayList<>();
    private int currentPlayerIndex = 0;
    private Deck deck;
    Stack<Card> discardPile = new Stack<>();

}
