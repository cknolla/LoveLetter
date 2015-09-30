package engine;

import java.io.Serializable;

public class NameChangeRequest implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6926505002704434749L;
	public final String name;
	
	public NameChangeRequest(String name) {
		this.name = name;
	}
}
