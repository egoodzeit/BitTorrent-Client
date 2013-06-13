/**
 * @author Marinela G. Haldeman
 * @author Elliot Goodzeit
 * @author Aditya Sai
 */
package cs352.RUBTClient.Group06;
import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import cs352.RUBTClient.RobertMoore.*;
/**
 * @author Marinela G. Haldeman
 * @author Elliot Goodzeit
 * @author Aditya Sai
 * 240 lines of code
 */
public class Tracker extends Thread{ 
	/**
	 * pointer to client info managed by the manager
	 */
	private final RUBTClientInfo client_info;
	/**
	 * client id to send to tracker
	 */
	private final String client_id_hex = ToolKit2.toHexString(RUBTConstants.CLIENT_ID);
	/**
	 * client hash to send to tracker
	 */
	private final String client_hash_hex;
	/**
	 * interval to wait between successful contacts to tracker
	 */
	private int interval;
	/**
	 * min interval to wait between successful contacts to tracker
	 */
	private int min_interval;
	/**
	 * Flag to shutdown the tracker
	 */
	private boolean keepRunning = true;
	/**
	 * list of listeners for the Tracker
	 */
	private final Collection<RUBTClientListener> listeners = new ConcurrentLinkedQueue<RUBTClientListener>();
	/**
	 * event to send to tracker
	 */
	private String event;
	/**creates a Tracker object with a reference to the client's info
	 * @param clientInfo
	 * 				created in main, contains torrentInfo, clientId, onlinePeers etc
	 */
	public Tracker(RUBTClientInfo client_info)
	{
		this.client_info = client_info;
		this.client_hash_hex = ToolKit2.toHexString(this.client_info.info_hash.array());
		this.interval = 1;
		this.min_interval = 1;
		this.setEvent(RUBTConstants.EVENT_STARTED);
	}
	@Override
	public void run()
	{
		RUBTConstants.main_log.info("Tracker is up"); 
		while (this.keepRunning)
		{
			RUBTConstants.main_log.info("Tracker sending event " + this.getEvent()); 
			//send http request
			byte[] httpResponseArray = null;
			try {
				httpResponseArray = httpConnectionToTracker();
			} catch (IOException ex) {
				RUBTConstants.main_log.severe("Connecting to tracker failed!");
				//sleep min_interval upon a failed connection to the tracker
				try {
					RUBTConstants.main_log.info("Tracker will go to sleep for " + this.min_interval + " secs"); 
					Thread.sleep(this.min_interval*1000);
				} catch (InterruptedException e) {
					//ignore interrupts
					RUBTConstants.main_log.info("Thread Tracker was interrupted while sleeping!");
				}
			}
			//exit after sending stopped event
			if (this.getEvent().equals(RUBTConstants.EVENT_STOPPED))
			{
				//disconnect tracker
				this.disconnect();
				continue;
			}

			//reset event
			this.setEvent(null);

			//get list of peers
			ArrayList<Peer> onlinePeers = null;
			try {
				onlinePeers = getPeerList(httpResponseArray);
			} catch (BencodingException ex) {
				//sleep min_interval upon failing to get list of peers
				RUBTConstants.main_log.severe("Parsing tracker's response failed!");
				try {
					RUBTConstants.main_log.info("Tracker will go to sleep for " + this.min_interval + " secs"); 
					Thread.sleep(this.min_interval*1000);
				} catch (InterruptedException e) {
					//ignore interrupts
					RUBTConstants.main_log.info("Thread Tracker was interrupted while sleeping!");
				}
			}

			//if we get here all is good
			//announce your listeners that you got a new peer list
			for (RUBTClientListener listener : Tracker.this.listeners) {
				listener.onlinePeersListReady(onlinePeers);
			}

			//then sleep for interval seconds
			try {
				RUBTConstants.main_log.info("Tracker will go to sleep for " + this.interval + " secs"); 
				Thread.sleep(this.interval*1000);
			} catch (InterruptedException e) {
				//ignore interrupts
				RUBTConstants.main_log.info("Thread Tracker was interrupted while sleeping!");
			}
		}
		RUBTConstants.main_log.info("Tracker is down!"); 
		//end session in clients activity log
		RUBTConstants.main_log.info("*** END SESSION ***");
		RUBTConstants.main_log.info("closing activity and stats logs"); 
		//close RUBT log
		RUBTConstants.main_log.getHandlers()[0].close();
		//close STATS log
		RUBTConstants.stats_log.getHandlers()[0].close();
	}
	/**
	 * terminate thread
	 */
	protected void disconnect() {
		//shut down tracker
		this.keepRunning = false;
	}
	/**method to establish connection to tracker and get the status stream
	 * @return byte[] received as response from tracker
	 * @throws IOException 
	 * 					caught in Tracker.run
	 * @throws MalformedURLException 
	 */
	private byte[] httpConnectionToTracker() throws MalformedURLException, IOException 
	{
		String httpGetString = null;
		//compose String for http get request
		if (this.getEvent() == null)
		{
			httpGetString = String.format("info_hash=%s&peer_id=%s&port=%d&uploaded=%d&downloaded=%d&left=%d", 
					this.client_hash_hex, 
					this.client_id_hex,
					RUBTConstants.SERVER_SOCKET.getLocalPort(),
					this.client_info.getClient_stats().getUploaded(), 
					this.client_info.getClient_stats().getDownloaded(),
					this.client_info.getClient_stats().getLeft());
		}
		else
		{
			httpGetString = String.format("info_hash=%s&peer_id=%s&port=%d&uploaded=%d&downloaded=%d&left=%d&event=%s", 
					this.client_hash_hex, 
					this.client_id_hex,
					RUBTConstants.SERVER_SOCKET.getLocalPort(),
					this.client_info.getClient_stats().getUploaded(), 
					this.client_info.getClient_stats().getDownloaded(),
					this.client_info.getClient_stats().getLeft(),
					this.getEvent());
		}
		//establish connection to tracker
		URLConnection connection = new URL(this.client_info.announce_url + "?" + httpGetString).openConnection();
		int length = connection.getContentLength();
		byte[] httpResponseArray = new byte[(int)length];
		DataInputStream inStream = new DataInputStream(connection.getInputStream());

		//read trackers response fully
		inStream.readFully(httpResponseArray);
		inStream.close();
		RUBTConstants.main_log.info("Tracker response: " + new String(httpResponseArray)); 
		return httpResponseArray;
	}
	/**method to decode the tracker's response 
	 * 
	 * @param httpResponseByteArray - byte[] with the trackers response
	 * @throws BencodingException 
	 * 					caught in Tracker.run
	 */
	@SuppressWarnings("unchecked")
	private ArrayList<Peer> getPeerList(byte[] httpResponseByteArray) throws BencodingException 
	{   
		ArrayList<Peer> onlinePeers = new ArrayList<Peer>();
		//extract dictionary from byte[]
		Map<ByteBuffer,Object>  				map = (Map<ByteBuffer,Object>)Bencoder2.decode(httpResponseByteArray);
		String                                  peerIp;
		byte[]                                  peerId;
		int                                     peerPort;
		ByteBuffer                              byteArrayObject;

		//check if tracker has failed
		//cannot recover, must exit
		if (map.get(RUBTConstants.KEY_FAILURE_REASON) != null)
		{
			RUBTConstants.main_log.severe("tracker failed to get response"); 
			System.exit(0);
		}
		//extract interval from dictionary
		this.interval = Integer.parseInt(map.get(RUBTConstants.KEY_INTERVAL).toString());
		//extract min interval from dictionary, if specified
		//else it is half the interval
		if (map.get(RUBTConstants.KEY_MIN_INTERVAL).toString() != null)
		{
			this.min_interval = Integer.parseInt(map.get(RUBTConstants.KEY_MIN_INTERVAL).toString());
		}
		else
		{
			RUBTConstants.main_log.info("Tracker hasnt provided min_interval"); 
			this.min_interval = this.interval / 2;
		}	
		//		System.out.println("Tracker stats: ");
		//		System.out.println("Complete: " + Integer.parseInt(map.get(RUBTConstants.KEY_COMPLETE).toString()));
		//		System.out.println("Downloaded: " + Integer.parseInt(map.get(RUBTConstants.KEY_DOWNLOADED).toString()));
		//		System.out.println("Incomplete: " + Integer.parseInt(map.get(RUBTConstants.KEY_INCOMPLETE).toString()));
		//		System.out.println("interval is " + this.interval + " secs");
		//		System.out.println("min interval is " + this.min_interval + " secs");


		//extract onlinePeers list from dictionary
		ArrayList<Map<ByteBuffer,Object>> list = (ArrayList<Map<ByteBuffer,Object>>) map.get(RUBTConstants.KEY_PEERS);
		//extract onlinePeers from list
		for (Map<ByteBuffer,Object> item:list)
		{
			byteArrayObject = (ByteBuffer)item.get(RUBTConstants.KEY_PEER_IP);
			if(byteArrayObject == null) 
				throw new BencodingException("Could not extract peer ip");

			try{
				peerIp = new String(byteArrayObject.array(), "ASCII");
			}
			catch(UnsupportedEncodingException uee){
				throw new BencodingException(uee.getLocalizedMessage());
			}

			byteArrayObject = (ByteBuffer)item.get(RUBTConstants.KEY_PEER_ID);
			if (byteArrayObject == null) 
				throw new BencodingException("Could not extract peer ID");
			else
				peerId = byteArrayObject.array();

			peerPort = (Integer)item.get(RUBTConstants.KEY_PEER_PORT);

			//Add peer only if not in the list
			if (!client_info.isConnect_to_all() && !RUBTConstants.RUBT_PEERS_IPS.contains(peerIp))
			{
				//ignore peer if peer not RUBT and connect_to_all is not set
				continue;
			}
			Peer newPeer = new Peer(peerIp,peerId,peerPort,this.client_info.getNumber_of_pieces(), this.client_info.isDebug_mode());
			onlinePeers.add(newPeer);
		}  
		return onlinePeers;
	}
	/**
	 * add listener to the list of listeners for the RUBTClientListener interface
	 * @param listener
	 * 					thread that attempts to listen
	 */
	public void addTrackerListener(final RUBTClientListener listener) {
		this.listeners.add(listener);
	}
	/**
	 * remove listener from the list of listeners for the RUBTClientListener interface
	 * @param listener
	 * 					thread that is listening
	 */
	public void removeMessageListener(final RUBTClientListener listener) {
		this.listeners.remove(listener);
	}
	/**
	 * getter for event
	 * @return
	 */
	public String getEvent() {
		return event;
	}
	/**
	 * setter for event
	 * @param event
	 */
	public void setEvent(String event) {
		this.event = event;
	}
}
