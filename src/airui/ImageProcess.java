package airui;

import org.opencv.core.Core;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.KeyPoint;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * ImageProcess allow you methods to compute with one or two images.
 * This class use libraries from OpenCV and WorldWind.
 * @version 7 august 2014
 * @author Geoffrey Vincent, Matthew Parkan
 */
public class ImageProcess
{
	/**
	 * The raw Picture under a matrix format
	 * @see ImageProcess#loadPicture(String)
	 */
	private Mat Picture;	
	
	/**
	 * The raw Picture under a matrix format plus an alpha channel
	 * @see ImageProcess#loadPicture(String)
	 */
	private Mat Picture_rgba;
	
	/**
	 * The key-points founded by the SIFT extractor.
	 * @see ImageProcess#ComputeKeyPointsAndDescriptors()
	 */
	private MatOfKeyPoint keyPoints;
	
	/**
	 * The descriptors founded with the key-points.
	 * @see ImageProcess#ComputeKeyPointsAndDescriptors()
	 */
	private Mat descriptors;
	
	/**
	 * The raw number of matching points.
	 * @see ImageProcess#getNumberOfMatchingPoints()
	 * @see ImageProcess#ComputeKeyPointsAndDescriptors()
	 */
	private int NumberOfMatchingPoints;
	
	/**
	 * The descriptors whose passed all the filters.
	 * @see ImageProcess#getgoodMatches()
	 */
	private LinkedList<DMatch> goodMatches;
	
	/**
	 * The transformation between two images.
	 * @see ImageProcess#ComputeHomography(ImageProcess, double)
	 */
	private Mat homography;
	
	/**
	 * The ArrayList which contain the four corners coordinates after homography of you picture.
	 * @see ImageProcess#CornerExtractor(ImageProcess, Sector)
	 */
	private ArrayList<LatLon> ImgCoordGeo;
	
	/**
	 * The constructor of the class actually does nothing.
	 */
	public ImageProcess()
	{	
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}
	
	/**
	 * It gives back the raw number of matching points.
	 * @return NumberOfMatchingPoints
	 * @see ImageProcess#NumberOfMatchingPoints
	 */
	public int getNumberOfMatchingPoints()
	{
		return NumberOfMatchingPoints;
	}
	
	/**
	 * It gives back the descriptors whose passed all the filters.
	 * @return goodMatches
	 * @see ImageProcess#goodMatches
	 */
	public LinkedList<DMatch> getgoodMatches()
	{
		return goodMatches;
	}	
	
	/**
	 * It gives back the ArrayList which contain the four corners coordinates after homography of you picture
	 * @return ImgCoordGeo
	 * @see ImageProcess#CornerExtractor
	 */
	public ArrayList<LatLon> getImgCoordGeo() 
	{
		return ImgCoordGeo;
	}
	
	//====================================================================================================
	//							Methods for computing with one image 
	//====================================================================================================
	
	/**
	 * Load a picture with his own path.
	 * @param RawImagePath
	 * The directory where the image is located
	 */
	public void loadPicture(String RawImagePath)
	{
		Picture = Highgui.imread(RawImagePath); // load image
		Picture_rgba = new Mat(Picture.rows(), Picture.cols(), CvType.CV_8UC4); // CV_8UC4
		Imgproc.cvtColor(Picture, Picture_rgba, Imgproc.COLOR_BGR2BGRA, 4); // Add alpha channel	
	}

	/**
	 * Allow you to Resize an image. This image must be already load in the object.
	 * @param SizeFact
	 * The parameter for the resize between 0 and 1.
	 * @see ImageProcess#loadPicture	
	 */
	public void ImgResizer(double SizeFact)
	{
		Size dst_s = new Size();
		Imgproc.resize(Picture, Picture, dst_s, SizeFact, SizeFact, Imgproc.INTER_LINEAR); //INTER_AREA
		Imgproc.resize(Picture_rgba, Picture_rgba, dst_s, SizeFact, SizeFact, Imgproc.INTER_LINEAR); //INTER_AREA
	}
	
