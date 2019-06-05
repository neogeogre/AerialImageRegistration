package airui;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.data.GDAL;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Allow you to load an read a cvs file that contain image name and corners latitude longitude, using opencsv libraries.
 * @author Geoffrey Vincent
 */
public class CsvManager 
{
	/**
	 * The path where aerial images are stocked.
	 */
	public String[] imPath;
	
	/**
	 * The name of the image.
	 */
	public String[] imName;

	/**
	 * aerial image timestamp list
	 */
	public double[] imCount;
	
	/**
	 * reference image geographic sector list
	 */
	public List<Sector> sector;
	
	/**
	 * aerial image file list
	 */
	public List<File> imFile;
	
	/**
	 * The corners coordinates of each aerial image.
	 */
	public ArrayList<ArrayList<LatLon>> cornersLatLon;
	
	public CsvManager()
	{
		
	}
	
	/**
	 * Read a CVS file
	 * @param path
	 * The path where the CSV is located
	 * @param AerialImagesPath
	 * The path where the images are located
	 * @throws FileNotFoundException
	 * Exception managing.
	 */
	public void CsvInput(String path, String AerialImagesPath) throws FileNotFoundException
	{ 

	File mydata_csv = new File(path);
	
	au.com.bytecode.opencsv.CSVReader reader = new au.com.bytecode.opencsv.CSVReader(new FileReader(mydata_csv),',', '"','|', 1);
	
	List<String[]> csvEntries = null;
	
	try 
	{csvEntries = reader.readAll();} 
	catch (IOException e1) 
	{e1.printStackTrace();}
	
	try 
	{reader.close();} 
	catch (IOException e)
	{e.printStackTrace();}
	
	Iterator<String[]> iter = csvEntries.iterator();
	  
	//Initialize input arrays
	List<String> imPathList = new ArrayList<String>();
	List<Double> imCountList = new ArrayList<Double>();
	List<Double> ll_E_List = new ArrayList<Double>();
	List<Double> lr_E_List = new ArrayList<Double>();
	List<Double> ur_E_List = new ArrayList<Double>();
	List<Double> ul_E_List = new ArrayList<Double>();
	List<Double> ll_N_List = new ArrayList<Double>();
	List<Double> lr_N_List = new ArrayList<Double>();
	List<Double> ur_N_List = new ArrayList<Double>();
	List<Double> ul_N_List = new ArrayList<Double>();
	
	while (iter.hasNext()) 
	{
	    String[] row = iter.next();
	    imPathList.add(AerialImagesPath + File.separator +  row[0]);
	    imCountList.add(Double.parseDouble(row[1]));
	    ll_E_List.add(Double.parseDouble(row[2]));
	    lr_E_List.add(Double.parseDouble(row[3]));
	    ur_E_List.add(Double.parseDouble(row[4]));
	    ul_E_List.add(Double.parseDouble(row[5]));
	    ll_N_List.add(Double.parseDouble(row[6]));
	    lr_N_List.add(Double.parseDouble(row[7]));
	    ur_N_List.add(Double.parseDouble(row[8]));
	    ul_N_List.add(Double.parseDouble(row[9]));
	}	
	int csvSize = imPathList.size();
	String[] imgPath = new String[csvSize];
	String[] nameList = new String[csvSize];
	double[] imgCount = new double[csvSize];
	
	imPath = imPathList.toArray(imgPath);
	List<Sector> SectorList = new ArrayList<Sector>();
	List<File> fileList = new ArrayList<File>();
	
	final double Margin = 500;
	
	for(int i = 0; i < csvSize; i++)
	{
		Point2D.Double[] croppedImageCorners = new Point2D.Double[4];
		croppedImageCorners[0] = new Point2D.Double(ll_E_List.get(i), ll_N_List.get(i)); // Lower left corner.
		croppedImageCorners[1] = new Point2D.Double(lr_E_List.get(i), lr_N_List.get(i)); // Lower right corner.
		croppedImageCorners[2] = new Point2D.Double(ur_E_List.get(i), ur_N_List.get(i)); // Upper right corner.
		croppedImageCorners[3] = new Point2D.Double(ul_E_List.get(i), ul_N_List.get(i)); // Upper left corner.
		
		double  minEasting = GDAL.getMinX(croppedImageCorners)-Margin;
		double  maxEasting = GDAL.getMaxX(croppedImageCorners)+Margin;
		double  minNorthing = GDAL.getMinY(croppedImageCorners)-Margin;
		double  maxNorthing = GDAL.getMaxY(croppedImageCorners)+Margin;
		
		SectorList.add(Sector.fromUTMRectangle(48, AVKey.NORTH, minEasting, maxEasting, minNorthing, maxNorthing));
		fileList.add(new File(imPath[i]));
	
	    String fname = fileList.get(i).getName();
	    int pos = fname.lastIndexOf(".");
	    if (pos > 0) {
	        fname = fname.substring(0, pos); //aerial image filename without extension
	    	}
	    nameList[i] = fname;
	    imgCount[i] = imCountList.get(i);
	}
	
	imPath = imgPath;
	imName = nameList;
	imFile = fileList;
	sector = SectorList;
	imCount = imgCount;
	}
	
