package airui;


import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

/**
 * Class for displaying  a loading bar.
 * @version 7 august 2014
 * @author Geoffrey
 */
public class loadingBar extends JPanel 
{
	private static final long serialVersionUID = 1L;
	JProgressBar ProgBar;
	JFrame LoadBarFrame;
	int
	NumberOfExecutions,
	currentExecutionIncrement,
	numberofUpdate;
	static final int
	MY_MINIMUM = 0,
	MY_MAXIMUM = 100;
	
	/**
	 * Create and display a frame
	 * @param BigTitle
	 * Title of the loading bar.
	 */
	public loadingBar(String BigTitle)
	{
	    // initialize a Progress Bar
		ProgBar = new JProgressBar();
		ProgBar.setMinimum(MY_MINIMUM);
		ProgBar.setMaximum(MY_MAXIMUM);
		ProgBar.setStringPainted(true);
	    this.add(ProgBar); // add to JPanel
	    
	    // Creating a jFrame
        LoadBarFrame = new JFrame(BigTitle);
        LoadBarFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        LoadBarFrame.setContentPane(this);
        LoadBarFrame.pack();
        LoadBarFrame.setVisible(true);
        LoadBarFrame.setLocationRelativeTo(null);
        LoadBarFrame.setSize(300, 70);
	}
	  
	/**
	 * Increase the percentage of the bar.
	 */
	public synchronized void updateBar()
	{
		currentExecutionIncrement = currentExecutionIncrement + 1;
		int newValue = currentExecutionIncrement * 100 / (NumberOfExecutions * numberofUpdate);
		ProgBar.setValue(newValue);
	}
}