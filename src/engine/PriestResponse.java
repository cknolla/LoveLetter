package engine;

import java.io.Serializable;

import card.Card;

public class PriestResponse implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8054261388920535080L;
	public final String playerName;
	public final Card card;
	
	public PriestResponse(String playerName, Card card) {
		this.playerName = playerName;
		this.card = card;
	}
}
