package engine;

import java.io.Serializable;

import card.Card;

public class WinnerResponse implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1322357399103514773L;
	public final String name;
	public final Card card;
	
	public WinnerResponse(String name, Card card) {
		this.name = name;
		this.card = card;
	}
}
