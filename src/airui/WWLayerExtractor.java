package airui;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.Timer;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.util.layertree.LayerTree;
import gov.nasa.worldwindx.examples.ApplicationTemplate;
import gov.nasa.worldwindx.examples.util.HotSpotController;

/**
 * This program call the World wind globe AppFrame only to get the Bing map of the area of interest.
 * @author Geoffrey
 */
public class WWLayerExtractor extends ApplicationTemplate
{
	static String 
	StaticInputCSVPath,
	StaticOutputRefImaDir,
	StaticAerialImagesPath;
	
    public static void main(String[] args, String InputCSVPath, String OutputRefImaDir, String AerialImagesPath)
    {
    	// Path setting used in the AppFrame
    	StaticInputCSVPath = InputCSVPath; 
    	StaticOutputRefImaDir = OutputRefImaDir;  
    	StaticAerialImagesPath = AerialImagesPath;
    	
    	// Launch the world Wind template
    	ApplicationTemplate.start("World Wind Image Matching", WWLayerExtractor.AppFrame.class);
    }

    /**
     * Call the frame developed by NASA World Wind to dowmload the Bing map of the area of interest.
     * @author Geoffrey
     * @see WWLayerExtractor
     */
	public static class AppFrame extends ApplicationTemplate.AppFrame implements ActionListener
	  {	  
		  private static final long serialVersionUID = 1L;
		  private final WorldWindow wwd;
		  protected LayerTree layerTree;  
		  protected RenderableLayer hiddenLayer; 
		  protected HotSpotController controller;
		  
	      public AppFrame()
	      {
	          //=================================================================================================
	          // 								World-Wind application parameters
	          //=================================================================================================
	    	  super(true, true, false);
	          
	          wwd = this.getWwd();
	          layerTree = new LayerTree();
	          
	          // Specify which World Wind layers to display on screen.
	          List<String> EnableLayers = Arrays.asList("Bing Imagery", "View Controls");
	          LayerList layers = wwd.getModel().getLayers();
	          LayerList currentlayers = new LayerList();
	          for (int i = 0; i < EnableLayers.size(); i++)
	          {
	        	  currentlayers.add(layers.getLayerByName(EnableLayers.get(i)));
	          }
	          
	          // Remove layers which are not needed
	          List<Layer> RemovedLayers = LayerList.getLayersRemoved(layers, currentlayers);
	          for (int i = 0; i < RemovedLayers.size(); i++)
	          {
	        	  layers.remove(RemovedLayers.get(i));
	          }
	          
	          // Enable Bing layer
	          layers.getLayerByName("Bing Imagery").setEnabled(true); 	    
	          
	          // Set up a layer to display the on-screen layer tree in the WorldWindow.
	          hiddenLayer = new RenderableLayer();
	          hiddenLayer.setValue(AVKey.DISPLAY_NAME, "Aerial image outlines");
	          hiddenLayer.addRenderable(this.layerTree);
	          this.getWwd().getModel().getLayers().add(this.hiddenLayer);
	          
	          // Mark the layer as hidden to prevent it being included in the layer tree's model. Including the layer in
	          // the tree would enable the user to hide the layer tree display with no way of bringing it back.
	          hiddenLayer.setValue(AVKey.HIDDEN, true);
	          
	          // Add a controller to handle input events on the layer tree.
	          controller = new HotSpotController(this.getWwd());
	          
	          // Update layer panel
	          this.getLayerPanel().update(this.getWwd());
	          
	          LayerList ActualLayers = this.getWwd().getModel().getLayers();
	          //=================================================================================================
	          
	          // Read the csv file
	          CsvManager PathContainer = new CsvManager();
	          try {PathContainer.CsvInput(StaticInputCSVPath, StaticAerialImagesPath);}
	          catch (FileNotFoundException e1) {e1.printStackTrace();}
	          
	          //=================================================================================================
	          // 				Multi-threading for the download of the Bing Ground Images 
	          //=================================================================================================
	          // Creating a loading-bar
	          final loadingBar LoadBarObj = new loadingBar("Wait for downloading ...");
	          LoadBarObj.NumberOfExecutions = PathContainer.imFile.size();
	          LoadBarObj.numberofUpdate = 1;
	          LoadBarObj.ProgBar.setIndeterminate(true);
	          
	          System.out.println("Downloading " + PathContainer.imFile.size() + " ground images from Bing Map.");
	          
	          // Creating a fix-table which will contain future thread.
	          Thread[] DownloadBingThreadGrid = new Thread[PathContainer.imFile.size()];
	          
	          // Initializing each separated Thread for downloading image.
	          for(int i = 0; i < PathContainer.imFile.size(); i++)
	          {
	        	  String PathForDownloadedGroundImage = StaticOutputRefImaDir + PathContainer.imName[i] + "_ref.tif";
	        	  BingImgDownloader ObjForSaveTiffOnDisc = new BingImgDownloader(PathContainer.sector.get(i), ActualLayers);
	        	  DownloadBingThreadGrid[i] = new Thread(new ThreadBingImgDownload(PathForDownloadedGroundImage, ObjForSaveTiffOnDisc, LoadBarObj));
	          }
	          
	    	  // Creating a object from ExecutorService type,allowing you to create exactly the same number of Tread than your needed download.
	    	  ExecutorService ThreadForYourCPU = Executors.newFixedThreadPool(PathContainer.imFile.size());
	          
	          // Launching of each separated thread.
	          for (int i =  0 ; i < PathContainer.imFile.size() ; i++) 
	          {
	        	  ThreadForYourCPU.execute(DownloadBingThreadGrid[i]);
	          }
	          
	          // Waiting for the end of each thread.
	          ThreadForYourCPU.shutdown();
			  try {ThreadForYourCPU.awaitTermination(1, TimeUnit.HOURS);}
			  catch (InterruptedException e) {e.printStackTrace();}
			  
			  // Close the waiting bar.
			  LoadBarObj.LoadBarFrame.dispose();
	          //=================================================================================================
			  
			  Timer t = new Timer(100,this);
			  t.setRepeats(false);
			  t.start();
	      }
	      
		@Override
		public void actionPerformed(ActionEvent arg0)
		{
			this.dispose();
		}
	  }
}