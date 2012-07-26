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

import java.awt.Graphics;

import javax.swing.text.BadLocationException;

import processing.app.syntax.JEditTextArea;
import processing.app.syntax.TextAreaDefaults;
import processing.app.syntax.TextAreaPainter;
import processing.app.syntax.TokenMarker;

/**
 * Custom painter for XQTextArea. Handles underlining of error lines.
 * 
 * @author Manindra Moharana &lt;mkmoharana29@gmail.com&gt;
 * 
 */
public class XQTextAreaPainter extends TextAreaPainter {

	protected JEditTextArea ta;
	public ErrorCheckerService errorCheckerService;
	public int horizontalAdjustment = 0;

	public XQTextAreaPainter(JEditTextArea textArea, TextAreaDefaults defaults) {
		super(textArea, defaults);
		ta = textArea;
	}

	/**
	 * Paint a line.
	 * 
	 * @param gfx
	 *            the graphics context
	 * @param tokenMarker
	 * @param line
	 *            0-based line number
	 * @param x
	 */
	@Override
	protected void paintLine(Graphics gfx, TokenMarker tokenMarker, int line,
			int x) {

		super.paintLine(gfx, tokenMarker, line, x);
		paintErrorLine(gfx, line, x);

	}

	ErrorMarker currentMarker = null;

	/**
	 * Paints the underline for an error line
	 * 
	 * @param gfx
	 *            the graphics context
	 * @param tokenMarker
	 * @param line
	 *            0-based line number: NOTE
	 * @param x
	 */
	private void paintErrorLine(Graphics gfx, int line, int x) {
		if (errorCheckerService == null)
			return;
		if (errorCheckerService.errorBar.errorPoints == null)
			return;
		boolean notFound = true;
		boolean isWarning = false;

		// Check if current line contains an error. If it does, find if it's an
		// error or warning
		for (ErrorMarker emarker : errorCheckerService.errorBar.errorPoints) {
			if (emarker.problem.lineNumber == line + 1) {
				currentMarker = emarker;
				notFound = false;
				if (emarker.type == ErrorMarker.Warning)
					isWarning = true;
				break;
			}
		}

		if (notFound)
			return;

		// Determine co-ordinates
		// System.out.println("Hoff " + ta.getHorizontalOffset() + ", " +
		// horizontalAdjustment);
		int y = ta.lineToY(line);
		y += fm.getLeading() + fm.getMaxDescent();
		int height = fm.getHeight();
		int start = ta.getLineStartOffset(line);

		try {
			String linetext = null;

			try {
				linetext = ta.getDocument().getText(start,
						ta.getLineStopOffset(line) - start - 1);
			} catch (BadLocationException bl) {
				// Error in the import statements or end of code.
				// System.out.print("BL caught. " + ta.getLineCount() + " ,"
				// + line + " ,");
				// System.out.println((ta.getLineStopOffset(line) - start - 1));
			}

			// Take care of offsets
			int aw = fm.stringWidth(trimRight(linetext))
					+ ta.getHorizontalOffset(); // apparent width. Whitespaces
												// to the left of line + text
												// width
			int rw = fm.stringWidth(linetext.trim()); // real width
			int x1 = 0 + (aw - rw), y1 = y + fm.getHeight() - 2, x2 = x1 + rw;
			// gfx.fillRect(x1, y, rw, height);

			// Let the painting begin!
			gfx.setColor(ErrorBar.errorColor);
			if (isWarning)
				gfx.setColor(ErrorBar.warningColor);
			gfx.fillRect(1, y + 2, 3, height - 2);
			int xx = x1;

			// Jagged lines are pretty.
			while (xx < x2) {
				gfx.drawLine(xx, y1, xx + 2, y1 + 1);
				xx += 2;
				gfx.drawLine(xx, y1 + 1, xx + 2, y1);
				xx += 2;
			}
		} catch (Exception e) {
			System.out
					.println("Looks like I messed up! XQTextAreaPainter.paintLine() : "
							+ e);
			// e.printStackTrace();
		}
		
		// Won't highlight the line. Select the text instead.
		// gfx.setColor(Color.RED);
		// gfx.fillRect(2, y, 3, height);
	}

	/**
	 * Trims out trailing whitespaces (to the right)
	 * 
	 * @param string
	 * @return - String
	 */
	public String trimRight(String string) {
		String newString = "";
		for (int i = 0; i < string.length(); i++) {
			if (string.charAt(i) != ' ') {
				newString = string.substring(0, i) + string.trim();
				break;
			}
		}
		return newString;
	}

}