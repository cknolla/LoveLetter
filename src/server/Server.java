package server;
// Written by Casey Knolla
import static main.Main.log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

import card.Card;
import card.Card.Type;
import card.Deck;
import engine.BaronResponse;
import engine.ClientVersion;
import engine.GameState;
import engine.GuardRequest;
import engine.GuardResponse;
import engine.NameChangeRequest;
import engine.NameChangeResponse;
import engine.PlayRequest;
import engine.Player;
import engine.PriestResponse;
import engine.PrinceRequest;
import engine.WinnerResponse;
import main.Utility;
import netgame.common.Hub;

public class Server extends Hub {
	private final String version = "1.2.1";

	private Deck deck = new Deck();
	
	private final static int WAITING_FOR_DEAL = 0;
	private final static int WAITING_FOR_PLAY = 1;
	
	private int status; // one of the above options
	private int listeningPort;
	private Boolean persistent;
	
	private int currentPlayer;
	private int dealer = -1;
	private PlayRequest currentPlay = null;
	
	private TreeMap<Integer, Player> players = new TreeMap<>();
//	private TreeMap<Integer, ArrayList<Card>> hands = new TreeMap<>();
//	private TreeMap<Integer, Boolean> shielded = new TreeMap<>();
	private ArrayList<Card> discardHand = new ArrayList<>();
	
	public Server(int port, Boolean persistent) throws IOException {
		super(port);
		listeningPort = port;
		this.persistent = persistent; 
		setAutoreset(true);
		log.info("Server v"+version+" started");
	}
	
	protected void playerDisconnected(Integer playerID) {
		players.remove(playerID);
		if(players.size() == 0 && !persistent) {
			shutDownHub();
		}
		try {
			restartServer(listeningPort);
		} catch(IOException ex) {
			System.err.println("Failed to restart listening socket on port "+listeningPort);
		}
		newGame();
		sendToAll(Utility.PLAYER_DISCONNECTED_MESSAGE);
       
    }
	
	protected void playerConnected(Integer playerID) {
		players.put(playerID, new Player(playerID));
		TreeMap<Integer, String> names = new TreeMap<>();
		for(Integer ID: players.keySet()) {
			names.put(ID, players.get(ID).getName());
		}
		sendToAll(names);
        if (players.size() == 2) {
            shutdownServerSocket();
            log.info("Shutting down server socket");
            newGame();
 /*           dealer = players.firstKey();
            currentPlayer = dealer;
            money[0] = 1000;
            money[1] = 1000;
         sendToOne(1, new GameState(null,discardHand,GameState.DEAL));
            sendToOne(2, new GameState(null,discardHand,GameState.WAIT_FOR_DEAL));
            
            sendToAll(Utility.READY_TO_START_MESSAGE);
    */    }
    }
	
