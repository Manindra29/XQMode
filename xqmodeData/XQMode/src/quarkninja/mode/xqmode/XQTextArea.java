package quarkninja.mode.xqmode;

import java.awt.event.ComponentListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import processing.app.syntax.JEditTextArea;
import processing.app.syntax.TextAreaDefaults;

public class XQTextArea extends JEditTextArea {
	XQTextAreaPainter xqpainter;

	public XQTextArea(TextAreaDefaults defaults) {
		super(defaults);
		ComponentListener[] componentListeners = painter
				.getComponentListeners();
		MouseListener[] mouseListeners = painter.getMouseListeners();
		MouseMotionListener[] mouseMotionListeners = painter
				.getMouseMotionListeners();

		remove(painter);

		xqpainter = new XQTextAreaPainter(this, defaults);
		painter = xqpainter;

		for (ComponentListener cl : componentListeners)
			painter.addComponentListener(cl);

		for (MouseListener ml : mouseListeners)
			painter.addMouseListener(ml);

		for (MouseMotionListener mml : mouseMotionListeners)
			painter.addMouseMotionListener(mml);

		add(CENTER, painter);
	}

	public void setECS(ErrorCheckerService ecs) {
		xqpainter.errorCheckerService = ecs;
	}

}
