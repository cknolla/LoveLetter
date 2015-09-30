package card;

import java.util.ArrayList;

import card.Card.Type;

public class Deck {
	private ArrayList<Card> deck = new ArrayList<>();
	private Integer cardID = 0;
	
	private int cardsUsed;
	
	public Deck() {
		cardsUsed = 0;
		addCard(Type.GUARD);
		addCard(Type.GUARD);
		addCard(Type.GUARD);
		addCard(Type.GUARD);
		addCard(Type.GUARD);
		addCard(Type.PRIEST);
		addCard(Type.PRIEST);
		addCard(Type.BARON);
		addCard(Type.BARON);
		addCard(Type.HANDMAID);
		addCard(Type.HANDMAID);
		addCard(Type.PRINCE);
		addCard(Type.PRINCE);
		addCard(Type.KING);
		addCard(Type.COUNTESS);
		addCard(Type.PRINCESS);
	}
	
	public void shuffle() {
        for ( int i = deck.size()-1; i > 0; i-- ) {
            int rand = (int)(Math.random()*(i+1));
            Card temp = deck.get(i);
            deck.set(i, deck.get(rand));
            deck.set(rand, temp);
        }
        cardsUsed = 0;
    }
	
	public int cardsLeft() {
        return deck.size() - cardsUsed;
    }
	
	private void addCard(Card.Type type) {
		Card card = new Card(type);
		card.setID(cardID);
		deck.add(card);
		cardID++;
	}
	
	public Card dealCard() {
        if (cardsUsed == deck.size())
            throw new IllegalStateException("No cards are left in the deck.");
        cardsUsed++;
        return deck.get(cardsUsed - 1);
        // Programming note:  Cards are not literally removed from the array
        // that represents the deck.  We just keep track of how many cards
        // have been used.
    }
}
