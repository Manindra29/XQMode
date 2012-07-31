package quarkninja.mode.xqmode;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPanel;

public class XQConsoleToggle extends JPanel implements MouseListener {
	public static String[] text = { "Console", "Problems" };
	public boolean toggleText = true, toggleBG = true;
	public int height;
	public XQEditor editor;

	public XQConsoleToggle(XQEditor editor, int height){
		this.height = height;
	}
	public Dimension getPreferredSize() {
		return new Dimension(50, height);
	}

	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

	public Dimension getMaximumSize() {
		return getPreferredSize();
	}

	public void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		if (toggleBG) {
			g.setColor(new Color(0xff9DA7B0));
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
			g.setColor(Color.BLACK);
		} else {
			g.setColor(Color.DARK_GRAY);
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
			g.setColor(Color.WHITE);
		}

		if (toggleText) {
			g.drawString(text[0], 6, this.getHeight() - 6);
		} else {
			g.drawString(text[1], 4, this.getHeight() - 6);
		}
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		CardLayout cl = (CardLayout)(editor.cards.getLayout());
		cl.show(editor.cards, toggleText? text[0]:text[1]);
		toggleText = !toggleText;
		
		this.repaint();
		System.out.println("Clicked! ");
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		toggleBG = !toggleBG;
		this.repaint();
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		toggleBG = !toggleBG;
		this.repaint();
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
	}
}