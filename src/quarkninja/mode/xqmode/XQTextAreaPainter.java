package quarkninja.mode.xqmode;

import java.awt.Graphics;

import processing.app.syntax.JEditTextArea;
import processing.app.syntax.TextAreaDefaults;
import processing.app.syntax.TextAreaPainter;
import processing.app.syntax.TokenMarker;

public class XQTextAreaPainter extends TextAreaPainter {

	protected JEditTextArea ta;
	public ErrorCheckerService errorCheckerService;
	public int horizontalAdjustment = 0;

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

		super.paintLine(gfx, tokenMarker, line, x + 2);
		paintErrorLine(gfx, line, x + 2);

	}

	private void paintErrorLine(Graphics gfx, int line, int x) {
		if (errorCheckerService == null)
			return;
		if (errorCheckerService.errorBar.errorPoints == null)
			return;
		boolean notFound = true;
		boolean isWarning = false;
		for (ErrorMarker emarker : errorCheckerService.errorBar.errorPoints) {
			if (emarker.problem.lineNumber == line + 1) {
				notFound = false;
				if (emarker.type == ErrorMarker.Warning)
					isWarning = true;
				break;
			}
		}

		if (notFound)
			return;
		try {
			// System.out.println("Hoff " + ta.getHorizontalOffset() + ", " +
			// horizontalAdjustment);
			int y = ta.lineToY(line);
			y += fm.getLeading() + fm.getMaxDescent();
			int height = fm.getHeight();
			int start = ta.getLineStartOffset(line);

			String linetext = ta.getDocument().getText(start,
					ta.getLineStopOffset(line) - start - 1);
			// String linetext = ta.getLineText(line);

			int aw = fm.stringWidth(linetext) + ta.getHorizontalOffset(); // apparent
																			// width
			int rw = fm.stringWidth(linetext.trim()); // real width
			int x1 = 0 + (aw - rw), y1 = y + fm.getHeight() - 2, x2 = x1 + rw;
			// gfx.fillRect(x1, y, rw, height);
			gfx.setColor(ErrorBar.errorColor);
			if (isWarning)
				gfx.setColor(ErrorBar.warningColor);
			gfx.fillRect(1, y + 2, 3, height - 2);
			int xx = x1;
			while (xx < x2) {
				gfx.drawLine(xx, y1, xx + 2, y1 + 1);
				xx += 2;
				gfx.drawLine(xx, y1 + 1, xx + 2, y1);
				xx += 2;
			}
		} catch (Exception e) {
			// System.out.println("paintLine " + e);
			// e.printStackTrace();
		}
		// gfx.setColor(Color.RED);
		// gfx.fillRect(2, y, 3, height);
	}

}