package cs352.RUBTClient.Group06;
/**
 * @author Marinela G. Haldeman
 * @author Elliot Goodzeit
 * @author Aditya Sai
 * 60 lines of code
 */
public class FilePiece extends RequestPiece{
	/**
	 * constructor for FilePiece which is a requestPiece with a bigger size
	 * @param index
	 * @param begin
	 * @param size
	 * @param downloaded
	 */
	public FilePiece(int index, int begin, int size) {
		super(index, begin, size, new byte[size]);
	}
	/**
	 * constructor used in manager to set the uploading piece for peer
	 * @param request
	 * @param bytes
	 */
	public FilePiece(Request request, byte[] bytes) {
		super(request, bytes);
	}
	/**
	 * copy a request size piece to the filepiece
	 * @param requestPiece
	 */
	public void copyToFilePiece(RequestPiece requestPiece)
	{
		System.arraycopy(requestPiece.getDownloaded(), 0, this.getDownloaded(), requestPiece.getBegin(), requestPiece.getSize());
		this.setBegin(requestPiece.getBegin() + requestPiece.getSize());
	}
	/**
	 * copy bytes from the file piece needed for the request from peer
	 * @param request
	 * @return
	 */
	public byte[] copyFromFilePiece(Request request)
	{
		byte[] requestLoad = new byte[request.getSize()];
		System.arraycopy(this.getDownloaded(), request.getBegin(), requestLoad , 0, request.getSize());
		return requestLoad;
	}
	/**
	 * checks if the piece is complete
	 * @return
	 */
	public boolean isComplete()
	{
		if (this.getBegin() == this.getSize())
		{
			return true;
		}
		return false;
	}
}
