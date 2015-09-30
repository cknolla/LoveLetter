package client;
// written by Casey Knolla
import static main.Main.log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

import card.Card;
import card.Card.Type;
import engine.BaronResponse;
import engine.ClientVersion;
import engine.GameState;
import engine.GuardRequest;
import engine.GuardResponse;
import engine.NameChangeRequest;
import engine.NameChangeResponse;
import engine.PlayRequest;
import engine.PriestResponse;
import engine.PrinceRequest;
import engine.WinnerResponse;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import main.Utility;
import netgame.common.Client;

public class ClientWindow {
	private final String version = "1.2.1";
	private static Stage clientStage = null;	
	
	private LoveLetterClient connection;
	private GameState state;
	private PlayRequest currentPlay = null;
	private Boolean targetShielded = false;
	private TreeMap<Integer, String> playerNames = new TreeMap<>();
	private ArrayList<Label> playerNameLabels = new ArrayList<>();

	private Display display;
	// LABELS
	private Label message = new Label();
	private Label messageFromServer = new Label();
	private Label playerName = new Label();
	private Label shielded = new Label();
	private Label previousWinner = new Label();
	private Label winningCard = new Label();
	private Label wins = new Label();
	private Label losses = new Label();

	// CONTROLS
	private Button dealButton = new Button("Deal");
	private Button playButton = new Button("Play Card");
	private Button newGameButton = new Button("New Game");
	private Button quitButton = new Button("Fuck this");
	
	// CARDBOXES
//	private CardBox opponentCardBox = new CardBox();
	private ArrayList<CardBox> playerCardBoxes = new ArrayList<CardBox>() {
		private static final long serialVersionUID = -3954772290830954653L;

	{
		add(new CardBox());
		add(new CardBox());
	}};
	private CardBox selectedCardBox = null;
	private CardPile discardPile = new CardPile();
	private CardBox referenceCardBox = new CardBox();

	public ClientWindow(String host, int port, String name) {
		log.info("Client v"+version+" started");
		clientStage = new Stage();
		clientStage.setResizable(false);
		display = new Display();
		configureButtons();
		
		
		Scene scene = new Scene(display);
		scene.getStylesheets().add(Utility.styleSheet);
		clientStage.setTitle(Utility.GAME_TITLE+" v"+version);
		clientStage.setScene(scene);
		clientStage.show();
		
		clientStage.setOnCloseRequest(event -> {
			doQuit();
		});
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		new Thread() {
			public void run() {
				try {
					final LoveLetterClient c = new LoveLetterClient(host,port);
					Platform.runLater(() -> {
						connection = c;
						if(c.getID() == 1) {
							messageFromServer.setText(Utility.WAITING_FOR_OPPONENT_MESSAGE);
						}
						connection.send(new NameChangeRequest(name));
						connection.send(new ClientVersion(version));
					});
				} catch(final IOException ex) {
					Platform.runLater(() -> {
						new Alert(AlertType.ERROR, "Could not connect to "+host+" at port "+port).showAndWait();
						log.severe("Could not connect to "+host);
						System.exit(1);
					});
				}
			}
		}.start();
	}
	
	private void newState(GameState state) {
        
        this.state = state;
        if(state.player != null) {
        	log.finest("My ("+connection.getID()+") name: "+state.player.getName());
        	if(state.player.isShielded()) {
        		shielded.setText("Shielded: Yes");
        	} else {
        		shielded.setText("Shielded: No");
        	}
        	playerName.setText(state.player.getName());
        	wins.setText("Wins: "+state.player.getWins());
        	losses.setText("Losses: "+state.player.getLosses());
        }
        
		dealButton.setDisable(!(state.status == GameState.DEAL));
        
        switch(state.status) {
        case GameState.DEAL:
        	message.setText("Click Deal to start");
        	display.refreshHand();
        	newGameButton.setDisable(true);
        	playButton.setDisable(true);
        	break;
        case GameState.WAIT_FOR_DEAL:
        	message.setText("Waiting for opponent to deal");
        	display.refreshHand();
        	newGameButton.setDisable(true);
        	playButton.setDisable(true);
        	break;
        case GameState.WAIT_FOR_PLAY:
        	message.setText(Utility.YOUR_TURN_STATE);
        	playButton.setDisable(false);
        	newGameButton.setDisable(false);
        //	log.finest("My ("+connection.getID()+") turn. Hand size:"+state.player.getHand().size());
        	break;
        case GameState.WAIT_FOR_OPPONENT:
        	message.setText(Utility.OPP_TURN_STATE);
        	playButton.setDisable(true);
        	newGameButton.setDisable(true);
        //	log.finest("His turn. Hand size:"+state.player.getHand().size());
        	break;
        }
	}

