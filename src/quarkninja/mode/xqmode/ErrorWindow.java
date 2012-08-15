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

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableModel;

import processing.app.Base;
import processing.app.Editor;

/**
 * Error Window that displays a tablular list of errors. Clicking on an error
 * scrolls to its location in the code.
 * 
 * @author Manindra Moharana &lt;mkmoharana29@gmail.com&gt;
 * 
 */
public class ErrorWindow extends JFrame {

	private JPanel contentPane;
	/**
	 * The table displaying the errors
	 */
	protected XQErrorTable errorTable;
	/**
	 * Scroll pane that contains the Error Table
	 */
	protected JScrollPane scrollPane;
	protected Editor thisEditor;
	private JFrame thisErrorWindow;
	private DockTool2Base Docker;
	public ErrorCheckerService errorCheckerService;
	public JCheckBoxMenuItem problemWindowMenuCB;

	public static void main(String[] args) {
		// EventQueue.invokeLater(new Runnable() {
		// public void run() {
		// try {
		// ErrorWindow frame = new ErrorWindow(null, null);
		// frame.setVisible(true);
		// frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// }
		// });
	}

	public ErrorWindow(Editor editor, ErrorCheckerService syncheck) {
		thisErrorWindow = this;
		errorCheckerService = syncheck;
		thisEditor = editor;
		setTitle("Problems");
		prepareFrame();
	}

