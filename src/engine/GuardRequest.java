package engine;

import java.io.Serializable;

import card.Card;

public class GuardRequest implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5990149518043830547L;
	public final Card card;
	
	public GuardRequest(Card card) {
		this.card = card;
	}
}