	/**
	 * This method remove lens Distorsion with taking in count of the radial (K1 K2 K3) and tangential (P1 P2) distortion.
	 * ACTUALLY NOT WORKING.
	 */
	public void RemoveLensDistortion()
	{
		final double fx = 3.2844077689899364e+003; //focal length (in pixel units)
		final double fy = 3.2844077689899364e+003; //focal length (in pixel units)
		final double cx = 2.4670676800270344e+003; //principal point x coordinate (in pixel units)
		final double cy = 1.6260762562827842e+003; //principal point y coordinate (in pixel units)
		final double k1 = -6.9862762372676510e-002; //first order radial distortion coefficient
		final double k2 = 9.2726988939666549e-002; //second order radial distortion coefficient
		final double k3 = -1.8405815839983038e-004; //third order radial distortion coefficient
		final double p1 = 0; //first order tangential distortion coefficient
		final double p2 = 0; //second order tangential distortion coefficient
		
		// camera matrix
		Mat cameraMatrix = new Mat(3, 3, CvType.CV_64FC1);
		cameraMatrix.put(1, 1, fx);
		cameraMatrix.put(1, 2, 0);
		cameraMatrix.put(1, 3, cx);
		cameraMatrix.put(2, 1, 0);
		cameraMatrix.put(2, 2, fy);
		cameraMatrix.put(2, 3, cy);
		cameraMatrix.put(3, 1, 0);
		cameraMatrix.put(3, 2, 0);
		cameraMatrix.put(3, 3, 1);
		
		// distortion parameters
		Mat distCoeffs = new Mat(5, 1, CvType.CV_64FC1);
		distCoeffs.put(1, 1, k1);
		distCoeffs.put(2, 1, k2);
		distCoeffs.put(3, 1, p1);
		distCoeffs.put(4, 1, p2);
		distCoeffs.put(5, 1, k3);
		
		Mat Picture2 = Picture;
		// Fonction pour enlever la distortion
		Imgproc.undistort(Picture, Picture2, cameraMatrix, distCoeffs);
		
		Picture = Picture2;
	}
	
	/**
	 * Compute  the keyPoints and the Descriptors for one Picture.
	 * The Picture must already be loaded in the object.
	 *@see ImageProcess#loadPicture
	 */
	public void ComputeKeyPointsAndDescriptors()
	{
		// Convert images to gray-scale
		Mat grayPicture = new Mat(Picture.rows(), Picture.cols(), CvType.CV_8UC1); // aerial image
		Imgproc.cvtColor(Picture, grayPicture, Imgproc.COLOR_BGR2GRAY);
		Core.normalize(grayPicture, grayPicture, 0, 255, Core.NORM_MINMAX);
		
		// Detect key-points,abstract base class for 2D image feature detectors.
		FeatureDetector siftDetector = FeatureDetector.create(FeatureDetector.SIFT); 
		
		keyPoints = new MatOfKeyPoint();
		siftDetector.detect(grayPicture, keyPoints); // Detects key-points in an image (first variant).
		
		// Preallocation for a descriptor matrix 
		descriptors = new Mat(Picture.rows(), Picture.cols(), Picture.type()); 
		
		// Compute descriptors for each key-points, descriptors describeparticularity of each key-points.
		DescriptorExtractor siftExtractor = DescriptorExtractor.create(DescriptorExtractor.SIFT);
		siftExtractor.compute(grayPicture, keyPoints, descriptors); 
	}
	
	//====================================================================================================
	//						Next methods use a pair of images.
	//====================================================================================================

	/**
	 * Find the best keyPoints between two images using there Descriptors.
	 * @param ImgForMatch
	 * You need a second object from ImageProcess type with descriptors already computed.
	 * @param distRatio
	 *  A parameter needed for filter all the matching points, need to be between 0 and 1.
	 * @see ImageProcess#ImageProcess
	 * @see ImageProcess#ComputeKeyPointsAndDescriptors
	 */
	public void MatchingWithDescriptors(ImageProcess ImgForMatch, double distRatio)
	{
		// Preallocation for a DescriptorMatcher matrix 
		MatOfDMatch matchs = new MatOfDMatch();
		DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_SL2); // Please use only a BRUTEFORCE type !!
		
		matcher.match(descriptors, ImgForMatch.descriptors, matchs);
		
		// Give the number of matching points
		NumberOfMatchingPoints = matchs.toList().size();
		
		// Filter all the  matching points using distance ratio
		LinkedList<DMatch> goodMatches1 = ratioFilter(matcher, descriptors, ImgForMatch.descriptors, distRatio);
		LinkedList<DMatch> goodMatches2 = ratioFilter(matcher, ImgForMatch.descriptors, descriptors, distRatio);				
					