	protected void messageReceived(Integer playerID, Object message) {
		if(message instanceof ClientVersion) {
			ClientVersion request = (ClientVersion)message;
			log.info("Client "+playerID+", "+players.get(playerID).getName()+", is running Client version "+request.version);
			return;
		} else if(message instanceof NameChangeRequest) {
			changeName((NameChangeRequest)message, playerID);
			return;
		} 
		if (playerID != currentPlayer) {
			if(!message.equals(Utility.END_TURN_REQUEST)) {
		         log.severe("Error: message ("+message.toString()+") received from the wrong player.");
			}
			return;
		}
		if(message.equals(Utility.CLIENT_ALIVE_MESSAGE)) {
			log.info("Player "+playerID+" is alive");
		} else if(message.equals(Utility.CLIENT_NAME_REQUEST_MESSAGE)) {
			TreeMap<Integer, String> names = new TreeMap<>();
			for(Integer ID: players.keySet()) {
				names.put(ID, players.get(ID).getName());
			}
			sendToOne(playerID, names);
			//TODO: handle names on client
		} else if(message instanceof PlayRequest) {
			PlayRequest request = (PlayRequest) message;
			Boolean legal = validatePlay(request);
			if(legal) {
				if(players.get(currentPlay.getTarget()).isShielded() && 
						request.getCard().getType() != Type.HANDMAID &&
						request.getCard().getType() != Type.PRINCE &&
						request.getCard().getType() != Type.PRINCESS &&
						request.getCard().getType() != Type.COUNTESS) {
					log.info("Target is shielded. Cannot execute");
					sendToAll(players.get(playerID).getName()+"'s action is thwarted by "+players.get(currentPlay.getTarget()).getName()+"'s shield");
					sendToOne(playerID, Utility.LEGAL_BUT_SHIELDED);
					endTurn();
				} else {
					log.info("Target is unshielded or a defensive card was played. Executing card logic.");
					sendToOne(playerID, Utility.LEGAL_PLAY_MESSAGE);
				}
			} else {
				sendToOne(playerID, Utility.ILLEGAL_PLAY_MESSAGE);
			}
		} else if(message instanceof Card) {
			if(currentPlay == null) {
				log.severe("A card was sent as valid, but the play hasn't been validated!");
				return;
			}
			Card card = (Card)message;
			if(card.getType() == Type.PRIEST) {
				Card opponentCard = players.get(currentPlay.getTarget()).getHand().get(0);
				sendToOne(playerID,new PriestResponse(players.get(currentPlay.getTarget()).getName(),opponentCard));
				sendToAll(players.get(playerID).getName()+" plays PRIEST and views "+players.get(currentPlay.getTarget()).getName()+"'s hand");
				endTurn();
			} else if(card.getType() == Type.BARON) {
				BaronResponse response = compareHands();
				sendToAll(players.get(playerID).getName()+" plays BARON");
				sendToAll(response);
				endTurn();
			} else if(card.getType() == Type.HANDMAID) {
				sendToAll(players.get(playerID).getName()+" plays HANDMAID and gains a shield");
				players.get(playerID).setShielded(true); // gets removed in endTurn
				endTurn();
			} else if(card.getType() == Type.KING) {
				swapHands();
				endTurn();
			}  else if(card.getType() == Type.COUNTESS) {
				sendToAll(players.get(playerID).getName()+" discards COUNTESS");
				endTurn();
			} else if(card.getType() == Type.PRINCESS) {
				sendToAll(players.get(playerID).getName()+" discards PRINCESS and loses");
				players.get(playerID).setLoser(true);
				endTurn();
			}
		} else if (message.equals(Utility.DEAL_MESSAGE)) {
			if (status != WAITING_FOR_DEAL) {
				log.severe("Error: DEAL message received at incorrect time");
				return;
			}
			deal();
			sendToAll(Utility.CARDS_DEALT_MESSAGE);
		} else if(message.equals(Utility.END_TURN_REQUEST)) {
			if(status == WAITING_FOR_DEAL) {
				return;
			}
			endTurn();
		} else if(message.equals(Utility.NEW_GAME_REQUEST)) {
			log.info(players.get(playerID).getName()+" forfeits");
			players.get(playerID).setLoser(true);
			newGame();
		} else if(message instanceof GuardRequest) {
			GuardResponse response = guessHand((GuardRequest)message);
			sendToAll(response);
			endTurn();
		} else if(message instanceof PrinceRequest) {
			PrinceRequest request = (PrinceRequest) message;
			if(players.get(request.playerID).isShielded()) {
				sendToOne(playerID, Utility.ILLEGAL_PLAY_MESSAGE);
				return;
			}
			String targetName = "himself";
			if(!playerID.equals(request.playerID)) {
				targetName = players.get(currentPlay.getTarget()).getName();
			}
			sendToAll(players.get(playerID).getName()+" plays PRINCE and forces "+targetName+" to discard his hand");
			forceDiscard((PrinceRequest)message);
			endTurn();
		} 
	}

	private void changeName(NameChangeRequest request, Integer playerID) {
		players.get(playerID).setName(request.name);
		sendToAll(new NameChangeResponse(playerID, request.name));
	}

