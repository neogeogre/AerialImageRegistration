package airui;


import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwindx.examples.ApplicationTemplate;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Display the transformed image on the WorlWind Globe in calling an AppFrame.
 * @author Geoffrey
 */
public class ViewResultsInWorldWind extends ApplicationTemplate
{    
	static String StaticOutputCSVPath, StaticAerialImagesPath;
	
	/**
	 * Display the transformed image on the WorlWind Globe
	 * @param args
	 * Classic parameters.
	 * @param OutputCSVPath
	 * Path of the CSV.
	 * @param AerialImagesPath
	 * Path of the aerial images.
	 * @param CornersListGeo
	 * List of coordinates corner.
	 * @param csvContainer
	 * Object containing all CSV informations.
	 * @param BadImgnbr
	 * Number of images badly transformed.
	 */
    public static void main(String[] args, String OutputCSVPath, String AerialImagesPath, ArrayList<List<LatLon>> CornersListGeo,CsvManager csvContainer,int BadImgnbr)
    {
    	// Path setting used in the AppFrame
    	StaticOutputCSVPath = OutputCSVPath;
    	StaticAerialImagesPath = AerialImagesPath;
    	
		double LongTot = 0, LatTot = 0;
		for(int i = 0; i < CornersListGeo.size(); i++)
		{
			if(CornersListGeo.get(i) != null)
			{
				LongTot = LongTot + CornersListGeo.get(i).get(0).getLongitude().degrees + 
						CornersListGeo.get(i).get(1).getLongitude().degrees + 
						CornersListGeo.get(i).get(2).getLongitude().degrees + 
						CornersListGeo.get(i).get(3).getLongitude().degrees;
				LatTot = LatTot + CornersListGeo.get(i).get(0).getLatitude().degrees + 
						CornersListGeo.get(i).get(1).getLatitude().degrees + 
						CornersListGeo.get(i).get(2).getLatitude().degrees + 
						CornersListGeo.get(i).get(3).getLatitude().degrees;
			}
		}
		
		double[] CenterOfView = new double[2];
		CenterOfView[0] = LongTot/ ((csvContainer.imFile.size() - BadImgnbr) * 4);
		CenterOfView[1] = LatTot/ ((csvContainer.imFile.size() - BadImgnbr) * 4);
	    
    	// Set the initial configurations for the WW environment, lat, long, alt, and window caption.
    	Configuration.setValue(AVKey.INITIAL_LATITUDE, CenterOfView[1]);
        Configuration.setValue(AVKey.INITIAL_LONGITUDE, CenterOfView[0]);
        Configuration.setValue(AVKey.INITIAL_ALTITUDE, 10000);
        
    	ApplicationTemplate.start("ULM images Geo-referanced on ground with SIFT", ViewResultsInWorldWind.AppFrame.class);
    }
    
    /**
     * Call the frame developed by NASA World Wind to display computing result on a global earth.
     * @author Geoffrey
     * @see ViewResultsInWorldWind
     */
    public static class AppFrame extends ApplicationTemplate.AppFrame
    {
		private static final long serialVersionUID = 1L;
		
		public AppFrame()
        {
			//=================================================================================================
			// 								World-Wind application parameters
			//=================================================================================================
            super(true, true, false);

            CsvManager csvContainer = new CsvManager();
            
            csvContainer.RefFootprint(StaticOutputCSVPath, StaticAerialImagesPath);
            
            for(int i = 0; i < csvContainer.imFile.size(); i++)
            {
            	RenderableLayer layer = new RenderableLayer();
                layer.setName(csvContainer.imName[i]);
                layer.setPickEnabled(false);
                
                SurfaceImage ImgProjectedOnDEM = new SurfaceImage(csvContainer.imPath[i], csvContainer.cornersLatLon.get(i));
                layer.addRenderable(ImgProjectedOnDEM);
                Polyline boundaryProjectedOnDEM = new Polyline(ImgProjectedOnDEM.getCorners(), 0);
                boundaryProjectedOnDEM.setFollowTerrain(true);
                boundaryProjectedOnDEM.setClosed(true);
                boundaryProjectedOnDEM.setPathType(Polyline.RHUMB_LINE);
                boundaryProjectedOnDEM.setColor(new Color(0, 102, 204));
                layer.addRenderable(boundaryProjectedOnDEM);
                
                insertBeforeCompass(this.getWwd(), layer);
                this.getLayerPanel().update(this.getWwd());
            }
          //=================================================================================================
        }
    }
}