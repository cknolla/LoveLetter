package engine;

import java.io.Serializable;

import card.Card;

public class PlayRequest implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8099172545700461388L;
	private Integer sourcePlayer;
	private Integer targetPlayer;
	private Card card;
	
	public PlayRequest(Integer source, Integer target, Card card) {
		this.sourcePlayer = source;
		this.targetPlayer = target;
		this.card = card;
	}
	
	public Integer getSource() {
		return sourcePlayer;
	}
	
	public Integer getTarget() {
		return targetPlayer;
	}
	
	public Card getCard() {
		return card;
	}
}
