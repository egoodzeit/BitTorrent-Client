/**
 * @author Marinela G. Haldeman
 * @author Elliot Goodzeit
 * @author Aditya Sai
 */
package cs352.RUBTClient.Group06;

import java.io.*;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import cs352.RUBTClient.RobertMoore.*;

/**
 * @author Marinela G. Haldeman
 * @author Elliot Goodzeit
 * @author Aditya Sai
 * 195 lines of code
 */
public class RUBTClient extends Thread{
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// THE PROGRAM HAS 3 MAIN THREADS:RUBTCLIENT, MANAGER AND TRACKER
	// AND ONE THREAD FOR EACH PEER THE MANAGER CONNECTS TO
	// 
	// RUBTCLIENT IT'S STARTED IN MAIN
	// IT CREATES A CLIENTINFO OBJECT AND STARTS THE MANAGER
	// IT LISTENS FOR USER INPUT
	// QUIT - QUIT THE APPLICATION
	// STATUS - GET STATUS
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	/**
	 * list of listeners for the RUBTClient 
	 */
	protected final Collection<RUBTClientListener> listeners = new ConcurrentLinkedQueue<RUBTClientListener>();
	/**
	 * Flag to shutdown the client
	 */
	protected boolean keepRunning = true;
	/**
	 * buffered reader to read from console
	 */
	protected final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
	/**
	 * CONSTRUCTOR FOR CLIENT
	 * @param args
	 * 				ARGS THAT WERE PASSED IN FROM MAIN
	 * @throws BencodingException 
	 * @throws FileNotFoundException 
	 */
	public RUBTClient(String[] args){
		//check .torrent file if it exists and if it's null
		File torrentFile = new File(args[0]);
		FileInputStream fileStream = null;
		try {
			fileStream = new FileInputStream(torrentFile);
		} catch (FileNotFoundException e) {
			//exit if file not found
			RUBTConstants.main_log.severe("open file failed because " + e.getMessage());
			System.exit(0);
		}
		DataInputStream inStream = new DataInputStream(fileStream);
		long len = torrentFile.length();
		if (len == 0)
		{
			//exit if length of file is 0
			RUBTConstants.main_log.severe("length of .torrent file is 0");
			System.exit(0);
		}
		byte[] torrent_file_bytes = new byte[(int)len];
		try {
			inStream.readFully(torrent_file_bytes);
			inStream.close();
		} catch (IOException e) {
			//exit when reading of file failed
			RUBTConstants.main_log.severe("Reading torrent stream failed because " + e.getMessage());
			System.exit(0);
		}

		//start manager
		RUBTClientInfo clientInfo = null;
		try {
			clientInfo = new RUBTClientInfo(torrent_file_bytes, args);
		} catch (FileNotFoundException e) {
			//exit when creating clientInfo failed
			RUBTConstants.main_log.severe("Create ClientInfo failed because " + e.getMessage());
			System.exit(0);
		}catch (BencodingException e) {
			//exit when creating clientInfo failed
			RUBTConstants.main_log.severe("Create ClientInfo failed because " + e.getMessage());
			System.exit(0);
		}

		Manager manager = new Manager(clientInfo);
		this.addUserInputListener(manager);
		//for debug purposes
		manager.setName("Manager");
		manager.start();
	}
	@Override
	public void run()
	{
		RUBTConstants.main_log.info("Starting RUBTClient UserInput Listener");
		System.out.println("Welcome to RUBTClient");
		System.out.println("Terminate program by typing quit");
		System.out.println("To get current status, type status.");
		while (this.keepRunning) {
			//sleep when reader empty
			try {
				if (!this.reader.ready()) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException ie) {
						// Ignored
					}
					continue;
				}
				//read user input
				String line = this.reader.readLine().trim();
				if ("quit".equalsIgnoreCase(line)) {
					RUBTConstants.main_log.info("User requested quit");
					System.out.println("Shutting down...");
					for (RUBTClientListener listener : this.listeners) {
						listener.userRequestedShutdown();
					}
					break;
				}
				else if ("status".equalsIgnoreCase(line))
				{
					RUBTConstants.main_log.info("User requested status");
					for (RUBTClientListener listener : this.listeners) {
						listener.userRequestedStatus();
					}
				}
				else
				{
					System.out.println("No such command. Try again ...");
				}
			} catch (IOException e) {
				// leave for debug
				RUBTConstants.main_log.info("Reading user input failed because " + e.getMessage());
			}
		}
		RUBTConstants.main_log.info("RUBTClient is shutting down");
	}
	/**
	 * terminate thread and close reader
	 */
	protected void disconnect() 
	{
		try {
			this.reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.keepRunning = false;
	}
	/**
	 * add listener
	 * @param listener
	 */
	public void addUserInputListener(RUBTClientListener listener) {
		this.listeners.add(listener);
	}
	/**
	 * remove listener
	 * @param listener
	 */
	public void removeUserInputListener(RUBTClientListener listener) {
		this.listeners.remove(listener);
	}
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// 
	// WHERE ALL THE MAGIC STARTS :)
	//
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++	
	/**
	 * main class for the project 
	 * @param args
	 * 			expects 2 arguements: .torrent file and name of file to download
	 */
	public static void main(String[] args){
		//checking args
		if (args.length < 2)
		{
			System.out.println("Need to specify a .torrent file name and the name of the file to download");
			System.exit(0);
		}
		//new session in clients activity log
		RUBTConstants.main_log.info("*** BEGIN NEW SESSION ***");
		
		//start the client
		RUBTClient client = new RUBTClient(args);
		//set thread name for debug purposes
		client.setName("RUBTClient");
		client.start();

		//main exits, client takes over
		return;
	}
}
