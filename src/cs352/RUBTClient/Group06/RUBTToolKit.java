/**
 * @author Marinela G. Haldeman
 * @author Elliot Goodzeit
 * @author Aditya Sai
 * 185 lines of code
 */
package cs352.RUBTClient.Group06;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

public class RUBTToolKit {
	/**
	 * @author http://stackoverflow.com/questions/686231/java-quickly-read-the-last-line-of-a-text-file
	 * used to get the last logged line in the file
	 * @param file
	 * @return
	 */
	@SuppressWarnings("resource")
	public static String tail(File file) { 
	    try { 
	        RandomAccessFile fileHandler = new RandomAccessFile(file, "r" ); 
	        long fileLength = file.length() - 1; 
	        StringBuilder sb = new StringBuilder(); 
	 
	        for( long filePointer = fileLength; filePointer != -1; filePointer-- ) { 
	            fileHandler.seek( filePointer ); 
	            int readByte = fileHandler.readByte(); 
	 
	            if( readByte == 0xA ) { 
	                if( filePointer == fileLength ) { 
	                    continue; 
	                } else { 
	                    break; 
	                } 
	            } else if( readByte == 0xD ) { 
	                if( filePointer == fileLength - 1 ) { 
	                    continue; 
	                } else { 
	                    break; 
	                } 
	            } 
	            sb.append( ( char ) readByte ); 
	        } 
	 
	        String lastLine = sb.reverse().toString(); 
	        return lastLine; 
	    } catch( java.io.FileNotFoundException e ) { 
	        //leave for debug
	    	RUBTConstants.main_log.severe("stats file not found");
	        return null; 
	    } catch( java.io.IOException e ) { 
	    	//leave for debug
	    	RUBTConstants.main_log.severe("error trying to access stats file");
	    	return null;
	    } 
	} 
	/**
	 * computes the hash on the input data and it checks if it's the same
	 * as the input hash
	 * @param data
	 * 				input data to compute the hash on
	 * @param sha_hash
	 * 				input hash
	 * @return
	 *  		outputs true if computed_hash is same as input hash
	 *  		false otherwise
	 * @throws NoSuchAlgorithmException
	 * 									needs to be caught in calling method
	 */
	public static Boolean verifySHA1(byte[] data, byte[] sha_hash) throws NoSuchAlgorithmException
	{
		MessageDigest md = MessageDigest.getInstance("SHA");
		byte[] computed_hash = md.digest(data);
		if (Arrays.equals(computed_hash, sha_hash))
		{
			return true;
		}
		return false;
	}
	/**
	 * attempts to open a ServerSocket listening on one of the available ports
	 * it picks 10 random ports
	 * if none is available it returns null
	 * @return
	 * 			a serverSocket 
	 */
	public static ServerSocket openServerSocket()
	{
		Random r = new Random(System.currentTimeMillis());
		int count = 0;
		//ten attempts to randomly find an available port between 1025 and 65535
		while (count < 10)
		{
			int availablePort = 1025 + r.nextInt(64515);
			try{
				ServerSocket listenSocket = new ServerSocket(availablePort);
				//configure server socket
				listenSocket.setReuseAddress(true);
				// Wait for 250ms at a time
				listenSocket.setSoTimeout(250);
				return listenSocket;
			}catch(IOException ex){
				//ignore exception
				//means port was not available
			}
			count ++;
		}
		return null;
	}
	/**
	 * checks if the left array has an element which right array doesn't have
	 * @param left
	 * @param right
	 * @return
	 */
	public static boolean checkArraysDifference(boolean[] left, boolean[] right)
	{
		for (int index = 0; index < left.length; index ++)
		{
			if ((left[index]) && (!right[index]))
			{
				return true;
			}
		}
		return false;
	}
	/**
	 * recomputes the availability when a new vector arrives
	 * @param availability
	 * @param available
	 * @return
	 */
	public static int[] recomputeAvailability(int[] availability, boolean[] available)
	{ 
		for(int index = 0;index < available.length;index++){  
			if (available[index])
			{  
				availability[index] ++;  
			}  
		}  
		return availability;
	}
	/**
	 * used to check if the peer is a seed based on bitfield
	 * @return
	 */
	public static boolean isSeed(boolean[] bitfield)
	{
		for (int pieceIndex = 0; pieceIndex < bitfield.length; pieceIndex ++)
		{
			if (!bitfield[pieceIndex])
			{
				return false;
			}
		}
		return true;
	}
	/**
	 * convinience method to print the peers bitfield
	 * @return
	 */
	public static String getBitfieldString(boolean[] bitfield)
	{
		StringBuilder builder = new StringBuilder(); 
		for (int pieceIndex = 0; pieceIndex< bitfield.length; pieceIndex++)
		{
			if (bitfield[pieceIndex])
			{
				builder.append(1);
			}
			else
			{
				builder.append(0);
			}
		}
		return builder.toString();
	}
}
