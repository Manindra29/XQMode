package quarkninja.mode.xqmode;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import processing.app.Base;
import processing.app.EditorState;
import processing.app.Mode;
import processing.mode.java.JavaEditor;

@SuppressWarnings("serial")
public class XQEditor extends JavaEditor {

	XQMode xqmode;
	Thread syntaxCheckerThread = null;
	protected XQEditor(Base base, String path, EditorState state, Mode mode) {
		super(base, path, state, mode);
		xqmode = (XQMode) mode;
		System.out.println("Editor initialized.");
		initializeSyntaxChecker();
	}
	
	private void initializeSyntaxChecker(){
		if (syntaxCheckerThread == null) {
			final SyntaxCheckerService synCheck = new SyntaxCheckerService();
			synCheck.editor = this;
			syntaxCheckerThread = new Thread(synCheck);
			try {
				syntaxCheckerThread.start();
			} catch (Exception e) {
				System.out.println("Oops! [XQEditor]: " + e);
//				e.printStackTrace();
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
