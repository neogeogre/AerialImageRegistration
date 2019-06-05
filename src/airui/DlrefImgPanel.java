package airui;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Polyline;
import gov.nasa.worldwind.render.SurfaceImage;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

import gov.nasa.worldwindx.examples.LayerPanel;
import gov.nasa.worldwind.WorldWindow;

public class DlrefImgPanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	
	private String
	CurrentDirectory,
	AerialImagesPath,
	OutputRefImaDir,
	InputCSVPath,
	OutputCSVPath;
	private double textBox;
	private DlrefImgPanel ThisPanel;
	private final JButton LaunchButt, DisplayButt;
	private final JCheckBox DlcheckBox;
	public JProgressBar ProgBar;
	private int NumberOfExecutions,
	currentExecutionIncrement,
	numberofUpdate;
	static String[] Args;

	private final WorldWindow actWWframe;
	private RenderableLayer layer;
	private ArrayList<List<LatLon>> CornersListGeo;
	
	public DlrefImgPanel(String[] args,  WorldWindow WWframe)
	{
		super();
                Args = args;
		actWWframe = WWframe;

		this.setPreferredSize(new Dimension(200,300));
		
	    // initialize a Progress Bar
		ProgBar = new JProgressBar();
		ProgBar.setMinimum(0);
		ProgBar.setMaximum(100);
		ProgBar.setStringPainted(true);
		ProgBar.setVisible(true);
		ProgBar.setValue(0);
		
		JLabel Text1 = new JLabel("Dossier : ");
		
		final JTextField pathfield = new JTextField("C:/Users/Geoffrey/Desktop/AIR/data");
		pathfield.addKeyListener(new KeyListener() 
		{
                        @Override
			public void keyTyped(KeyEvent arg0) {}
                        @Override
			public void keyReleased(KeyEvent ke) 
			{
				JTextField pathfield = (JTextField) ke.getSource();
//				ld = Double.parseDouble(pathfield.getText());
				CurrentDirectory = pathfield.getText();
			}
                        @Override
			public void keyPressed(KeyEvent arg0) {}
		});
		
		DlcheckBox = new JCheckBox("Download ref images");
		DlcheckBox.setMnemonic(KeyEvent.VK_C);
		DlcheckBox.setSelected(false);
		
		JButton DirectoryButt = new JButton("Set directory");
		DirectoryButt.addActionListener(new ActionListener() 
				{
					public void actionPerformed(ActionEvent actionEvent) 
					{
						CurrentDirectory = null;
						DirectoryManager();
					}
				});
		
		LaunchButt = new JButton("Computing");
		LaunchButt.addActionListener(new ActionListener() 
				{
					public void actionPerformed(ActionEvent actionEvent) 
					{
						CurrentDirectory = pathfield.getText();
						DirectoryManager();
						AerialImageregistration();
					}
				});
		
		
		DisplayButt = new JButton("Display Results");
		DisplayButt.addActionListener(new ActionListener() 
				{
					public void actionPerformed(ActionEvent actionEvent) 
					{
						DisplayResults();
					}
				});
		
		DirectoryButt.setEnabled(true);
		LaunchButt.setEnabled(true);
		
		
		// add to JPanel
		this.add(Text1);
		this.add(pathfield);
		this.add(DirectoryButt);
		this.add(DlcheckBox);
		this.add(LaunchButt);
		this.add(ProgBar); 
		this.add(DisplayButt); 
	}
	
	void DirectoryManager()
	{
		  if (CurrentDirectory == null)
		  {
		  do
		  {
			  JFileChooser chooser = new JFileChooser();
			  chooser.setDialogTitle("Choose your work folder");
			  chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			  chooser.showOpenDialog(null);
			  CurrentDirectory = chooser.getSelectedFile().getPath();
		  }
		  while(CurrentDirectory == null);
		  }
		  
		  final String BadAerialImagesPath = CurrentDirectory + File.separator + "AerialImages" + File.separator;
		  AerialImagesPath = BadAerialImagesPath.replace('\\', '/'); // directory where aerial images are stored
		  
		  String dwr = new String();
		  dwr = "BingMapRef";
		  final String BadOutputRefImaDir = CurrentDirectory + File.separator + dwr + File.separator;
		  new File(BadOutputRefImaDir).mkdir();
		  OutputRefImaDir = BadOutputRefImaDir.replace('\\', '/'); // directory where geo-tiff downloaded from Bing map will be stored
		  
		  final String BadInputCSVPath = CurrentDirectory + File.separator + "ApproximateCoordinates.csv"; 
		  InputCSVPath = BadInputCSVPath.replace('\\', '/'); // location of the csv containing approximates coordinates corners known before computing
		  
		  final String BadOutputCSVPath = CurrentDirectory + File.separator + "AIRresults.csv";
		  new File(BadOutputCSVPath);
		  OutputCSVPath = BadOutputCSVPath.replace('\\', '/'); // location where the csv containing aerial coordinates corners results will be stored
		  
		  LaunchButt.setEnabled(true);

	}
	
	void AerialImageregistration()
	{
		ProgBar.setValue(0);
		ThisPanel = this;
		
		ExecutorService ShiftedExecutor = Executors.newFixedThreadPool(1);

		ShiftedExecutor.execute( new Runnable() 
		{
			public void run() 
			{
//				System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	      
				if(DlcheckBox.getSelectedObjects() != null)
				{ // Launch the World Wind viewer needed to download Bing Layers
					WWLayerExtractor.main(Args, InputCSVPath, OutputRefImaDir, AerialImagesPath);
				}
	      
				// Start a Timer
				long ComputationTimer = System.currentTimeMillis();
		  
				// Read the csv file which contain aproximate aerial coordinate
				CsvManager csvContainer = new CsvManager();
				try {csvContainer.CsvInput(InputCSVPath, AerialImagesPath);}
				catch (FileNotFoundException e1)
				{e1.printStackTrace();}
	      
				//=================================================================================================
				// 			Multi-threading computation with Future and Callable interface
				//================================================================================================= 	  
				// Initialise the loading-bar
				NumberOfExecutions = csvContainer.imFile.size();
				numberofUpdate = 5; 
	      
				// ExecutorService allow your computer to create exactly the same number of simultaneous threads than your number of CPU core.
				ExecutorService executorForYourCPU = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	      
				// Creating a dynamic list of Future which will contain the coordinate results of each Thread
				ArrayList<Future<ArrayList<LatLon>>> futuresContainer =  new ArrayList<Future<ArrayList<LatLon>>>();
	      
				for (int i =  0 ; i < csvContainer.imFile.size() ; i++) 
				{
					String GroundPath = OutputRefImaDir + csvContainer.imName[i] + "_ref.tif";
	    	  
					// Creating each thread and stocking in a dynamic list of Callable
					Callable<ArrayList<LatLon>> TheIemeThread = new ThreadSiftGeoreference(GroundPath, csvContainer.imFile.get(i).getPath(),
							csvContainer.sector.get(i), csvContainer.imName[i], ThisPanel);
	    	  
					// Launching the execution of each Thread
					futuresContainer.add(executorForYourCPU.submit(TheIemeThread));
				}
	      
				// Creating dynamic list for stocking Geographic coordinates results
				CornersListGeo = new ArrayList<List<LatLon>>();
				int BadImgnbr = 0;
	      
				// Extracting the coordinates results from the Future list and counting images which didn't work.
				for (int i =  0 ; i < csvContainer.imFile.size() ; i++)
				{
					try 
					{ // if no result is given back to the CornersListGeo from one thread after 1 hours, this one thread is closed automatically
	    		  CornersListGeo.add(futuresContainer.get(i).get(1, TimeUnit.HOURS));
					}
					catch (InterruptedException e) {e.printStackTrace();}
					catch (ExecutionException e) {e.printStackTrace();} 
					catch (TimeoutException e) {e.printStackTrace();}
	    	  
					if (CornersListGeo.get(i) == null)
					{
						++BadImgnbr;
					}
				}
		  
				// Closing the ExecutorService and waiting for the end of each Thread
				executorForYourCPU.shutdown();
	      
				// Close the loading-bar
	      
				//=================================================================================================
				// Export results to AIRresults.csv file  
				try
				{csvContainer.WriteGeorefToCSV(OutputCSVPath, csvContainer.imFile, csvContainer.imCount, CornersListGeo);} 
				catch (IOException e) 
				{e.printStackTrace();}
	      
				System.out.println("");
				System.out.println("number of aerial images :                       " + csvContainer.imFile.size());
				System.out.println("number of aerial images correctly transformed : " + (csvContainer.imFile.size() - BadImgnbr));
				System.out.println("number of aerial images badly transformed :     " + BadImgnbr);
				System.out.println("");
	      
				//=================================================================================================

				// Display the time taking for the computation 
				System.out.println("Elapsed computing time : " + (System.currentTimeMillis() - ComputationTimer)/1000.0 + " seconds");

			}
		});
		ShiftedExecutor.shutdown();
		ProgBar.setValue(0);
	}

	void DisplayResults()
	{
		// Display results in the World Wind viewer with the images which were correctly transformed
        
        CsvManager csvContainer2 = new CsvManager();
        csvContainer2.RefFootprint(OutputCSVPath, AerialImagesPath);
        
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
		CenterOfView[0] = LongTot/ ((csvContainer2.imFile.size()) * 4);
		CenterOfView[1] = LatTot/ ((csvContainer2.imFile.size()) * 4);
	    
    	// Set the initial configurations for the WW environment, lat, long, alt, and window caption.
		Configuration.setValue(AVKey.INITIAL_LATITUDE, CenterOfView[1]);
        Configuration.setValue(AVKey.INITIAL_LONGITUDE, CenterOfView[0]);
        Configuration.setValue(AVKey.INITIAL_ALTITUDE, 10000);
        
        

        
        for(int i = 0; i < csvContainer2.imFile.size(); i++)
        {
        	layer = new RenderableLayer();
            layer.setName(csvContainer2.imName[i]);
            layer.setPickEnabled(false);
            
            SurfaceImage ImgProjectedOnDEM = new SurfaceImage(csvContainer2.imPath[i], csvContainer2.cornersLatLon.get(i));
            layer.addRenderable(ImgProjectedOnDEM);
            
            // boundary colored.
            Polyline boundaryProjectedOnDEM = new Polyline(ImgProjectedOnDEM.getCorners(), 0);
            boundaryProjectedOnDEM.setFollowTerrain(true);
            boundaryProjectedOnDEM.setClosed(true);
            boundaryProjectedOnDEM.setPathType(Polyline.RHUMB_LINE);
            boundaryProjectedOnDEM.setColor(new Color(0, 102, 204));
            layer.addRenderable(boundaryProjectedOnDEM);
            
            // For drawing directly in WorldWind !!!
            LayerList StockLayer = actWWframe.getModel().getLayers();
            StockLayer.add(layer);
            LayerPanel OtherThing = new LayerPanel(actWWframe);
            OtherThing.update(actWWframe);
        } 
	}
	
	public synchronized void updateBar()
	{
		currentExecutionIncrement = currentExecutionIncrement + 1;
		int newValue = currentExecutionIncrement * 100 / (NumberOfExecutions * numberofUpdate);
		ProgBar.setValue(newValue);
	}
}