	protected void deal() {
		deck.shuffle();
		discardHand.clear();
		Card drawnCard = null;
		if(players.size() == 2) {
			drawnCard = deck.dealCard();
			log.info(drawnCard.getType()+" drawn and immediately discarded");
			discardHand.add(drawnCard);
			drawnCard = deck.dealCard();
			log.info(drawnCard.getType()+" drawn and immediately discarded");
			discardHand.add(drawnCard);
			drawnCard = deck.dealCard();
			log.info(drawnCard.getType()+" drawn and immediately discarded");
			discardHand.add(drawnCard);
		}
		for(Integer playerID: players.keySet()) {
			ArrayList<Card> hand = new ArrayList<>();
			drawnCard = deck.dealCard();
			log.info(players.get(playerID).getName()+" draws a "+drawnCard.getType());
			hand.add(drawnCard);
//			hand.add(new Card(Type.NONE));
			players.get(playerID).setHand(hand);
			if(playerID == dealer) {
				if(players.higherKey(playerID) != null) {
					currentPlayer = players.higherKey(playerID);
				} else {
					currentPlayer = players.firstKey();
				}
			}
		}
		ArrayList<Card> hand = players.get(currentPlayer).getHand();
		drawnCard = deck.dealCard();
		log.info(players.get(currentPlayer).getName()+" draws a "+drawnCard.getType());
		hand.add(drawnCard);
		players.get(currentPlayer).setHand(hand);
		
		status = WAITING_FOR_PLAY;
		sendState(GameState.WAIT_FOR_PLAY, GameState.WAIT_FOR_OPPONENT);

	}
	
	@SuppressWarnings("unchecked")
	private void sendState(int currentPlayerState, int opponentPlayerState) {
 //       TreeMap<Integer,Integer> playerStates = new TreeMap<Integer,Integer>();
        for(Integer playerID: getPlayerList()) {
        	if(playerID == currentPlayer) {
        	//	playerStates.put(player, currentPlayerState);
        		// Don't send anything but a clone! It will mysteriously vanish once it hits the ClientWindow
				sendToOne(playerID, new GameState((Player)players.get(playerID), (ArrayList<Card>)discardHand.clone(), currentPlayerState));
        	} else {
        	//	playerStates.put(player, opponentState);
				sendToOne(playerID, new GameState((Player)players.get(playerID), (ArrayList<Card>)discardHand.clone(), opponentPlayerState));
        	}
        }
//        for(int i = 0; i < playerCount; i++) {
//        	sendToOne(i+1, new GameState(hands.get(i),playerStates.get(key)));
//        }
	}
	
	private Boolean validatePlay(PlayRequest play) {
		ArrayList<Card> sourceHand = players.get(play.getSource()).getHand(); // has 2 cards
		
		int countessIndex = -1;
		for(int i = 0; i < sourceHand.size(); i++) {
			if(sourceHand.get(i).getType() == Type.COUNTESS) {
				countessIndex = i;
			}
		}
		if(countessIndex != -1) {
			if(play.getCard().getType() == Type.PRINCE || play.getCard().getType() == Type.KING) {
				currentPlay = null;
				return false;
			}
		}
		currentPlay = play;
		log.info(players.get(currentPlay.getSource()).getName()+" plays "+currentPlay.getCard().getType()+" against "+players.get(currentPlay.getTarget()).getName());
		return true;
	}
	