	/**
	 * Write a CSV file 
	 * @param OutputCSVPath
	 * The path when the CSV is saved.
	 * @param fileList
	 * The list of images to write.
	 * @param timestampList
	 * A counter.
	 * @param CornersListGeo
	 * Coordinates lists.
	 * @throws IOException
	 * Exception managing.
	 */
	public void WriteGeorefToCSV(String OutputCSVPath, List<File> fileList, double[] timestampList,
			ArrayList<List<LatLon>> CornersListGeo) throws IOException
	{

		CSVWriter writer = new CSVWriter(new FileWriter(OutputCSVPath), ',', CSVWriter.NO_QUOTE_CHARACTER);
		
		writer.writeNext(new String[] {"image","imcount",
										"ll_Lon","lr_Lon","ur_Lon","ul_Lon",
										"ll_Lat","lr_Lat","ur_Lat","ul_Lat",
										"ll_E","lr_E","ur_E","ul_E",
										"ll_N","lr_N","ur_N","ul_N"
										});
		
		for(int i = 0; i < fileList.size(); i++)
		{
			
			//Lower left corner, Lower right corner, Upper right corner, Upper left corner
			if(CornersListGeo.get(i) != null)
			{
				writer.writeNext(new String [] 
						{
					fileList.get(i).getName(), // path to aerial image file
					String.valueOf(timestampList[i]), // time stamp

					String.valueOf(CornersListGeo.get(i).get(0).getLongitude().degrees), // Lower left corner longitude
					String.valueOf(CornersListGeo.get(i).get(1).getLongitude().degrees), // Lower right corner longitude
					String.valueOf(CornersListGeo.get(i).get(2).getLongitude().degrees), // Upper right corner longitude
					String.valueOf(CornersListGeo.get(i).get(3).getLongitude().degrees), // Upper left corner longitude
					String.valueOf(CornersListGeo.get(i).get(0).getLatitude().degrees), // Lower left corner latitude
					String.valueOf(CornersListGeo.get(i).get(1).getLatitude().degrees), // Lower right corner latitude
					String.valueOf(CornersListGeo.get(i).get(2).getLatitude().degrees), // Upper right corner latitude
					String.valueOf(CornersListGeo.get(i).get(3).getLatitude().degrees), // Upper left corner latitude

						});
			}
		}
		writer.close();
	}
	
