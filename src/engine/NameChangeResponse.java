package engine;

import java.io.Serializable;

public class NameChangeResponse implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2339029406285223333L;
	public final Integer id;
	public final String name;
	
	public NameChangeResponse(Integer playerID, String name) {
		this.id = playerID;
		this.name = name;
	}
}