	/**
	 * Sets up ErrorWindow
	 */
	private void prepareFrame() { // Not createAndShowGUI().
		Base.setIcon(this);
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		// Default size: setBounds(100, 100, 458, 160);
		setBounds(100, 100, 458, 160);
		contentPane = new JPanel();
		contentPane.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				handlePause(e);
				if (e.getButton() == MouseEvent.BUTTON3) {
					// TODO: Remove this later. For stopping syntax
					// checker.
					errorCheckerService.stopThread();
					System.out.println("Syntax checker thread paused.");
				}
			}
		});

		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		scrollPane = new JScrollPane();
		contentPane.add(scrollPane);

		scrollPane.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				handlePause(e);
				if (e.getButton() == MouseEvent.BUTTON3) {
					// TODO: Remove this later. For stopping syntax
					// checker.
					errorCheckerService.stopThread();
					System.out.println("Syntax checker thread paused.");
				}
			}
		});

		errorTable = new XQErrorTable(errorCheckerService);
		scrollPane.setViewportView(errorTable);

		try {
			Docker = new DockTool2Base();
			addListeners();
		} catch (Exception e) {
			System.out.println("addListeners() acted silly.");
			e.printStackTrace();
		}

		if (thisEditor != null) {
			setLocation(new Point(thisEditor.getLocation().x
					+ thisEditor.getWidth(), thisEditor.getLocation().y));
		}

	}


	/**
	 * Updates the error table with new data(Table Model). Called from Syntax
	 * Checker Service.
	 * 
	 * @param tableModel
	 *            - Table Model
	 * @return True - If error table was updated successfully.
	 */
	synchronized public boolean updateTable(final TableModel tableModel) {
		return errorTable.updateTable(tableModel);
	}

	/**
	 * Adds various listeners to components of EditorWindow and also to the
	 * Editor window
	 */
	private void addListeners() {

		if (thisErrorWindow == null)
			System.out.println("ERW null");

		thisErrorWindow.addComponentListener(new ComponentListener() {

			@Override
			public void componentShown(ComponentEvent e) {

			}

			@Override
			public void componentResized(ComponentEvent e) {
				Docker.tryDocking();
			}

			@Override
			public void componentMoved(ComponentEvent e) {
				Docker.tryDocking();
			}

			@Override
			public void componentHidden(ComponentEvent e) {

			}
		});

		thisErrorWindow.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				problemWindowMenuCB.setSelected(false);
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				thisEditor.setExtendedState(Editor.NORMAL);
			}

		});

		if (thisEditor == null) {
			System.out.println("Editor null");
			return;
		}

		thisEditor.addWindowListener(new WindowAdapter() {
		

			@Override
			public void windowClosing(WindowEvent e) {

			}

			@Override
			public void windowClosed(WindowEvent e) {
				errorCheckerService.pauseThread = true;
				errorCheckerService.stopThread(); // Bye bye thread.
				thisErrorWindow.dispose();
			}

			@Override
			public void windowIconified(WindowEvent e) {
				thisErrorWindow.setExtendedState(Frame.ICONIFIED);
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				thisErrorWindow.setExtendedState(Frame.NORMAL);
			}

		});

		thisEditor.addComponentListener(new ComponentListener() {

			@Override
			public void componentShown(ComponentEvent e) {

			}

			@Override
			public void componentResized(ComponentEvent e) {
				if (Docker.isDocked()) {
					Docker.dock();
				} else {
					Docker.tryDocking();
				}
			}

			@Override
			public void componentMoved(ComponentEvent e) {

				if (Docker.isDocked()) {
					Docker.dock();
				} else {
					Docker.tryDocking();
				}

			}

			@Override
			public void componentHidden(ComponentEvent e) {
				// System.out.println("ed hidden");
			}
		});

	}

	/**
	 * Hnadle pausing the Error Checker Service thread.
	 * 
	 * @param e
	 *            - MouseEvent
	 */
	public void handlePause(MouseEvent e) {

		if (e.isControlDown()) {
			errorCheckerService.pauseThread = !errorCheckerService.pauseThread;

			if (errorCheckerService.pauseThread)
				System.out.println(thisEditor.getSketch().getName()
						+ " - Error Checker paused.");
			else
				System.out.println(thisEditor.getSketch().getName()
						+ " - Error Checker resumed.");
		}
	}

	/**
	 * Implements the docking feature of the tool - The frame sticks to the
	 * editor and once docked, moves along with it as the editor is resized,
	 * moved, or closed.
	 * 
	 * This class has been borrowed from Tab Manager tool by Thomas Diewald. It
	 * has been slightly modified and used here.
	 * 
	 * @author: Thomas Diewald , http://thomasdiewald.com
	 */
	private class DockTool2Base {

		private int docking_border = 0;
		private int dock_on_editor_y_offset_ = 0;
		private int dock_on_editor_x_offset_ = 0;

		// ///////////////////////////////
		// ____2____
		// | |
		// | |
		// 0 | editor | 1
		// | |
		// |_________|
		// 3
		// ///////////////////////////////

		// public void reset() {
		// dock_on_editor_y_offset_ = 0;
		// dock_on_editor_x_offset_ = 0;
		// docking_border = 0;
		// }

		public boolean isDocked() {
			return (docking_border >= 0);
		}

		private final int MAX_GAP_ = 20;

		//
		public void tryDocking() {
			if (thisEditor == null)
				return;
			Editor editor = thisEditor;
			Frame frame = thisErrorWindow;

			int ex = editor.getX();
			int ey = editor.getY();
			int ew = editor.getWidth();
			int eh = editor.getHeight();

			int fx = frame.getX();
			int fy = frame.getY();
			int fw = frame.getWidth();
			int fh = frame.getHeight();

			if (((fy > ey) && (fy < ey + eh))
					|| ((fy + fh > ey) && (fy + fh < ey + eh))) {
				int dis_border_left = Math.abs(ex - (fx + fw));
				int dis_border_right = Math.abs((ex + ew) - (fx));

				if (dis_border_left < MAX_GAP_ || dis_border_right < MAX_GAP_) {
					docking_border = (dis_border_left < dis_border_right) ? 0
							: 1;
					dock_on_editor_y_offset_ = fy - ey;
					dock();
					return;
				}
			}

			if (((fx > ex) && (fx < ex + ew))
					|| ((fx + fw > ey) && (fx + fw < ex + ew))) {
				int dis_border_top = Math.abs(ey - (fy + fh));
				int dis_border_bot = Math.abs((ey + eh) - (fy));

				if (dis_border_top < MAX_GAP_ || dis_border_bot < MAX_GAP_) {
					docking_border = (dis_border_top < dis_border_bot) ? 2 : 3;
					dock_on_editor_x_offset_ = fx - ex;
					dock();
					return;
				}
			}
			docking_border = -1;
		}

		public void dock() {
			if (thisEditor == null)
				return;
			Editor editor = thisEditor;
			Frame frame = thisErrorWindow;

			int ex = editor.getX();
			int ey = editor.getY();
			int ew = editor.getWidth();
			int eh = editor.getHeight();

			// int fx = frame.getX();
			// int fy = frame.getY();
			int fw = frame.getWidth();
			int fh = frame.getHeight();

			int x = 0, y = 0;
			if (docking_border == -1) {
				return;
			}

			if (docking_border == 0) {
				x = ex - fw;
				y = ey + dock_on_editor_y_offset_;
			}
			if (docking_border == 1) {
				x = ex + ew;
				y = ey + dock_on_editor_y_offset_;
			}

			if (docking_border == 2) {
				x = ex + dock_on_editor_x_offset_;
				y = ey - fh;
			}
			if (docking_border == 3) {
				x = ex + dock_on_editor_x_offset_;
				y = ey + eh;
			}
			frame.setLocation(x, y);
		}

	}
}
