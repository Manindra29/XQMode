package quarkninja.mode.xqmode;

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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPanel;

public class XQConsoleToggle extends JPanel implements MouseListener {
	public static String[] text = { "Console", "Error" };
	public boolean toggleText = true, toggleBG = true;
	public int height;
	public XQEditor editor;

	public XQConsoleToggle(XQEditor editor, int height) {
		this.editor = editor;
		this.height = height;
	}

	public Dimension getPreferredSize() {
		return new Dimension(70, height);
	}

	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

	public Dimension getMaximumSize() {
		return getPreferredSize();
	}

	public void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		if (toggleBG) {
			g.setColor(new Color(0xff9DA7B0));
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
			g.setColor(Color.BLACK);
		} else {
			g.setColor(Color.DARK_GRAY);
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
			g.setColor(Color.WHITE);
		}

		if (toggleText) {
			g.drawString(text[0], getWidth() / 2
					- getFontMetrics(getFont()).stringWidth(text[0]) / 2,
					this.getHeight() - 6);
		} else {
			g.drawString(text[1], getWidth() / 2
					- getFontMetrics(getFont()).stringWidth(text[1]) / 2,
					this.getHeight() - 6);
		}
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {

		this.repaint();
		try {
			editor.toggleView();
		} catch (Exception e) {
			System.out.println(e);
			// e.printStackTrace();
		}
		toggleText = !toggleText;
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		toggleBG = !toggleBG;
		this.repaint();
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		toggleBG = !toggleBG;
		this.repaint();
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
	}
}