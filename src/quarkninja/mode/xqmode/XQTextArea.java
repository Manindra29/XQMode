/*
  Part of the XQMode project - https://github.com/Manindra29/XQMode
  
  Under Google Summer of Code 2012 - 
  http://www.google-melange.com/gsoc/homepage/google/gsoc2012
  
  Copyright (C) 2012 Manindra Moharana
	
  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package quarkninja.mode.xqmode;

import java.awt.event.ComponentListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import processing.app.syntax.JEditTextArea;
import processing.app.syntax.TextAreaDefaults;

/**
 * Custom TextArea for XQMode
 * 
 * @author Manindra Moharana &lt;mkmoharana29@gmail.com&gt;
 * 
 */
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

	/**
	 * Set the error checker service object. Also loads the colors from theme.txt
	 * 
	 * @param ecs - ErrorCheckerService
	 */
	public void setECSandThemes(ErrorCheckerService ecs, XQMode mode) {
		xqpainter.errorCheckerService = ecs;
		xqpainter.loadTheme(mode);
	}

}
