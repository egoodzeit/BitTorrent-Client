package cs352.RUBTClient.Group06;
/**
 * @author Marinela G. Haldeman
 * @author Elliot Goodzeit
 * @author Aditya Sai
 * 54 lines of code
 */
public class HandshakeInfo {
	/**
	 * to hold the hash for the handshake
	 */
	private final byte[] hash;
	/**
	 * to hold the id for the handshake
	 */
	private final byte[] id;
	/**
	 * constructor
	 * @param message
	 * 				handshake response from peer
	 */
	public HandshakeInfo(byte[] message) 
	{
		hash = new byte[20];
		id = new byte[20];
		System.arraycopy(message, 28, this.hash, 0, 20);
		System.arraycopy(message, 48, this.id, 0, 20);
	}
	/**
	 * main constructor
	 * @param hash
	 * 				encryption used by peer
	 * @param id
	 * 				id of peer
	 */
	public HandshakeInfo(byte[] hash, byte[] id) {
		this.hash = hash;
		this.id = id;
	}
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// 
	// GET-ERS AND SET-ERS
	//
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public byte[] getHash()
	{
		return this.hash;
	}
	public byte[] getId()
	{
		return this.id;
	}
}