	private class LoveLetterClient extends Client {
		public LoveLetterClient(String hubHostName, int hubPort) throws IOException {
			super(hubHostName, hubPort);
			clientStage.setX(getID() * 3);
		}

		@SuppressWarnings("unchecked")
		protected void messageReceived(final Object message) {
			Platform.runLater(() -> {
				boolean suppress = false;
				if(message instanceof GameState) {
					newState((GameState)message);
					discardPile.setCards(state.discardHand);
				} else if(message instanceof TreeMap<?,?>) {
					playerNames = (TreeMap<Integer,String>)message;
				} else if(message instanceof String) {
//					Pattern pCount = Pattern.compile("playerCount,(\\d+)");
//					Matcher m = pCount.matcher((String)message);
//					if(m.matches()) {
//						playerCount = Integer.parseInt(m.group(1));
//						suppress = true;
					//	log.info("playerCount set to "+playerCount);
					if (message.equals(Utility.CARDS_DEALT_MESSAGE)) {
						display.refreshHand();
					} else if(message.equals(Utility.READY_TO_START_MESSAGE)) {
						suppress = true;
					} else if (message.equals(Utility.LEGAL_PLAY_MESSAGE)) {
						targetShielded = false;
						suppress = true;
						playCard();
					} else if(message.equals(Utility.LEGAL_BUT_SHIELDED)) {
						targetShielded = true;
						suppress = true;
						playCard();
					} else if(message.equals(Utility.ILLEGAL_PLAY_MESSAGE)) {
						currentPlay = null;
						new Alert(AlertType.ERROR, Utility.ILLEGAL_PLAY_MESSAGE).showAndWait();
						suppress = true;
//					} else if(message.equals(Utility.SHIELDED_MESSAGE)) {
	//					endTurn();
					} else if(message.equals(Utility.END_TURN_CONFIRM)) {
						display.refreshHand();
						suppress = true;
					} else if(message.equals(Utility.PLAYER_DISCONNECTED_MESSAGE)) {
						dealButton.setDisable(true);
						playButton.setDisable(true);
					}
					if(!suppress) {
						messageFromServer.setText((String)message);
					}
				} else if(message instanceof PriestResponse) {
					priestResult((PriestResponse)message);
				} else if(message instanceof BaronResponse) {
					baronResult((BaronResponse)message);
				} else if(message instanceof GuardResponse) {
					GuardResponse response = (GuardResponse) message;
					guardResult(response);
				} else if(message instanceof WinnerResponse) {
					WinnerResponse response = (WinnerResponse)message;
					previousWinner.setText("Previous Winner: "+response.name);
					winningCard.setText("Winning Card: "+response.card.getType());
				} else if(message instanceof NameChangeResponse) {
					NameChangeResponse response = (NameChangeResponse) message;
					playerNames.put(response.id, response.name);
					if(response.id.equals(connection.getID())) {
						playerName.setText(response.name);
					}
					int i = 0;
					for(Integer playerID: playerNames.keySet()) {
						if(!playerID.equals(connection.getID())) {
							playerNameLabels.get(i).setText(playerNames.get(playerID));
							i++;
						}
					}
				} 
			});
		}
		
		protected void serverShutdown(String message) {
			Platform.runLater(() -> {
				log.severe("Your opponent quit");
				System.exit(0);
			});
		}
	}
	
	private class CardBox {
		private Card card;
		private ImageView view;
		private Boolean selectable = false;
		private DropShadow shadow;
		
		public CardBox() {
			selectable = false;
			shadow = new DropShadow();
			setCard(new Card(Type.NONE));
		}
		
		public void setCard(Card newCard) {
			card = newCard;
			view = new ImageView(new Image(card.getImage()));
			view.setEffect(shadow);
			view.setOnMousePressed(event -> {
				if(event.isPrimaryButtonDown()) {
					event.consume(); // prevent BorderPane from deselecting immediately. Consume the click
					if(selectable) {
						selectCard(this);
					}
				}
			});
		//	view.setImage(new Image(card.getImage()));
			view.setFitHeight(Utility.DEFAULT_CARD_HEIGHT);
			view.setFitWidth(Utility.DEFAULT_CARD_WIDTH);
			shadow.setColor(Color.BLACK);
			shadow.setRadius(20.0);
			shadow.setSpread(0.5);
			if(card.getType() != Type.NONE) {
				setSelectable(true);
				view.setFitWidth(Utility.DEFAULT_CARD_WIDTH);
				view.setVisible(true);
			} else {
				setSelectable(false);
				//view.setFitWidth(0.0);
				view.setFitWidth(1.0);
				view.setVisible(false);
			}
		}
		
