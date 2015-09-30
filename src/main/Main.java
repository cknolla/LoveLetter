package main;

import java.io.File;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import launcher.Launcher;
import server.Server;

public class Main {

	public static final Logger log = Logger.getLogger(Main.class.getName());
	
	public static void main(String[] args) {
		try {
			// configure Logger
			log.setUseParentHandlers(false);
			ConsoleHandler consoleHandler = new ConsoleHandler();
			log.addHandler(consoleHandler);
			consoleHandler.setLevel(Level.INFO);
			
			File logfile = new File("./LoveLetter/lastGame.log");
			logfile.getParentFile().mkdirs();
			FileHandler fileHandler = new FileHandler(logfile.toString());
			fileHandler.setFormatter(new SimpleFormatter());
		//	java.util.logging.FileHandler.class = this;
			log.addHandler(fileHandler);
			fileHandler.setLevel(Level.INFO);
		} catch(IOException ex) {
			System.err.println("Couldn't open log to file");
		}
		log.setLevel(Level.ALL); // set to ALL to allow handlers to filter independently
		
		log.severe("Logger started");
		if(args.length > 0) {
			if(args.length != 3) {
				explainArgs();
			}
			if(!args[0].equals("server")) {
				explainArgs();
			}
			Integer port = Utility.DEFAULT_PORT;
			try {
				port = Integer.parseInt(args[1]);
			} catch(NumberFormatException ex) {
				System.err.println("Port invalid: "+args[1]);
				explainArgs();
			}
			Boolean persistent = false;
			persistent = Boolean.parseBoolean(args[2]);
			try {
				new Server(port, persistent);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			new Launcher(args);
		}
		log.info("Exiting main");

	}
	
	private static void explainArgs() {
		System.out.println("To run a headless server, execute with the following arguments: ");
		System.out.println("java -jar LoveLetter.jar server [port] [isPersistent]");
		System.out.println("[port] must be a number and [isPersistent] must be a boolean (true/false)");
		System.out.println("A persistent server will stay live even after all clients disconnect. It must be forcefully stopped");
		System.exit(1);
	}

}
