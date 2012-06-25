package quarkninja.mode.xqmode;

import java.awt.BorderLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import processing.app.Base;
import processing.app.EditorState;
import processing.app.Mode;
import processing.mode.java.JavaEditor;

/**
 * Editor for XQMode
 * 
 * @author Manindra Moharana
 * 
 */
public class XQEditor extends JavaEditor {

	XQMode xqmode;
	protected Thread syntaxCheckerThread = null;
	protected ErrorWindow errorWindow;
	protected ErrorCheckerService errorChecker;
	protected ErrorBar errorBar;

	@SuppressWarnings("rawtypes")
	protected XQEditor(Base base, String path, EditorState state,
			final Mode mode) {
		super(base, path, state, mode);
		final XQEditor thisEditor = this;
		xqmode = (XQMode) mode;
		errorBar = new ErrorBar(thisEditor,
				textarea.getMinimumSize().height);
		initializeSyntaxChecker();
		
		// - Start
		
		errorBar.errorCheckerService = errorChecker;
		JPanel textAndError = new JPanel();
		Box box = (Box) textarea.getParent();
		box.remove(2); // Remove textArea from it's container, i.e Box
		textAndError.setLayout(new BorderLayout());
		textAndError.add(errorBar, BorderLayout.EAST);
		textarea.setBounds(0, 0, errorBar.getX() - 1,
				textarea.getHeight());
		textAndError.add(textarea);
		box.add(textAndError);
		// - End
		
		SwingWorker worker = new SwingWorker() {

			protected Object doInBackground() throws Exception {
				return null;
			}

			protected void done() {
				
				System.out.println("Editor initialized.");
				
				

				
			}
		};
		try {
			worker.execute();
		} catch (Exception e) {
			System.out.println("Editor's Worker is slacking." + e.getMessage());
			// e.printStackTrace();
		}

		

		// textarea.setBounds(errorBar.getX() + errorBar.getWidth(),
		// errorBar.getY(), textarea.getWidth(), textarea.getHeight());

		// for (int i = 0; i < consolePanel.getComponentCount(); i++) {
		// System.out.println("Console: " + consolePanel.getComponent(i));
		// }
		// consolePanel.remove(1);
		// JTable table = new JTable(new String[][] { { "Problems", "Line no" },
		// { "Missing semicolon", "12" },
		// { "Extra  )", "15" },{ "Missing semicolon", "34" } },
		// new String[] { "A", "B" });
		// consolePanel.add(table);

	}

	/**
	 * Initializes and starts Syntax Checker Service
	 */
	private void initializeSyntaxChecker() {
		if (syntaxCheckerThread == null) {
			errorChecker = new ErrorCheckerService(errorWindow, errorBar);
			errorChecker.editor = this;
//			synCheck.initializeErrorWindow();
			syntaxCheckerThread = new Thread(errorChecker);
			try {
				syntaxCheckerThread.start();
			} catch (Exception e) {
				System.err
						.println("Syntax Checker Service not initialized [XQEditor]: "
								+ e);
				// e.printStackTrace();
			}
			System.out.println("Syntax Checker Service initialized.");
			this.addWindowListener(new WindowListener() {
				@Override
				public void windowOpened(WindowEvent arg0) {
				}

				@Override
				public void windowIconified(WindowEvent arg0) {
				}

				@Override
				public void windowDeiconified(WindowEvent arg0) {
				}

				@Override
				public void windowDeactivated(WindowEvent arg0) {
				}

				@Override
				public void windowClosing(WindowEvent arg0) {
				}

				@Override
				public void windowClosed(WindowEvent arg0) {
					errorChecker.stopThread(); // Bye bye thread.
				}

				@Override
				public void windowActivated(WindowEvent arg0) {
				}
			});
		}

	}

}
