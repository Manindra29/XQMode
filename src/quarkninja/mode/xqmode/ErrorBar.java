package quarkninja.mode.xqmode;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JPanel;

import processing.app.Base;
import processing.app.SketchCode;

/**
 * The bar on the left of the text area whih displays all errors as dots. <br>
 * <br>
 * Current idea: All errors of a sketch are drawn on the bar, clikcing on one,
 * scrolls to the tab and location. Red dot not in sync with the error line.
 * Similar to eclipse's right error bar which displays the overall errors in a
 * document
 * 
 * @author Manindra Moharana
 * 
 */
@SuppressWarnings("serial")
public class ErrorBar extends JPanel {
	public int height;
	public final int errorMarkerHeight = 4;
	XQEditor editor;
	public ErrorWindow errorWindow;
	Color errorStatus = Color.GREEN;
	/**
	 * Stores the Y co-ordinates of the errors along the error bar. X
	 * co-ordinate of all points is fixed.
	 */
	ArrayList<Integer> errorPoints = new ArrayList<Integer>();

	public void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(0x2C343D));
		g.fillRect(0, 0, getWidth(), getHeight());
		// g.setColor(new Color(0x2C343D));
		// g.fillRect(0, 0, getWidth(), getHeight() - 15);
		g.setColor(new Color(0xED2630));
		for (Integer y : errorPoints) {
			// g.fillOval(getWidth()/2, y, (getWidth() - 6), (getWidth() - 6));
			g.fillRect(2, y, (getWidth() - 3), errorMarkerHeight);
		}
	}

	public Dimension getPreferredSize() {
		return new Dimension(12, height);
	}

	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

	public ErrorBar(XQEditor editor, int height) {
		this.editor = editor;
		this.height = height;
		// syntaxCheckerService = synCheck;
		addListeners();
	}

	public void updateErrorPoints(ArrayList<Problem> problems) {

		int bigCount = 0;
		int totalLines = 0;
		int currentTab = 0;
		for (SketchCode sc : editor.getSketch().getCode()) {
			if (sc.isExtension("pde")) {
				sc.setPreprocOffset(bigCount);

				try {
					if (editor.getSketch().getCurrentCode().equals(sc)) {
						// Adding + 1 to len because \n gets appended for each
						// sketchcode extracted during processPDECode()
						totalLines = Base.countLines(sc.getDocument().getText(
								0, sc.getDocument().getLength())) + 1;
						break;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			currentTab++;
		}
		// System.out.println("Total lines: " + totalLines);

		// TODO: Swing Worker approach?
		errorPoints = new ArrayList<Integer>();
		errorPoints.clear();
		// Each problem.getSourceLine() will have an extra line added because of
		// class declaration in the beginnning
		for (Problem problem : problems) {
			if (problem.tabIndex == currentTab) {
				// Ratio of error line to total lines
				float y = problem.lineNumber / ((float) totalLines);
				// Ratio multiplied by height of the error bar
				y *= this.getHeight() - 15;
				errorPoints.add(new Integer((int) y));
				// System.out.println("Y: " + y);
			}
		}
		if (errorPoints.size() > 0)
			errorStatus = Color.RED;
		else
			errorStatus = Color.GREEN;
		repaint();
	}

	// public void updateErrorPoints(IProblem problems[]) {
	//
	// int bigCount = 0;
	// int totalLines = 0;
	// for (SketchCode sc : editor.getSketch().getCode()) {
	// if (sc.isExtension("pde")) {
	// sc.setPreprocOffset(bigCount);
	//
	// try {
	// int len = 0;
	// if (editor.getSketch().getCurrentCode().equals(sc)) {
	// len = Base.countLines(sc.getDocument().getText(0,
	// sc.getDocument().getLength())) + 1;
	// } else {
	// len = Base.countLines(sc.getProgram()) + 1;
	// }
	// totalLines += len;
	// // Adding + 1 to len because \n gets appended for each
	// // sketchcode extracted during processPDECode()
	//
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// bigCount += sc.getLineCount();
	// }
	// }
	// // System.out.println("Total lines: " + totalLines);
	//
	// // TODO: Swing Worker approach?
	// errorPoints = new ArrayList<Integer>();
	//
	// // Each problem.getSourceLine() will have an extra line added because of
	// // class declaration in the beginnning
	// for (IProblem problem : problems) {
	// // Ratio of error line to total lines
	// float y = (problem.getSourceLineNumber() - 1)
	// / ((float) totalLines);
	// // Ratio multiplied by height of the error bar
	// y *= this.getHeight() - 15;
	// errorPoints.add(new Integer((int) y));
	// // System.out.println("Y: " + y);
	// }
	// if (errorPoints.size() > 0)
	// errorStatus = Color.RED;
	// else
	// errorStatus = Color.GREEN;
	// repaint();
	// }

	private void addListeners() {

		this.addMouseListener(new MouseAdapter() {
			@Override
			synchronized public void mouseClicked(MouseEvent e) {
				for (Integer y : errorPoints) {
					if (e.getY() >= y.intValue()
							&& e.getY() <= y.intValue() + errorMarkerHeight) {
						// System.out.println("Index: " +
						// errorPoints.indexOf(y));
						int currentTab = editor.getSketch().getCodeIndex(
								editor.getSketch().getCurrentCode());
						int currentTabErrorCount = 0;

						for (int i = 0; i < errorWindow.problemList.size(); i++) {
							Problem p = errorWindow.problemList.get(i);
							if (p.tabIndex == currentTab) {
								if (currentTabErrorCount == errorPoints
										.indexOf(y)) {
									// System.out.println("Roger that.");
									errorWindow.scrollToErrorLine(i);
									return;
								} else {
									currentTabErrorCount++;
									// System.out.println("Still looking..");
								}
							}

						}
					}
				}
			}

		});
	}

}
