package quarkninja.mode.xqmode;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.TableModel;

public class XQErrorTable extends JTable {

	/**
	 * Column Names of JTable
	 */
	public static final String[] columnNames = { "Problem", "Tab", "Line" };

	/**
	 * Column Widths of JTable.
	 */
	public int[] columnWidths = { 600, 100, 50 }; // Default Values

	/**
	 * Is the column being resized?
	 */
	private boolean columnResizing = false;

	ErrorCheckerService errorCheckerService;

	public boolean isCellEditable(int rowIndex, int colIndex) {
		return false; // Disallow the editing of any cell
	}

	public XQErrorTable(final ErrorCheckerService errorCheckerService) {
		this.errorCheckerService = errorCheckerService;
		for (int i = 0; i < this.getColumnModel().getColumnCount(); i++) {
			this.getColumnModel().getColumn(i)
					.setPreferredWidth(columnWidths[i]);
		}

		this.getTableHeader().setReorderingAllowed(false);

		this.addMouseListener(new MouseAdapter() {
			// TODO: synchronized or Swing Worker ?
			synchronized public void mouseReleased(MouseEvent e) {
				try {
					errorCheckerService.scrollToErrorLine(((XQErrorTable) e
							.getSource()).getSelectedRow());
					// System.out.print("Row clicked: "
					// + ((XQErrorTable) e.getSource()).getSelectedRow());
				} catch (Exception e1) {
					System.out.println("Exception EW mouseReleased " + e);
				}
			}
		});
		
		this.getTableHeader().addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				columnResizing = true;
			}

			public void mouseReleased(MouseEvent e) {
				columnResizing = false;
				for (int i = 0; i < ((XQErrorTable) e.getSource()).getColumnModel()
						.getColumnCount(); i++) {
					columnWidths[i] = ((XQErrorTable) e.getSource()).getColumnModel().getColumn(i)
							.getWidth();
				}
			}
		});
	}
	
	synchronized public boolean updateTable(final TableModel tableModel) {

		// If problems list is not visible, no need to update
		if (!this.isVisible())
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
					setModel(tableModel);
					for (int i = 0; i < getColumnModel()
							.getColumnCount(); i++) {
						getColumnModel().getColumn(i)
								.setPreferredWidth(columnWidths[i]);
					}
					// errorTable.setPreferredScrollableViewportSize(new
					// Dimension(1000, 500));
					getTableHeader().setReorderingAllowed(false);
					validate();
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

}
