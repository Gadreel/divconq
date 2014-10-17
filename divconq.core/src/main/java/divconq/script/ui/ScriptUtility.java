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

import javax.swing.*;

import divconq.work.TaskRun;

public class ScriptUtility extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1235058341209416285L;


	public ScriptUtility(TaskRun run) {
		EditorPane epane = new EditorPane();
		
		this.setRootPane(epane);		
		
		this.addWindowListener(new WindowListener() {			
			@Override
			public void windowActivated(WindowEvent arg0) {
			}

			@Override
			public void windowClosed(WindowEvent arg0) {
				epane.shutdown();
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
				epane.start(run);
			}
		});
		
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setTitle("dcScript Debugger");
		
		this.setMinimumSize(new Dimension(500, 500));
		
		this.setExtendedState(this.getExtendedState() | Frame.MAXIMIZED_BOTH);
	}

	public static void goSwing(TaskRun run) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {				
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} 
				catch (Exception x) {
				}
				
				Toolkit.getDefaultToolkit().setDynamicLayout(true);
				new ScriptUtility(run).setVisible(true);
			}
		});
	}

}