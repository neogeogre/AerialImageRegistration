package airui;

/**
 * This thead allow you multiple download images from Bing Map.
 * @author Geoffrey
 */
public class ThreadBingImgDownload implements Runnable
{
	/**
	 * The path where downloaded images will be stocked. 
	 */
	private String PathForDownloadedGroundImage;
	
	/**
	 * An object from BingImgDownloader Class. 
	 * @see BingImgDownloader
	 */
	private BingImgDownloader SaveTiffOnDisc;
	
	/**
	 * The loading bar.
	 */
	private loadingBar waitingBar;
	
	/**
	 * The constructor of the thread, initializing each Field of the Thread.
	 * @param PathForDownloadedGroundImg
	 * The path where downloaded images will be stocked. 
	 * @param SaveTiffOnDiscObj
	 * An object from BingImgDownloader Class.
	 * @param LoadBarObj
	 * The loading bar.
	 */
	public ThreadBingImgDownload(String PathForDownloadedGroundImg, BingImgDownloader SaveTiffOnDiscObj, loadingBar LoadBarObj)
	{
		PathForDownloadedGroundImage = PathForDownloadedGroundImg;
		SaveTiffOnDisc = SaveTiffOnDiscObj;
		waitingBar = LoadBarObj;
	}
	
	//==================================================
	//				Thread with interface Runnable
	//==================================================
	public void run()
	{
		SaveTiffOnDisc.fromCurrentLayer(PathForDownloadedGroundImage);
		waitingBar.ProgBar.setIndeterminate(false);
		waitingBar.updateBar();
	}
}
