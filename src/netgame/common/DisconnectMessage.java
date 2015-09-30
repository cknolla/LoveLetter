package netgame.common;

import java.io.Serializable;

/**
 * A DisconnectMesaage is sent from a Client to the Hub when that
 * client wants to disconnect.  A DisconnectMessage is also sent from
 * the Hub to each client just before it shuts down normally.  DisconnectMessages
 * are for internal use in the netgame.common package and  are not used 
 * directly by users of the package.
 */
final class DisconnectMessage implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = -6601845001774878381L;
	/**
     * The message associated with the disconnect.  When the Hub
     * sends disconnects because it is shutting down, the message
     * is "*shutdown*".
     */
	public final  String message;
    
    /**
     * Creates a DisconnectMessage containing a given String, which
     * is meant to describe the reason for the disconnection.
     */
    public DisconnectMessage(String message) {
        this.message = message;
    }
    
}
