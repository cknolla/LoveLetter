package launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import client.ClientWindow;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import main.Utility;
import server.Server;

public class Launcher extends Application {
	
	private static Stage _primaryStage = null;
	private final Logger log = Logger.getLogger(this.getClass().getName());
	private String name = "";
	private String host = Utility.DEFAULT_HOST;
	private Integer cPort = Utility.DEFAULT_PORT;
	private Integer sPort = Utility.DEFAULT_PORT;
	private File configFile = new File("./LoveLetter/client.conf");
	
	public Launcher() {
		// required by Application - do not call directly
	}
	
	public Launcher(String[] args) {
		_init(args);
	}
	
	public void start(Stage primaryStage) {
		log.setLevel(Level.FINER);
		log.severe("Launcher log started");
		configFile.getParentFile().mkdirs();
		_primaryStage = primaryStage;
		
		VBox mainFrame = new VBox();
		mainFrame.setSpacing(15.0);
		mainFrame.setPadding(new Insets(15.0));
		
		Scene scene = new Scene(mainFrame);
		primaryStage.setTitle("Love Letter Launcher");
		primaryStage.setScene(scene);
		scene.getStylesheets().add(Utility.styleSheet);
		mainFrame.getStyleClass().add("window");
		
		HBox nameBox = new HBox();
		Label nameLabel = new Label("Your name: ");
		TextField playerName = new TextField();
		nameBox.getChildren().addAll(
				nameLabel,
				playerName
				);
		
		CheckBox enableClient = new CheckBox();
		CheckBox enableServer = new CheckBox();
		Label enableClientLabel = new Label("Start a client");
		Label enableServerLabel = new Label("Start a server");
		HBox enableClientBox = new HBox();
		enableClientBox.getChildren().addAll(
				enableClient,
				enableClientLabel
				);
		HBox enableServerBox = new HBox();
		enableServerBox.getChildren().addAll(
				enableServer,
				enableServerLabel
				);
		Label hostLabel = new Label("Server: ");
		TextField hostField = new TextField("localhost");
		Label separator = new Label(":");
		TextField clientPort = new TextField(""+cPort);
		clientPort.setMaxWidth(80.0);
		HBox clientBox = new HBox();
		clientBox.getChildren().addAll(
				hostLabel,
				hostField,
				separator,
				clientPort);
		TextField serverPort = new TextField(""+sPort);
		serverPort.setDisable(true);
		serverPort.setMaxWidth(80.0);
		Label serverLabel = new Label("Host on port: ");
		HBox serverBox = new HBox();
		serverBox.getChildren().addAll(
				serverLabel,
				serverPort);
		
		Button startButton = new Button("Start");
		startButton.setDefaultButton(true);
		HBox startBox = new HBox();
		startBox.setAlignment(Pos.CENTER);
		startBox.getChildren().addAll(
				startButton
				);
		
		mainFrame.getChildren().addAll(
				nameBox,
				enableClientBox,
				clientBox,
				enableServerBox,
				serverBox,
				startBox);
//		Button startServer = new Button("Start Server");
//		Button startClient = new Button("Start Client");
//		VBox buttonBox = new VBox();
//		buttonBox.getChildren().addAll(startServer, startClient);
		
		enableClient.setOnAction(action -> {
			if(enableClient.isSelected()) {
				hostField.setDisable(false);
				clientPort.setDisable(false);
			} else {
				hostField.setDisable(true);
				clientPort.setDisable(true);
			}
		});
		enableClientLabel.setOnMouseClicked(action -> {
			enableClient.fire();
		});
		enableServer.setOnAction(action -> {
			if(enableServer.isSelected()) {
				serverPort.setDisable(false);
			} else {
				serverPort.setDisable(true);
			}
		});
		enableServerLabel.setOnMouseClicked(action -> {
			enableServer.fire();
		});
		startButton.setOnAction(action -> {
			name = playerName.getText();
			host = hostField.getText();
			try {
				cPort = Integer.parseInt(clientPort.getText());
			} catch(NumberFormatException ex) {
				new Alert(AlertType.ERROR, "Client port is not valid").showAndWait();
				return;
			}
			try {
				sPort = Integer.parseInt(serverPort.getText());
			} catch(NumberFormatException ex) {
				new Alert(AlertType.ERROR, "Server port is not valid").showAndWait();
				return;
			} 
			saveConfig();
			if(enableServer.isSelected()) {
				try {
					serverStart(sPort);
				} catch(IOException ex) {
					new Alert(AlertType.ERROR,"Error starting server:\n"+ex).showAndWait();
				}
			}
			if(enableClient.isSelected()) {
				clientStart(host, cPort, name);		
			}
			
			
		});
		loadConfig();
		playerName.setText(name);
		hostField.setText(host);
		clientPort.setText(cPort.toString());
		serverPort.setText(sPort.toString());
		primaryStage.show();
		enableClient.fire();
		enableClient.setDisable(true);
		
/*
		startServer.setOnAction(e -> {
			try {
				serverStart();
				clientStart();
			} catch(IOException ex) {
				log.severe(ex.toString());
			}
		});
		startClient.setOnAction(e -> {
			clientStart();
		});
	*/
		// auto-start server and clients : just for testing
	/*	
		try {
			serverStart();
		} catch(IOException ex) {
			log.severe(ex.toString());
		}
		clientStart();
/*		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
*///		clientStart();
	}
	
	private void _init(String[] args) {
		launch(args);
		
	}
	
	private void serverStart(Integer port) throws IOException {
		new Server(port, false);
	}
	
	private void clientStart(String host, Integer port, String name) {
		new ClientWindow(host, port, name);
		_primaryStage.hide();
	}
	
	private void loadConfig() {
		Properties props = new Properties();
		FileInputStream in = null;
		try {
			in = new FileInputStream(configFile);
			
		} catch(FileNotFoundException ex) {
			log.warning("No config file found");
			// no config file exists
			return;
		}
		try {
			props.load(in);
		} catch(IOException ex) {
			new Alert(AlertType.ERROR, "Error loading "+configFile).show();
			return;
		}
		name = props.getProperty("name");
		host = props.getProperty("host");
		cPort = Integer.parseInt(props.getProperty("clientPort"));
		sPort = Integer.parseInt(props.getProperty("serverPort"));
		
	}
	
	private void saveConfig() {
		Properties props = new Properties();
		FileOutputStream out = null;

		try {
			out = new FileOutputStream(configFile);
		} catch(FileNotFoundException ex) {
			log.warning("Cannot open "+configFile+" for writing");
			return;
		}
		props.setProperty("name", name);
		props.setProperty("host", host);
		props.setProperty("clientPort", cPort.toString());
		props.setProperty("serverPort", sPort.toString());
		try {
			props.store(out, "Previously saved values in Love Letter");
		} catch(IOException ex) {
			log.warning("Cannot write to "+configFile);
			return;
		}
	}

}
