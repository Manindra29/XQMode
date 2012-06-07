package quarkninja.mode.xqmode;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.Box;
import javax.swing.JPanel;

import processing.app.Base;
import processing.app.EditorState;
import processing.app.Mode;
import processing.app.syntax.JEditTextArea;
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
		JPanel textAndError = new JPanel();
	    JPanel errorStrip = new JPanel(){
	      public void paintComponent(Graphics g) {
	        g.setColor(Color.DARK_GRAY);
	        g.fillRect(0, 0, getWidth(), getHeight());
	        g.setColor(Color.RED);
	        g.fillOval(5, 100, 9, 9);
	      }
	      
	      public Dimension getPreferredSize() {
	        return new Dimension(20,300);
	      }

	      public Dimension getMinimumSize() {
	        return getPreferredSize();
	      }

	      public Dimension getMaximumSize() {
	        return new Dimension(20, 400);
	      }
	    };
	    Box box =  (Box) textarea.getParent();	    
	    box.remove(2); // Remove textArea from it's container, i.e Box
	    textAndError.setLayout(new BorderLayout()); 
	    textAndError.add(errorStrip, BorderLayout.WEST);
	    textarea.setBounds(errorStrip.getX() ,errorStrip.getY()+errorStrip.getWidth() , textarea.getWidth(), textarea.getHeight());
	    textAndError.add(textarea);	    
	    box.add(textAndError);
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
