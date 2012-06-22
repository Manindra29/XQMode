package quarkninja.mode.xqmode;

import java.awt.BorderLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.Box;
import javax.swing.JPanel;

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
@SuppressWarnings("serial")
public class XQEditor extends JavaEditor {

	XQMode xqmode;
	protected Thread syntaxCheckerThread = null;
	protected ErrorWindow errorWindow;
	protected ErrorCheckerService synCheck;
	protected ErrorBar errorBar;

	protected XQEditor(Base base, String path, EditorState state, Mode mode) {
		super(base, path, state, mode);
		xqmode = (XQMode) mode;
		System.out.println("Editor initialized.");
		errorBar = new ErrorBar(this, textarea.getMinimumSize().height);
		initializeSyntaxChecker();

		JPanel textAndError = new JPanel();

		Box box = (Box) textarea.getParent();
		box.remove(2); // Remove textArea from it's container, i.e Box
		textAndError.setLayout(new BorderLayout());
		textAndError.add(errorBar, BorderLayout.EAST);
		// textarea.setBounds(errorBar.getX() + errorBar.getWidth(),
		// errorBar.getY(), textarea.getWidth(), textarea.getHeight());
		textarea.setBounds(0, 0, errorBar.getX() - 1, textarea.getHeight());
		for (int i = 0; i < consolePanel.getComponentCount(); i++) {
			System.out.println("Console: " + consolePanel.getComponent(i));
		}
		// consolePanel.remove(1);
		// JTable table = new JTable(new String[][] { { "Problems", "Line no" },
		// { "Missing semicolon", "12" },
		// { "Extra  )", "15" },{ "Missing semicolon", "34" } },
		// new String[] { "A", "B" });
		// consolePanel.add(table);
		textAndError.add(textarea);
		box.add(textAndError);
	}

	/**
	 * Initializes and starts Syntax Checker Service
	 */
	private void initializeSyntaxChecker() {
		if (syntaxCheckerThread == null) {
			synCheck = new ErrorCheckerService(errorWindow, errorBar);
			synCheck.editor = this;
			syntaxCheckerThread = new Thread(synCheck);
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
					synCheck.stopThread(); // Bye bye thread.
				}

				@Override
				public void windowActivated(WindowEvent arg0) {
				}
			});
		}

	}

}
