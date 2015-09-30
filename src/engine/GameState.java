package engine;

import java.io.Serializable;
import java.util.ArrayList;

import card.Card;

public class GameState implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8786085807896245618L;
	public final static int DEAL = 0;
	public final static int WAIT_FOR_DEAL = 1;
	public final static int WAIT_FOR_PLAY = 2;
	public final static int WAIT_FOR_OPPONENT = 3;
	public int status;
	public final Player player;
	public final ArrayList<Card> discardHand;
	
	public GameState(Player player, ArrayList<Card> discard, int status) {
		this.player = player;
		this.discardHand = discard;
		this.status = status;
	}
}
