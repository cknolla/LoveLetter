package engine;

import java.io.Serializable;

import card.Card;

public class GuardResponse implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1158360262171748278L;
	public final Integer sourceID;
	public final Integer targetID;
	public final Card card;
	public final Boolean correct;
	
	public GuardResponse(Integer sourceID, Integer targetID, Card card, Boolean correct) {
		this.sourceID = sourceID;
		this.targetID = targetID;
		this.card = card;
		this.correct = correct;
	}
}