	/**
	 * Read the resulting CSV for mapping the WorldWind Frame
	 * @param path
	 * The path where the result CSV is located
	 * @param AerialImagesPath
	 * The path where the images are located
	 * @see ViewResultsInWorldWind
	 */
	public void  RefFootprint(String path, String AerialImagesPath)
	{
    	File mydata_csv = new File(path);
    	
    	au.com.bytecode.opencsv.CSVReader reader = null;
		try 
		{reader = new au.com.bytecode.opencsv.CSVReader(new FileReader(mydata_csv),',', '"','|', 1);}
		catch (FileNotFoundException e2) 
		{e2.printStackTrace();}
    	
    	List<String[]> csvEntries = null;
    	
    	try 
    	{csvEntries = reader.readAll();} 
    	catch (IOException e1)
    	{e1.printStackTrace();}
    	
    	try
    	{reader.close();}
    	catch (IOException e)
    	{e.printStackTrace();}
    	
    	Iterator<String[]> iter = csvEntries.iterator();
    	  
    	// Initialize input arrays
    	// image,timestamp,ll_Lon,lr_Lon,ur_Lon,ul_Lon,ll_Lat,lr_Lat,ur_Lat,ul_Lat,ll_E,lr_E,ur_E,ul_E,ll_N,lr_N,ur_N,ul_N
    	List<String> imPathList = new ArrayList<String>();
    	List<Double> timestampList = new ArrayList<Double>();
    	
    	List<Double> ll_Lon_List = new ArrayList<Double>();
    	List<Double> lr_Lon_List = new ArrayList<Double>();
    	List<Double> ur_Lon_List = new ArrayList<Double>();
    	List<Double> ul_Lon_List = new ArrayList<Double>();
    	List<Double> ll_Lat_List = new ArrayList<Double>();
    	List<Double> lr_Lat_List = new ArrayList<Double>();
    	List<Double> ur_Lat_List = new ArrayList<Double>();
    	List<Double> ul_Lat_List = new ArrayList<Double>();
    	
    	while (iter.hasNext()) 
    	{
    	    String[] row = iter.next();
    	    imPathList.add(AerialImagesPath + File.separator + row[0]);
    	    timestampList.add(Double.parseDouble(row[1]));
    	    
        	ll_Lon_List.add(Double.parseDouble(row[2]));
        	lr_Lon_List.add(Double.parseDouble(row[3]));
        	ur_Lon_List.add(Double.parseDouble(row[4]));
        	ul_Lon_List.add(Double.parseDouble(row[5]));
        	ll_Lat_List.add(Double.parseDouble(row[6]));
        	lr_Lat_List.add(Double.parseDouble(row[7]));
        	ur_Lat_List.add(Double.parseDouble(row[8]));
        	ul_Lat_List.add(Double.parseDouble(row[9]));
    	}
    	
    	int csvSize = imPathList.size();
    	String[] imgPath = new String[csvSize];
    	String[] nameList = new String[csvSize];
    	
    	List<File> fileList = new ArrayList<File>();
    	ArrayList<ArrayList<LatLon>> LatLonList = new ArrayList<ArrayList<LatLon>>();
    	imgPath = imPathList.toArray(imgPath);
    	
    	for(int i = 0; i < csvSize; i++)
    	{
    		fileList.add(new File(imgPath[i]));
    	    String fname = fileList.get(i).getName();
    	    int pos = fname.lastIndexOf(".");
    	    if (pos > 0) 
    	    {
    	        fname = fname.substring(0, pos); //aerial image filename without extension
    	    }
    	    nameList[i] = fname;
    
    	    LatLonList.add(new ArrayList<LatLon>(Arrays.asList(
                LatLon.fromDegrees(ll_Lat_List.get(i), ll_Lon_List.get(i)),
                LatLon.fromDegrees(lr_Lat_List.get(i), lr_Lon_List.get(i)),
                LatLon.fromDegrees(ur_Lat_List.get(i), ur_Lon_List.get(i)),
                LatLon.fromDegrees(ul_Lat_List.get(i), ul_Lon_List.get(i))
    	    		)));
    	}
    	
    	imPath = imgPath;
    	imName = nameList;
    	imFile = fileList;
    	cornersLatLon = LatLonList;
    	
    }
}
