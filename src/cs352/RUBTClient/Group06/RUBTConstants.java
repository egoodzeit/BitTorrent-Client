package cs352.RUBTClient.Group06;
/**
 * @author Marinela G. Haldeman
 * @author Elliot Goodzeit
 * @author Aditya Sai
 * 313 lines of code
 */
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import cs352.RUBTClient.RobertMoore.*;
import cs352.RUBTClient.Group06.Message.ChokeMessage;
import cs352.RUBTClient.Group06.Message.InterestedMessage;
import cs352.RUBTClient.Group06.Message.KeepAliveMessage;
import cs352.RUBTClient.Group06.Message.UnchokeMessage;
import cs352.RUBTClient.Group06.Message.UninterestedMessage;

public class RUBTConstants {
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// 
	// CONSTANTS
	//
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	/**
	 * date formatter
	 */
	protected final static DateFormat date_format = new SimpleDateFormat("HH:mm:ss");
	/**
	 * Logger for the entire application
	 */
	protected final static Logger main_log = Logger.getLogger("CLIENT_ACTIVITY_LOGGER");
	/**
	 * Logger for the stats
	 */
	protected final static Logger stats_log = Logger.getLogger("CLIENT_STATS_LOGGER");
	/**
	 * stats filename
	 */
	protected final static String STATS_LOG_FILENAME = "RUBTClient_STATS.txt";
	/**
	 * activity log filename
	 */
	protected final static String ACTIVITY_LOG_FILENAME = "RUBTClient_ACTIVITY.txt";
	/**
	 * removes all the existing log handlers
	 * add custom file handlers 
	 */
	static {
		//remove all handlers including the console handler
		for(Handler handler:main_log.getParent().getHandlers())
		{
			main_log.getParent().removeHandler(handler);
		}
		//add file handler for the main activity log
		RUBTConstants.main_log.setLevel(Level.ALL);

		//add a custom handler to it
		try {
			Handler fileHandler = new FileHandler(ACTIVITY_LOG_FILENAME, false);
			fileHandler.setLevel(Level.ALL);
			fileHandler.setFormatter(new CustomRecordFormatter());
			RUBTConstants.main_log.addHandler(fileHandler);
		} catch (Exception e) {
			System.err.println("Unable to create log file: " + e.getMessage());
		}

		//add a file handler for the stats log
		RUBTConstants.stats_log.setLevel(Level.ALL);

		//set file handler for the stats logger
		try {
			//go with appending to the existing stats file
			Handler fileHandler = new FileHandler(STATS_LOG_FILENAME, true);
			fileHandler.setLevel(Level.ALL);
			fileHandler.setFormatter(new CustomRecordFormatter());
			RUBTConstants.stats_log.addHandler(fileHandler);
		} catch (Exception e) {
			System.err.println("Unable to create stats log file: " + e.getMessage());
		}
	} 
	/**
	 * holds the IPS OF THE RUBT PEERS
	 */
	public final static ArrayList<String> RUBT_PEERS_IPS = new ArrayList<String>();
	static {
		RUBT_PEERS_IPS.add("128.6.5.130");
		RUBT_PEERS_IPS.add("128.6.5.131");
	}
	/**
	 * holds the randomly generated client id
	 */
	public final static byte[] CLIENT_ID;
	/**
	 * Socket for accepting incoming connections.
	 */
	protected final static ServerSocket SERVER_SOCKET;
	static{
		CLIENT_ID = ToolKit2.genLocalMachinePeerId();
		//open serverSocket
		SERVER_SOCKET = RUBTToolKit.openServerSocket();
	}
	/**
	 * size of request piece
	 */
	public final static int REQUEST_PIECE_SIZE = 16384;
	/**
	 * enum to store the peer's status stages
	 *
	 */
	public static enum PeerStatus{
		CHOKED, 
		UNCHOKED, 
		INTERESTED,
		UNINTERESTED,
		REQUESTING,
	};
	/**
	 * interval for a keepalive messages
	 */
	protected final static long INTERVAL_TO_SEND_KEEPALIVE = 115000; //almost 2 minutes
	/**
	 * min number of unchoked connections 
	 */
	protected final static int MIN_UNCHOKED_PEERS = 3;
	/**
	 * max number of unchocked connections
	 */
	protected final static int MAX_UNCHOKED_PEERS = 6;
	/**
	 * number of seconds to conduct performance analysis regularly
	 */
	protected final static int INTERVAL_TO_ANALYZE = 30000; //30 seconds
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// USER PREFERRENCES 
	//
	// FLAGS
	// -a connect to all peers
	// -r rarest optimization
	// -d run in debug mode
	// -s start over
	//
	// DEFAULT
	// a false
	// r false
	// d true
	// s false
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++	
	public final static String PEERS_FLAG = "a";
	public final static String OPTIMIZE_FLAG = "r";
	public final static String DEBUG_FLAG = "d";
	public final static String START_FLAG = "s";
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// 
	// TRACKER CONSTANTS
	//
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	/**
	 * EVENTS TO SEND TO THE TRACKER
	 */
	public final static String EVENT_STARTED = "started";
	public final static String EVENT_COMPLETED = "completed";
	public final static String EVENT_STOPPED = "stopped";
	public final static String EVENT_EMPTY = "";
	/**
	 * key used to detect tracker failure
	 */
	public final static ByteBuffer KEY_FAILURE_REASON = ByteBuffer.wrap(new byte[]
			{ 'f', 'a', 'i', 'l','u', 'r', 'e', ' ', 'r', 'e', 'a', 's' , 'o', 'n'});
	/**
	 * Key used to retrieve info about complete
	 */
	public final static ByteBuffer KEY_COMPLETE = ByteBuffer.wrap(new byte[]
			{ 'c', 'o', 'm', 'p', 'l', 'e', 't', 'e' });