		public Card getCard() {
			return card;
		}
		
		public ImageView getView() {
			return view;
		}
		
		public void setSelectable(Boolean isSelectable) {
			selectable = isSelectable;
		}
		
		public DropShadow getShadow() {
			return shadow;
		}
	}
	
	private void selectCard(CardBox cardBox) {
		if(selectedCardBox != null) {
			selectedCardBox.getView().setEffect(selectedCardBox.getShadow());
		}
		if(cardBox == null) {
			selectedCardBox = null;
			return;
		}
		DropShadow shadow = new DropShadow();
		shadow.setColor(Color.YELLOW);
		shadow.setRadius(10.0);
		shadow.setSpread(0.9);
		
		selectedCardBox = cardBox;
		selectedCardBox.getView().setEffect(shadow);
//		selectedCard.getPane().getStyleClass().add("selectedCard");
	}
	
	private void validatePlay(Card card, Integer targetPlayerID) {
		currentPlay = new PlayRequest(connection.getID(), targetPlayerID, card);
		connection.send(currentPlay); // validate play is legal
		// if so, implement some client-side logic
	}
	
	private void playCard() {
		Card card = currentPlay.getCard();
		if(card.getType() == Type.HANDMAID) {
			connection.send(card);
		} else if(card.getType() == Type.COUNTESS) {
			connection.send(card);
		} else if(card.getType() == Type.PRINCESS) {
			connection.send(card);
		} else if(card.getType() == Type.PRINCE) {
			princeRequest();
		} else
		if(!targetShielded) {
			if(card.getType() == Type.PRIEST) {
				connection.send(card);
			} else if(card.getType() == Type.BARON) {
				connection.send(card);
			} else if(card.getType() == Type.KING) {
				connection.send(card);
			} else if(card.getType() == Type.GUARD) {
				guardRequest();
			}
		} 
	}
	
	private void guardRequest() {
		Stage dialog = new Stage();
		
		VBox mainFrame = new VBox();
		HBox row1 = new HBox();
		row1.setSpacing(30.0);
		HBox row2 = new HBox();
		row2.setSpacing(30.0);
		Scene scene = new Scene(mainFrame);
		scene.getStylesheets().add(Utility.styleSheet);
		mainFrame.getStyleClass().add("window");
		dialog.setScene(scene);
		mainFrame.setSpacing(30.0);
		mainFrame.setPadding(new Insets(20.0));
		int elements = 0;
		for(Type type: Type.values()) {
			if(type == Type.NONE || type == Type.GUARD || type == Type.REFERENCE) {
				continue;
			}
			elements++;
			CardBox box = new CardBox();
			box.setCard(new Card(type));
			box.getView().setOnMousePressed(event -> {
				if(event.isPrimaryButtonDown()) {
					event.consume();
					dialog.hide();
					connection.send(new GuardRequest(box.getCard()));
				}
			});
			if(elements <= 4) {
				row1.getChildren().add(box.getView());
			} else {
				row2.getChildren().add(box.getView());
			}
			
		}
		mainFrame.getChildren().addAll(
				row1,
				row2
				);
		
		dialog.initStyle(StageStyle.UTILITY);
		dialog.initOwner(clientStage);
		dialog.initModality(Modality.WINDOW_MODAL);
		scene.setOnKeyPressed(event -> {
			if(event.getCode().equals(KeyCode.ESCAPE)) {
				dialog.hide();
			}
		});
		dialog.setTitle("Guard Card Selection");
		dialog.show();
		dialog.toFront();
	}
	
