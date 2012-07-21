package quarkninja.mode.xqmode;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.Box;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import processing.app.Base;
import processing.app.EditorState;
import processing.app.Mode;
import processing.mode.java.JavaEditor;

/**
 * Editor for XQMode
 * 
 * @author Manindra Moharana
 * 
 */
public class XQEditor extends JavaEditor {

	XQMode xqmode;
	protected Thread errorCheckerThread = null;
	protected ErrorCheckerService errorCheckerService;
	protected ErrorBar errorBar;

	protected XQEditor(Base base, String path, EditorState state,
			final Mode mode) {
		super(base, path, state, mode);
		final XQEditor thisEditor = this;
		EventQueue.invokeLater(new Runnable() {
			public void run() {

				xqmode = (XQMode) mode;
				errorBar = new ErrorBar(thisEditor,
						textarea.getMinimumSize().height);

				// Starts it too! Error bar should be ready beforehand
				initializeErrorChecker();
				errorBar.errorCheckerService = errorCheckerService;

				// - Start
				JPanel textAndError = new JPanel();
				Box box = (Box) textarea.getParent();
				box.remove(2); // Remove textArea from it's container, i.e Box
				textAndError.setLayout(new BorderLayout());
				textAndError.add(errorBar, BorderLayout.EAST);
				textarea.setBounds(0, 0, errorBar.getX() - 1,
						textarea.getHeight());
				textAndError.add(textarea);
				box.add(textAndError);
				// - End

				// for (int i = 0; i < consolePanel.getComponentCount(); i++) {
				// System.out.println("Console: "
				// + consolePanel.getComponent(i));
				// }
				// consolePanel.remove(0);
				// consolePanel.remove(1);
				// JTable table = new JTable(new String[][] {
				// { "Problems", "Line no" },
				// { "Missing semicolon", "12" }, { "Extra  )", "15" },
				// { "Missing semicolon", "34" } }, new String[] { "A",
				// "B" });
				// consolePanel.add(table);
				// consolePanel.validate();
				// console.updateUI();

			}
		});

		// textarea.setBounds(errorBar.getX() + errorBar.getWidth(),
		// errorBar.getY(), textarea.getWidth(), textarea.getHeight());

		// for (int i = 0; i < consolePanel.getComponentCount(); i++) {
		// System.out.println("Console: " + consolePanel.getComponent(i));
		// }
		// consolePanel.remove(1);
		// JTable table = new JTable(new String[][] { { "Problems", "Line no" },
		// { "Missing semicolon", "12" },
		// { "Extra  )", "15" },{ "Missing semicolon", "34" } },
		// new String[] { "A", "B" });
		// consolePanel.add(table);

	}

	public JMenu buildModeMenu() {

		// Enable Error Checker - CB
		// Show/Hide Problem Window - CB

		// TODO:
		// Enable Syntax Check - CB
		// Enable Compile Check - CB
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

		item = new JCheckBoxMenuItem("Show Problem Window");
		item.setSelected(true);
		item.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (errorCheckerService.errorWindow == null)
					return;
				errorCheckerService.errorWindow
						.setVisible(((JCheckBoxMenuItem) e.getSource())
								.isSelected());
			}
		});
		menu.add(item);

		return menu;
	}

	/**
	 * Initializes and starts Syntax Checker Service
	 */
	private void initializeErrorChecker() {
		if (errorCheckerThread == null) {
			errorCheckerService = new ErrorCheckerService(this, errorBar);
			errorCheckerThread = new Thread(errorCheckerService);
			try {
				errorCheckerThread.start();
			} catch (Exception e) {
				System.err
						.println("Syntax Checker Service not initialized [XQEditor]: "
								+ e);
				// e.printStackTrace();
			}
			// System.out.println("Syntax Checker Service initialized.");
		}

	}

	public void handleRun() {
		errorCheckerService.pauseThread = true;
		System.out.println("Paused ECS.");
		super.handleRun();
	}

	// public void handlePresent() {
	// errorCheckerService.pauseThread = true;
	// System.out.println("Paused ECS.");
	// super.handlePresent();
	// }

	// public void handleStop() {
	// errorCheckerService.pauseThread = false;
	// System.out.println("Resuming ECS.");
	// super.handleStop();
	// }

	public void deactivateRun() {
		errorCheckerService.pauseThread = false;
		System.out.println("Resuming ECS.");
		super.deactivateRun();
	}

}