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
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
	protected XQErrorTable errorTable;
	protected final XQEditor thisEditor;
	protected boolean compilationCheckEnabled = true;
	
	/**
	 * Show Console button
	 */
	XQConsoleToggle btnShowConsole;
	
	/**
	 * Show Problems button
	 */
	XQConsoleToggle btnShowErrors;
	
	
	final JScrollPane errorTableScrollPane;
	
	public JPanel consoleProblemsPane;

	protected XQEditor(Base base, String path, EditorState state,
			final Mode mode) {
		super(base, path, state, mode);
		thisEditor = this;

		xqmode = (XQMode) mode;

		checkForJavaTabs();

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
		errorTable = new XQErrorTable(errorCheckerService);
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

	/**
	 * Toggle between Console and Errors List
	 * @param buttonName - Button Label
	 */
	public void toggleView(String buttonName) {
		CardLayout cl = (CardLayout) consoleProblemsPane.getLayout();
		cl.show(consoleProblemsPane, buttonName);
	}

	synchronized public boolean updateTable(final TableModel tableModel) {
		return errorTable.updateTable(tableModel);
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
				// switch to console, now that Error Window is open
				toggleView(XQConsoleToggle.text[0]);
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

	/**
	 * Checks if the sketch contains java tabs. If it does, XQMode ain't built
	 * for it, yet. Also, user should really start looking at Eclipse. Disable
	 * compilation check.
	 */
	private void checkForJavaTabs() {
		for (int i = 0; i < thisEditor.getSketch().getCodeCount(); i++) {
			if (thisEditor.getSketch().getCode(i).getExtension().equals("java")) {
				compilationCheckEnabled = false;
				JOptionPane.showMessageDialog(new Frame(), thisEditor
						.getSketch().getName()
						+ " contains .java tabs. XQMode doesn't "
						+ "support java tabs. Only "
						+ "syntax errors will be reported for .pde tabs.");
				break;
			}
		}
	}

}
