package com.dotc.nova.examples.events.mousemirror;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.dotc.nova.events.wrappers.SingleParameterEventListener;

public class MouseEventTranslator extends SingleParameterEventListener<MouseEvent> {
	private final JFrame targetFrame;

	public MouseEventTranslator(JFrame targetFrame) {
		this.targetFrame = targetFrame;
	}

	@Override
	public void handle(final MouseEvent event) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				switch (event.getID()) {
					case MouseEvent.MOUSE_MOVED:
						handleMouseMove(event, false);
						return;
					case MouseEvent.MOUSE_DRAGGED:
						handleMouseMove(event, true);
						return;
					case MouseEvent.MOUSE_CLICKED:
						handleMouseClicked(event);
						return;
					default:
						// no handling
				}
			}
		});
	}

	private void handleMouseMove(MouseEvent e, boolean mouseClicked) {
		Graphics2D g2 = (Graphics2D) this.targetFrame.getContentPane().getGraphics();
		g2.setStroke(new BasicStroke(mouseClicked ? 3 : 1));
		g2.drawLine(e.getX(), e.getY(), e.getX(), e.getY());
	}

	private void handleMouseClicked(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON3) {
			Graphics2D g2 = (Graphics2D) this.targetFrame.getContentPane().getGraphics();
			g2.clearRect(0, 0, 300, 300);
		}
	}

}