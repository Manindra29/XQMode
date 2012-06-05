package quarkninja.mode.xqmode;

import java.awt.EventQueue;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.table.DefaultTableModel;

import processing.app.Base;
import processing.app.EditorState;
import processing.app.Mode;
import processing.mode.java.JavaEditor;

@SuppressWarnings("serial")
public class XQEditor extends JavaEditor {

	XQMode xqmode;
	Thread syntaxCheckerThread = null;
	ErrorWindow err1;

	protected XQEditor(Base base, String path, EditorState state, Mode mode) {
		super(base, path, state, mode);
		xqmode = (XQMode) mode;
		System.out.println("Editor initialized.");
		try {
			initializeSyntaxChecker();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void initializeSyntaxChecker() {
		if (syntaxCheckerThread == null) {

			final SyntaxCheckerService synCheck = new SyntaxCheckerService(err1);
			synCheck.editor = this;
			syntaxCheckerThread = new Thread(synCheck);
			try {

				syntaxCheckerThread.start();

			} catch (Exception e) {
				System.out.println("Oops! [XQEditor]: " + e);
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
					synCheck.stopThread();
				}

				@Override
				public void windowActivated(WindowEvent arg0) {
				}
			});
		}

	}

}
