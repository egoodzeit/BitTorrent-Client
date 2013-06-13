/**
 * @author Marinela G. Haldeman
 * @author Elliot Goodzeit
 * @author Aditya Sai
 */
package cs352.RUBTClient.Group06;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import cs352.RUBTClient.RobertMoore.*;
import cs352.RUBTClient.Group06.Message.*;

/**
 * @author Marinela G. Haldeman
 * @author Elliot Goodzeit
 * @author Aditya Sai
 * 699 lines of code
 */
public class Peer extends Thread{
	/**
	 * Logger for this peer
	 */
	protected final Logger peer_log;
	/**
	 * stats log per peer
	 */
	protected final Logger peer_stats;
	/**
	 * time when last message was sent
	 */
	protected long timeStampLastMessage;
	/**
	 * communication line between peer and client
	 */
	private final Socket					socket;
	/**
	 * stats and info remote side of the client socket
	 */
	private long 							downloaded;
	private long 							uploaded;
	private byte[]   						remoteId;
	private String   						remoteIp;
	private int      						remotePort;
	private byte[]							remoteInfoHash;
	private RUBTConstants.PeerStatus		remoteStatus;
	/**
	 * local status
	 */
	private RUBTConstants.PeerStatus		localStatus;
	/**
	 * piece being downloaded
	 */
	private FilePiece 						downloadingPiece;
	/**
	 * piece being uploaded
	 */
	private FilePiece						uploadingPiece;
	/**
	 * Flag to shutdown the peer
	 */
	protected boolean 						keepRunning = true;
	/**
	 * bitarray to hold the bitfield for the peer 
	 */
	private  boolean[] 						peer_bitfield;
	/**
	 * Collection of MessageListener interfaces that should be notified of
	 * received messages.
	 */
	protected final Collection<RUBTClientListener> listeners = new ConcurrentLinkedQueue<RUBTClientListener>();
	/**
	 * Thread pool for handling the peers messaging and the tracker updates
	 */
	protected final ExecutorService workers = Executors.newCachedThreadPool();
	/**
	 * creates new peer 
	 * used in tracker
	 * @param remoteIp
	 * @param remoteId
	 * @param remotePort
	 */
	public Peer(String remoteIp, byte[] remoteId, int remotePort, int bitfieldSize, boolean debug_mode)
	{
		//set fields
		this.remoteId = remoteId;
		this.remoteIp = remoteIp;
		this.remotePort = remotePort;

		//create socket
		this.socket = new Socket();

		//init infohash
		this.setRemoteInfoHash(new byte[20]);
		//init bitfield 
		this.peer_bitfield = new boolean[bitfieldSize];

		//starting with chocked on both sides
		this.setLocalStatus(RUBTConstants.PeerStatus.CHOKED);
		this.setRemoteStatus(RUBTConstants.PeerStatus.CHOKED);

		//for debug purposes
		//set thread name as remote ip and port number
		this.setName(this.getRemoteIp() + "-" + this.getRemotePort());

		//reset stats of peer
		this.resetStats();

		//set activity logger
		peer_log = Logger.getLogger(Peer.this.getName() + "_logs");
		//check debug mode
		if (debug_mode)
		{
			//get a logger
			peer_log.setLevel(Level.ALL);

			//add a custom handler to it
			try {
				Handler fileHandler = new FileHandler(Peer.this.getName() + "_logs.txt", debug_mode);
				fileHandler.setLevel(Level.ALL);
				fileHandler.setFormatter(new CustomRecordFormatter());
				peer_log.addHandler(fileHandler);
			} catch (Exception e) {
				System.err.println("Unable to create peer log file: " + e.getMessage());
			}
		}

		//set stats logger
		peer_stats = Logger.getLogger(Peer.this.getName() + "_stats");
		if (debug_mode)
		{
			//get a logger
			peer_stats.setLevel(Level.ALL);

			//add a custom handler to it
			try {
				Handler fileHandler = new FileHandler(Peer.this.getName() + "_stats.txt", debug_mode);
				fileHandler.setLevel(Level.ALL);
				fileHandler.setFormatter(new CustomRecordFormatter());
				peer_stats.addHandler(fileHandler);
			} catch (Exception e) {
				System.err.println("Unable to create peer log file: " + e.getMessage());
			}
		}
		peer_stats.info("down\t\tup");
		//start timer for keepalive
		this.timeStampLastMessage = System.currentTimeMillis();
	}
	/**
	 * creates a new peer connected to the socket
	 * @param socket
	 */
	public Peer(Socket socket, int bitfieldSize, boolean debug_mode)
	{
		//set fields
		this.socket = socket;

		//get the remote ip and port
		StringTokenizer tokenizer = new StringTokenizer(socket.getInetAddress().toString());
		this.remoteIp = tokenizer.nextToken("/");
		this.remotePort = socket.getPort();

		//init unknown fields
		this.setRemoteInfoHash(new byte[20]);
		this.remoteId = new byte[20];
		this.peer_bitfield = new boolean[bitfieldSize];

		//starting with chocked on both sides
		this.setLocalStatus(RUBTConstants.PeerStatus.CHOKED);
		this.setRemoteStatus(RUBTConstants.PeerStatus.CHOKED);

		//for debug purposes
		//set thread name as remote ip and port number
		this.setName(this.getRemoteIp() + "-" + this.getRemotePort());

		//set logger
		peer_log = Logger.getLogger(Peer.this.getName());
		//check debug mode
		if (debug_mode)
		{
			//get a logger
			peer_log.setLevel(Level.ALL);

			//add a custom handler to it
			try {
				Handler fileHandler = new FileHandler(Peer.this.getName() + ".txt", debug_mode);
				fileHandler.setLevel(Level.ALL);
				fileHandler.setFormatter(new CustomRecordFormatter());
				peer_log.addHandler(fileHandler);
			} catch (Exception e) {
				System.err.println("Unable to create log file: " + e.getMessage());
			}
		}

		//set stats logger
		peer_stats = Logger.getLogger(Peer.this.getName() + "_stats");
		if (debug_mode)
		{
			//get a logger
			peer_stats.setLevel(Level.ALL);

			//add a custom handler to it
			try {
				Handler fileHandler = new FileHandler(Peer.this.getName() + "_stats.txt", debug_mode);
				fileHandler.setLevel(Level.ALL);
				fileHandler.setFormatter(new CustomRecordFormatter());
				peer_stats.addHandler(fileHandler);
			} catch (Exception e) {
				System.err.println("Unable to create peer log file: " + e.getMessage());
			}
		}
		peer_stats.info("down\t\tup");

		//start timer for keepalive
		this.timeStampLastMessage = System.currentTimeMillis();

		//set a timeout of 2 seconds on the reads for the socket
		try {
			this.socket.setSoTimeout(2000);
		} catch (SocketException e1) {
			// leave it to debug
			peer_log.severe("could not set a 2 seconds timeout on the peer's socket");
		}
	}
	/**
	 * used to connect to a peer provided by the tracker
	 * @throws IOException
	 */
	public void connect() throws IOException {
		//try to connect the socket with a timeout of 2 seconds
		if (this.socket != null && !this.socket.isConnected()) {
			this.socket.connect(new InetSocketAddress(this.remoteIp, this.remotePort), 2000);
		}
		//set a timeout of 2 seconds on the read for the socket
		this.socket.setSoTimeout(2000);
	}
	@Override
	public void run() {
		RUBTConstants.main_log.info("Peer is up");
		peer_log.info("Peer is up");
		while (this.keepRunning) {
			if ((System.currentTimeMillis() - this.timeStampLastMessage) >= RUBTConstants.INTERVAL_TO_SEND_KEEPALIVE)
			{
				try {
					this.sendMessageToPeer(RUBTConstants.KEEPALIVE_MESSAGE);
				} catch (IOException e) {
					this.keepRunning = false;
					peer_log.warning(this  + "Caught exception while sending keepalive to peer: " + e.getMessage());
				}
			}
			//listen for messages
			try {
				Message message = null;
				try {
					message = Message.decodeMessage(this.socket.getInputStream());
				} catch (IOException e) {
					// leave it to debug
					peer_log.warning("socket timed out");
				}

				if (message == null) {
					// Nothing received, try again?
					try {
						Thread.sleep(5);
					} catch (InterruptedException ie) {
						// Ignore interrupts
					}
					continue;
				}
				//keepalive, don't do anything
				if (message instanceof KeepAliveMessage)
				{
					//upon keepAlive do nothing
					peer_log.info("received keepalive");
					continue;
				}
				switch (message.getType()) {
				case RUBTConstants.TYPE_CHOKE_MESSAGE: 
				{
					peer_log.info("received choked");
					//remote end is chocked
					this.setRemoteStatus(RUBTConstants.PeerStatus.CHOKED);

					//check if downloading
					if (this.downloadingPiece != null)
					{
						peer_log.info("received choked while downloading");
						//let manager know chocked while downloading
						for (RUBTClientListener listener : Peer.this.listeners) {
							listener.peerChokedWhileDownloading(Peer.this);
						}
					}
					break;
				}
				case RUBTConstants.TYPE_UNCHOKE_MESSAGE:
				{
					this.setRemoteStatus(RUBTConstants.PeerStatus.UNCHOKED);
					peer_log.info("received unchocke, announce manager that peer can start downloading");
					for (RUBTClientListener listener : Peer.this.listeners) {
						listener.peerCanStartDownloading(Peer.this);
					}
					break;
				}
				case RUBTConstants.TYPE_INTERESTED_MESSAGE:
				{
					//if I have the piece, then send unchocke
					//got interested from peer
					peer_log.info("received interested, ask manager what to do");
					for (RUBTClientListener listener : Peer.this.listeners) {
						listener.peerReceivedInterested(Peer.this);
					}
					break;
				}
				case RUBTConstants.TYPE_REQUEST_MESSAGE:
				{
					RequestMessage requestMessage = (RequestMessage) message;
					peer_log.info("received request " + requestMessage.getRequest().toString());
					//check if the current uploading piece is the same as the requested piece
					if (this.getLocalStatus() != RUBTConstants.PeerStatus.UNCHOKED)
					{
						peer_log.info("peer is chocked, cant send request");
						break;
					}

					//check if the piece requested is the one buffered
					if ((this.uploadingPiece == null) || (requestMessage.getRequest().getIndex() != this.uploadingPiece.getIndex()))
					{
						//pass them to the manager
						peer_log.info("piece requested is diff than the one buffered, need to contact manager for the new piece");
						for (RUBTClientListener listener : Peer.this.listeners) {
							listener.peerRequestedNewPiece(Peer.this, 
									requestMessage);
						}
					}
					else
					{
						peer_log.info("piece requested is same as the one buffered, send piece to piece");
						this.uploadPiece(requestMessage.getRequest());
					}
					break;
				}
				case RUBTConstants.TYPE_UNINTERESTED_MESSAGE:
				{
					//not used yet
					break;
				}
				case RUBTConstants.TYPE_HAVE_MESSAGE:
				{
					//add piece to bitfield and announce manager
					HaveMessage haveMessage = (HaveMessage) message;
					peer_log.info("received have piece " + haveMessage.getPieceIndex());
					this.peer_bitfield[haveMessage.getPieceIndex()] = true;
					if (this.getRemoteStatus() != RUBTConstants.PeerStatus.INTERESTED)
					{
						//notify manager
						for (RUBTClientListener listener : Peer.this.listeners) {
							peer_log.info("notify manager of have pieces");
							listener.peerReceivedHaveMessage(Peer.this, haveMessage);
						}
					}
					break;
				}
				case RUBTConstants.TYPE_BITFIELD_MESSAGE:
				{
					//set bitfield and announce manager
					peer_log.info("received bitfield");
					BitfieldMessage bitfieldMessage = (BitfieldMessage) message;
					this.setBitfield(bitfieldMessage.getBitfield());
					if (this.getRemoteStatus() != RUBTConstants.PeerStatus.INTERESTED)
					{
						for (RUBTClientListener listener : Peer.this.listeners) {
							peer_log.info("notify manager of bitfield");
							listener.peerReceivedBitfieldMessage(Peer.this);
						}
					}
					break;
				}
				case RUBTConstants.TYPE_PIECE_MESSAGE:
				{
					//copy downloaded bytes to piece
					PieceMessage pieceMessage = (PieceMessage) message;
					peer_log.info("received piece " + pieceMessage.getRequestPiece().toString());
					if ((this.downloadingPiece == null) || (this.remoteStatus != RUBTConstants.PeerStatus.UNCHOKED))
					{
						peer_log.info("peer is not currently downloading any piece. downloading piece is NULL");
						//break out of the switch case
						break;
					}
					//peer is currently downloading a piece
					this.downloadingPiece.copyToFilePiece(pieceMessage.getRequestPiece());
					//if completed piece announce manager
					if (this.downloadingPiece.isComplete())
					{
						//announce the manager that the piece has been completely downloaded
						peer_log.info("peer finished downloading the piece");
						for (RUBTClientListener listener : Peer.this.listeners) {
							listener.peerFinishedDownloading(Peer.this);
						}
					}
					else
					{
						//request again
						this.downloadPiece();
					}
					break;
				}
				// Error handling
				default:
					RUBTConstants.main_log.warning("Unknown message type when decoding!");
					break;
				}
			} catch (Exception e) {
				// leave it for debug
				// something went wrong ask the manager to disconnect peer
				peer_log.info("peer got exception in run; peer will request manager to disconnect");
				for (RUBTClientListener listener : Peer.this.listeners) {
					listener.peerNeedsDisconnected(Peer.this);
				}
			}
		}
		RUBTConstants.main_log.info("Peer is down");
		peer_log.info("Peer is down");
	}
	/**
	 * method to request a piece from peer
	 * @throws IOException
	 */
	public  void downloadPiece()
	{
		int requestSize = RUBTConstants.REQUEST_PIECE_SIZE;
		if (this.downloadingPiece.getSize() - this.downloadingPiece.getBegin() < requestSize)
		{
			//check if the request size is smaller
			requestSize = this.downloadingPiece.getSize() - this.downloadingPiece.getBegin();
		}
		final RequestMessage requestMessage = new RequestMessage(new Request(this.downloadingPiece.getIndex(), this.downloadingPiece.getBegin(), requestSize));
		peer_log.info("->downloading piece " + requestMessage.getRequest().toString());
		this.downloaded += requestSize;
		peer_stats.info(this.downloaded + "\t\t\t" + this.uploaded);
		//sendMessageToPeer(requestMessage);
		this.workers.execute(new Runnable() {
			@Override
			public void run() {
				try {
					sendMessageToPeer(requestMessage);
				} catch (IOException e) {
					// socket is bad, ask the manager to disconnect peer
					peer_log.info("peer got IO exception while dowloading a new piece; peer will request manager to disconnect");
					for (RUBTClientListener listener : Peer.this.listeners) {
						listener.peerNeedsDisconnected(Peer.this);
					}
				}
			}
		});

	}
	public void uploadPiece(Request request) 
	{
		final PieceMessage pieceMessage = new PieceMessage(new RequestPiece(request, this.uploadingPiece.copyFromFilePiece(request) ));
		//Increment upload statistic accordingly
		//this.client_info.getClient_stats().addUploaded(requestMessage.getRequest().getSize());
		peer_log.info("<-uploading piece " + request.toString());
		this.uploaded += request.getSize();
		peer_stats.info(this.downloaded + "\t\t\t" + this.uploaded);
		this.workers.execute(new Runnable() {
			@Override
			public void run() {
				try {
					sendMessageToPeer(pieceMessage);
				} catch (IOException e) {
					// socket is bad, ask the manager to disconnect peer
					peer_log.info("peer got IO exception while sending unchoke; peer will request manager to diconnect");
					for (RUBTClientListener listener : Peer.this.listeners) {
						listener.peerNeedsDisconnected(Peer.this);
					}
				}
			}
		});
	}
	/**
	 * Sends a message to this peer
	 * 
	 * @throws IOException
	 *             if an IOException is thrown when writing the message.
	 */
	public synchronized void sendMessageToPeer(Message message)throws IOException {
		this.timeStampLastMessage = System.currentTimeMillis();
		Message.encodeMessage(message, this.socket.getOutputStream());
	}
	/**
	 * Causes this peer to close its socket and kill any running threads it
	 * may have started.
	 */
	public synchronized void disconnect() 
	{
		this.keepRunning = false;

		peer_log.info("trying to disconnect peer");
		// Close the socket if it isn't already closed
		if (this.socket != null && !this.socket.isClosed()) {
			try {
				this.socket.close();
			} catch (IOException e) {
				// Ignored, since we're closing anyways
				peer_log.warning("error in closing peer's socket");
			}
		} else {
			// This shouldn't happen, but print an error message just in case
			peer_log.warning("Already disconnected?");
		}

		this.workers.shutdown();
		peer_log.info("Peer Thread pool is down");

		peer_log.info("closing activity and stats logs");
		//close file handlers
		peer_log.getHandlers()[0].close();
		peer_stats.getHandlers()[0].close();
	}
	/**
	 * performs handshake with the peers
	 * @return
	 */
	public synchronized boolean performHandshake(byte[] client_info_hash) {
		peer_log.info("Trying to perform handshake exchange with " + this);
		HandshakeInfo newHI = new HandshakeInfo(client_info_hash, RUBTConstants.CLIENT_ID);
		// Try to create an outgoing handshake message
		HandshakeMessage sentMessage = new HandshakeMessage(newHI);
		try {
			// Try to encode the handshake onto this client's output stream
			HandshakeMessage.encodeHandshake(sentMessage,this.socket.getOutputStream());
		} catch (IOException e) {
			peer_log.warning("Unable to send handshake message: " + e.getMessage());
			return false;
		}
		HandshakeMessage receivedMessage = null;
		// Keep reading messages until a HandshakeMessage is received
		do {
			try {
				receivedMessage = HandshakeMessage.decodeHandshake(this.socket.getInputStream());
				// Either no message available, or a decoding error
				if (receivedMessage == null) {
					// Allow other threads to issue before trying again
					Thread.yield();
					continue;
				}
			} catch (IOException e) {
				peer_log.warning("Unable to read handshake from remote client: " + e.getMessage());
				return false;
			}

		}
		// Keep looping until we've received a handshake from the client
		while (receivedMessage == null);
		//configure remote side
		this.setRemoteId(receivedMessage.getHandshakeInfo().getId());
		this.setRemoteInfoHash(receivedMessage.getHandshakeInfo().getHash());
		return true;
	}
	/**
	 * convenience method to reset the stats for the peer
	 */
	public void resetStats()
	{
		this.downloaded = 0;
		this.uploaded = 0;
	}
	/**
	 * add listener for this thread
	 * @param listener
	 */
	public void addMessageListener(final RUBTClientListener listener) {
		this.listeners.add(listener);
	}
	/**
	 * remove listener for this thread
	 * @param listener
	 */
	public void removeMessageListener(final RUBTClientListener listener) {
		this.listeners.remove(listener);
	}
	/**
	 * convinience method to print peer
	 */
	@Override
	public String toString()
	{
		return "ID: " + new String(this.remoteId) + " IP: " + this.remoteIp + " PORT: " + this.remotePort;
	}
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// GET-ERS AND SET-ERS
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public byte[] getRemoteInfoHash() {
		return remoteInfoHash;
	}
	public void setRemoteInfoHash(byte[] remoteInfoHash) {
		this.remoteInfoHash = remoteInfoHash;
	}
	public RUBTConstants.PeerStatus getRemoteStatus() {
		return remoteStatus;
	}
	public void setRemoteStatus(RUBTConstants.PeerStatus remoteStatus) {
		this.remoteStatus = remoteStatus;
	}
	public RUBTConstants.PeerStatus getLocalStatus() {
		return localStatus;
	}
	public void setLocalStatus(RUBTConstants.PeerStatus localStatus) {
		this.localStatus = localStatus;
	}
	private void setRemoteId(byte[] remoteId) {
		this.remoteId = remoteId;
	}
	public void setBitfield(byte[] bitfieldPayload)
	{
		this.peer_bitfield = ToolKit2.convert(bitfieldPayload,this.peer_bitfield.length);

	}
	public boolean[] getBitfield()
	{
		return this.peer_bitfield;
	}
	public synchronized void setDownloadingPiece(FilePiece piece)
	{
		this.downloadingPiece = piece;	
	}
	public synchronized void setUploadingPiece(FilePiece piece) 
	{
		this.uploadingPiece = piece;	
	}
	public byte[] getRemoteId()
	{
		return this.remoteId;
	}
	public FilePiece getDownloadingPiece()
	{
		return this.downloadingPiece;
	}
	public FilePiece getUploadingPiece()
	{
		return this.uploadingPiece;
	}
	public void setHasPiece(int index, boolean hasPiece){
		if(index < 0 || index >= this.peer_bitfield.length){
			return;
		}
		this.peer_bitfield[index] = hasPiece;
	}
	public String getRemoteIp()
	{
		return this.remoteIp;
	}
	public int getRemotePort()
	{
		return this.remotePort;
	}
	public long getDownloaded()
	{
		return this.downloaded;
	}
	public long getUploaded()
	{
		return this.uploaded;
	}
	public void setUploaded(long uploaded){
		this.uploaded = uploaded;
	}
	public void setDownloaded(long downloaded){
		this.downloaded = downloaded;
	}
}
