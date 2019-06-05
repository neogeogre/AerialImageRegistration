package airui;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.data.BufferedImageRaster;
import gov.nasa.worldwind.formats.tiff.GeotiffWriter;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.TiledImageLayer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ListIterator;

/**
 * BingImgDownloader download automatically the ground picture from BING using the WorldWind API.
 * @author Geoffrey Vincent
 */
public class BingImgDownloader 
{
	/**
	 * The area of interest
	 */
	private Sector ListOfSector;
	
	/**
	 * Actual bin layer
	 */
	private LayerList ActualLayers;
	
	/**
	 * size of the downloaded image, fixed at 2048 due to NASA download restriction.
	 */
	private final int desiredSize;
	
	/**
	 *Image width.
	 */
	private int width;
	
	/**
	 * Image height.
	 */
	private int height;
	
	/**
	 * Constructor of the class BingImgDownloader
	 * @see BingImgDownloader
	 * @param sectorlist
	 * The area of interest
	 * @param ActLayer
	 * Actual bin layer
	 */
	public BingImgDownloader(Sector sectorlist, LayerList ActLayer)
	{
		ListOfSector = sectorlist;
		desiredSize = 2048;
		ActualLayers = ActLayer;
	}
	
	/**
	 * Creat a geo-tiff in the Outputpath, extracted from the Bing layer.
	 * @param Outputpath
	 *  Write reference image to file in this path.
	 */
	public void fromCurrentLayer(String Outputpath)
	{
		//=====================================================
		// NASA code for downloading the image from bing server
		TiledImageLayer currentLayer = null;
		ListIterator<Layer> iterator = ActualLayers.listIterator();
		while (iterator.hasNext())
		{
		    Object o = iterator.next();
		    if (o instanceof TiledImageLayer)
		    {
		        TiledImageLayer layerTile = (TiledImageLayer) o;
		        if (layerTile.isEnabled())
		        {
		            currentLayer = layerTile;
		        }
		        else
		        {
		        	System.err.println("An error occured during the download.");
		        }
		    }
		}
		//=====================================================

        // Preparing parameters for using the composeImageForSector method.
		AdjustSize();
		
        String mimeType = currentLayer.getDefaultImageFormat();
        
        if (currentLayer.isImageFormatAvailable("image/png"))
        {mimeType = "image/png";}
        else
        {
        	if (currentLayer.isImageFormatAvailable("image/jpg"))
        	{mimeType = "image/jpg";}
        }
		//=====================================================
        
        BufferedImage ImageComposedForAsector = null;
		try 
		{ImageComposedForAsector = currentLayer.composeImageForSector(ListOfSector, width, height, 1d, -1, mimeType, true, null, 300000);} 
		catch (Exception e) 
		{e.printStackTrace();}
		
		// Write reference image to file
		System.out.println("A reference image has been saved to : "  + Outputpath); // + saveToFile.getAbsolutePath());
		
		AVList params = new AVListImpl();

        params.setValue(AVKey.SECTOR, ListOfSector);
        params.setValue(AVKey.COORDINATE_SYSTEM, AVKey.COORDINATE_SYSTEM_GEOGRAPHIC);
        params.setValue(AVKey.PIXEL_FORMAT, AVKey.IMAGE);
        params.setValue(AVKey.BYTE_ORDER, AVKey.BIG_ENDIAN);

        GeotiffWriter writer = null;
        try 
		{
        	File saveToFile = new File(Outputpath); 
        	writer = new GeotiffWriter(saveToFile);
        }
		catch (IOException e) 
		{e.printStackTrace();}
        
        try
        {
            try 
            {writer.write(BufferedImageRaster.wrapAsGeoreferencedRaster(ImageComposedForAsector, params));}
            catch (IllegalArgumentException e) 
            {e.printStackTrace();} 
            catch (IOException e) 
            {e.printStackTrace();}
        }
        finally
        {
            writer.close();
        }
        
	}
	
	/**
	 * Adjust some parameters for the creation of the Tiff image.
	 * @see BingImgDownloader#fromCurrentLayer(String)
	 */
    private void AdjustSize()
    {
    	int[] size = new int[] {desiredSize, desiredSize};

        if (null != ListOfSector && desiredSize > 0)
        {
            LatLon centroid = ListOfSector.getCentroid();
            Angle dLat = LatLon.greatCircleDistance(new LatLon(ListOfSector.getMinLatitude(), ListOfSector.getMinLongitude()),
                new LatLon(ListOfSector.getMaxLatitude(), ListOfSector.getMinLongitude()));
            Angle dLon = LatLon.greatCircleDistance(new LatLon(centroid.getLatitude(), ListOfSector.getMinLongitude()),
                new LatLon(centroid.getLatitude(), ListOfSector.getMaxLongitude()));

            double max = Math.max(dLat.radians, dLon.radians);
            double min = Math.min(dLat.radians, dLon.radians);

            int minSize = (int) ((min == 0d) ? desiredSize : ((double) desiredSize * min / max));

            if (dLon.radians > dLat.radians)
            {
                size[0] = desiredSize; // width
                size[1] = minSize; // height
            }
            else
            {
                size[0] = minSize; // width
                size[1] = desiredSize; // height
            }
        }

        width = size[0];
        height = size[1];
    }
}
