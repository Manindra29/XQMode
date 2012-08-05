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
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import processing.app.Base;
import processing.app.EditorState;
import processing.app.Mode;
import processing.app.syntax.JEditTextArea;
import processing.app.syntax.PdeTextAreaDefaults;
import processing.mode.java.JavaEditor;

/**
 * Editor for XQMode
 * 
 * @author Manindra Moharana &lt;mkmoharana29@gmail.com&gt;
 * 
 */
public class XQEditor extends JavaEditor {

	XQMode xqmode;
	protected Thread errorCheckerThread = null;
	protected ErrorCheckerService errorCheckerService;
	protected ErrorBar errorBar;
	/**
	 * Check box menu item for show/hide Problem Window
	 */
	public JCheckBoxMenuItem problemWindowMenuCB;

	/**
	 * Custom TextArea
	 */
	protected XQTextArea xqTextArea;
	JTable errorTable;
	protected final XQEditor thisEditor;
	/**
	 * Column Widths of JTable.
	 */
	public int[] columnWidths = { 600, 100, 50 }; // Default Values

	private boolean columnResizing = false;

	protected XQEditor(Base base, String path, EditorState state,
			final Mode mode) {
		super(base, path, state, mode);
		thisEditor = this;

		xqmode = (XQMode) mode;
		errorBar = new ErrorBar(thisEditor, textarea.getMinimumSize().height);
		// Starts it too! Error bar should be ready beforehand
		initializeErrorChecker();
		errorBar.errorCheckerService = errorCheckerService;
		xqTextArea.setErrorCheckerService(errorCheckerService);

		// Adding ErrorBar
		JPanel textAndError = new JPanel();
		Box box = (Box) textarea.getParent();
		box.remove(2); // Remove textArea from it's container, i.e Box
		textAndError.setLayout(new BorderLayout());
		textAndError.add(errorBar, BorderLayout.EAST);
		textarea.setBounds(0, 0, errorBar.getX() - 1, textarea.getHeight());
		textAndError.add(textarea);
		box.add(textAndError);
		// - End

		// Adding Error Table in a scroll pane
		errorTableScrollPane = new JScrollPane();
		errorTable = new JTable() {
			public boolean isCellEditable(int rowIndex, int colIndex) {
				return false; // Disallow the editing of any cell
			}
		};
		errorTable.setModel(new DefaultTableModel(new Object[][] {},
				ErrorWindow.columnNames));
		errorTable.getColumnModel().getColumn(0).setPreferredWidth(300);
		errorTable.getColumnModel().getColumn(1).setPreferredWidth(100);
		errorTable.getColumnModel().getColumn(2).setPreferredWidth(50);
		errorTable.getTableHeader().setReorderingAllowed(false);
		// errorTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		errorTable.addMouseListener(new MouseAdapter() {
			// TODO: synchronized or Swing Worker ?
			synchronized public void mouseReleased(MouseEvent e) {
				try {
					errorCheckerService.scrollToErrorLine(errorTable
							.getSelectedRow());
					// System.out.print("Row clicked: "
					// + (errorTable.getSelectedRow()));
				} catch (Exception e1) {
					System.out.println("Exception EW mouseReleased " + e);
				}
			}
		});

		errorTable.getTableHeader().addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				columnResizing = true;
			}

			public void mouseReleased(MouseEvent e) {
				columnResizing = false;
				for (int i = 0; i < errorTable.getColumnModel()
						.getColumnCount(); i++) {
					columnWidths[i] = errorTable.getColumnModel().getColumn(i)
							.getWidth();
				}
			}
		});

		errorTableScrollPane.setViewportView(errorTable);

		// Adding toggle console button
		consolePanel.remove(2);
		JPanel lineStatusPanel = new JPanel();
		lineStatusPanel.setLayout(new BorderLayout());
		btnShowConsole = new XQConsoleToggle(thisEditor,
				XQConsoleToggle.text[0], lineStatus.getHeight());
		btnShowErrors = new XQConsoleToggle(thisEditor,
				XQConsoleToggle.text[1], lineStatus.getHeight());
		btnShowConsole.addMouseListener(btnShowConsole);

		// lineStatusPanel.add(btnShowConsole, BorderLayout.EAST);
		// lineStatusPanel.add(btnShowErrors);
		btnShowErrors.addMouseListener(btnShowErrors);

		JPanel toggleButtonPanel = new JPanel(new BorderLayout());
		toggleButtonPanel.add(btnShowConsole, BorderLayout.EAST);
		toggleButtonPanel.add(btnShowErrors, BorderLayout.WEST);
		lineStatusPanel.add(toggleButtonPanel, BorderLayout.EAST);
		lineStatus.setBounds(0, 0, toggleButtonPanel.getX() - 1,
				toggleButtonPanel.getHeight());
		lineStatusPanel.add(lineStatus);
		consolePanel.add(lineStatusPanel, BorderLayout.SOUTH);
		lineStatusPanel.repaint();

		// Adding JPanel with CardLayout for Console/Problems Toggle
		consolePanel.remove(1);
		consoleProblemsPane = new JPanel(new CardLayout());
		consoleProblemsPane.add(errorTableScrollPane, XQConsoleToggle.text[1]);
		consoleProblemsPane.add(console, XQConsoleToggle.text[0]);
		consolePanel.add(consoleProblemsPane, BorderLayout.CENTER);
	}

	public void toggleView(String buttonName) {
		CardLayout cl = (CardLayout) consoleProblemsPane.getLayout();
		cl.show(consoleProblemsPane, buttonName);
	}

	XQConsoleToggle btnShowConsole;
	XQConsoleToggle btnShowErrors;
	public JLayeredPane jlp;
	boolean ab = true;
	final JScrollPane errorTableScrollPane;
	public JPanel consoleProblemsPane;

	@SuppressWarnings("rawtypes")
	synchronized public boolean updateTable(final TableModel tableModel) {

		// If problems list is not visible, no need to update
		if (!btnShowConsole.toggleText)
			return false;
		/*
		 * SwingWorker - The dirty side of Swing. Turns out that if you update a
		 * swing component outside of the swing event thread, all sort of weird
		 * exceptions are thrown. The worse part is, these exception don't get
		 * caught easily either.
		 * 
		 * The 'correct' way of updating swing components is therefore, to do
		 * the updating through a SwingWorker, inside done(). The updating then
		 * takes place in a thread safe manner.
		 */

		SwingWorker worker = new SwingWorker() {

			protected Object doInBackground() throws Exception {
				return null;
			}

			protected void done() {

				try {
					errorTable.setModel(tableModel);
					for (int i = 0; i < errorTable.getColumnModel()
							.getColumnCount(); i++) {
						errorTable.getColumnModel().getColumn(i)
								.setPreferredWidth(columnWidths[i]);
					}
					// errorTable.setPreferredScrollableViewportSize(new
					// Dimension(1000, 500));
					errorTable.getTableHeader().setReorderingAllowed(false);
					errorTable.validate();
					errorTable.updateUI();
					errorTable.repaint();
					// errorTable.setFocusable(false);
				} catch (Exception e) {
					System.out.println("Exception at updatTable " + e);
					// e.printStackTrace();
				}
				// errorTable.getModel()

			}
		};
		try {
			if (!columnResizing)
				worker.execute();
		} catch (Exception e) {
			System.out.println("Errorwindow updateTable Worker's slacking."
					+ e.getMessage());
			// e.printStackTrace();
		}
		return true;
	}

	/**
	 * Override creation of the default textarea. Neat hack Martin!
	 */
	protected JEditTextArea createTextArea() {
		xqTextArea = new XQTextArea(new PdeTextAreaDefaults(mode));
		return xqTextArea;
	}

	public JCheckBoxMenuItem showWarnings;

	public JMenu buildModeMenu() {

		// Enable Error Checker - CB
		// Show/Hide Problem Window - CB
		// Show Warnings - CB
		JMenu menu = new JMenu("XQMode");
		JCheckBoxMenuItem item;

		item = new JCheckBoxMenuItem("Error Checker Enabled");
		item.setSelected(true);
		item.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				errorCheckerService.pauseThread = !((JCheckBoxMenuItem) e
						.getSource()).isSelected();
				if (errorCheckerService.pauseThread)
					System.out.println(getSketch().getName()
							+ " - Error Checker paused.");
				else
					System.out.println(getSketch().getName()
							+ " - Error Checker resumed.");
			}
		});
		menu.add(item);

		problemWindowMenuCB = new JCheckBoxMenuItem("Show Problem Window");
		// problemWindowMenuCB.setSelected(true);
		problemWindowMenuCB.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (errorCheckerService.errorWindow == null)
					return;
				errorCheckerService.errorWindow
						.setVisible(((JCheckBoxMenuItem) e.getSource())
								.isSelected());
			}
		});
		menu.add(problemWindowMenuCB);

		showWarnings = new JCheckBoxMenuItem("Warnings Enabled");
		showWarnings.setSelected(true);
		showWarnings.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				errorCheckerService.warningsEnabled = ((JCheckBoxMenuItem) e
						.getSource()).isSelected();
			}
		});
		menu.add(showWarnings);

		return menu;
	}

	/**
	 * Initializes and starts Error Checker Service
	 */
	private void initializeErrorChecker() {
		if (errorCheckerThread == null) {
			errorCheckerService = new ErrorCheckerService(thisEditor, errorBar);
			errorCheckerService.problemWindowMenuCB = this.problemWindowMenuCB;
			errorCheckerThread = new Thread(errorCheckerService);
			try {
				errorCheckerThread.start();
			} catch (Exception e) {
				System.err
						.println("Error Checker Service not initialized [XQEditor]: "
								+ e);
				// e.printStackTrace();
			}
			// System.out.println("Error Checker Service initialized.");
		}

	}

}
