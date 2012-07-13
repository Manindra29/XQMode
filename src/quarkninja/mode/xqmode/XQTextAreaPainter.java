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

//	public XQTextAreaPainter(JEditTextArea textArea, TextAreaDefaults defaults,
//			ErrorCheckerService ecs) {
//		super(textArea, defaults);
//		errorCheckerService = ecs;
//		ta = textArea;
//		if (errorCheckerService == null){
//			System.out.println("ECS null");			
//		}
//		else
//			System.out.println("ECS.... OK");
//	}

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
		
			paintLineBgColor(gfx, line, x);
		super.paintLine(gfx, tokenMarker, line, x);
	}

	private void paintLineBgColor(Graphics gfx, int line, int x) {
		if(errorCheckerService!= null){
			System.out.println("ECS Ok." + errorCheckerService.problemsList.size());
		}
		else{
			System.out.println("ECS null. :(");
		}
		// System.out.println("1");
		int y = ta.lineToY(line);
		// System.out.println("2");
		y += fm.getLeading() + fm.getMaxDescent();
		// System.out.println("3");
		int height = fm.getHeight();

		// get the color
		// System.out.println("4");
		Color col = Color.lightGray;
		// System.out.print("bg line " + line + ": ");
		// no need to paint anything
		if (col == null) {
			// System.out.println("none");
			return;
		}
		// paint line background
		// System.out.println("5");
		gfx.setColor(col);
		// System.out.println("6");
		gfx.fillRect(0, y, getWidth(), height);
	}

}