	private void guardResult(GuardResponse response) {
		if(response.correct) {
			Stage dialog = new Stage();
			
			CardBox box = new CardBox();
			box.setCard(response.card);
			box.setSelectable(false);
	//		box.getView().setOnMouseClicked(event -> {
	//			dialog.hide();
	//		});
			VBox mainFrame = new VBox();
			mainFrame.getStyleClass().add("window");
			mainFrame.setSpacing(20.0);
			mainFrame.setPadding(new Insets(50.0));
			mainFrame.setAlignment(Pos.CENTER);
			Scene scene = new Scene(mainFrame);
			scene.getStylesheets().add(Utility.styleSheet);
			dialog.setScene(scene);
			
			DropShadow shadow = new DropShadow();
			shadow.setRadius(15.0);
			shadow.setSpread(0.8);
			
			
			Label message = new Label(playerNames.get(response.sourceID)+" guesses "+ response.card.getType());
			Label result = new Label("CORRECT");
			result.getStyleClass().add("announcement");
			if(response.targetID.equals(connection.getID())) {
				shadow.setColor(Color.RED);
			} else {
				shadow.setColor(Color.GREEN);
			}			
			box.getView().setEffect(shadow);
			
			mainFrame.getChildren().addAll(
					box.getView(),
					message,
					result
					);
			
			dialog.initStyle(StageStyle.UTILITY);
			dialog.initOwner(clientStage);
			dialog.initModality(Modality.WINDOW_MODAL);
			scene.setOnKeyPressed(event -> {
				if(event.getCode().equals(KeyCode.ESCAPE)) {
					dialog.hide();
				}
			});
			dialog.setTitle(playerNames.get(response.sourceID)+"'s GUARD Guess");
			log.fine("Showing GUARD result");
			dialog.show();
			dialog.toFront();
		}
	}
	
	
	
	private void priestResult(PriestResponse response) {
		Stage dialog = new Stage();
		
		CardBox box = new CardBox();
		box.setCard(response.card);
		box.setSelectable(false);
//		box.getView().setOnMouseClicked(event -> {
//			dialog.hide();
//		});
		StackPane mainFrame = new StackPane();
		mainFrame.getStyleClass().add("window");
		mainFrame.setPadding(new Insets(50.0));
		Scene scene = new Scene(mainFrame);
		scene.getStylesheets().add(Utility.styleSheet);
		dialog.setScene(scene);
		mainFrame.getChildren().add(box.getView());
		
		dialog.initStyle(StageStyle.UTILITY);
		dialog.initOwner(clientStage);
		dialog.initModality(Modality.WINDOW_MODAL);
		scene.setOnKeyPressed(event -> {
			if(event.getCode().equals(KeyCode.ESCAPE)) {
				dialog.hide();
			}
		});
		dialog.setTitle(response.playerName+"'s Hand");
		log.fine("Showing PRIEST dialog");
		dialog.show();
		dialog.toFront();
	}
	
	
	private void baronResult(BaronResponse response) {
		Stage dialog = new Stage();
		
		CardBox winnerBox = new CardBox();
		CardBox loserBox = new CardBox();
		winnerBox.setCard(response.winningCard);
		loserBox.setCard(response.losingCard);
		winnerBox.setSelectable(false);
		loserBox.setSelectable(false);
		DropShadow winnerShadow = new DropShadow();
		
		winnerShadow.setRadius(15.0);
		winnerShadow.setSpread(0.8);
		winnerBox.getView().setEffect(winnerShadow);
		DropShadow loserShadow = new DropShadow();
		
		loserShadow.setRadius(15.0);
		loserShadow.setSpread(0.8);
		loserBox.getView().setEffect(loserShadow);
		
		VBox mainFrame = new VBox();
		Scene scene = new Scene(mainFrame);
		scene.getStylesheets().add(Utility.styleSheet);
		mainFrame.getStyleClass().add("window");
		dialog.setScene(scene);
		
		mainFrame.setAlignment(Pos.CENTER);
		mainFrame.setPadding(new Insets(50.0));
		mainFrame.setSpacing(20.0);
		HBox pictureFrame = new HBox();
		Region pictureSpacer = new Region();
		pictureSpacer.setMinWidth(80.0);
		VBox winnerFrame = new VBox();
		winnerFrame.setAlignment(Pos.CENTER);
		Region winnerSpacer = new Region();
		winnerSpacer.setMinHeight(30.0);
		Label winner = new Label(response.winner);
		VBox loserFrame = new VBox();
		loserFrame.setAlignment(Pos.CENTER);
		Region loserSpacer = new Region();
		loserSpacer.setMinHeight(30.0);
		Label loser = new Label(response.loser);
		HBox announcementFrame = new HBox();
		announcementFrame.setAlignment(Pos.CENTER);
		Label announcement = new Label(response.winner+"'s "+response.winningCard.getType()+" defeats "+response.loser+"'s "+response.losingCard.getType());
		announcement.getStyleClass().add("announcement");
		if(response.isTie) {
			winnerShadow.setColor(Color.ORANGE);
			loserShadow.setColor(Color.ORANGE);
			announcement.setText(response.winner+"'s "+response.winningCard.getType()+" matches "+response.loser+"'s "+response.losingCard.getType());
		} else {
			winnerShadow.setColor(Color.GREEN);
			loserShadow.setColor(Color.RED);
		}
		
		announcementFrame.getChildren().add(announcement);
		loserFrame.getChildren().addAll(
				loserBox.getView(),
				loserSpacer,
				loser);
		winnerFrame.getChildren().addAll(
				winnerBox.getView(),
				winnerSpacer,
				winner);
		pictureFrame.getChildren().addAll(
				winnerFrame,
				pictureSpacer,
				loserFrame);
		mainFrame.getChildren().addAll(
				pictureFrame,
				announcement);
		
		
		dialog.initStyle(StageStyle.UTILITY);
		dialog.initOwner(clientStage);
		dialog.initModality(Modality.WINDOW_MODAL);
		scene.setOnKeyPressed(event -> {
			if(event.getCode().equals(KeyCode.ESCAPE)) {
				dialog.hide();
			}
		});
		dialog.setTitle("Baron Battle!");
		dialog.show();
		dialog.toFront();
	}
	
