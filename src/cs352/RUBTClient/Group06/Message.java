package cs352.RUBTClient.Group06;
import java.io.*;

import java.net.*;
/**
 * @author Marinela G. Haldeman
 * @author Elliot Goodzeit
 * @author Aditya Sai
 * 446 lines of code
 */
public abstract class Message {
	/**
	 * Represents a keep-alive message exchanged between peers.
	 */
	public static final class KeepAliveMessage extends Message {
		/**
		 * Creates a new keep-alive message. 
		 */
		protected KeepAliveMessage() {
			super(0);
		}
	}
	/**
	 * Represents a unchoke message exchanged between peers.
	 */
	public static final class UnchokeMessage extends Message 
	{
		/**
		 * Creates a new unchoke message. 
		 */
		protected UnchokeMessage() 
		{
			super(RUBTConstants.MESSAGE_LENGTHS[RUBTConstants.TYPE_UNCHOKE_MESSAGE], RUBTConstants.TYPE_UNCHOKE_MESSAGE);
		}
	}
	/**
	 * Represents a interested message exchanged between peers.
	 */
	public static final class InterestedMessage extends Message 
	{
		/**
		 * Creates a new interested message. 
		 */
		protected InterestedMessage()
		{
			super(RUBTConstants.MESSAGE_LENGTHS[RUBTConstants.TYPE_INTERESTED_MESSAGE], RUBTConstants.TYPE_INTERESTED_MESSAGE);
		}
	}
	/**
	 * Represents a chocke message exchanged between peers.
	 */
	public static final class ChokeMessage extends Message 
	{
		/**
		 * Creates a new chocke message. 
		 */
		protected ChokeMessage() 
		{
			super(RUBTConstants.MESSAGE_LENGTHS[RUBTConstants.TYPE_CHOKE_MESSAGE], RUBTConstants.TYPE_CHOKE_MESSAGE);
		}
	}
	/**
	 * Represents a uninterested message exchanged between peers.
	 */
	public static final class UninterestedMessage extends Message 
	{
		/**
		 * Creates a new uninterested message. 
		 */
		protected UninterestedMessage()
		{
			super(RUBTConstants.MESSAGE_LENGTHS[RUBTConstants.TYPE_UNINTERESTED_MESSAGE], RUBTConstants.TYPE_UNINTERESTED_MESSAGE);
		}
	}
	/**
	 * Represents a bitfield message exchanged between peers.
	 */
	public static final class BitfieldMessage extends Message 
	{
		/**
		 * to hold the bitfield payload
		 */
		private final byte[] payload;
		/**
		 * Creates a new bitfield message. 
		 */
		protected BitfieldMessage(byte[] payload)
		{
			super(RUBTConstants.MESSAGE_LENGTHS[RUBTConstants.TYPE_BITFIELD_MESSAGE] + payload.length, RUBTConstants.TYPE_BITFIELD_MESSAGE);
			this.payload = payload;
		}
		protected byte[] getBitfield()
		{
			return this.payload;
		}
	}
	/**
	 * Represents a handshake message exchanged between peers.
	 */
	public static final class HandshakeMessage extends Message {
		/**
		 * to store the handshake info that is not predefined
		 */
		private final HandshakeInfo handshakeInfo;

