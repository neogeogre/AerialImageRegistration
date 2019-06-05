package airui;

import java.awt.BorderLayout;
import gov.nasa.worldwindx.examples.ApplicationTemplate;

public class AIRUImain extends ApplicationTemplate
{
	static String[] Args;
	
	public static void main(String[] args)
	  {
		Args = args;
		ApplicationTemplate.start("AIR Tool", AppFrame.class);
	  }
	
	public static class AppFrame extends ApplicationTemplate.AppFrame
    {
		
		private static final long serialVersionUID = 1L;
		public AppFrame()
        {
            super(true, false, false);
            // Ajout d'un panel
            MonPanel GeneralPanel =  new MonPanel(Args, this.getWwd());
            this.getContentPane().add(GeneralPanel, BorderLayout.EAST);
            
        }
    }
}