	private void princeRequest() {
		Stage dialog = new Stage();
		
		VBox mainFrame = new VBox();
		Scene scene = new Scene(mainFrame);
		scene.getStylesheets().add(Utility.styleSheet);
		mainFrame.getStyleClass().add("window");
		dialog.setScene(scene);
		mainFrame.setAlignment(Pos.CENTER);
		mainFrame.setSpacing(20.0);
		mainFrame.setPadding(new Insets(20.0));
		Label question = new Label("Who must discard their hand?");
		mainFrame.getChildren().add(question);
		for(Integer playerID: playerNames.keySet()) {
			Button button = new Button(playerNames.get(playerID));
			button.setOnAction(event -> {
				connection.send(new PrinceRequest(playerID));
				dialog.hide();
			});
			mainFrame.getChildren().add(button);
		}
		
		
		dialog.initStyle(StageStyle.UTILITY);
		dialog.initOwner(clientStage);
		dialog.initModality(Modality.WINDOW_MODAL);
		scene.setOnKeyPressed(event -> {
			if(event.getCode().equals(KeyCode.ESCAPE)) {
				dialog.hide();
			}
		});
		dialog.setTitle("Prince Target Selection");
		dialog.show();
		dialog.toFront();
	}
	
//	private void endTurn() {
//		targetShielded = false;
//		connection.send(Utility.END_TURN_REQUEST);
//	}
	
	private class CardPile {
		private StackPane pilePane = new StackPane();
		private ArrayList<CardBox> boxes = new ArrayList<>();
		private ArrayList<Card> cards = new ArrayList<>();
		private Double separation = Utility.DEFAULT_CARD_HEIGHT * 0.15;
		
		public CardPile() {
			
		}
		
		public StackPane getPane() {
			return pilePane;
		}
		
		public void setCards(ArrayList<Card> newCards) {
			cards = newCards;
			double offset = 0.0;
			boxes.clear();
			pilePane.getChildren().clear();
			for(Card card: cards) {
				CardBox box = new CardBox();
				box.setCard(card);
				box.setSelectable(false);
				boxes.add(box);
				offset = ((boxes.size()-1)*separation);
				box.getView().setTranslateY(offset);
				pilePane.getChildren().add(box.getView());
			}
		}
	}
	
	private class Display extends BorderPane {
		private HBox playerCardBox = new HBox();
		
