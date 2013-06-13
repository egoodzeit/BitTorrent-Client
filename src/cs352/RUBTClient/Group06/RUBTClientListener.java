package cs352.RUBTClient.Group06;
/**
 * @author Marinela G. Haldeman
 * @author Elliot Goodzeit
 * @author Aditya Sai
 * 80 lines of code
 */
import java.util.ArrayList;
/*You need to support resuming downloads. 
 *So, if the user closes the program during a download, and opens the program again later, it should pick up exactly where it left off.
 *
 *You'll need to store the uploaded and downloaded counts in order to properly communicate with the tracker.  
 *Rob suggested storing these as two lines in a text file, which you could call outputname.ext.stats (for example, song.mp3.stats).
 *You'll also need to verify the data that was downloaded already.  
 *This is where you could use a RandomAccessFile. 
 *If you use a RAF to store the data in the first place, then, when attempting a resume, 
 *you can recreate your RAF object and you'll have access to the exact same ByteBuffer as you were working with before.  
 *Then, you can start to verify pieces of that ByteBuffer using the hashes in the metainfo file.
 * 
 */

import cs352.RUBTClient.Group06.Message.*;

public interface RUBTClientListener {
    
    /**
     * Method to notify manager when the user has requested that the local client shut down
     * gracefully. Remote peers and tracker should be notified of the shutdown.
     */
    public void userRequestedShutdown();
    /**
     * method to notify the manager that the user has requested status
     * manager will print the status to the console
     */
    public void userRequestedStatus();
    /**
	 * method to notify the Manager that a new list of onlinePeers has been extracted
	 * @param onlinePeers
	 * 					list of online Peers
	 */
	public void onlinePeersListReady(final ArrayList<Peer> onlinePeers);
	/**
	 * method used to notify manager that a message has arrived from peer
	 * @param peer
	 * @param message
	 */
	public void peerRequestedNewPiece(final Peer peer, final RequestMessage requestMessage);
	/**
	 * method used to notify manager that peer has been choked while downloading
	 * @param peer
	 */
	public void peerChokedWhileDownloading(final Peer peer);
	/**
	 * method used to notify manager that peer has finished downloading a piece
	 * @param peer
	 */
	public void peerFinishedDownloading(final Peer peer);
	/**
	 * method used to notify manager that peer can start downloading
	 * @param peer
	 */
	public void peerCanStartDownloading(final Peer peer);
	/**
	 * method used to notify manager that peer received have
	 * @param peer
	 * @param haveMessage
	 */
	public void peerReceivedHaveMessage(final Peer peer, final Message.HaveMessage haveMessage);
	/**
	 * method used to notify manager that peer received have
	 * @param peer
	 */
	public void peerReceivedBitfieldMessage(final Peer peer);
	/**
	 * method to notify the manager the peer got an IO exception on socket and needs disconnected
	 * @param peer
	 */
	public void peerNeedsDisconnected(final Peer peer);
	/**
	 * method to notify the manager the peer got an interested message
	 * @param peer
	 */
	public void peerReceivedInterested(final Peer peer);
}
