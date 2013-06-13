/**
 * @author Marinela G. Haldeman
 * @author Elliot Goodzeit
 * @author Aditya Sai
 * 794 lines of code
 */
package cs352.RUBTClient.Group06;
import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import cs352.RUBTClient.Group06.Message.*;
import cs352.RUBTClient.RobertMoore.ToolKit2;

public class Manager extends Thread implements RUBTClientListener{
	/**
	 * reference to client_info info object
	 */
	protected final RUBTClientInfo client_info;
	/**
	 * reference to tracker
	 */
	protected final Tracker tracker;
	/**
	 * Flag to shutdown the manager
	 */
	protected boolean keepRunning = true;
	/**
	 * List of currently connectedPeers.
	 */
	protected final Collection<Peer> connectedPeers = new ConcurrentLinkedQueue<Peer>();
	/**
	 * List of active connectedPeers.
	 */
	protected final Collection<Peer> activePeers = new ConcurrentLinkedQueue<Peer>();
	/**
	 * List of currently downloading pieces
	 */
	protected final Collection<Integer> pieces_downloading = new ConcurrentLinkedQueue<Integer>();
	/**
	 * Thread pool for handling the peers messaging and the tracker updates
	 */
	protected final ExecutorService workers = Executors.newCachedThreadPool();
	/**
	 * Timer for scheduling repeated performance analysis of peers
	 */
	protected final Timer timer = new Timer();
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// STARTING MANAGER THREAD
	//
	// MANAGER THREAD STARTS AND MANAGES THE TRACKER AND THE PEERS
	// IT LISTENS TO THE RUBTclient_info FOR USER REQUESTS
	// IT SUPERVISES THE ENTIRE OPERATION OF THE APPLICATION
	// ITS MAIN JOB IS TO LISTEN FOR INCOMING CONNECTIONS (SEE RUN)
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	/**creates a Manager object
	 * 
	 * @param client_info.torrentInfo
	 * 						holds the torrent info, client_info.torrentInfo id and some other stats about the application
	 */
	public Manager(RUBTClientInfo client_info) 
	{	
		//set field
		this.client_info = client_info;
		//start tracker and add the listener to it
		this.tracker = new Tracker(client_info);
		this.tracker.addTrackerListener(this);
		//for debug purposes
		this.tracker.setName("Tracker");
		this.tracker.start();
		//Timer to schedule performance analysis; 
		this.timer.scheduleAtFixedRate(new TimerTask() {
			public void run(){
				Manager.this.runPerformanceAnalyzer();
				Manager.this.runOptimisticUnchoke();
			}
		}, RUBTConstants.INTERVAL_TO_ANALYZE, RUBTConstants.INTERVAL_TO_ANALYZE);// RUBTConstants.analysisInterval*1000);
	}
	/**
	 * method used by manager to connect to an incomming connection
	 * @param peerSocket
	 */
	private void addPeerBySocket(final Socket peerSocket) {
		this.workers.execute(new Runnable() {
			@Override
			public void run() {
				addPeer(peerSocket);
			}
		});
	}
	@Override
	public void run() 
	{
		RUBTConstants.main_log.info("Manager is up");
		while (this.keepRunning) 
		{
			try {
				// This will block for 250ms to allow checking for user exit conditions.
				Socket peerSocket = RUBTConstants.SERVER_SOCKET.accept();
				RUBTConstants.main_log.info("In Manager: Got an incoming connection from peer!");
				/* Pass the actual work of adding the peer to another thread,
				 * freeing this thread to accept new peers*/
				//Add peer only if not in the list
				StringTokenizer tokenizer = new StringTokenizer(peerSocket.getInetAddress().toString());
				if (!RUBTConstants.RUBT_PEERS_IPS.contains(tokenizer.nextToken("/")))
				{
					//ignore peer if peer not RUBT for now
					continue;
				}
				addPeerBySocket(peerSocket);
			} catch (SocketTimeoutException ste) {
				// Left in for debugging
				RUBTConstants.main_log.info("Server socket timed out");
			} catch (IOException e) {
				// Left in for debugging
				RUBTConstants.main_log.severe("Server socket error: " + e.getStackTrace().toString());
			}
		}
		RUBTConstants.main_log.info("Manager is down");
	}
	/**
	 * shut down the entire application and do the necessary clean-up
	 */
	protected void doShutdown() {
		//cancel performance analysis
		this.timer.cancel();
		//shut down all peers
		for (Peer peer : this.connectedPeers) {
			//update the stats with the peers stats first
			this.client_info.updateClient_stats(peer.getDownloaded(), peer.getUploaded());
			peer.removeMessageListener(this);
			peer.disconnect();
		}
		//save file to download to disk and stats
		this.client_info.saveToDisk();

		//wake up tracker and sent stopped
		this.tracker.interrupt();
		this.tracker.setEvent(RUBTConstants.EVENT_STOPPED);

		// Shut down the thread pool
		this.workers.shutdown();
		RUBTConstants.main_log.info("Manager's Threadpool is down");

		//diconnect manager
		this.disconnect();
	}
	/**
	 * method to stop the manager run loop
	 */
	public void disconnect()
	{
		this.keepRunning = false;
	}
	/**
	 * convenience method used by the manger to chocke a peer based on its performance
	 */
	public void runPerformanceAnalyzer()
	{
		RUBTConstants.main_log.info("*** RUNNING PERFORMANCE ANALYZER ***");
		if (this.activePeers.size() <= RUBTConstants.MIN_UNCHOKED_PEERS)
		{
			RUBTConstants.main_log.info("too many active peers: " + this.activePeers.size());
			return;
		}
		//find the worst performing peer
		final Peer worstPeer = this.findWorstPeer();
		RUBTConstants.main_log.info("Peer with worst performance is " + worstPeer.toString());
		this.workers.execute(new Runnable() {
			@Override
			public void run() {
				try {
					//chocke both sides
					worstPeer.setRemoteStatus(RUBTConstants.PeerStatus.CHOKED);
					worstPeer.setLocalStatus(RUBTConstants.PeerStatus.CHOKED);
					worstPeer.sendMessageToPeer(new ChokeMessage());
					worstPeer.peer_log.info("Peer is being chocked based on worse performance!");
					//this is a hack
					Manager.this.activePeers.remove(worstPeer);
				} catch (IOException e) {
					//peer socket is bad disconnect
					Manager.this.connectedPeers.remove(worstPeer);
					Manager.this.activePeers.remove(worstPeer);
					worstPeer.removeMessageListener(Manager.this);
					worstPeer.disconnect();
				}
			}
		});
		for (Peer peer : this.connectedPeers) {
			//update the stats with the peers stats first
			this.client_info.updateClient_stats(peer.getDownloaded(), peer.getUploaded());
			//log the stats in the peer's stats log ??
			//then reset peer's stats
			peer.resetStats();
		}
	}
	/**
	 * convinience methos used above by the performance analyzer
	 */
	public Peer findWorstPeer()
	{
		//changed remote status to local status
		//when seed download rate takes precedence
		if(RUBTToolKit.isSeed(this.client_info.getClient_bitfield())){
			//look for worst download rate
			long worstDownloadRate = Integer.MAX_VALUE;
			for(Peer peer : this.connectedPeers){
				if(peer.getDownloaded() <= worstDownloadRate && peer.getLocalStatus() == RUBTConstants.PeerStatus.UNCHOKED){
					worstDownloadRate = peer.getDownloaded();
				}
			}
			//find a peer with this worst download rate and return it
			for(Peer peer: this.connectedPeers){
				if(peer.getDownloaded() == worstDownloadRate && peer.getLocalStatus() == RUBTConstants.PeerStatus.UNCHOKED){
					return peer;
				}
			}
			//Otherwise, repeat same process with upload rates
		}else{
			long worstUploadRate = Integer.MAX_VALUE;
			for(Peer peer : this.connectedPeers){
				if(peer.getUploaded() < worstUploadRate && peer.getRemoteStatus() == RUBTConstants.PeerStatus.UNCHOKED){
					worstUploadRate = peer.getUploaded();
				}
			}
			for(Peer peer: this.connectedPeers){
				if(peer.getUploaded() == worstUploadRate && peer.getRemoteStatus() == RUBTConstants.PeerStatus.UNCHOKED){
					return peer;
				}
			}
		}
		//Ignore
		return null;
	}
	/**
	 * convenience method used by the manger to uncoke a peer randomly
	 */
	public void runOptimisticUnchoke(){
		RUBTConstants.main_log.info("*** RUNNING OPTIMISTIC UNCHOKE ***");
		if (this.activePeers.size() >= RUBTConstants.MAX_UNCHOKED_PEERS)
		{
			RUBTConstants.main_log.info("too little active peers: " + this.activePeers.size());
			return;
		}
		//Lookup selected peer in active list to unchoke
		ArrayList<Peer> chokedPeers =  new ArrayList<Peer>();
		if(RUBTToolKit.isSeed(this.client_info.getClient_bitfield())){
			// when only uploading our end needs unchocked
			for(Peer peer: this.connectedPeers){
				if(peer.getLocalStatus() == RUBTConstants.PeerStatus.CHOKED){
					chokedPeers.add(peer);
				}
			}
			//Otherwise, repeat same process with upload rates
		}else{
			// when still downloading their end needs unchocked
			for(Peer peer: this.connectedPeers){
				if(peer.getRemoteStatus() == RUBTConstants.PeerStatus.CHOKED){
					chokedPeers.add(peer);
				}
			}
		}
		//Generate a random peer to unchoke within choked list
		//seed your random
		Random rand = new Random(System.currentTimeMillis());
		final Peer luckyPeer = chokedPeers.get(rand.nextInt(chokedPeers.size()));
		RUBTConstants.main_log.info("Peer choosed by optimistic unchocke is " + luckyPeer.toString());
		this.workers.execute(new Runnable() {
			@Override
			public void run() {
				try {
					if(RUBTToolKit.isSeed(Manager.this.client_info.getClient_bitfield()))
					{
						//open our end
						luckyPeer.setLocalStatus(RUBTConstants.PeerStatus.UNCHOKED);
						luckyPeer.sendMessageToPeer(new UnchokeMessage());
						luckyPeer.peer_log.info("Peer is unchoked in optimistic unchoke!");
					}
					else
					{
						//try to open their end, send interested
						luckyPeer.setRemoteStatus(RUBTConstants.PeerStatus.INTERESTED);
						luckyPeer.peer_log.info("sending interested to peer");
						luckyPeer.sendMessageToPeer(RUBTConstants.INTERESTED_MESSAGE);
					}
					if (!Manager.this.activePeers.contains(luckyPeer))
					{
						Manager.this.activePeers.add(luckyPeer);
					}
				} catch (IOException e) {
					//peer socket is bad disconnect
					Manager.this.connectedPeers.remove(luckyPeer);
					Manager.this.activePeers.remove(luckyPeer);
					luckyPeer.removeMessageListener(Manager.this);
					luckyPeer.disconnect();
				}
			}
		});
	}
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// PEER MANAGEMENT
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	/**
	 * gets the rarest piece the peer has
	 * @param peer
	 * @return
	 */
	protected synchronized int getRarestPiece(Peer peer)
	{
		int[] availability = new int[this.client_info.getNumber_of_pieces()];
		//compute availability
		for (Peer aPeer:connectedPeers)
		{
			availability = RUBTToolKit.recomputeAvailability(availability, aPeer.getBitfield());
		}
		ArrayList<Integer>	rarestPieces = new ArrayList<Integer>();
		for (int pieceIndex = 0; pieceIndex < this.client_info.getNumber_of_pieces(); pieceIndex++)
		{
			if (!this.client_info.getClient_bitfield()[pieceIndex] && peer.getBitfield()[pieceIndex] && !this.pieces_downloading.contains(Integer.valueOf(pieceIndex)))
			{				
				if (rarestPieces.isEmpty())
				{
					//found the first piece in the list the peer has and it has not been downloaded yet
					rarestPieces.add(Integer.valueOf(pieceIndex));
				}
				else if (availability[pieceIndex] < availability[rarestPieces.get(0)])
				{
					//found a piece more rare
					rarestPieces.removeAll(rarestPieces);
					rarestPieces.add(Integer.valueOf(pieceIndex));
				}
			}
		}
		Random r = new Random(System.currentTimeMillis());
		if (rarestPieces.isEmpty())
		{
			return -1;
		}
		else
		{
			//choose a random piece from rarestPieces
			return rarestPieces.get(r.nextInt(rarestPieces.size()));
		}
	}
	/**
	 * return the next available piece index for the peer
	 * @param peer
	 * @return
	 */
	protected synchronized int getPiece(Peer peer)
	{
		//TODO::print availability + bitfield
		for (int pieceIndex = 0; pieceIndex < this.client_info.getNumber_of_pieces(); pieceIndex++)
		{
			if (!this.client_info.getClient_bitfield()[pieceIndex] && peer.getBitfield()[pieceIndex] && !this.pieces_downloading.contains(Integer.valueOf(pieceIndex)))
			{				
				return pieceIndex;
			}
		}
		return -1;
	}
	/**
	 * finds a piece for to download from peer and it starts downloading
	 * synchronized because you dont want 2 or more peers to start downloading same piece
	 * @param peer
	 * @throws IOException 
	 */
	public synchronized void startDownloadingPieceFrom(final Peer peer) throws IOException
	{
		int pieceIndex = -1;
		if (this.client_info.isDebug_mode())
		{
			//get rarest piece
			pieceIndex = this.getRarestPiece(peer);
		}
		else
		{
			//get first piece that peer has
			pieceIndex = this.getPiece(peer);	
		}
		if (pieceIndex != -1) 
		{
			//add piece to queue of currently downloading pieces
			this.pieces_downloading.add(Integer.valueOf(pieceIndex));
			RUBTConstants.main_log.info("piece " + pieceIndex + " in queue. starts being downloaded by " + peer.toString());
			//create a piece object
			int pieceBegin = 0;
			int pieceSize = this.client_info.piece_length;
			if (pieceIndex == (this.client_info.getNumber_of_pieces() - 1))
			{
				pieceSize = this.client_info.file_length - this.client_info.piece_length * pieceIndex;
			}

			//peer will start downloading immediately
			peer.setLocalStatus(RUBTConstants.PeerStatus.REQUESTING);
			//pass the piece to the peer
			peer.setDownloadingPiece(new FilePiece(pieceIndex, pieceBegin, pieceSize));
			peer.downloadPiece();
		}
		else
		{
			peer.peer_log.info("could not find a piece for peer to download");
			//this is a hack
			if (peer.getLocalStatus() != RUBTConstants.PeerStatus.UNCHOKED)
			{
				this.activePeers.remove(peer);
			}
			
			//check if too little active peers
			if (this.activePeers.size() < RUBTConstants.MIN_UNCHOKED_PEERS)
			{
				//unchoke one randomly
				this.runOptimisticUnchoke();
			}
		}
	}
	/**
	 * Sends peers a Have message indicating the piece has been downloaded and verified
	 * @param piece
	 */
	protected void sendHaveToAllPeers(final FilePiece piece)
	{
		this.workers.execute(new Runnable() {
			@Override
			public void run() {
				for (final Peer peer:connectedPeers)
				{
					try {
						peer.peer_log.info("sending have message to peer, piece " + piece.getIndex());
						peer.sendMessageToPeer(new HaveMessage(piece.getIndex()));

					} catch (IOException e) {
						Manager.this.connectedPeers.remove(peer);
						Manager.this.activePeers.remove(peer);
						peer.removeMessageListener(Manager.this);
						peer.disconnect();
					}
				}
			}
		});
	}
	/**
	 * method to copy piece from random access file
	 * @param request
	 * @return
	 */
	public synchronized byte[] copyPieceFromFile(int pieceIndex)
	{
		int pieceBegin = pieceIndex * this.client_info.piece_length;
		int pieceSize = this.client_info.piece_length;
		if (pieceIndex == (this.client_info.getNumber_of_pieces() - 1))
		{
			pieceSize = this.client_info.file_length - pieceBegin;
		}

		byte[] piece = new byte[pieceSize];
		try {
			client_info.getFile_to_download().seek(pieceBegin);
			client_info.getFile_to_download().readFully(piece);
		} catch (IOException e) {
			RUBTConstants.main_log.severe("Error seeking in file_to_download!");
			e.printStackTrace();
		}
		RUBTConstants.main_log.info("successfully copied from file, piece " + pieceIndex);
		return piece;
	}
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// RUBTClient Listener methods
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	/**
	 * user has typed quit
	 */
	@Override
	public void userRequestedShutdown() {
		//shutdown application
		this.doShutdown();
	}
	/**
	 * user has typed status
	 */
	@Override
	public void userRequestedStatus() {
		//print status
		int percentComplete = 100 - (int)(((double)this.client_info.getClient_stats().getLeft()/(double)this.client_info.file_length) * 100);
		System.out.println("Percent Completed: " + percentComplete + "%");
		System.out.println("Total peers: " + this.connectedPeers.size());
		System.out.println("Active peers: " + this.activePeers.size());
	}
	/**
	 * 	INTERFACE PEER MANAGEMENT METHODS
	 */
	@Override
	public void peerReceivedInterested(final Peer peer)
	{
		if (!this.activePeers.contains(peer))
		{
			if (this.activePeers.size() >= RUBTConstants.MAX_UNCHOKED_PEERS)
			{
				return;
			}
			this.activePeers.add(peer);
		}
		peer.setLocalStatus(RUBTConstants.PeerStatus.UNCHOKED);
		this.workers.execute(new Runnable() {
			@Override
			public void run() {
				try {
					peer.peer_log.info("unchoke peer");
					peer.sendMessageToPeer(RUBTConstants.UNCHOKE_MESSAGE);
				} catch (IOException e) {
					Manager.this.connectedPeers.remove(peer);
					Manager.this.activePeers.remove(peer);
					peer.removeMessageListener(Manager.this);
					peer.disconnect();
				}
			}
		});
	}
	@Override
	public void peerChokedWhileDownloading(final Peer peer) {
		//remove peer's downloading piece from queue
		RUBTConstants.main_log.info("piece " + peer.getDownloadingPiece().getIndex() + " out of queue. " + peer.toString() + " was choked.");
		this.pieces_downloading.remove(Integer.valueOf(peer.getDownloadingPiece().getIndex()));

		//downloading piece of peer is null
		peer.setDownloadingPiece(null);
		
		//this is a hack
		if (peer.getLocalStatus() != RUBTConstants.PeerStatus.UNCHOKED)
		{
			this.activePeers.remove(peer);
		}
		
		//check if too little active peers
		if (this.activePeers.size() < RUBTConstants.MIN_UNCHOKED_PEERS)
		{
			//unchoke one randomly
			this.runOptimisticUnchoke();
		}
	}
	@Override
	public void peerFinishedDownloading(final Peer peer)
	{
		//start downloading a new piece
		this.workers.execute(new Runnable() {
			@Override
			public void run() {
				//check piece
				try {
					if (RUBTToolKit.verifySHA1(peer.getDownloadingPiece().getDownloaded(), client_info.piece_hashes[peer.getDownloadingPiece().getIndex()].array()))
					{
						//write piece to file
						RUBTConstants.main_log.info("passed sha-1 verification, trying to write to file piece " + peer.getDownloadingPiece().getIndex());
						if (client_info.writePiece(peer.getDownloadingPiece()))
						{
							//wake tracker and send complete to tracker
							tracker.setEvent(RUBTConstants.EVENT_COMPLETED);
							tracker.interrupt();
						}

						//send have to all
						sendHaveToAllPeers(peer.getDownloadingPiece());
					}
				} catch (NoSuchAlgorithmException e) {
					// leave for debug
					RUBTConstants.main_log.info("error in sha-1 algorithm " + e.getStackTrace().toString());
				}
				//remove piece index from queue
				RUBTConstants.main_log.info("piece " + peer.getDownloadingPiece().getIndex() + " out of queue");
				pieces_downloading.remove(Integer.valueOf(peer.getDownloadingPiece().getIndex()));
				//downloading piece of peer is null
				peer.setDownloadingPiece(null);
				try {
					startDownloadingPieceFrom(peer);
				} catch (IOException e) {
					Manager.this.connectedPeers.remove(peer);
					Manager.this.activePeers.remove(peer);
					peer.removeMessageListener(Manager.this);
					peer.disconnect();
				}
			}
		});
	}
	@Override
	public void peerCanStartDownloading(final Peer peer) {
		//start downloading a new piece
		this.workers.execute(new Runnable() {
			@Override
			public void run() {
				try {
					startDownloadingPieceFrom(peer);
				} catch (IOException e) {
					Manager.this.connectedPeers.remove(peer);
					Manager.this.activePeers.remove(peer);
					peer.removeMessageListener(Manager.this);
					peer.disconnect();
				}
			}
		});
	}
	@Override
	public void peerReceivedHaveMessage(final Peer peer, HaveMessage haveMessage) 
	{
		if (!this.activePeers.contains(peer))
		{
			if (this.activePeers.size() >= RUBTConstants.MAX_UNCHOKED_PEERS)
			{
				return;
			}
			this.activePeers.add(peer);
		}
		//print the 2 bitfields
		peer.peer_log.info(RUBTToolKit.getBitfieldString(peer.getBitfield())+ " -> peers bitfield");
		peer.peer_log.info(RUBTToolKit.getBitfieldString(this.client_info.getClient_bitfield()) + " -> clients bitfield");
		//if piece missing, send interested
		if (!this.client_info.getClient_bitfield()[haveMessage.getPieceIndex()])
		{
			peer.peer_log.info("peer has pieces the client needs");
			peer.setRemoteStatus(RUBTConstants.PeerStatus.INTERESTED);
			this.workers.execute(new Runnable() {
				@Override
				public void run() {
					try {
						peer.peer_log.info("sending interested to peer");
						peer.sendMessageToPeer(RUBTConstants.INTERESTED_MESSAGE);
					} catch (IOException e) {
						Manager.this.connectedPeers.remove(peer);
						Manager.this.activePeers.remove(peer);
						peer.removeMessageListener(Manager.this);
						peer.disconnect();
					}
				}

			});
		}
	}
	@Override
	public void peerReceivedBitfieldMessage(final Peer peer) {
		if (!this.activePeers.contains(peer))
		{
			if (this.activePeers.size() >= RUBTConstants.MAX_UNCHOKED_PEERS)
			{
				return;
			}
			this.activePeers.add(peer);
		}
		//print the 2 bitfields
		peer.peer_log.info(RUBTToolKit.getBitfieldString(peer.getBitfield())+ " -> peers bitfield");
		peer.peer_log.info(RUBTToolKit.getBitfieldString(this.client_info.getClient_bitfield()) + " -> clients bitfield");
		//if piece missing, send interested
		if (RUBTToolKit.checkArraysDifference(peer.getBitfield(), this.client_info.getClient_bitfield()))
		{
			peer.peer_log.info("peer has pieces the client needs");
			peer.setRemoteStatus(RUBTConstants.PeerStatus.INTERESTED);
			this.workers.execute(new Runnable() {
				@Override
				public void run() {
					try {
						peer.peer_log.info("sending interested to peer");
						peer.sendMessageToPeer(RUBTConstants.INTERESTED_MESSAGE);
					} catch (IOException e) {
						Manager.this.connectedPeers.remove(peer);
						Manager.this.activePeers.remove(peer);
						peer.removeMessageListener(Manager.this);
						peer.disconnect();
					}
				}
			});
		}
	}
	@Override
	public void peerRequestedNewPiece(final Peer peer, final RequestMessage requestMessage ) 
	{
		RUBTConstants.main_log.info("peer " + peer.toString() + " requested piece " + requestMessage.getRequest().getIndex());
		//check if the client has the requested piece
		if (!this.client_info.getClient_bitfield()[requestMessage.getRequest().getIndex()])
		{
			peer.peer_log.info("client does not have piece " + requestMessage.getRequest().getIndex() + " which peer requested");
			return;
		}
		peer.peer_log.info("client has piece " + requestMessage.getRequest().getIndex() + " which peer requested");
		this.workers.execute(new Runnable() {
			@Override
			public void run() {
				//set uploading piece for the peer
				peer.setUploadingPiece(new FilePiece(requestMessage.getRequest(), copyPieceFromFile(requestMessage.getRequest().getIndex())));
				//start uploading
				peer.uploadPiece(requestMessage.getRequest());
			}

		});
	}
	/**
	 * tracker has extracted a new list of peers
	 */ 
	@Override
	public void onlinePeersListReady(final ArrayList<Peer> onlinePeers) {
		RUBTConstants.main_log.info("in online peers is ready");
		this.workers.execute(new Runnable() {
			@Override
			public void run() {
				if (!onlinePeers.isEmpty())
				{
					for (Peer peer:onlinePeers)
					{
						addPeer(peer);  
					}
				}
				else
				{
					RUBTConstants.main_log.info("Online Peers list is empty!");
				}
			}
		});
	}
	@Override
	public void peerNeedsDisconnected(final Peer peer)
	{
		Manager.this.connectedPeers.remove(peer);
		Manager.this.activePeers.remove(peer);
		peer.removeMessageListener(Manager.this);
		peer.disconnect();
	}
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// ADDING A PEER TO THE LIST OF CONNECTED PEERS
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	/**
	 * method used to add peers provided by the tracker
	 * @param newPeer
	 * 				is a peer that is not connected yet
	 * 				configured with the information from tracker only: id, ip, port
	 */
	protected synchronized void addPeer(final Peer newPeer) {
		// Check to see if this peer is already known
		Peer oldPeer = findDuplicate(newPeer);
		// If we already have this peer in our list, then check to make sure
		// it's still live
		if (oldPeer != null) {
			if (this.testPeer(oldPeer)) {
				// Old peer is fine, so discard the new one
				newPeer.peer_log.info("peer is duplicate");
				newPeer.disconnect();
				return;
			}
		}
		// If null, then an exception was thrown
		if (newPeer == null) {
			return;
		}
		// Connect the socket to the remote peer, discard the peer on errors
		try {
			newPeer.peer_log.info("trying to connect peer");
			newPeer.connect();
		} catch (IOException ioe) {
			RUBTConstants.main_log.severe("Unable to connect to " + newPeer.toString() + ": " + ioe.getMessage());
			newPeer.disconnect();
			return;
		}
		// Try to handshake
		//if successful, register peer and add in queue of connected peers
		if (newPeer.performHandshake(this.client_info.info_hash.array())) 
		{
			//send bitfield to peer
			try {
				if (client_info.hasPieces())
				{
					newPeer.peer_log.info("sending bitfield to peer: " + RUBTToolKit.getBitfieldString(client_info.getClient_bitfield()));
					newPeer.sendMessageToPeer(new BitfieldMessage(ToolKit2.convert(client_info.getClient_bitfield())));
				}
			} catch (IOException e) {
				// failed to send bitfield
				newPeer.peer_log.severe("peer failed to send bitfield");
			}
			//register and adding peer to connected list
			this.registerPeer(newPeer);
			this.connectedPeers.add(newPeer);
		}
		else
		{
			newPeer.peer_log.severe("peer failed to handshake");
		}
	}
	/**
	 * method to register peer to Manager
	 * @param peer
	 * 				newly connected peer 
	 */
	protected void registerPeer(final Peer peer) {
		RUBTConstants.main_log.info("Registering peer: " + peer);
		peer.addMessageListener(this);
		peer.start();
	}
	/**
	 * method used to add peers provided by the listening server thread
	 * @param newPeerSocket
	 * 				is a socket peer connection established by the server thread
	 */
	protected synchronized void addPeer(final Socket newPeerSocket) 
	{
		final Peer newPeer = new Peer(newPeerSocket, this.client_info.getNumber_of_pieces(), this.client_info.isDebug_mode());
		// Need to handshake first since we need to get the remote port info
		// before checking for duplicates
		if (!newPeer.performHandshake(this.client_info.info_hash.array())) {
			newPeer.disconnect();
			return;
		}
		//send bitfield to peer
		try {
			//send bitfield to peer
			if (client_info.hasPieces())
			{
				newPeer.peer_log.info("sending bitfield to peer: " + RUBTToolKit.getBitfieldString(client_info.getClient_bitfield()));
				newPeer.sendMessageToPeer(new BitfieldMessage(ToolKit2.convert(client_info.getClient_bitfield())));
			}
		} catch (IOException e) {
			// failed to send bitfield
			newPeer.peer_log.severe("peer failed to send bitfield");
		}
		newPeer.peer_log.severe("peer failed to handshake");
		// Try to find an old version of this client_info.torrentInfo (same IP/port)
		Peer oldPeer = findDuplicate(newPeer);
		// If we already have this peer in our list, then check to make sure
		// it's still live
		if (oldPeer != null) {
			if (this.testPeer(oldPeer)) {
				// Old peer is fine, so discard the new one
				newPeer.disconnect();
				return;
			}
		}
		// Register the new peer
		this.registerPeer(newPeer);
		// Add the peer to the list of connected peers
		this.connectedPeers.add(newPeer);
	}
	/**
	 * checks to see if another peer with the same id is connected
	 * @param searchPeer
	 * 					peer to search for
	 * @return
	 * 					duplicate of the search peer already connected
	 */
	protected Peer findDuplicate(Peer searchPeer) {
		for (Peer peer: this.connectedPeers) {
			if (Arrays.equals(peer.getRemoteId(), searchPeer.getRemoteId())) 
			{
				return peer;
			}
		}
		return null;
	}
	/**
	 * Tests a peer for liveness by sending a Keep-Alive message. If the send
	 * fails, then the client_info.torrentInfo is disconnected, removed from the list of connected peers.
	 * 
	 * @param peer
	 *            the peer to test
	 * @return true if the test succeeds, else false
	 */
	protected boolean testPeer(Peer peer) {
		try {
			peer.sendMessageToPeer(RUBTConstants.KEEPALIVE_MESSAGE);
			return true;
		} catch (IOException ioe) {
			this.connectedPeers.remove(peer);
			peer.removeMessageListener(this);
			peer.disconnect();
		}
		return false;
	}
}
