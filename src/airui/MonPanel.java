package airui;

import gov.nasa.worldwind.WorldWindow;

import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class MonPanel extends JPanel
{

	private static final long serialVersionUID = 1L;	
	private  DlrefImgPanel dl1;
	private BBAparamPanel BBA1;

	
	public MonPanel(String[] args,  WorldWindow worldWindow)
	{
		super();
		
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.dl1 = new DlrefImgPanel(args, worldWindow);
		this.BBA1 = new BBAparamPanel();
		

		// Initialisation of the tabbed Pane
		JTabbedPane GroupOfPanel = new JTabbedPane();
		GroupOfPanel.setPreferredSize(new Dimension(300, 900));
		GroupOfPanel.add(dl1, "GCP finder");
		GroupOfPanel.add(BBA1,"Bundle Block Adjustment");

		this.add(GroupOfPanel);
	}
	
}
