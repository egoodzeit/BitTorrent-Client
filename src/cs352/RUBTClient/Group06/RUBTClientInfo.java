/**
 * @author Marinela G. Haldeman
 * @author Elliot Goodzeit
 * @author Aditya Sai
 * 304 lines of code
 */
package cs352.RUBTClient.Group06;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.NoSuchAlgorithmException;
import java.util.StringTokenizer;

import cs352.RUBTClient.RobertMoore.*;

public class RUBTClientInfo extends TorrentInfo{
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// USER FLAGS
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	/**
	 * store user preference regarding running only with rubt's or the whole swarm
	 */
	private boolean connect_to_all = true;
	/**
	 * store user preference regarding running in debug mode or not
	 */
	private boolean debug_mode = true;
	/**
	 * store user preference regarding whether to resume or start fresh
	 */
	private boolean start_mode = false;
	/**
	 * store user preference regarding whether to run with rarest optimization or not
	 */
	private boolean optimize_mode = true;
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// CLIENT INFORMATION
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++	
	/**
	 * number of pieces in the file to download
	 */
	private final int number_of_pieces;
	/**
	 * filename for the file to download
	 */
	private final String filename;
	/**
	 * file to download and save to hard drive
	 */
	private final RandomAccessFile file_to_download;
	/**
	 * client bitfield
	 */
	private final boolean[] client_bitfield;
	/**
	 * hold the stats for the client
	 */
	private final RUBTStats client_stats;
	/**
	 * constructor
	 * @param torrent_file_bytes
	 * @param args
	 * @throws BencodingException
	 * @throws FileNotFoundException 
	 */
	public RUBTClientInfo(byte[] torrent_file_bytes, String[] args) throws BencodingException, FileNotFoundException {
		super(torrent_file_bytes);
		//set flags for user preferences
		if (args.length >= 3)
		{
			String flags = args[2].toLowerCase();
			if (flags.contains(RUBTConstants.PEERS_FLAG))
			{
				this.setConnect_to_all(true);
			}
			if (flags.contains(RUBTConstants.DEBUG_FLAG))
			{
				this.setDebug_mode(true);
			}
			if (flags.contains(RUBTConstants.START_FLAG))
			{
				this.setStart_mode(true);
			}
			if (flags.contains(RUBTConstants.OPTIMIZE_FLAG))
			{
				this.setOptimize_mode(true);
			}
		}

		//calculate pieces 
		this.number_of_pieces = (int) Math.ceil((double)this.file_length / this.piece_length);

		//set pieces status to 0 for empty
		this.client_bitfield = new boolean[this.number_of_pieces];

		//init stats
		this.client_stats = new RUBTStats(0,0,this.file_length);

		//open file to download and check if it has good pieces
		this.filename = args[1];
		this.file_to_download = new RandomAccessFile(filename, "rwd");

		try {
			this.file_to_download.setLength(this.file_length);
		} catch (IOException e) {
			RUBTConstants.main_log.severe("Error in setLength() for the random access file: " + e.getMessage());
		}

		//if the client wants to resume
		if (!this.start_mode)
		{
			//validate existing pieces
			this.checkForPiecesOnResume(this.file_to_download);
			//retrieve stats
			this.retrieveStatsOnResume();
		}		
		//add start session time and start stats to the stats file
		RUBTConstants.stats_log.info("*** BEGIN NEW SESSION ***");
		RUBTConstants.stats_log.info("download\t\t\tupload\t\t\tleft");
		RUBTConstants.stats_log.info(this.client_stats.getDownloaded() + "\t\t\t" + this.client_stats.getUploaded() + "\t\t\t" + this.client_stats.getLeft());
	}
	/**
	 * Checks SHA-1 of previously downloaded pieces and adds them to file_pieces in ClientInfo.
	 * Also, marks off bitfield with verified pieces.
	 * @param file_to_download
	 */
	private void checkForPiecesOnResume(RandomAccessFile file_to_download)
	{
		//loops through all file_pieces in the array to get index, size, and to store data if needed.
		for (int pieceIndex = 0; pieceIndex < this.number_of_pieces; pieceIndex ++)
		{
			//move the filePointer
			try {
				file_to_download.seek(pieceIndex*this.piece_length);
			} catch (IOException e) {
				RUBTConstants.main_log.severe("Error traversing the random access file: " + e.getMessage());
			}

			//try to read a piece
			byte[] pieceBytes;
			if (pieceIndex == (this.number_of_pieces - 1))
			{
				pieceBytes = new byte[this.file_length - pieceIndex*this.piece_length];
			}
			else
			{
				pieceBytes = new byte[this.piece_length];
			}
			try {
				file_to_download.readFully(pieceBytes);
			} catch (IOException e) {
				RUBTConstants.main_log.severe("Error reading from random access file: " + e.getMessage());
			}

			//got piece, now check the hash on it
			try {
				if(RUBTToolKit.verifySHA1(pieceBytes, this.piece_hashes[pieceIndex].array()))
				{
					//set bitfield as true
					this.client_bitfield[pieceIndex] = true;
					//substract the amount that has been already downloaded and it s valid
					this.client_stats.substractLeft(pieceBytes.length);
				}
			} catch (NoSuchAlgorithmException e) {
				RUBTConstants.main_log.severe("Error verifying SHA1 hash: " + e.getMessage());
			}
		}
	}
	/**
	 * retrieve the last stats logged in the stats file
	 */
	private void retrieveStatsOnResume()
	{
		String tail = RUBTToolKit.tail(new File(RUBTConstants.STATS_LOG_FILENAME));
		if (tail == null)
		{
			//return if tail of stats file is null
			return;
		}
		StringTokenizer tokenizer = new StringTokenizer(tail);
		if (tokenizer.hasMoreTokens())
		{
			tokenizer.nextToken();
			this.client_stats.setDownloaded(Integer.parseInt(tokenizer.nextToken()));
			this.client_stats.setUploaded(Integer.parseInt(tokenizer.nextToken()));	
		}
	}
	/**
	 * save file _to_download to disk and write stats
	 */
	protected void saveToDisk()
	{
		try {
			this.file_to_download.close();
		} catch (IOException e) {
			// leave it to debug
			RUBTConstants.main_log.severe("closing of the download file failed");
		}
		//add end session time and end stats to the stats file
		RUBTConstants.stats_log.info("*** END SESSION ***");
		RUBTConstants.stats_log.info("download\t\t\tupload\t\t\tleft");
		RUBTConstants.stats_log.info(this.client_stats.getDownloaded() + "\t\t\t" + this.client_stats.getUploaded() + "\t\t\t" + this.client_stats.getLeft());
	}
	/**
	 * method to write the file piece to the file
	 * @param piece
	 * @return
	 * 			true if the file is complete
	 * 			false otherwise
	 */
	protected synchronized boolean writePiece(FilePiece piece) {
		//move write head to the beginning of the piece in file and write piece
		try {
			this.file_to_download.seek(piece.getIndex() * this.piece_length);
			this.file_to_download.write(piece.getDownloaded());
			//update stats
			this.client_stats.substractLeft(piece.getDownloaded().length);
			//update bitfield
			this.client_bitfield[piece.getIndex()] = true;
			RUBTConstants.main_log.severe("succesfully written to file piece " + piece.getIndex());
		} catch (IOException e) {
			//leave it to debug
			RUBTConstants.main_log.severe("Error in writting to the file piece " + piece.getIndex());
		}
		//return true if file download is complete
		if (this.getClient_stats().getLeft() <=0 )
		{
			return true;
		}
		return false;
	}
	/**
	 * method use by the manager to decide whether to send bitfield or not
	 * @return
	 */
	protected boolean hasPieces()
	{
		for (int pieceIndex=0; pieceIndex < this.number_of_pieces; pieceIndex++)
		{
			if (this.client_bitfield[pieceIndex])
			{
				return true;
			}
		}
		return false;
	}
	/**
	 * convenience method to update the stats on the client
	 * @param downloaded
	 * @param uploaded
	 */
	public synchronized void updateClient_stats(long downloaded, long uploaded)
	{
		this.client_stats.addDownloaded(downloaded);
		this.client_stats.addUploaded(uploaded);
	}
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// GET-ERS AND SET-ERS
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public boolean isConnect_to_all() {
		return connect_to_all;
	}
	public void setConnect_to_all(boolean connect_to_all) {
		this.connect_to_all = connect_to_all;
	}
	public boolean isDebug_mode() {
		return debug_mode;
	}
	public void setDebug_mode(boolean debug_mode) {
		this.debug_mode = debug_mode;
	}
	public boolean isStart_mode() {
		return start_mode;
	}
	public void setStart_mode(boolean start_mode) {
		this.start_mode = start_mode;
	}
	public boolean isOptimize_mode() {
		return optimize_mode;
	}
	public void setOptimize_mode(boolean optimize_mode) {
		this.optimize_mode = optimize_mode;
	}
	public String getFilename() {
		return filename;
	}
	public boolean[] getClient_bitfield() {
		return client_bitfield;
	}
	public RUBTStats getClient_stats() {
		return client_stats;
	}
	public RandomAccessFile getFile_to_download() {
		return this.file_to_download;
	}
	public int getNumber_of_pieces() {
		return this.number_of_pieces;
	}
}
