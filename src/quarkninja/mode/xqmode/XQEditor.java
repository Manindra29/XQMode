package quarkninja.mode.xqmode;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.Box;
import javax.swing.JPanel;

import processing.app.Base;
import processing.app.EditorState;
import processing.app.Mode;
import processing.app.syntax.JEditTextArea;
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
	Thread syntaxCheckerThread = null;
	ErrorWindow errorWindow;

	protected XQEditor(Base base, String path, EditorState state, Mode mode) {
		super(base, path, state, mode);
		xqmode = (XQMode) mode;
		System.out.println("Editor initialized.");

		initializeSyntaxChecker();

		JPanel textAndError = new JPanel();
		JPanel errorStrip = new JPanel() {
			public void paintComponent(Graphics g) {
				Graphics2D g2d = (Graphics2D) g;
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setColor(Color.DARK_GRAY);
				g.fillRect(0, 0, getWidth(), getHeight());
				g.setColor(Color.RED);
				g.fillOval(5, 100, 9, 9);
			}

			public Dimension getPreferredSize() {
				return new Dimension(18, textarea.getMinimumSize().height);
			}

			public Dimension getMinimumSize() {
				return getPreferredSize();
			}

			public Dimension getMaximumSize() {
				return new Dimension(18, textarea.getHeight());
			}
		};
		Box box = (Box) textarea.getParent();
		box.remove(2); // Remove textArea from it's container, i.e Box
		textAndError.setLayout(new BorderLayout());
		textAndError.add(errorStrip, BorderLayout.WEST);
		textarea.setBounds(errorStrip.getX() + errorStrip.getWidth(), errorStrip.getY(),
				textarea.getWidth(), textarea.getHeight());
		textAndError.add(textarea);
		box.add(textAndError);
	}

	/**
	 * Initializes and starts Syntax Checker Service 
	 */
	private void initializeSyntaxChecker() {
		if (syntaxCheckerThread == null) {

			final SyntaxCheckerService synCheck = new SyntaxCheckerService(errorWindow);
			synCheck.editor = this;
			syntaxCheckerThread = new Thread(synCheck);
			try {

				syntaxCheckerThread.start();

			} catch (Exception e) {
				System.err.println("Syntax Checker Service not initialized [XQEditor]: " + e);
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
