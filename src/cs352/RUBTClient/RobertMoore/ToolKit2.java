package cs352.RUBTClient.RobertMoore;

import java.util.Random;

public class ToolKit2 extends ToolKit {
	/*
	 * Rob's code from the Java Programming Sakai site.
	 * https://sakai.rutgers.edu/portal/site/e07619c5-a492-4ebe-8771-179dfe450ae4/page/0a7200cf-0538-479a-a197-8d398c438484
	 */
	/**
	 * @author Robert Moore
	 * 
	 * Converts a {@code byte[]} to a {@code boolean[]}. It is assumed that the
	 * values are in most-significant-bit first order. Meaning that most
	 * significant bit of the 0th byte of {@code bits} is the first boolean
	 * value.
	 * 
	 * @param bits
	 *            a binary array of boolean values stored as a {@code byte[]}.
	 * @param significantBits
	 *            the number of important bits in the {@code byte[]}, and
	 *            therefore the length of the returned {@code boolean[]}
	 * @return a {@code boolean[]} containing the same boolean values as the
	 *         {@code byte[]}
	 */
	public static boolean[] convert(byte[] bits, int significantBits) {
		boolean[] retVal = new boolean[significantBits];
		int boolIndex = 0;
		for (int byteIndex = 0; byteIndex < bits.length; ++byteIndex) {
			for (int bitIndex = 7; bitIndex >= 0; --bitIndex) {
				if (boolIndex >= significantBits) {
					// Bad to return within a loop, but it's the easiest way
					return retVal;
				}

				retVal[boolIndex++] = (bits[byteIndex] >> bitIndex & 0x01) == 1 ? true
						: false;
			}
		}
		return retVal;
	}
	/**
	 * @author Robert Moore
	 * 
	 * Converts an {@code boolean[]} to a {@code byte[]} where each bit of the
	 * {@code byte[]} contains a 1 bit for a {@code true} value, and a 0 bit for
	 * a {@code false} value. The {@code byte[]} will contain the 0th index
	 * {@code boolean} value in the most significant bit of the 0th byte.
	 * 
	 * @param bools
	 *            an array of boolean values
	 * @return a {@code byte[]} containing the boolean values of {@code bools}
	 *         as bits.
	 */
	public static byte[] convert(boolean[] bools) {
		int length = bools.length / 8;
		int mod = bools.length % 8;
		if(mod != 0){
			++length;
		}
		byte[] retVal = new byte[length];
		int boolIndex = 0;
		for (int byteIndex = 0; byteIndex < retVal.length; ++byteIndex) {
			for (int bitIndex = 7; bitIndex >= 0; --bitIndex) {
				// Another bad idea
				if (boolIndex >= bools.length) {
					return retVal;
				}
				if (bools[boolIndex++]) {
					retVal[byteIndex] |= (byte) (1 << bitIndex);
				}
			}
		}

		return retVal;
	}
	/**method to generate random id's for the client
	 * 
	 * @author posted in the discussion board
	 * 
	 * @param - null
	 * 
	 * @return - a byte[] that represents a peer ID
	 */
	public static byte[] genLocalMachinePeerId(){
		Random r = new Random(System.currentTimeMillis());
		byte[] peerId = new byte[20];
		peerId[0] = '-';
		peerId[1] = 'G';
		peerId[2] = 'P';
		peerId[3] = '0';
		peerId[4] = '6';
		peerId[5] = '-';
		for(int i = 6; i < 20; ++i){
			peerId[i] = (byte)('A' + r.nextInt(26));
		}
		return peerId;
	}
	public static final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6','7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
	/**method to transform byte[] into a ASCII escaped code
	 * 
	 * @author discussion board
	 * 
	 * @param bytes
	 * 				byte array to be escaped
	 * @return String
	 */
	public static String toHexString(byte[] bytes) {
		if (bytes == null) {
			return null;
		}
		if (bytes.length == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder(bytes.length * 3);
		for (byte b : bytes) {
			byte hi = (byte) ((b >> 4)&0x0f);
			byte lo = (byte) (b & 0x0f);
			sb.append('%').append(HEX_CHARS[hi]).append(HEX_CHARS[lo]);
		}
		return sb.toString();
	}
	/*
	 * End Rob's code.
	 */
}