	private BaronResponse compareHands() {
		ArrayList<Card> sourceHand = players.get(currentPlay.getSource()).getHand();
		Card sourceCard = null;
		Card targetCard = players.get(currentPlay.getTarget()).getHand().get(0);
		
		for(int i = 0; i < sourceHand.size(); i++) {
			if(!currentPlay.getCard().getID().equals(sourceHand.get(i).getID())) {
				sourceCard = sourceHand.get(i);
				break;
			}
		}
		Integer winner;
		Integer loser;
		Card winnerCard;
		Card loserCard;
		Boolean isTie = false;
		if(sourceCard.getValue() > targetCard.getValue()) {
			winner = currentPlay.getSource();
			loser = currentPlay.getTarget();
			winnerCard = sourceCard;
			loserCard = targetCard;
		} else if(targetCard.getValue() > sourceCard.getValue()) {
			winner = currentPlay.getTarget();
			loser = currentPlay.getSource();
			winnerCard = targetCard;
			loserCard = sourceCard;
		} else {
			winner = currentPlay.getSource();
			loser = currentPlay.getTarget();
			winnerCard = sourceCard;
			loserCard = targetCard;
			isTie = true;
		}
		log.info(players.get(winner).getName()+" challenges "+players.get(loser).getName()+" in BARON battle. "+winnerCard.getType()+" VS "+loserCard.getType());
		if(!isTie) {
			players.get(loser).setLoser(true);
		}
		return new BaronResponse(
				players.get(winner).getName(),
				players.get(loser).getName(),
				winnerCard,
				loserCard,
				isTie);
		
	}
	
	private void swapHands() {
		ArrayList<Card> sourceHand = players.get(currentPlay.getSource()).getHand();
		Card sourceCard = null;
		Card targetCard = players.get(currentPlay.getTarget()).getHand().get(0);
		int i;
		for(i = 0; i < sourceHand.size(); i++) {
			if(!currentPlay.getCard().getID().equals(sourceHand.get(i).getID())) {
				sourceCard = sourceHand.get(i);
				break;
			}
		}
		try {
			players.get(currentPlay.getSource()).getHand().set(i, targetCard);
		} catch(IndexOutOfBoundsException ex) {
			log.warning("End of deck. No card to swap");
		}
		try {
			players.get(currentPlay.getTarget()).getHand().set(0, sourceCard);
		} catch(IndexOutOfBoundsException ex) {
			log.warning("End of deck. No card to swap");
		}
		sendToAll(players.get(currentPlay.getSource()).getName()+" plays KING and swaps "+sourceCard.getType()+" for "+players.get(currentPlay.getTarget()).getName()+"'s "+targetCard.getType());
		log.info(players.get(currentPlay.getSource()).getName()+" plays KING and swaps "+sourceCard.getType()+" for "+players.get(currentPlay.getTarget()).getName()+"'s "+targetCard.getType());
	}
	
	private void forceDiscard(PrinceRequest request) {
		if(players.get(request.playerID).isShielded()) {
			return;
		}
		Card otherCard = null;
		ArrayList<Card> targetHand = players.get(request.playerID).getHand();
		int i;
		for(i = 0; i < targetHand.size(); i++) {
			if(!currentPlay.getCard().getID().equals(targetHand.get(i).getID())) {
				otherCard = targetHand.get(i);
				targetHand.remove(i);
				discardHand.add(otherCard);
				break;
			}
		}
		try {
			Card drawnCard = deck.dealCard();
			log.info(players.get(request.playerID).getName()+" draws a "+drawnCard.getType());
			targetHand.add(drawnCard);
		} catch(IllegalStateException ex) {
			log.warning("NOT drawing a card because the deck is empty");
		}
		
		log.info(players.get(request.playerID).getName()+" must forcefully discard "+otherCard.getType()+" due to a PRINCE being played");
		if(otherCard.getType() == Type.PRINCESS) {
			players.get(request.playerID).setLoser(true);
		}
	}
	
