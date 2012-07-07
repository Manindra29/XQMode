package quarkninja.mode.xqmode;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import processing.app.Base;
import processing.app.Editor;
import processing.app.EditorState;
import processing.mode.java.JavaMode;

/**
 * Mode Template for extending Java mode in Processing IDE 2.0a5 or later.
 * 
 */
public class XQMode extends JavaMode {

	public XQMode(Base base, File folder) {
		super(base, folder);
		System.out.println("XQMode initialized.");
	}

	public Editor createEditor(Base base, String path, EditorState state) {
		return new XQEditor(base, path, state, this);
	}

	/**
	 * Called by PDE
	 */
	@Override
	public String getTitle() {
		return "XQMode";
	}

}
