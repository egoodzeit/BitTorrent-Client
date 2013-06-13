package cs352.RUBTClient.Group06;
/**
 * @author Marinela G. Haldeman
 * @author Elliot Goodzeit
 * @author Aditya Sai
 * 61 lines of code
 */
public class Request {
	/**
	 * to store stats about the request/file piece
	 */
	private int index;
	private int begin;
	private int size;
	/**
	 * constructor with stats only
	 * @param index
	 * @param begin
	 * @param size
	 */
	public Request(int index, int begin, int size) {
		this.index = index;
		this.begin = begin; 
		this.size = size;
	}
	@Override
	public String toString()
	{
		return "index: " + this.index + "\tbegin: " + this.begin + "\tsize: " + this.size;
	}
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// 
	// GET-ERS AND SET-ERS
	//
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public int getSize() 
	{
		return this.size;
	}
	public int getIndex() 
	{
		return this.index;
	}
	public int getBegin() 
	{
		return this.begin;
	}
	public void setBegin(int begin)
	{
		this.begin = begin;
	}
	public void setIndex(int index)
	{
		this.index = index;
	}
	public void setSize(int size)
	{
		this.size = size;
	}
}
