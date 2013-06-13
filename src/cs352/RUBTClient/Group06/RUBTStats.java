package cs352.RUBTClient.Group06;
/**
 * @author Marinela G. Haldeman
 * @author Elliot Goodzeit
 * @author Aditya Sai
 * 84 lines of code
 */

public class RUBTStats {
	/**
	 * stats for the bt client
	 */
	private long uploaded;
	private long downloaded;
	private long left;
	/**
	 * constructor
	 * @param uploaded
	 * @param downloaded
	 * @param left
	 */
	public RUBTStats(int uploaded, int downloaded, int left)
	{
		this.setUploaded(uploaded);
		this.setDownloaded(downloaded);
		this.setLeft(left);
	}
	/**
	 * used by manager to update left
	 * @param size
	 */
	public void substractLeft(long size)
	{
		this.setLeft(this.getLeft() - size);
	}
	/**
	 * used by manager to update uploaded
	 * @param size
	 */
	public void addUploaded(long size)
	{
		this.setUploaded(this.getUploaded() + size);
	}
	/**
	 * used by manager to update downloaded
	 * @param size
	 */
	public void addDownloaded(long size)
	{
		this.setDownloaded(this.getDownloaded() + size);
	}
	/**
	 * used to print the stats
	 */
	@Override
	public String toString()
	{
		return "up: " + this.getUploaded() + "\tdown: " + this.getDownloaded() + "\tleft: " + this.getLeft();
	}
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// 
	// GET-ERS AND SET-ERS
	//
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public long getUploaded() {
		return uploaded;
	}
	public void setUploaded(long uploaded) {
		this.uploaded = uploaded;
	}
	public long getLeft() {
		return left;
	}
	public void setLeft(long left) {
		this.left = left;
	}
	public long getDownloaded() {
		return downloaded;
	}
	public void setDownloaded(long downloaded) {
		this.downloaded = downloaded;
	}
}
