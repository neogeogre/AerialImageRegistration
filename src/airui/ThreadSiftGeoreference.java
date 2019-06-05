package airui;


import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;

import java.util.ArrayList;
import java.util.concurrent.Callable;


/**
 * This class contain the code allowing to compute a multi-threading georeferancement with SIFT especially for an aerial image.
 * The class implement Callable allowing the thread to returns a result.
 * @author Geoffrey
 * @see Callable
 */
public class ThreadSiftGeoreference implements Callable<ArrayList<LatLon>>
{
	//==================================================
	//				Properties
	//==================================================
	
	/**
	 * An object containing the ground reference image.
	 * @see ImageProcess
	 */
	private ImageProcess GroungImageObj;
	
	/**
	 * An object containing the aerial image.
	 * @see ImageProcess
	 */
	private ImageProcess AerialImageObj;
	
	/**
	 * The image name.
	 */
	private String ImgName;
	
//	/**
//	 * A loading bar.
//	 * @see loadingBar
//	 */
//	private loadingBar waitingBar;
	private DlrefImgPanel waitingBar;
	
	/**
	 * The directory where the ground image is located.
	 */
	private String GroundImgPath;
	
	/**
	 * The directory where the aerial image is located.
	 */
	private String AerialImgPath;
	
	/**
	 * The area of interest
	 * @see ImageProcess#CornerExtractor
	 */
	private Sector SelectedSector;

	//==================================================
	//				Constructor
	//==================================================
	
	/**
	 * Constructor for the thread, initializing each Field of the Thread.
	 *
	 * @see ImageProcess
	 * @see ThreadSiftGeoreference
	 * @see ThreadSiftGeoreference#AerialImageObj
	 * @see ThreadSiftGeoreference#AerialImgPath
	 * @see ThreadSiftGeoreference#GroundImgPath
	 * @see ThreadSiftGeoreference#GroungImageObj
	 * @see ThreadSiftGeoreference#ImgName
	 * @see ThreadSiftGeoreference#waitingBar
	 * @see loadingBar#updateBar() 
	 * 
	 * @param grdPath
	 * The directory where the ground image is located.
	 * @param ArlImgPath
	 * The directory where the aerial image is located.
	 * @param SlctSector
	 * The area of interest
	 * @see ImageProcess#CornerExtractor(ImageProcess, Sector)
	 * @param ImgInName
	 *  The image name.
	 * @param WaitBarObj
	 * A loading bar. @see loadingBar
	 */
//	public ThreadSiftGeoreference(String grdPath, String ArlImgPath, Sector SlctSector, String ImgInName, loadingBar WaitBarObj)
	public ThreadSiftGeoreference(String grdPath, String ArlImgPath, Sector SlctSector, String ImgInName, DlrefImgPanel WaitBarObj)
	{		
		GroungImageObj = new ImageProcess();
		AerialImageObj = new ImageProcess();
		GroundImgPath = grdPath;
		AerialImgPath = ArlImgPath;
		SelectedSector = SlctSector;
		ImgName = ImgInName;
		waitingBar = WaitBarObj;
	}
	
	//==================================================
	//				Thread with interface Callable
	//==================================================
	
	/**
	 * The main code of the thread.
 	 * @see ImageProcess
	 * @return AerialImageObj.getImgCoordGeo()
	 */
	public ArrayList<LatLon> call() throws Exception 
	{
		System.out.println("Image " + ImgName + ".tif, starting initialisation.");
		
		waitingBar.updateBar();
		
		GroungImageObj.loadPicture(GroundImgPath);
		GroungImageObj.ImgResizer(0.6);
	    GroungImageObj.ComputeKeyPointsAndDescriptors();
	    
	    waitingBar.updateBar();
	    
	    AerialImageObj.loadPicture(AerialImgPath);
	    // AerialImageObj.RemoveLensDistortion();
	    AerialImageObj.ComputeKeyPointsAndDescriptors();
	    
	    waitingBar.updateBar();
	    
	    AerialImageObj.MatchingWithDescriptors(GroungImageObj, 0.60);
	  	
	    waitingBar.updateBar();
	    
	    System.out.println("Image " + ImgName + ".tif, number of raw matching-points :           " + AerialImageObj.getNumberOfMatchingPoints());
	    System.out.println("Image " + ImgName + ".tif, number of valid keypoints after filters :   " + Integer.toString(AerialImageObj.getgoodMatches().size()));
	    
	    if (AerialImageObj.getgoodMatches().size() < 4)
		{
	    	System.err.println("Image " + ImgName + ".tif" + " skipped, number of matches were insufficient to compute homography.");
		}
	    else
	    {
	    	AerialImageObj.ComputeHomography(GroungImageObj, 5);
	    	AerialImageObj.CornerExtractor(GroungImageObj, SelectedSector);
	    	
	    	if (AerialImageObj.getImgCoordGeo() == null)
	    	{
	    		System.err.println("Image " + ImgName + ".tif" + " skipped, matches were sufficient but the homography was computed incoherently."); 	
	    	}
	    	else
	    	{
	    		System.out.println("Image " + ImgName + ".tif" + " has been transformed correctly !!");
	    	}
	    }
	   
	    waitingBar.updateBar();
	    
		return AerialImageObj.getImgCoordGeo();
	}
}
