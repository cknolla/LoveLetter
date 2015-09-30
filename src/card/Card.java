package card;

import java.io.Serializable;

import main.Utility;

public class Card implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7903296299802453925L;

	public static enum Type {
		NONE, GUARD, PRIEST, BARON, HANDMAID, PRINCE, KING, COUNTESS, PRINCESS, REFERENCE
	}

	private Integer id; // <0 indicates it's not part of the deck
	private Type type;
	private Integer value;
	private String image;
	private String back = Utility.cardImages[0];

	public Card(Type typ) {
		id = -1;
		type = typ;
//		log.info(""+typ.ordinal());
		value = type.ordinal();
		image = Utility.cardImages[type.ordinal()];
	}
	
	public void setID(Integer deckID) {
		id = deckID;
	}
	
	public Integer getID() {
		return id;
	}

	public String getImage() {
		return image;
	}

	public String getBack() {
		return back;
	}
	
	public Type getType() {
		return type;
	}
	
	public Integer getValue() {
		return value;
	}
}
