package engine;

import static main.Main.log;

import java.io.Serializable;
import java.util.ArrayList;

import card.Card;

public class Player implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1071619364049172921L;
	private Integer id;
	private String name;
	private Boolean shielded;
	private ArrayList<Card> hand;
	private Boolean loser;
	private Integer wins;
	private Integer losses;
	
	public Player(Integer assignedID) {
		id = assignedID;
		name = "Player "+id;
		wins = 0;
		losses = 0;
		reset();
	}
	
	public void reset() {
		shielded = false;
		hand = new ArrayList<>();
		loser = false;
	}
	
	public Integer getID() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String newName) {
		name = newName;
	}
	
	public Boolean isShielded() {
		return shielded;
	}
	
	public void setShielded(Boolean shielded) {
		this.shielded = shielded;
		if(shielded) {
			log.info(name+" gains a shield");
		}
	}
	
	public ArrayList<Card> getHand() {
		return hand;
	}
	
	public void setHand(ArrayList<Card> newHand) {
		hand = newHand;
	}
	
	public Boolean isLoser() {
		return loser;
	}
	
	public void setLoser(Boolean loser) {
		this.loser = loser;
		if(loser) {
			log.info(name+" loses the round!");
		}
		addLoss();
	}
	
	public void addWin() {
		wins++;
	}
	
	public Integer getWins() {
		return wins;
	}
	
	public void addLoss() {
		losses++;
	}
	
	public Integer getLosses() {
		return losses;
	}
	
}
