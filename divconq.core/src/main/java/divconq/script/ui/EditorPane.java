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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;

import divconq.hub.Hub;
import divconq.lang.OperationObserver;
import divconq.lang.OperationResult;
import divconq.script.Activity;
import divconq.script.IDebugger;
import divconq.session.Session;
import divconq.struct.CompositeStruct;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.struct.builder.JsonStreamBuilder;
import divconq.util.IOUtil;
import divconq.work.Task;
import divconq.work.TaskLogger;
import divconq.work.TaskRun;

@SuppressWarnings("rawtypes")
public class EditorPane extends JRootPane implements SyntaxConstants, IDebugger {
	private static final long serialVersionUID = -162698015493544827L;
	
	protected RSyntaxTextArea editor = null;
	protected JTextArea console = null;
	protected JList stacklst = null;
	protected StackListModel stackmodel = new StackListModel();
	protected JList varslst = null;
	protected JTextArea vardetail = null;
	
	protected TaskRun currrun = null;
	protected Session fssession = null;
	protected RecordStruct debuginfo = null;

	protected JFileChooser fc = new JFileChooser(".");

	protected File currfile = null;

	protected JLabel statuslbl = new JLabel("Status: stopped");
	protected SmallButton runbtn = null;
	protected SmallButton stepbtn = null;
	
	protected Timer uitimer = null;
	protected long lastinstrun = 0;
	
	protected TaskLogger logfmt = new TaskLogger();
	
	@SuppressWarnings("unchecked")
	public EditorPane() {
		// -------------------------- LEFT --------------------------------
		
		// EDITOR
		this.editor = this.createTextArea();
		this.editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
		
		// scroll the editor
		RTextScrollPane edscroll = new RTextScrollPane(this.editor, true);
		
		Gutter gutter = edscroll.getGutter();
		gutter.setBookmarkingEnabled(true);
		URL url = this.getClass().getClassLoader().getResource("bookmark.png");		// review/remove or import location TODO
		gutter.setBookmarkIcon(new ImageIcon(url));
		
		edscroll.setMinimumSize(new Dimension(300, 250));
		//edscroll.setPreferredSize(new Dimension(1000, 1000));
		
		// CONSOLE		
		JLabel conlbl = new JLabel("Console");
		conlbl.setBorder(BorderFactory.createEmptyBorder(4,8,2,8));
		conlbl.setAlignmentX(Component.LEFT_ALIGNMENT);
		
	    this.console = new JTextArea();
	    
	    // show bottom of text automatically
	    ((DefaultCaret)this.console.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);	
		
	    // scroll the console
	    JScrollPane conscroll = new JScrollPane(this.console, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		conscroll.setMinimumSize(new Dimension(300, 250));
	    conscroll.setAlignmentX(Component.LEFT_ALIGNMENT);
	    
		JPanel conPane = new JPanel();
		conPane.setLayout(new BoxLayout(conPane, BoxLayout.PAGE_AXIS));
		
		conPane.add(conlbl);
		conPane.add(conscroll);
	    
		final JSplitPane leftsplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, edscroll, conPane);		
		
		// -------------------------- RIGHT --------------------------------
		
		ImageIcon pbicon = new ImageIcon(this.getClass().getResource("/divconq/script/ui/media-playback-start.png"), "Run script");
		ImageIcon psicon = new ImageIcon(this.getClass().getResource("/divconq/script/ui/media-playback-stop.png"), "Stop script");
		ImageIcon sficon = new ImageIcon(this.getClass().getResource("/divconq/script/ui/media-seek-forward.png"), "Step into script");		
		
		Action stepact = new AbstractAction("Step", sficon) {
			private static final long serialVersionUID = 6291259738564026019L;

			public void actionPerformed(ActionEvent e) {
				EditorPane.this.step();
			}
		};
		
		Action runact = new AbstractAction("Run", pbicon) {
			private static final long serialVersionUID = -7125666000153246057L;

			public void actionPerformed(ActionEvent e) {
				EditorPane.this.run();
			}
		};
		
		Action stopact = new AbstractAction("Stop", psicon) {
			private static final long serialVersionUID = 1389157371678339040L;

			public void actionPerformed(ActionEvent e) {
				EditorPane.this.stop(true);
			}
		};
		
		JToolBar toolBar = new JToolBar();
		toolBar.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		this.stepbtn = new SmallButton(stepact, "Step into script"); 
		toolBar.add(this.stepbtn);
		
		this.runbtn = new SmallButton(runact, "Run script");
		toolBar.add(this.runbtn);
		
		toolBar.add(new SmallButton(stopact, "Stop script"));
		
		toolBar.addSeparator(new Dimension(24, 4));
		//toolBar.add(new JLabel(" | "));
		toolBar.add(this.statuslbl);
		
		JLabel stacklbl = new JLabel("Stack");
		stacklbl.setBorder(BorderFactory.createEmptyBorder(4,8,2,8));
		stacklbl.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		this.stacklst = new JList(this.stackmodel);
		this.stacklst.setCellRenderer(new ListCellRenderer() {			
			@Override
			public Component getListCellRendererComponent(JList list, Object value,
					int index, boolean isSelected, boolean cellHasFocus) {
				RecordStruct rec = (RecordStruct)value;
				
				JLabel lbl = new JLabel(rec.getFieldAsString("Command"));
				lbl.setOpaque(true);
				
				if (isSelected) {
		            lbl.setBackground(list.getSelectionBackground());
		            lbl.setForeground(list.getSelectionForeground());
		        } 
				else {
		        	lbl.setBackground(list.getBackground());
		        	lbl.setForeground(list.getForeground());
		        }
				
				return lbl;
			}
		});
		
		this.stacklst.addListSelectionListener(new ListSelectionListener() {			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				RecordStruct currinst = null;
				int pos = EditorPane.this.stacklst.getSelectedIndex();
				
				if (pos != -1)
					currinst = (RecordStruct) EditorPane.this.stacklst.getSelectedValue();
				
				EditorPane.this.varslst.setModel(new VarsListModel((currinst == null) ? null : currinst.getFieldAsRecord("Variables")));
			}
		});

