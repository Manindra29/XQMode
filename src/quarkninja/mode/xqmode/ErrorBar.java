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

import java.awt.Color;
import java.awt.Cursor;
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
 * @author Manindra Moharana &lt;mkmoharana29@gmail.com&gt;
 * 
 */
public class ErrorBar extends JPanel {
	public int preffHeight;
	public final int errorMarkerHeight = 4;
	public static final Color errorColor = new Color(0xED2630);
	public static final Color warningColor = new Color(0xFFC30E);

	XQEditor editor;
	// public ErrorWindow errorWindow;
	public ErrorCheckerService errorCheckerService;
	Color errorStatus = Color.GREEN;

	/**
	 * Stores error markers displayed PER TAB along the error bar.
	 */
	public ArrayList<ErrorMarker> errorPoints = new ArrayList<ErrorMarker>();

	public ArrayList<ErrorMarker> errorPointsOld = new ArrayList<ErrorMarker>();

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
		return new Dimension(12, preffHeight);
	}

	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

	public ErrorBar(XQEditor editor, int height) {
		this.editor = editor;
		this.preffHeight = height;
		// syntaxCheckerService = synCheck;
		addListeners();
	}

	/**
	 * Update error markers in the error bar.
	 * 
	 * @param problems
	 *            - List of problems.
	 */
	@SuppressWarnings("unchecked")
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
		errorPointsOld = (ArrayList<ErrorMarker>) errorPoints.clone();
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
													+ p.message;
											setToolTipText(msg);
											setCursor(Cursor
													.getPredefinedCursor(Cursor.HAND_CURSOR));
											return;
										} else {
											currentTabErrorCount++;
											// System.out.println("Still looking..");
										}
									}

								}
							}
							// Reset cursor and tooltip
							else {
								setToolTipText("");
								setCursor(Cursor
										.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
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

}