	/**
	 * Key used to retrieve info about downloaded
	 */
	public final static ByteBuffer KEY_DOWNLOADED = ByteBuffer.wrap(new byte[]
			{ 'd', 'o', 'w', 'n', 'l', 'o', 'a', 'd' ,'e', 'd'});
	/**
	 * Key used to retrieve info about incomplete
	 */
	public final static ByteBuffer KEY_INCOMPLETE = ByteBuffer.wrap(new byte[]
			{ 'i', 'n', 'c', 'o', 'm', 'p', 'l', 'e', 't', 'e' });
	/**
	 * Key used to retrieve the interval to wait between http requests to tracker
	 */
	public final static ByteBuffer KEY_INTERVAL = ByteBuffer.wrap(new byte[]
			{ 'i', 'n', 't', 'e', 'r', 'v', 'a', 'l' });  
	/**
	 * Key used to retrieve min interval to wait between http requests to tracker
	 */
	public final static ByteBuffer KEY_MIN_INTERVAL = ByteBuffer.wrap(new byte[]
			{ 'm', 'i', 'n', ' ','i', 'n', 't', 'e', 'r', 'v', 'a', 'l' });
	/**  
	 * Key used to retrieve an online Peers id
	 */
	public final static ByteBuffer KEY_PEER_ID = ByteBuffer.wrap(new byte[]
			{ 'p', 'e', 'e', 'r',' ','i','d'});
	/**
	 * Key used to retrieve an online Peers ip
	 */
	public final static ByteBuffer KEY_PEER_IP = ByteBuffer.wrap(new byte[]
			{ 'i','p'});
	/**
	 * Key used to retrieve the online Peers list
	 */
	public final static ByteBuffer KEY_PEERS = ByteBuffer.wrap(new byte[]
			{ 'p', 'e', 'e', 'r', 's'});
	/**
	 * Key used to retrieve a online Peers port
	 */
	public final static ByteBuffer KEY_PEER_PORT = ByteBuffer.wrap(new byte[]
			{ 'p', 'o', 'r', 't'});
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// 
	// MESSAGE CONSTANTS
	//
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	/**
	 * choke message type
	 */
	public static final byte TYPE_CHOKE_MESSAGE = 0;

	/**
	 * unchoke message type
	 */
	public static final byte TYPE_UNCHOKE_MESSAGE = 1;

	/**
	 * interested message type
	 */
	public static final byte TYPE_INTERESTED_MESSAGE = 2;

	/**
	 * uninterested message type
	 */
	public static final byte TYPE_UNINTERESTED_MESSAGE = 3;

	/**
	 * have message type
	 */
	public static final byte TYPE_HAVE_MESSAGE = 4;

	/**
	 * bitfield message type
	 */
	public static final byte TYPE_BITFIELD_MESSAGE = 5;
	/**
	 * request message type
	 */
	public static final byte TYPE_REQUEST_MESSAGE = 6;

	/**
	 * piece message type
	 */
	public static final byte TYPE_PIECE_MESSAGE = 7;

	/**
	 * cancel message type
	 */
	public static final byte TYPE_CANCEL_MESSAGE = 8;

	/**
	 * array to hold the protocol part of the handshake
	 */
	public static final byte[] PROTOCOL = new byte[] {19, 'B', 'i', 't', 'T', 'o', 'r', 'r', 'e', 'n', 't', ' ', 'p', 'r', 'o', 't', 'o', 'c', 'o', 'l'};
	/**
	 * array to hold the flags part of the handshake
	 */
	public static final byte[] FLAGS = new byte[] {0, 0, 0, 0, 0, 0, 0, 0};
	/**
	 * array for holding the length of each message type
	 */
	public static final int[] MESSAGE_LENGTHS = {1, 1, 1, 1, 5, 1, 13, 9, 1, 9};
	/**
	 * Convenience array for printing message types.
	 */
	public static final String[] MESSAGE_NAMES = { "Choke", "Unchoke", "Interested", "Uninterested", "Have", "Bitfield", "Request", "Piece", "Cancel"};
	/**
	 * Static reference to a keep-alive message so that new objects don't need to
	 * be allocated.
	 */
	public static final KeepAliveMessage KEEPALIVE_MESSAGE = new KeepAliveMessage();
	/**
	 * Static reference to a interested message so that new objects don't need to
	 * be allocated.
	 */
	public static final InterestedMessage INTERESTED_MESSAGE = new InterestedMessage();
	/**
	 * Static reference to a unchoke message so that new objects don't need to
	 * be allocated.
	 */
	public static final UnchokeMessage UNCHOKE_MESSAGE = new UnchokeMessage();
	/**
	 * Static reference to a choke message so that new objects don't need to
	 * be allocated.
	 */
	public static final ChokeMessage CHOKE_MESSAGE = new ChokeMessage();
	/**
	 * Static reference to a uninterested message so that new objects don't need to
	 * be allocated.
	 */
	public static final UninterestedMessage UNINTERESTED_MESSAGE = new UninterestedMessage();
}
