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

		paintLineBgColor(gfx, line, x + 2);
		super.paintLine(gfx, tokenMarker, line, x + 2);
	}

	public void getCurrentTabErrorLines() {
		// ErrorMarker em = errorCheckerService.errorBar.errorPoints.get(0);
	}

	private void paintLineBgColor(Graphics gfx, int line, int x) {
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

		int y = ta.lineToY(line);
		y += fm.getLeading() + fm.getMaxDescent();
		int height = fm.getHeight();
		Color col = new Color(255, 196, 204);
		gfx.setColor(col);
		gfx.fillRect(0, y, getWidth(), height);
		// gfx.setColor(Color.RED);
		// gfx.fillRect(2, y, 3, height);
	}

}