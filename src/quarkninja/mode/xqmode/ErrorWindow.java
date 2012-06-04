package quarkninja.mode.xqmode;

import java.awt.Component;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Vector;

@SuppressWarnings("serial")
public class ErrorWindow extends JFrame {

	private JPanel contentPane;
	private JTable errorTable;
	private JScrollPane scrollPane;
	private JButton btnAddRow;
	private JButton btnDeleteRow;

	// protected InteractiveTableModel tableModel;

	public static final String[] columnNames = { "Error", "Line Number", "" };

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ErrorWindow frame = new ErrorWindow();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public ErrorWindow() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setBounds(100, 100, 458, 312);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		scrollPane = new JScrollPane();
		scrollPane.setBounds(10, 11, 422, 226);
		contentPane.add(scrollPane);

		// tableModel = new InteractiveTableModel(columnNames);
		// tableModel.addTableModelListener(new
		// InteractiveTableModelListener());

		errorTable = new JTable();
		errorTable.setModel(new DefaultTableModel(new Object[][] {},
				new String[] { "Error", "Line Number" }));
		// errorTable.setModel(tableModel);
		// errorTable.setSurrendersFocusOnKeystroke(true);
		// if (!tableModel.hasEmptyRow()) {
		// tableModel.addEmptyRow();
		// }
		// TableColumn hidden = errorTable.getColumnModel().getColumn(
		// InteractiveTableModel.HIDDEN_INDEX);
		// hidden.setMinWidth(2);
		// hidden.setPreferredWidth(2);
		// hidden.setMaxWidth(2);
		// hidden.setCellRenderer(new InteractiveRenderer(
		// InteractiveTableModel.HIDDEN_INDEX));

		scrollPane.setViewportView(errorTable);

		btnAddRow = new JButton("Add Row");
		btnAddRow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				((DefaultTableModel) errorTable.getModel()).insertRow(0,
						new String[] { "dfgdfgd", "324" });
				
			}
		});
		btnAddRow.setBounds(20, 248, 89, 23);
		contentPane.add(btnAddRow);

		btnDeleteRow = new JButton("Delete Row");
		btnDeleteRow.setBounds(343, 248, 89, 23);
		contentPane.add(btnDeleteRow);
	}
	
	public boolean updateTable(TableModel tm) {
		errorTable.setModel(tm);
		return true;		
	}

	// public void highlightLastRow(int row) {
	// int lastrow = tableModel.getRowCount();
	// if (row == lastrow - 1) {
	// errorTable.setRowSelectionInterval(lastrow - 1, lastrow - 1);
	// } else {
	// errorTable.setRowSelectionInterval(row + 1, row + 1);
	// }
	//
	// errorTable.setColumnSelectionInterval(0, 0);
	// }

	class InteractiveRenderer extends DefaultTableCellRenderer {
		protected int interactiveColumn;

		public InteractiveRenderer(int interactiveColumn) {
			this.interactiveColumn = interactiveColumn;
		}

		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			Component c = super.getTableCellRendererComponent(table, value,
					isSelected, hasFocus, row, column);
			// if (column == interactiveColumn && hasFocus) {
			// if ((ErrorWindow.this.tableModel.getRowCount() - 1) == row
			// && !ErrorWindow.this.tableModel.hasEmptyRow()) {
			// ErrorWindow.this.tableModel.addEmptyRow();
			// }
			//
			// // highlightLastRow(row);
			// }

			return c;
		}
	}

	public class InteractiveTableModelListener implements TableModelListener {
		public void tableChanged(TableModelEvent evt) {
			if (evt.getType() == TableModelEvent.UPDATE) {
				int column = evt.getColumn();
				int row = evt.getFirstRow();
				System.out.println("row: " + row + " column: " + column);
				errorTable.setColumnSelectionInterval(column + 1, column + 1);
				errorTable.setRowSelectionInterval(row, row);
			}
		}
	}
}