		/**
		 * Creates a new keep-alive message. 
		 */
		protected HandshakeMessage(HandshakeInfo handshakeInfo) {
			super(68);
			this.handshakeInfo = handshakeInfo;
		}
		protected HandshakeInfo getHandshakeInfo()
		{
			return this.handshakeInfo;
		}
		/**
		 * special decode for handshake
		 * @param in
		 * @return
		 * @throws IOException
		 */
		public static HandshakeMessage decodeHandshake(final InputStream in) throws IOException
		{
			// Wrap for convenience
			DataInputStream din = new DataInputStream(in);

			// Message length and type should always be present
			byte[] response= new byte[68];
			din.readFully(response);

			//send handshake message back to peer
			HandshakeInfo newHI = new HandshakeInfo(response);
			HandshakeMessage message = new HandshakeMessage(newHI);

			return message;
		}
		/**
		 * special incode for handshake
		 * @param message
		 * @param out
		 * @throws IOException
		 */
		public static void encodeHandshake(final HandshakeMessage message,final OutputStream out) throws IOException 
		{

			// Wrap a DataOutputStream for convenience.
			DataOutputStream dout = new DataOutputStream(out);
			dout.write(RUBTConstants.PROTOCOL);
			dout.write(RUBTConstants.FLAGS);
			dout.write(message.getHandshakeInfo().getHash());
			dout.write(message.getHandshakeInfo().getId());
			dout.flush();
		}

	}
	/**
	 * Represents a piece message exchanged between peers.
	 */
	public static final class PieceMessage extends Message {
		/**
		 * needed to hold the payload of the piece message
		 */
		private final RequestPiece requestPiece;
		/**
		 * Creates a new piece message. 
		 */
		protected PieceMessage(RequestPiece requestPiece) {
			super(RUBTConstants.MESSAGE_LENGTHS[RUBTConstants.TYPE_PIECE_MESSAGE] + requestPiece.getSize(), RUBTConstants.TYPE_PIECE_MESSAGE);
			this.requestPiece = requestPiece;
		}
		protected RequestPiece getRequestPiece()
		{
			return this.requestPiece;
		}
	}
	/**
	 * Represents a request message exchanged between peers.
	 */
	public static final class RequestMessage extends Message {
		/**
		 * needed to hold the payload of the request message
		 */
		private final Request request;
		/**
		 * Creates a new request message. 
		 */
		protected RequestMessage(Request request) {
			super(RUBTConstants.MESSAGE_LENGTHS[RUBTConstants.TYPE_REQUEST_MESSAGE], RUBTConstants.TYPE_REQUEST_MESSAGE);
			this.request = request;
		}
		protected Request getRequest()
		{
			return this.request;
		}
	}
	/**
	 * Represents a have message exchanged between peers.
	 */
	public static final class HaveMessage extends Message {
		/**
		 * needed to hold the payload of the request message
		 */
		private final int payload;
		/**
		 * Creates a new request message. 
		 */
		protected HaveMessage(int payload) {
			super(RUBTConstants.MESSAGE_LENGTHS[RUBTConstants.TYPE_HAVE_MESSAGE], RUBTConstants.TYPE_HAVE_MESSAGE);
			this.payload = payload;
		}
		protected int getPieceIndex()
		{
			return this.payload;
		}
	}
	/**
	 * The length of the encoded message.
	 */
	private final int length;

