package engine;

import java.io.Serializable;

import card.Card;

public class BaronResponse implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7048369846493415648L;
	public final String winner;
	public final String loser;
	public final Card winningCard;
	public final Card losingCard;
	public final Boolean isTie;
	
	public BaronResponse(String winner, String loser, Card winningCard, Card losingCard, Boolean isTie) {
		this.winner = winner;
		this.loser = loser;
		this.winningCard = winningCard;
		this.losingCard = losingCard;
		this.isTie = isTie;
	}
}
