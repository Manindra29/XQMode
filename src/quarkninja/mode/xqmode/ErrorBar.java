package quarkninja.mode.xqmode;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.JPanel;

import org.eclipse.jdt.core.compiler.IProblem;

import processing.app.Base;
import processing.app.Editor;
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
	Editor editor;
	Color errorStatus = Color.GREEN;
	/**
	 * Stores the Y co-ordinates of the errors along the error bar. X
	 * co-ordinate of all points is fixed.
	 */
	ArrayList<Integer> errorPoints = new ArrayList<Integer>();

	public void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(0x2C343D));
		g.fillRect(0, 0, getWidth(), getHeight());
//		g.setColor(new Color(0x2C343D));
//		g.fillRect(0, 0, getWidth(), getHeight() - 15);
		g.setColor(new Color(0xED2630));
		for (Integer y : errorPoints) {
//			g.fillOval(getWidth()/2, y, (getWidth() - 6), (getWidth() - 6));
			g.fillRect(2, y, (getWidth() - 3), 4);
		}
	}

	public Dimension getPreferredSize() {
		return new Dimension(12, height);
	}

	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

	public ErrorBar(Editor editor, int height) {
		this.editor = editor;
		this.height = height;
		// syntaxCheckerService = synCheck;
		addListeners();
	}

	public void updateErrorPoints(IProblem problems[]) {

		int bigCount = 0;
		int totalLines = 0;
		for (SketchCode sc : editor.getSketch().getCode()) {
			if (sc.isExtension("pde")) {
				sc.setPreprocOffset(bigCount);

				try {
					int len = 0;
					if (editor.getSketch().getCurrentCode().equals(sc)) {
						len = Base.countLines(sc.getDocument().getText(0, sc.getDocument().getLength())) + 1;
					} else {
						len = Base.countLines(sc.getProgram()) + 1;
					}
					totalLines += len;
					// Adding + 1 to len because \n gets appended for each
					// sketchcode extracted during processPDECode()

				} catch (Exception e) {
					e.printStackTrace();
				}
				bigCount += sc.getLineCount();
			}
		}
//		System.out.println("Total lines: " + totalLines);

		// Swing Worker
		errorPoints = new ArrayList<Integer>();

		// Each problem.getSourceLine() will have an extra line added because of
		// class declaration in the beginnning
		for (IProblem problem : problems) {
			// Ratio of error line to total lines
			float y = (problem.getSourceLineNumber() - 1) / ((float) totalLines);
			// Ratio multiplied by height of the error bar
			y *= this.getHeight() - 15;
			errorPoints.add(new Integer((int) y));
//			System.out.println("Y: " + y);
		}
		if(errorPoints.size()>0)
			errorStatus = Color.RED;
		else
			errorStatus = Color.GREEN;
		repaint();
	}

	private void addListeners() {
		this.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				System.out.println(e);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseReleased(MouseEvent e) {
				// TODO Auto-generated method stub

			}

		});
	}

	public void drawDot(int x, int y, Graphics g) {

	}

}