	/**
	 * The type of this message.
	 */
	private final byte type;
	/**
	 * Creates a new abstract message with the specified length and message type.
	 * 
	 * @param length
	 *          the length (in bytes) of the encoded message.
	 * @param type
	 *          the type of message.
	 */
	protected Message(final int length, final byte type) {
		this.length = length;
		this.type = type;
	}
	/**
	 * Creates a new abstract message with the specified length for messages with no type
	 * 
	 * @param length
	 *          the length (in bytes) of the encoded message.
	 */
	public Message(int length) 
	{
		this.length = length;
		//default type is -1
		this.type = -1;
	}
	/**
	 * Returns the encoded length of this message in bytes.
	 * 
	 * @return the encoded length of this message in bytes.
	 */
	public int getLength() {
		return this.length;
	}
	/**
	 * Returns the type of this message.
	 * 
	 * @return the type of this message.
	 */
	public byte getType() {
		return this.type;
	}
	@Override
	public String toString() {
		return "(" + this.length + ")" + " (" + this.type + ") " + RUBTConstants.MESSAGE_NAMES[this.type];
	}
	/**
	 * Encodes the specified message onto the provided OutputStream.
	 * 
	 * @param message
	 *          the message to encode
	 * @param out
	 *          the OutputStream on which to write the message.
	 * @throws IOException
	 *           if an IOException is thrown by the OutputStream.
	 */
	public static void encodeMessage(final Message message,final OutputStream out) throws IOException 
	{
		// Wrap a DataOutputStream for convenience.
		DataOutputStream dout = new DataOutputStream(out);

		// Check to see if there's more to encode.
		if (message.getLength() >= 1) 
		{
			switch (message.getType()) {
			case RUBTConstants.TYPE_CHOKE_MESSAGE: 
			{
				dout.writeInt(message.getLength());
				dout.writeByte(message.getType());
				break;
			}
			case RUBTConstants.TYPE_UNCHOKE_MESSAGE:
			{
				dout.writeInt(message.getLength());
				dout.writeByte(message.getType());
				break;
			}
			case RUBTConstants.TYPE_INTERESTED_MESSAGE:
			{
				dout.writeInt(message.getLength());
				dout.writeByte(message.getType());
				break;
			}
			case RUBTConstants.TYPE_UNINTERESTED_MESSAGE:
			{
				dout.writeInt(message.getLength());
				dout.writeByte(message.getType());
				break;
			}
			case RUBTConstants.TYPE_HAVE_MESSAGE:
			{
				HaveMessage haveMessage = (HaveMessage) message;
				dout.writeInt(haveMessage.getLength());
				dout.writeByte(haveMessage.getType());
				dout.writeInt(haveMessage.getPieceIndex());
				break;
			}
			case RUBTConstants.TYPE_BITFIELD_MESSAGE:
			{
				BitfieldMessage bitfieldMessage = (BitfieldMessage) message;
				dout.writeInt(bitfieldMessage.getLength());
				dout.writeByte(bitfieldMessage.getType());
				dout.write(bitfieldMessage.getBitfield());
				break;
			}
			case RUBTConstants.TYPE_REQUEST_MESSAGE:
			{
				RequestMessage reqMessage = (RequestMessage) message;
				dout.writeInt(reqMessage.getLength());
				dout.writeByte(reqMessage.getType());
				dout.writeInt(reqMessage.getRequest().getIndex());
				dout.writeInt(reqMessage.getRequest().getBegin());
				dout.writeInt(reqMessage.getRequest().getSize());
				break;
			}
			case RUBTConstants.TYPE_PIECE_MESSAGE:
			{
				PieceMessage pieceMessage = (PieceMessage) message;
				dout.writeInt(pieceMessage.getLength());
				dout.writeByte(pieceMessage.getType());
				dout.writeInt(pieceMessage.getRequestPiece().getIndex());
				dout.writeInt(pieceMessage.getRequestPiece().getBegin());
				dout.write(pieceMessage.requestPiece.getDownloaded());
				break;
			}
			// Error handling
			default:
				RUBTConstants.main_log.warning("Unknown message type when encoding: " + message.getType());
				break;
			}
		}
		else
		{
			//for keepAlive messages
			dout.writeInt(message.length);
		}
		// Always flush the output stream in case it's buffered.
		dout.flush();
	}
	/**
	 * Decodes and returns the next message from the provided InputStream. If no
	 * message can be decoded, returns null.
	 * 
	 * @param in
	 *          the InputStream from which to decode the next message.
	 * @return the decoded message, or null if no message can be decoded.
	 * @throws IOException
	 *           if the socket is closed or an IOException is thrown by the
	 *           InputStream.
	 */
	public static Message decodeMessage(final InputStream in) throws IOException 
	{
		// Check for null socket or EOF
		if (in == null || in.available() < 0) {
			throw new SocketException("Socket is null or closed.");
		}
		// Wrap for convenience
		DataInputStream din = new DataInputStream(in);
		// Message length should always be present
		int messageLength = din.readInt();
		if (messageLength == 0)
		{
			return RUBTConstants.KEEPALIVE_MESSAGE;
		}
		byte messageType = din.readByte();
		switch (messageType) {
		case RUBTConstants.TYPE_CHOKE_MESSAGE: 
		{
			return RUBTConstants.CHOKE_MESSAGE;
		}
		case RUBTConstants.TYPE_UNCHOKE_MESSAGE:
		{
			return RUBTConstants.UNCHOKE_MESSAGE;
		}
		case RUBTConstants.TYPE_INTERESTED_MESSAGE:
		{
			return RUBTConstants.INTERESTED_MESSAGE;
		}
		case RUBTConstants.TYPE_UNINTERESTED_MESSAGE:
		{
			return RUBTConstants.UNINTERESTED_MESSAGE;
		}
		case RUBTConstants.TYPE_HAVE_MESSAGE:
		{
			int messagePayload = din.readInt();
			return new HaveMessage(messagePayload);
		}
		case RUBTConstants.TYPE_BITFIELD_MESSAGE:
		{
			int payloadSize = messageLength - 1;
			byte[] messagePayload = new byte[payloadSize];
			din.readFully(messagePayload);
			return new BitfieldMessage(messagePayload);
		}
		case RUBTConstants.TYPE_REQUEST_MESSAGE:
		{
			int index = din.readInt();
			int begin = din.readInt();
			int size = din.readInt();
			Request request = new Request(index, begin, size);
			RequestMessage requestMessage = new RequestMessage(request);
			return requestMessage;
		}
		case RUBTConstants.TYPE_PIECE_MESSAGE:
		{
			int index = din.readInt();
			int begin = din.readInt();
			int size = messageLength - 9;
			byte[] messagePayload = new byte[size];
			din.readFully(messagePayload);
			RequestPiece requestPiece = new RequestPiece(index, begin, size, messagePayload);
			PieceMessage pieceMessage = new PieceMessage(requestPiece);
			return pieceMessage;
		}
		// Error handling
		default:
			RUBTConstants.main_log.warning("Unknown message type when decoding: " + messageType);
			break;
		}
		return null;
	}
}
