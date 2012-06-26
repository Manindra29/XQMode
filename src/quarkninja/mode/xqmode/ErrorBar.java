package quarkninja.mode.xqmode;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.SwingWorker;

import processing.app.Base;
import processing.app.SketchCode;

/**
 * The bar on the left of the text area whih displays all errors as rectangles. <br>
 * <br>
 * Current idea: All errors and warnings of a sketch are drawn on the bar,
 * clikcing on one, scrolls to the tab and location. Error messages displayed on
 * hover. Markers are not in sync with the error line. Similar to eclipse's
 * right error bar which displays the overall errors in a document
 * 
 * @author Manindra Moharana
 * 
 */
public class ErrorBar extends JPanel {
	public int height;
	public final int errorMarkerHeight = 4;
	public final Color errorColor = new Color(0xED2630);
	public final Color warningColor = new Color(0xFFC30E);

	XQEditor editor;
	// public ErrorWindow errorWindow;
	public ErrorCheckerService errorCheckerService;
	Color errorStatus = Color.GREEN;

	/**
	 * Stores error markers displayed PER TAB along the error bar.
	 */
	ArrayList<ErrorMarker> errorPoints = new ArrayList<ErrorMarker>();

	public void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(0x2C343D));
		g.fillRect(0, 0, getWidth(), getHeight());
		// g.setColor(new Color(0x2C343D));
		// g.fillRect(0, 0, getWidth(), getHeight() - 15);
		// g.setColor();
		for (ErrorMarker emarker : errorPoints) {
			// g.fillOval(getWidth()/2, y, (getWidth() - 6), (getWidth() - 6));
			if (emarker.type == ErrorMarker.Error)
				g.setColor(errorColor);
			else
				g.setColor(warningColor);
			g.fillRect(2, emarker.y, (getWidth() - 3), errorMarkerHeight);
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

	/**
	 * Update error markers in the error bar.
	 * 
	 * @param problems
	 *            - List of problems.
	 */
	public void updateErrorPoints(ArrayList<Problem> problems) {

		// NOTE TO SELF: ErrorMarkers are calculated for the present tab only
		// Error Marker index in the arraylist is LOCALIZED for current tab.

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
		errorPoints = new ArrayList<ErrorMarker>();
		errorPoints.clear();
		// Each problem.getSourceLine() will have an extra line added because of
		// class declaration in the beginnning
		for (Problem problem : problems) {
			if (problem.tabIndex == currentTab) {
				// Ratio of error line to total lines
				float y = problem.lineNumber / ((float) totalLines);
				// Ratio multiplied by height of the error bar
				y *= this.getHeight() - 15;
				errorPoints.add(new ErrorMarker(problem, (int) y,
						problem.iProblem.isError() ? ErrorMarker.Error
								: ErrorMarker.Warning));
				// System.out.println("Y: " + y);
			}
		}
		if (errorPoints.size() > 0)
			errorStatus = Color.RED;
		else
			errorStatus = Color.GREEN;
		repaint();
	}

	/**
	 * Add various mouse listeners.
	 */
	private void addListeners() {

		this.addMouseListener(new MouseAdapter() {
			@SuppressWarnings("rawtypes")
			@Override
			public void mouseClicked(final MouseEvent e) {
				SwingWorker worker = new SwingWorker() {

					protected Object doInBackground() throws Exception {
						return null;
					}

					protected void done() {
						for (ErrorMarker eMarker : errorPoints) {
							if (e.getY() >= eMarker.y
									&& e.getY() <= eMarker.y
											+ errorMarkerHeight) {
								int currentTabErrorIndex = errorPoints
										.indexOf(eMarker);
								// System.out.println("Index: " +
								// currentTabErrorIndex);
								int currentTab = editor.getSketch()
										.getCodeIndex(
												editor.getSketch()
														.getCurrentCode());

								int totalErrorIndex = currentTabErrorIndex;
								for (int i = 0; i < errorCheckerService.problemsList
										.size(); i++) {
									Problem p = errorCheckerService.problemsList
											.get(i);
									if (p.tabIndex < currentTab)
										totalErrorIndex++;
									if (p.tabIndex == currentTab)
										break;
								}
								errorCheckerService
										.scrollToErrorLine(totalErrorIndex);
							}
						}

					}
				};
				try {
					worker.execute();
				} catch (Exception exp) {
					System.out.println("Errorbar mouseclicked is slacking."
							+ exp.getMessage());
					// e.printStackTrace();
				}

			}
		});

		this.addMouseMotionListener(new MouseMotionListener() {

			@SuppressWarnings("rawtypes")
			@Override
			public void mouseMoved(final MouseEvent e) {
				// System.out.println(e);
				SwingWorker worker = new SwingWorker() {

					protected Object doInBackground() throws Exception {
						return null;
					}

					protected void done() {

						for (ErrorMarker eMarker : errorPoints) {
							if (e.getY() >= eMarker.y
									&& e.getY() <= eMarker.y
											+ errorMarkerHeight) {
								// System.out.println("Index: " +
								// errorPoints.indexOf(y));
								int currentTab = editor.getSketch()
										.getCodeIndex(
												editor.getSketch()
														.getCurrentCode());
								int currentTabErrorCount = 0;

								for (int i = 0; i < errorPoints.size(); i++) {
									Problem p = errorPoints.get(i).problem;
									if (p.tabIndex == currentTab) {
										if (currentTabErrorCount == errorPoints
												.indexOf(eMarker)) {
											// System.out.println("Roger that.");
											String msg = (p.iProblem.isError() ? "Error: "
													: "Warning: ")
													+ p.iProblem.getMessage();
											setToolTipText(msg);

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
				};
				try {
					worker.execute();
				} catch (Exception exp) {
					System.out
							.println("Errorbar mousemoved Worker is slacking."
									+ exp.getMessage());
					// e.printStackTrace();
				}

			}

			@Override
			public void mouseDragged(MouseEvent arg0) {
			}
		});

	}

	/**
	 * Error markers displayed on the Error Bar.
	 * 
	 * @author Manindra Moharana
	 * 
	 */
	private class ErrorMarker {
		public int y;
		public int type = -1;
		public static final int Error = 1;
		public static final int Warning = 2;
		public Problem problem;

		public ErrorMarker(Problem problem, int y, int type) {
			this.problem = problem;
			this.y = y;
			this.type = type;
		}
	}

}
