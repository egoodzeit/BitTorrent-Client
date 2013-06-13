package cs352.RUBTClient.Group06;
/**
 * @author Marinela G. Haldeman
 * @author Elliot Goodzeit
 * @author Aditya Sai
 * 37 lines of code
 */
public class RequestPiece extends Request{
	/**
	 * to store the downloaded bytes of the piece message
	 */
	private final byte[] downloaded;
	/**
	 * constructor called in Message.java
	 * @param index
	 * @param begin
	 * @param size
	 * @param downloaded
	 */
	public RequestPiece(int index, int begin, int size, byte[] downloaded) {
		super(index, begin, size);
		this.downloaded = downloaded;
	}
	/**
	 * constructor
	 * @param request
	 * @param downloaded
	 */
	public RequestPiece(Request request, byte[] downloaded) {
		super(request.getIndex(),request.getBegin(), request.getSize());
		this.downloaded = downloaded;
	}
	public byte[] getDownloaded() {
		return downloaded;
	}
}
