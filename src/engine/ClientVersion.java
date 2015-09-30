package engine;

import java.io.Serializable;

public class ClientVersion implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7701951445373963136L;
	public final String version;
	
	public ClientVersion(String version) {
		this.version = version;
	}
}