		// Symmetry (crosscheck) filter
		goodMatches = symmetryFilter(goodMatches1,goodMatches2);
	}
	
	/**
	 * Allow you to compute a transformation if you want to pass to an other image with keyPoints. 
	 * @param RefImgForTransfo
	 * You need an other object from ImageProcess type with keyPoint already computed.
	 * @param ransacReprojThreshold
	 * This is a threshold for the RANSAC algorithm, between 1 and 10.
	 * @see ImageProcess#MatchingWithDescriptors
	 * @see ImageProcess#ComputeKeyPointsAndDescriptors
	 * @see ImageProcess#keyPoints
	 */
	public void ComputeHomography(ImageProcess RefImgForTransfo, double ransacReprojThreshold)
	{
		LinkedList<Point> ima01List = new LinkedList<Point>();
		LinkedList<Point> ima02List = new LinkedList<Point>();
		
		List<KeyPoint> keyPoint01List = keyPoints.toList();
		List<KeyPoint> keyPoint02List = RefImgForTransfo.keyPoints.toList();
		
		int NumberOfgoodMatches = goodMatches.size();
		for(int i = 0; i < NumberOfgoodMatches; i++)
		{
			ima01List.addLast(keyPoint01List.get(goodMatches.get(i).queryIdx).pt);
			ima02List.addLast(keyPoint02List.get(goodMatches.get(i).trainIdx).pt);
		}
		
		MatOfPoint2f ima01keys = new MatOfPoint2f();
		MatOfPoint2f ima02keys = new MatOfPoint2f();
		
		ima01keys.fromList(ima01List);
		ima02keys.fromList(ima02List);
		
		// Find and return the perspective transformation H between the source and the destination planes
		if (goodMatches.size() >= 4)
		{
			// Find the transformation with RANSAC
			homography = Calib3d.findHomography(ima01keys, ima02keys, Calib3d.RANSAC, ransacReprojThreshold); // RANSAC is better than LMEDS
		} 
		else 
		{
			homography = null;
		}
	}
	
	/**
	 * Gives you the coordinate of the image's corners.
	 * @param RefImage
	 * used for align 2D reference image grid coordinates to geographic coordinates in degrees 
	 * @param SelectedSector
	 * Area of interest
	 * @see ImageProcess#ComputeHomography
	 */
	public void CornerExtractor(ImageProcess RefImage, Sector SelectedSector)
	{
		if (homography != null)
		{
			// Define aerial image corners
			Mat img1corners = new Mat(4,1,CvType.CV_64FC2); // aerial image corners 
			Mat img2corners = new Mat(4,1,CvType.CV_64FC2);
			
			img1corners.put(0, 0, new double[] {0, 0}); // Upper left
			img1corners.put(1, 0, new double[] {Picture.cols(), 0}); // Upper right
			img1corners.put(2, 0, new double[] {Picture.cols(), Picture.rows()}); // Lower right
			img1corners.put(3, 0, new double[] {0, Picture.rows()}); // Lower left
			
			// Perform the perspective matrix transformation of vectors
			Core.perspectiveTransform(img1corners,img2corners, homography);
			
			// Compute Matrix that will map the aligned 2D reference image grid coordinates to geographic coordinates in degrees
			Matrix imageToGeographic = Matrix.fromImageToGeographic(RefImage.Picture.cols(), RefImage.Picture.rows(), SelectedSector); 
			
			// Compute aerial image corner coordinates (geographic)
			ArrayList<LatLon> img1cornersGeo = new ArrayList<LatLon>();
	
			
	        Vec4 vec = new Vec4(img2corners.get(3,0)[0], img2corners.get(3,0)[1], 1).transformBy3(imageToGeographic);// Lower left corner
	        img1cornersGeo.add(LatLon.fromDegrees(vec.y, vec.x));
	        
	        vec = new Vec4(img2corners.get(2,0)[0], img2corners.get(2,0)[1], 1).transformBy3(imageToGeographic); // Lower right corner
	        img1cornersGeo.add(LatLon.fromDegrees(vec.y, vec.x));
	        
	        
	        vec = new Vec4(img2corners.get(1,0)[0], img2corners.get(1,0)[1], 1).transformBy3(imageToGeographic); // Upper right corner
	        img1cornersGeo.add(LatLon.fromDegrees(vec.y, vec.x));
	        
	        vec = new Vec4(img2corners.get(0,0)[0], img2corners.get(0,0)[1], 1).transformBy3(imageToGeographic); // Upper left corner
	        img1cornersGeo.add(LatLon.fromDegrees(vec.y, vec.x));

			// Check coherence of corners by computing inter-corner distances 
			Angle dist1 = LatLon.linearDistance(img1cornersGeo.get(0),img1cornersGeo.get(1)); // distance bottom side
			Angle dist2 = LatLon.linearDistance(img1cornersGeo.get(2),img1cornersGeo.get(3)); // distance top side
			Angle dist3 = LatLon.linearDistance(img1cornersGeo.get(0),img1cornersGeo.get(3)); // distance left side
			Angle dist4 = LatLon.linearDistance(img1cornersGeo.get(1),img1cornersGeo.get(2)); // distance right side
			
			if(dist1.radians > 1.20*dist2.radians || dist1.radians < 0.8*dist2.radians || 
					dist3.radians > 1.20*dist4.radians || dist3.radians < 0.8*dist4.radians)
			{
				
			} 
			else
			{
				ImgCoordGeo = img1cornersGeo; // aerial image corner coordinates (geographic)
			}
		}
		else
		{
			ImgCoordGeo = null; // aerial image corner coordinates (geographic)
		}

	}
	
	//====================================================================================================
	//									filters.
	//====================================================================================================
	
	/**
	 * filter used to find the good Matches among all descriptors.
	 * @param matcher
	 * A matrix containing the descriptors match between two image.
	 * @param descripters01
	 * the descriptors for the first image
	 * @param descripters02
	 * the descriptors for the second image
	 * @param distRatio
	 * A distance in meters used to find whch descriptors where badly associated.
	 * @see ImageProcess#MatchingWithDescriptors(ImageProcess, double)
	 * @return goodMatchesVar
	 */
	private LinkedList<DMatch> ratioFilter(DescriptorMatcher matcher, Mat descripters01, Mat descripters02,  double distRatio) 
    {
    	List<MatOfDMatch> test = new ArrayList<MatOfDMatch>();
    	LinkedList<DMatch> goodMatchesVar = new LinkedList<DMatch>();
    	
        matcher.knnMatch(descripters01,descripters02,test,2); //Find the 2 nearest neighbors
        
        for(int i = 0; i < test.size(); i++)
        {       	 
        	if(test.get(i).toArray()[0].distance < distRatio * test.get(i).toArray()[1].distance)
        	{
        		goodMatchesVar.addLast(test.get(i).toArray()[0]);
        	}
        }
    	return goodMatchesVar;
    }
	
	/**
	 * It's a symmetry filter for the matches. Used in MatchingKeypointDescriptors method.
	 * @param goodMatches1
	 * Good descriptors for first image.
	 * @param goodMatches2
	 * Good descriptors for second image.
	 * @see ImageProcess#MatchingWithDescriptors(ImageProcess, double)
	 * @return goodMatches
	 */
	private LinkedList<DMatch> symmetryFilter(LinkedList<DMatch> goodMatches1, LinkedList<DMatch> goodMatches2)
    {
    	LinkedList<DMatch> goodMatches = new LinkedList<DMatch>();
    	HashSet<MatchPair> goodMatchesSet2 = new HashSet<MatchPair>();
    	
    	for(int i = 0; i < goodMatches2.size(); i++)
    	{
    		MatchPair p = new MatchPair(goodMatches2.get(i),goodMatches2.get(i).queryIdx,goodMatches2.get(i).trainIdx);
    		goodMatchesSet2.add(p);
    	}
    	
    	for(int i = 0; i < goodMatches1.size(); i++)
    	{
    		MatchPair p = new MatchPair(goodMatches1.get(i),goodMatches1.get(i).trainIdx,goodMatches1.get(i).queryIdx);

    		if(goodMatchesSet2.contains(p))
    		{
    			goodMatches.addLast(goodMatches1.get(i)); 	
    		}
    	}
    	return goodMatches;
    }
	
	 /**
	 * Used in symmetryFilter method.
	 * @see ImageProcess#symmetryFilter
	 * @author  Matthew Parkan
	 */
	class MatchPair
    {
    	public DMatch match;
    	public int queryidx, trainidx;
    	
    	public MatchPair(DMatch m, int qidx , int tidx)
    	{
    		match=m;
    		queryidx=qidx;
    		trainidx=tidx;
    	}
    	
    	@Override
    	public int hashCode() 
    	{
    		final int prime = 31;
    		int result = 1;
    		result = prime * result + queryidx;
    		result = prime * result + trainidx;
    		return result;
    	}

    	@Override
    	public boolean equals(Object obj) 
    	{
    		if (this == obj)
    			{return true;}
    		if (obj == null)
    			{return false;}
    		if (getClass() != obj.getClass())
    			return false;
    		MatchPair other = (MatchPair) obj;
    		if (queryidx != other.queryidx)
    			return false;
    		if (trainidx != other.trainidx)
    			return false;
    		
    		return true;
    	}
    }
}

