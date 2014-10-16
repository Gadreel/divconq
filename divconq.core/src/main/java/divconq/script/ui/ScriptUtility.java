/* ************************************************************************
#
#  DivConq
#
#  http://divconq.com/
#
#  Copyright:
#    Copyright 2014 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package divconq.script.ui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.concurrent.CountDownLatch;

import javax.swing.*;

import divconq.hub.Hub;
import divconq.hub.HubResources;
import divconq.lang.OperationCallback;
import divconq.lang.OperationResult;
import divconq.log.DebugLevel;
import divconq.log.Logger;

public class ScriptUtility extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1235058341209416285L;


	public ScriptUtility(final OperationCallback cb) {
		this.setRootPane(new EditorPane());		
		
		this.addWindowListener(new WindowListener() {			
			@Override
			public void windowActivated(WindowEvent arg0) {
			}

			@Override
			public void windowClosed(WindowEvent arg0) {
				cb.completed();
			}

			@Override
			public void windowClosing(WindowEvent arg0) {
			}

			@Override
			public void windowDeactivated(WindowEvent arg0) {
			}

			@Override
			public void windowDeiconified(WindowEvent arg0) {
			}

			@Override
			public void windowIconified(WindowEvent arg0) {
			}

			@Override
			public void windowOpened(WindowEvent arg0) {
			}
		});
		
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setTitle("dcScript FT Demo");
		
		//this.pack();
		
		this.setMinimumSize(new Dimension(500, 500));
		
		this.setExtendedState(this.getExtendedState() | Frame.MAXIMIZED_BOTH);
	}

	public static void goSwing(final OperationCallback cb) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {				
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} 
				catch (Exception x) {
				}
				
				Toolkit.getDefaultToolkit().setDynamicLayout(true);
				new ScriptUtility(cb).setVisible(true);
			}
		});
	}
	
	public static void main(String[] args) throws Exception {
		HubResources resources = new HubResources();
		resources.setSquadId("dcbackend0050x");
		resources.setDebugLevel(DebugLevel.Warn);
		OperationResult or = resources.init();
		
		if (or.hasErrors()) {
			Logger.error("Unable to continue, hub resources not properly initialized");
			return;
		}
		
		final CountDownLatch latch = new CountDownLatch(1);
		
		Hub.instance.start(resources);

		ScriptUtility.goSwing(new OperationCallback() {			
			@Override
			public void callback() {
				latch.countDown();
			}
		});
		
		latch.await();
		
		Hub.instance.stop();
	}


}