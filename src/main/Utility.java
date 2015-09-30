package main;

public class Utility {
	// CARDS
	public static final String cardImages[] = {
			"resources/cardBack.jpg",
			"resources/GuardHQ.PNG",
			"resources/PriestHQ.PNG",
			"resources/BaronHQ.PNG",
			"resources/HandmaidHQ.PNG",
			"resources/PrinceHQ.PNG",
			"resources/KingHQ.PNG",
			"resources/CountessHQ.PNG",
			"resources/PrincessHQ.PNG",
			"resources/referenceCard.png"
	};
	public static final String background = "resources/woodBack.jpg";

	public static final Integer DEFAULT_PORT = 6969;
	public static final String DEFAULT_HOST = "localhost";
	public static final Integer WINDOW_WIDTH = 1024;
	public static final Integer WINDOW_HEIGHT = 768;
	public static final Double DEFAULT_CARD_WIDTH = 210.0;
	public static final Double DEFAULT_CARD_HEIGHT = 280.0;

	public static final String GAME_TITLE = "Love Letter";
	public static final String styleSheet = "resources/ClientWindow.css";
	
	// SERVER MESSAGES
	public static final String PLAYER_DISCONNECTED_MESSAGE = "Player disconnected";
	public static final String NAME_CHANGE_ACCEPTED = "Name changed";
	public static final String WAITING_FOR_OPPONENT_MESSAGE = "Waiting for opponent to connect.";
	public static final String READY_TO_START_MESSAGE = "Ready to start";
	public static final String CARDS_DEALT_MESSAGE = "Cards have been dealt";
	public static final String ILLEGAL_PLAY_MESSAGE = "That move is illegal";
	public static final String LEGAL_PLAY_MESSAGE = "Legal move";
	public static final String LEGAL_BUT_SHIELDED = "Legal but shielded";
	public static final String NEXT_TURN_MESSAGE = "Next turn";
	public static final String SHIELDED_MESSAGE = "You are shielded";
	public static final String END_TURN_CONFIRM = "Turn complete";
	public static final String CORRECT_GUESS_MESSAGE = "You guessed correctly";
	public static final String INCORRECT_GUESS_MESSAGE = "You guessed incorrectly";
	
	// CLIENT MESSAGES
	public static final String CLIENT_ALIVE_MESSAGE = "Alive";
	public static final String CLIENT_NAME_REQUEST_MESSAGE = "getNames";
	public static final String DEAL_MESSAGE = "Deal";
	public static final String END_TURN_REQUEST = "End turn";
	public static final String NEW_GAME_REQUEST = "New game";
	
	// GAME STATES
	public static final String YOUR_TURN_STATE = "It's your turn";
	public static final String OPP_TURN_STATE = "It's your opponent's turn";
}