	private GuardResponse guessHand(GuardRequest request) {
		Boolean correct;
		sendToAll(players.get(currentPlay.getSource()).getName()+" plays GUARD against "+players.get(currentPlay.getTarget()).getName()+" and guesses "+request.card.getType());
		log.info(players.get(currentPlay.getSource()).getName()+" guesses "+request.card.getType());
		if(players.get(currentPlay.getTarget()).getHand().get(0).getType() == request.card.getType()) {
			log.info(players.get(currentPlay.getSource()).getName()+" guesses "+players.get(currentPlay.getTarget()).getName()+"'s hand correctly");
			players.get(currentPlay.getTarget()).setLoser(true);
			correct = true;
		//	sendToOne(currentPlay.getSource(),Utility.CORRECT_GUESS_MESSAGE);
		} else {
			log.info(players.get(currentPlay.getSource()).getName()+" guesses "+players.get(currentPlay.getTarget()).getName()+"'s hand incorrectly");
			correct = false;
		//	sendToOne(currentPlay.getSource(),Utility.INCORRECT_GUESS_MESSAGE);
		}
		return new GuardResponse(currentPlay.getSource(), currentPlay.getTarget(), request.card, correct);
	}
	
	private void endTurn() {
		
		Integer playersInGame = 0;
		Integer winner = 0;
		for(Integer playerID: players.keySet()) {
			if(!players.get(playerID).isLoser()) {
				playersInGame++;
				winner = playerID;
			}
		}
		discardHand.add(currentPlay.getCard());
		if(playersInGame < 2) {
			logWin(winner);
			newGame();
			return;
		}
		
		ArrayList<Card> targetHand = players.get(currentPlay.getTarget()).getHand(); // has 1 card
		ArrayList<Card> sourceHand = players.get(currentPlay.getSource()).getHand(); // has 2 cards
	
		for(int i = 0; i < sourceHand.size(); i++) {
			log.finest("play card ID: "+currentPlay.getCard().getID()+", source ID: "+sourceHand.get(i).getID());
			if(currentPlay.getCard().getID().equals(sourceHand.get(i).getID())) {
				sourceHand.remove(i);
				break;
			}
		}
		
		players.get(currentPlay.getSource()).setHand(sourceHand);
		players.get(currentPlay.getTarget()).setHand(targetHand);
		if(players.higherKey(currentPlayer) != null) {
			currentPlayer = players.higherKey(currentPlayer);
		} else {
			currentPlayer = players.firstKey();
		}
		
		ArrayList<Card> hand = players.get(currentPlayer).getHand();
		try {
			Card drawnCard = deck.dealCard();
			log.info(players.get(currentPlayer).getName()+" draws a "+drawnCard.getType());
			hand.add(drawnCard);
		} catch(IllegalStateException ex) {
			log.warning("NOT drawing a card because the deck is empty");
		}
		players.get(currentPlayer).setHand(hand);
		players.get(currentPlayer).setShielded(false);
		log.finer("new currentPlayer ("+currentPlayer+") hand size: "+players.get(currentPlayer).getHand().size());
		status = WAITING_FOR_PLAY;
		currentPlay = null;
		sendState(GameState.WAIT_FOR_PLAY, GameState.WAIT_FOR_OPPONENT);
		sendToAll(Utility.END_TURN_CONFIRM);
	}
	
	private void logWin(Integer winner) {
		players.get(winner).addWin();
		sendToAll(new WinnerResponse(players.get(winner).getName(),currentPlay.getCard()));
		log.info(players.get(winner).getName()+" wins the round!");
	}
	
	private void newGame() {
		if(players.higherKey(dealer) != null) {
			dealer = players.higherKey(dealer);
		} else {
			dealer = players.firstKey();
		}
		currentPlayer = dealer;
		for(Integer playerID: players.keySet()) {
			players.get(playerID).reset();
		}
		status = WAITING_FOR_DEAL;
		sendState(GameState.DEAL,GameState.WAIT_FOR_DEAL);
		/*
		for(Integer playerID: players.keySet()) {
			if(playerID.equals(dealer)) {
				//sendToOne(playerID, new GameState(null,discardHand,GameState.DEAL));
				sendState(GameState.DEAL);
			} else {
				//sendToOne(playerID, new GameState(null,discardHand,GameState.WAIT_FOR_DEAL));
			}
		}
		*/
        sendToAll(Utility.READY_TO_START_MESSAGE);
	}
	
	
}
