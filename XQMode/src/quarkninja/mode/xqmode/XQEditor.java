package quarkninja.mode.xqmode;

import processing.app.Base;
import processing.app.EditorState;
import processing.app.Mode;
import processing.mode.java.JavaEditor;

@SuppressWarnings("serial")
public class XQEditor extends JavaEditor {

	XQMode xqmode;
	protected XQEditor(Base base, String path, EditorState state, Mode mode) {
		super(base, path, state, mode);
		xqmode = (XQMode) mode;
	}

}
