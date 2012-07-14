package quarkninja.mode.xqmode;

import java.awt.Color;
import java.awt.Graphics;

import processing.app.syntax.JEditTextArea;
import processing.app.syntax.TextAreaDefaults;
import processing.app.syntax.TextAreaPainter;
import processing.app.syntax.TokenMarker;

public class XQTextAreaPainter extends TextAreaPainter {

	protected JEditTextArea ta;
	public ErrorCheckerService errorCheckerService;

	// public XQTextAreaPainter(JEditTextArea textArea, TextAreaDefaults
	// defaults,
	// ErrorCheckerService ecs) {
	// super(textArea, defaults);
	// errorCheckerService = ecs;
	// ta = textArea;
	// if (errorCheckerService == null){
	// System.out.println("ECS null");
	// }
	// else
	// System.out.println("ECS.... OK");
	// }

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

		paintErrorLine(gfx, line, x + 2);
		super.paintLine(gfx, tokenMarker, line, x + 2);
	}

	private void paintErrorLine(Graphics gfx, int line, int x) {
		if (errorCheckerService == null)
			return;
		if (errorCheckerService.errorBar.errorPoints == null)
			return;
		boolean notFound = true;
		for (ErrorMarker emarker : errorCheckerService.errorBar.errorPoints) {
			if (emarker.problem.lineNumber == line + 1) {
				notFound = false;
				break;
			}
		}

		if (notFound)
			return;
		try {
			int y = ta.lineToY(line);
			y += fm.getLeading() + fm.getMaxDescent();
			int height = fm.getHeight();
			Color col = new Color(255, 196, 204);
			// gfx.setColor(col);
			int start = ta.getLineStartOffset(line);

			String linetext = ta.getDocument().getText(start,
					ta.getLineStopOffset(line) - start - 1);
			// String linetext = ta.getLineText(line);

			int aw = fm.stringWidth(linetext); // apparent width
			int rw = fm.stringWidth(linetext.trim()); // real width
			int x1 = 0 + (aw - rw) + fm.stringWidth(";");
			// gfx.fillRect(x1, y, rw, height);
			gfx.setColor(Color.RED);
			gfx.fillRect(1, y + 2, 3, height - 2);
			gfx.drawLine(x1, y + fm.getHeight() - 1, x1 + rw,
					y + fm.getHeight() - 1);
		} catch (Exception e) {
			// System.out.println("paintLine " + e);
			// e.printStackTrace();
		}
		// gfx.setColor(Color.RED);
		// gfx.fillRect(2, y, 3, height);
	}

}