		public Display() {
			getStyleClass().add("window");
			setPrefSize(Utility.WINDOW_WIDTH, Utility.WINDOW_HEIGHT);
			HBox messageBox = new HBox();
			Region cardSpacer = new Region();
			cardSpacer.setMinWidth(Utility.DEFAULT_CARD_WIDTH);
			Region spacer = new Region();
			HBox.setHgrow(spacer, Priority.ALWAYS);
//			Label messageFromServer = new Label();
			messageFromServer.getStyleClass().add("statusMessages");
			message.getStyleClass().add("statusMessages");
//			messageBox.getStyleClass().add("statusMessages");
			messageBox.getChildren().addAll(
					cardSpacer,
					messageFromServer,
					spacer,
					message);
			
			setBottom(messageBox);
			
			VBox table = new VBox();
			table.setAlignment(Pos.CENTER);
			Region tableSpacer = new Region();
			VBox.setVgrow(tableSpacer, Priority.ALWAYS);

//			opponentCardBox.setSelectable(false);
//			opponentCardBox.getView().setFitHeight(DEFAULT_CARD_HEIGHT);
//			opponentCardBox.getView().setFitWidth(DEFAULT_CARD_WIDTH);
//			opponentCardBox.getView().setRotate(180.0);

			
			playerCardBox.setAlignment(Pos.CENTER);
			playerCardBox.setSpacing(20.0);
			refreshHand();
			
			table.getChildren().addAll(
			//		opponentCardBox.getView(),
			//		tableSpacer,
					playerCardBox);

			setCenter(table);

			VBox controls = new VBox();
			referenceCardBox.setCard(new Card(Type.REFERENCE));
			referenceCardBox.setSelectable(false);
			Region rightRegion = new Region();
			VBox.setVgrow(rightRegion, Priority.ALWAYS);
			controls.setAlignment(Pos.CENTER);
			controls.setSpacing(10.0);
			Label opponents = new Label("Opponents");
			playerName.getStyleClass().add("announcement");
			opponents.getStyleClass().add("announcement");
			
			controls.getChildren().addAll(
					playerName,
					shielded,
					wins,
					losses,
					dealButton, 
					playButton,
					opponents);
			for(int i = 0; i < 3; i++) {
				playerNameLabels.add(new Label(""));
				controls.getChildren().add(playerNameLabels.get(i));
			}
			controls.getChildren().addAll(
					rightRegion,
					newGameButton,
					quitButton,
					previousWinner,
					winningCard,
					referenceCardBox.getView()
					);

			setRight(controls);
			
			VBox deckBox = new VBox();
			
			Region leftRegion = new Region();
			VBox.setVgrow(leftRegion, Priority.ALWAYS);
//			CardBox deck = new CardBox();
//			deck.getView().setFitHeight(DEFAULT_CARD_HEIGHT);
//			deck.getView().setFitWidth(DEFAULT_CARD_WIDTH);
			deckBox.getChildren().addAll(
//					deck.getView(),
					discardPile.getPane(),
					leftRegion
					
					);
			setLeft(deckBox);
			
			this.setOnMousePressed(event -> {
				selectCard(null);
			});

		}
		
		public void refreshHand() {
			if(state != null) {
				for(int i = 0; i < playerCardBoxes.size(); i++) {
					try {
						playerCardBoxes.get(i).setCard(state.player.getHand().get(i));
					} catch(IndexOutOfBoundsException ex) {
						playerCardBoxes.get(i).setCard(new Card(Type.NONE));
					}
				}
			}
			playerCardBox.getChildren().clear();
			for(CardBox box: playerCardBoxes) {
//				box.setSelectable(true);
//				box.getView().setFitHeight(DEFAULT_CARD_HEIGHT);
//				box.getView().setFitWidth(DEFAULT_CARD_WIDTH);
				playerCardBox.getChildren().add(box.getView());
			}
		}
	}
	
	private void configureButtons() {
		dealButton.setOnAction(action -> {
			dealButton.setDisable(true);
			connection.send(Utility.DEAL_MESSAGE);
		});
		dealButton.setDisable(true);
		playButton.setDisable(true);
		playButton.setOnAction(action -> {
			if(selectedCardBox == null) {
				//TODO: alert
				return;
			} else {
				ArrayList<Integer> playerList = connection.getConnectedPlayers();
				for(Integer player: playerList) {
					if(!player.equals(connection.getID())) {
						validatePlay(selectedCardBox.getCard(), player);
						break;
					}
				}
				
			}
		});
		playButton.setDefaultButton(true);
		newGameButton.setOnAction(action -> {
			connection.send(Utility.NEW_GAME_REQUEST);
		});
		newGameButton.setDisable(true);
		quitButton.setOnAction(action -> {
			doQuit();
		});
	}
	

	private void doQuit() {
		clientStage.hide(); // Close the window.
		if (connection != null) {
			connection.disconnect();
			try { // time for the disconnect message to be sent.
				Thread.sleep(500);
			} catch (InterruptedException e) {
				log.severe(e.toString());
			}
		}
		System.exit(0);
	}

}