		this.stacklst.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent arg0) {
				int idx = EditorPane.this.stacklst.getSelectedIndex();
				
				if (idx != -1) {
					RecordStruct currinst = (RecordStruct) EditorPane.this.stacklst.getSelectedValue();
					
					long line = currinst.getFieldAsInteger("Line") - 1;
					
					try {
						int pos = EditorPane.this.editor.getLineStartOffset((int)line);
						EditorPane.this.editor.setCaretPosition(pos);
					} 
					catch (BadLocationException x) {
					}
				}
			}
		});
		
	    JScrollPane stackscroll = new JScrollPane(this.stacklst, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	    stackscroll.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		JLabel varslbl = new JLabel("Selected Level Variables");
		varslbl.setBorder(BorderFactory.createEmptyBorder(4,8,2,8));
		varslbl.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		this.varslst = new JList();
		
		this.varslst.addListSelectionListener(new ListSelectionListener() {			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				int pos = EditorPane.this.varslst.getSelectedIndex();
				
				if (pos != -1) {
					String currvar = (String) ((VarsListModel)EditorPane.this.varslst.getModel()).getFormatted(pos);
				
					if (currvar != null)
						 EditorPane.this.vardetail.setText(currvar);
				}
			}
		});
		
	    JScrollPane varsscroll = new JScrollPane(this.varslst, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	    varsscroll.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		JLabel vardlbl = new JLabel("Selected Variable Detail");
		vardlbl.setBorder(BorderFactory.createEmptyBorder(4,8,2,8));
		vardlbl.setAlignmentX(Component.LEFT_ALIGNMENT);
		
	    this.vardetail = new JTextArea();
		
	    // scroll the console
	    JScrollPane varscroll = new JScrollPane(this.vardetail, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	    varscroll.setAlignmentX(Component.LEFT_ALIGNMENT);
	    
		JPanel debugPane = new JPanel();
		
		debugPane.setLayout(new BoxLayout(debugPane, BoxLayout.PAGE_AXIS));
		
		debugPane.add(toolBar);
		debugPane.add(stacklbl);
		debugPane.add(stackscroll);
		debugPane.add(varslbl);
		debugPane.add(varsscroll);
		debugPane.add(vardlbl);
		debugPane.add(varscroll);
		
		debugPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		
	    
		final JSplitPane bigsplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftsplit, debugPane);
		
		this.getContentPane().add(bigsplit);
		
		this.setJMenuBar(this.createMenuBar());
		
		// resizing
		this.addComponentListener(new ComponentAdapter() {			
			@Override
			public void componentShown(ComponentEvent arg0) {
				bigsplit.setDividerLocation(0.66);
				leftsplit.setDividerLocation(0.7);
			}
			
			@Override
			public void componentResized(ComponentEvent arg0) {
				bigsplit.setDividerLocation(0.66);
				leftsplit.setDividerLocation(0.7);
			}
		});
	}
	
	public void start(TaskRun r) {
		this.currrun = r;
		
		if (this.currrun == null) {
			// TODO load last - better management
			this.setText(new File("./packages/dcTest/dcs/fileops-copy-2.dcs.xml"));
		}
		else {
			Activity act = (Activity) r.getTask().getWork();
			act.setInDebugger(true);
			act.setDebugger(this);
			
			this.setText(act.getScript().getSource());
			
			//this.console.setText(r.getLog());		// TODO just the messages
			
			r.getMessages().recordStream().forEach(entry -> this.log(entry));
			
			this.lastinstrun = act.getRunCount();
			
			r.addObserver(new OperationObserver() {
				@Override
				public void log(OperationResult or, RecordStruct entry) {
					EditorPane.this.log(entry);
				}
				
				@Override
				public void step(OperationResult or, int num, int of, String name) {
					EditorPane.this.console.append("##### Step " + num + " of " + of + ": " + name + " #####\n");
				}
				
				@Override
				public void progress(OperationResult or, String msg) {
					EditorPane.this.console.append("***** " + msg + " *****\n");
				}
				
				@Override
				public void amount(OperationResult or, int v) {
					EditorPane.this.console.append(">>>>> " + v + "% <<<<<\n");
				}
			});
		}
		
        ActionListener taskPerformer = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                //...Perform a task...

                //System.out.println("Reading SMTP Info.");
            	
            	EditorPane.this.updateDebuggerUI();
            }
        };
        
        this.uitimer = new Timer(1000, taskPerformer);
        this.uitimer.start();
	}
	
	public void shutdown() {
		if (this.uitimer != null)
			this.uitimer.stop();
	}
	
	public void prepTask(boolean debugmode) {
		this.console.setText("");
		this.lastinstrun = 0;
		
		Activity act = new Activity();
		act.setDebugMode(debugmode);
		act.setInDebugger(true);
		act.setDebugger(this);
		
		OperationResult compilelog = act.compile(this.editor.getText());
		
		// update text pane in case includes where added - TODO make sure user does not save over their file with includes intact
		this.setText(act.getScript().getSource());
		
		if (compilelog.hasErrors()) {
			JOptionPane.showMessageDialog(EditorPane.this, "Error compiling script: " + compilelog.getMessage(), "Compile Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		Task task = new Task()
			.withRootContext()
			.withTitle(act.getScript().getXml().getAttribute("Title", "Debugging dcScript"))	
			.withTimeout(0)							// no timeout in editor mode
			.withWork(act);
		
		this.currrun = new TaskRun(task);
				
		this.currrun.addObserver(new OperationObserver() {
			@Override
			public void log(OperationResult or, RecordStruct entry) {
				EditorPane.this.log(entry);
			}
			
			@Override
			public void step(OperationResult or, int num, int of, String name) {
				EditorPane.this.console.append("##### Step " + num + " of " + of + ": " + name + " #####\n");
			}
			
			@Override
			public void progress(OperationResult or, String msg) {
				EditorPane.this.console.append("***** " + msg + " *****\n");
			}
			
			@Override
			public void amount(OperationResult or, int v) {
				EditorPane.this.console.append(">>>>> " + v + "% <<<<<\n");
			}
		});
	}
	
	public void log(RecordStruct entry) {
		this.console.append(this.logfmt.formatLogEntry(entry) + "\n");
	}
	
	public void run() {
		if (this.currrun == null)
			this.prepTask(false);
		
		if (this.currrun == null)
			return;
		
		((Activity) this.currrun.getTask().getWork()).setDebugMode(false);
		
		this.statuslbl.setText("Status: running");
		this.stepbtn.setEnabled(false);
		this.runbtn.setEnabled(false);
		
		Hub.instance.getWorkPool().submit(this.currrun);
	}
	
	public void step() {
		if (this.currrun == null) 
			this.prepTask(true);
		
		if (this.currrun == null)
			return;
		
		((Activity) this.currrun.getTask().getWork()).setDebugMode(true);
		
		this.lastinstrun = 0;
		
		this.statuslbl.setText("Status: executing");
		this.editor.setEditable(false);
		this.stepbtn.setEnabled(false);
		this.runbtn.setEnabled(false);
		
		Hub.instance.getWorkPool().submit(this.currrun);
	}
	
	public void updateDebuggerUI() {
		TaskRun run = this.currrun;
		
		if (run != null) {
			Activity act = (Activity) run.getTask().getWork();
			
			// TODO maybe also every 5 seconds refresh?
			// only update UI if a new instruction has run
			//if (this.lastinstrun == act.getRunCount())
			//	return;
			
			this.debuginfo = act.getDebugInfo();
	
			ListStruct stack = this.debuginfo.getFieldAsList("Stack");
			
			RecordStruct currinst = stack.getItemAsRecord(stack.getSize() - 1);
			long line = currinst.getFieldAsInteger("Line") - 1;
			
			try {
				int linemustbevisible = Math.max((int)line - 2, 0);

				// make sure at least a couple lines appear above the current instruction
				int viewpos = this.editor.getLineStartOffset(linemustbevisible);
				Rectangle viewRect = this.editor.modelToView(viewpos);
				
				if (!this.editor.getVisibleRect().contains(viewRect)) {
					this.editor.scrollRectToVisible(viewRect);
				}
				else {
					// also try to make it so that at least a few lines show below current instruction
					linemustbevisible = (int)line + Math.min(this.editor.getLineCount() - (int)line, 5);
	
					viewpos = this.editor.getLineStartOffset(linemustbevisible);
					viewRect = this.editor.modelToView(viewpos);
					
					if (!this.editor.getVisibleRect().contains(viewRect)) 
						this.editor.scrollRectToVisible(viewRect);
				}
				
				int pos = this.editor.getLineStartOffset((int)line);
				this.editor.setCaretPosition(pos);
			} 
			catch (BadLocationException x) {
			}
			
			this.stackmodel.update(stack);
			
			if (act.isDebugMode() && (this.lastinstrun < act.getRunCount())) {
				this.statuslbl.setText("Status: ready");
				this.stepbtn.setEnabled(true);
				this.runbtn.setEnabled(true);
				
				this.lastinstrun = act.getRunCount();
			}
			
			if (act.isExitFlag()) {
				this.statuslbl.setText("Status: ready");
				this.stepbtn.setEnabled(true);
				this.runbtn.setEnabled(true);
				
				System.out.println("Script done. #" + act.getRuntime() + " - Code: " + run.getCode() + " - Message: " + run.getMessage());			
				this.stop(false);
			}
		}
	}

	@Override
	public void console(String msg) {
		this.console.append(msg + "\n");
	}	
	
	protected Runnable uirefresher = new Runnable() {		
		@Override
		public void run() {
			EditorPane.this.updateDebuggerUI();
		}
	};
	
	@Override
	public void stepped() {
		if (SwingUtilities.isEventDispatchThread()) 
			this.uirefresher.run();
		else
			SwingUtilities.invokeLater(this.uirefresher);
	}
	
	public void stop(boolean kill) {
		if (this.currrun == null)
			return;
		
		this.stackmodel.clear();
		this.vardetail.setText("");
		
		if (kill)
			this.currrun.kill();
		else
			this.currrun.complete();
		
		this.currrun = null;
		
		this.editor.setCaretPosition(0);
		this.editor.setEditable(true);
		
		this.statuslbl.setText("Status: stopped");
		this.stepbtn.setEnabled(true);
		this.runbtn.setEnabled(true);
	}
	
	private JMenuBar createMenuBar() {
		JMenuBar mb = new JMenuBar();

		JMenu menu = new JMenu("File");
		
		menu.add(new AbstractAction("New") {			
			private static final long serialVersionUID = 4212493611151700478L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
	        	EditorPane.this.currfile = null;
	        	EditorPane.this.editor.setText("");
		     }
		});
		
		menu.add(new AbstractAction("Open...") {			
			private static final long serialVersionUID = 4212493611151700478L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
		        int returnVal = EditorPane.this.fc.showOpenDialog(EditorPane.this);

		        if (returnVal == JFileChooser.APPROVE_OPTION) {
		        	EditorPane.this.setText(fc.getSelectedFile());
		        } 		
		     }
		});
		
		menu.add(new AbstractAction("Save") {			
			private static final long serialVersionUID = 4212493611151700478L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (EditorPane.this.currfile == null) {
			        int returnVal = EditorPane.this.fc.showSaveDialog(EditorPane.this);

			        if (returnVal != JFileChooser.APPROVE_OPTION) 
			        	return;
			        
			        EditorPane.this.currfile = fc.getSelectedFile();
				}
				
				OperationResult sres = IOUtil.saveEntireFile(EditorPane.this.currfile.toPath(), EditorPane.this.editor.getText());

				if (sres.hasErrors())
					JOptionPane.showMessageDialog(EditorPane.this, "Error saving file: " + sres.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
			}
		});
		
		menu.add(new AbstractAction("Save As...") {			
			private static final long serialVersionUID = 4212493611151700478L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
		        int returnVal = EditorPane.this.fc.showSaveDialog(EditorPane.this);

		        if (returnVal == JFileChooser.APPROVE_OPTION) {
		        	EditorPane.this.currfile = fc.getSelectedFile();
					
					OperationResult sres = IOUtil.saveEntireFile(EditorPane.this.currfile.toPath(), EditorPane.this.editor.getText());

					if (sres.hasErrors())
						JOptionPane.showMessageDialog(EditorPane.this, "Error saving file: " + sres.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
		        } 		
			}
		});
		
		menu.add(new AbstractAction("Exit") {			
			private static final long serialVersionUID = 4212493611151700478L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				((JFrame) EditorPane.this.getParent()).dispose();
			}
		});
		
		mb.add(menu);

		menu = new JMenu("Themes");
		menu.add(new JMenuItem(new ThemeAction("Default", "/default.xml")));		// TODO move these to better location in resources
		menu.add(new JMenuItem(new ThemeAction("Dark", "/dark.xml")));
		menu.add(new JMenuItem(new ThemeAction("Eclipse", "/eclipse.xml")));
		menu.add(new JMenuItem(new ThemeAction("Visual Studio", "/vs.xml")));
		mb.add(menu);

		menu = new JMenu("Help");
		JMenuItem item = new JMenuItem(new AboutAction());
		menu.add(item);
		mb.add(menu);

		return mb;
	}

	/**
	 * Creates the text area for this application.
	 *
	 * @return The text area.
	 */
	private RSyntaxTextArea createTextArea() {
		RSyntaxTextArea textArea = new RSyntaxTextArea(25, 70);
		textArea.setCaretPosition(0);
		textArea.requestFocusInWindow();
		textArea.setMarkOccurrences(true);
		textArea.setAntiAliasingEnabled(true);
		textArea.setCodeFoldingEnabled(true);
		textArea.setClearWhitespaceLinesEnabled(false);
		
		//textArea.setFont(new Font("VeraMono.ttf", Font.PLAIN, 13));
		//for (int i=0; i<textArea.getSyntaxScheme().styles.length; i++) {
		//	if (textArea.getSyntaxScheme().styles[i]!=null) {
		//		textArea.getSyntaxScheme().styles[i].font = textArea.getFont();
		//	}
		//}
		
		return textArea;
	}

	/**
	 * Focuses the text area.
	 */
	void focusTextArea() {
		this.editor.requestFocusInWindow();
	}

	/**
	 * Sets the content in the text area to that in the specified resource.
	 *
	 * @param resource The resource to load.
	 */
	private void setText(File resource) {
		this.editor.setText("");
		this.currfile = resource;
		
		BufferedReader r = null;
		
		try {
			r = new BufferedReader(new InputStreamReader(
					new FileInputStream(resource), "UTF-8"));
			
			this.editor.read(r, null);
			
			this.editor.setCaretPosition(0);
			this.editor.discardAllEdits();
		} 
		catch (RuntimeException x) {
			throw x; // FindBugs
		} 
		catch (Exception x) { 
			// TODO error
			this.editor.setText("Type here to see syntax highlighting");
		}
		
		try {
			if (r != null)
				r.close();
		}
		catch (IOException x) { 
		}
	}
	
	private void setText(String src) {
		this.currfile = null;
		
		this.editor.setText(src);
		this.editor.setCaretPosition(0);
		this.editor.discardAllEdits();
	}
	
	public class VarsListModel extends AbstractListModel {
		private static final long serialVersionUID = 100623371649283278L;

		protected RecordStruct inst = null;
		protected List<String> names = new ArrayList<String>();
		
		public VarsListModel(RecordStruct inst) {
			this.inst = inst;
			
			if (this.inst == null)
				return;
			
			for (FieldStruct fld : this.inst.getFields())
				this.names.add(fld.getName());

			Collections.sort(this.names);
		}

		public String getFormatted(int pos) {
			String name = this.names.get(pos);
			
			Struct struct = this.inst.getField(name);
			
			if (struct != null)
				if (struct instanceof CompositeStruct) {
					try {
						ByteArrayOutputStream os = new ByteArrayOutputStream();
						PrintStream ps = new PrintStream(os);
						
						((CompositeStruct) struct).toBuilder(new JsonStreamBuilder(ps, true));
						
						return os.toString("UTF8");
					} 
					catch (Exception x) {
						// TODO
						System.out.println("Error formatting structure: " + x);
					}
				}
				else
					return struct.toString();
			
			return "[null]";
		}

		@Override
		public Object getElementAt(int pos) {
			String name = this.names.get(pos);
			
			return name + " = " + this.inst.getField(name);
		}

		@Override
		public int getSize() {
			return this.names.size();
		}
	}
	
	public class StackListModel extends AbstractListModel {
		private static final long serialVersionUID = 4548254186810085022L;
		
		protected ListStruct stack = null;
		
		public void update(ListStruct stack) {
			int selpos = EditorPane.this.stacklst.getSelectedIndex();
			
			int changed = stack.getSize();
			
			if (this.stack != null)
				changed -= this.stack.getSize();
			else
				changed = 0;
			
			this.stack = stack;
			
			if (changed < 0)
				this.fireIntervalRemoved(this, 0, Math.abs(changed));
			else
				this.fireIntervalAdded(this, 0, changed);
			
			selpos = selpos + changed;
			
			if (selpos < 0)
				selpos = 0;
			
			EditorPane.this.stacklst.setSelectedIndex(selpos);
		}

		public void clear() {
			if (this.stack == null)
				return;
			
			int changed = this.stack.getSize();
			
			this.stack = null;
			
			if (changed > 0)
				this.fireIntervalRemoved(this, 0, changed);
		}

		@Override
		public Object getElementAt(int pos) {
			return (this.stack == null) ? null : this.stack.getItem(this.stack.getSize() - pos - 1);
		}

		@Override
		public int getSize() {
			return (this.stack == null) ? 0 : this.stack.getSize();
		}		
	}

	private class AboutAction extends AbstractAction {
		private static final long serialVersionUID = 3311869625217732652L;

		public AboutAction() {
			super("About dcScript...");
		}

		public void actionPerformed(ActionEvent e) {
			JOptionPane.showMessageDialog(EditorPane.this,
					"<html><b>dcScript</b> - Next Gen JCL" +
					"<br>Version 0.8.1" +
					"<br><a href=\"http://divconq.com/\">divconq.com</a>" +
					"<br>Licensed under an Apache license",
					"About dcScript",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private class ThemeAction extends AbstractAction {
		private static final long serialVersionUID = 7828839661874891753L;
		private String xml = null;

		public ThemeAction(String name, String xml) {
			super(name);
			this.xml = xml;
		}

		public void actionPerformed(ActionEvent e) {
			InputStream in = this.getClass().getResourceAsStream(xml);
			
			try {
				Theme theme = Theme.load(in);
				theme.apply(editor);
			} 
			catch (IOException x) {
				// TODO
			}
		}
	}

	public class SmallButton extends JButton implements MouseListener {
		private static final long serialVersionUID = -1851724407908438330L;
		
		protected Border raised = null;
		protected Border lowered = null;
		protected Border inactive = null;

		public SmallButton(Action act, String tip) {
			super((Icon) act.getValue(Action.SMALL_ICON));
			
			this.raised = new BevelBorder(BevelBorder.RAISED);
			this.lowered = new BevelBorder(BevelBorder.LOWERED);
			this.inactive = new EmptyBorder(2, 2, 2, 2);
			
			this.setBorder(this.inactive);
			this.setMargin(new Insets(1, 1, 1, 1));
			this.setToolTipText(tip);
			
			this.addActionListener(act);
			this.addMouseListener(this);
			this.setRequestFocusEnabled(false);
		}

		@Override
		public float getAlignmentY() {
			return 0.5f;
		}

		@Override
		public void mousePressed(MouseEvent e) {
			setBorder(this.lowered);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			setBorder(this.inactive);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			setBorder(this.raised);
		}

		@Override
		public void mouseExited(MouseEvent e) {
			setBorder(this.inactive);
		}
	}
}