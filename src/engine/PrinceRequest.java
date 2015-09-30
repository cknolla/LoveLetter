package engine;

import java.io.Serializable;

public class PrinceRequest implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8784529091706404118L;
	public final Integer playerID;
	
	public PrinceRequest(Integer playerID) {
		this.playerID = playerID;
	}
}
