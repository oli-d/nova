package com.dotc.nova.examples.mousemirror;

import java.awt.*;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;

import com.dotc.nova.events.EventEmitter;
import com.dotc.nova.events.EventListener;

public class TargetFrame extends JFrame {
	private final EventEmitter eventEmitter;

	public TargetFrame(EventEmitter eventEmitter) throws HeadlessException {
		super("Target Frame");
		this.eventEmitter = eventEmitter;

		// register listener, which translates all forwarded MouseEvents from the source frame
		this.eventEmitter.addListener(MouseEvent.class, new MyNovaEventListener());

		// some GUI stuff
		setBackground(Color.CYAN);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setResizable(false);
	}

	private class MyNovaEventListener implements EventListener<MouseEvent> {

		@Override
		public void handle(MouseEvent event) {
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

		private void handleMouseMove(MouseEvent e, boolean mouseClicked) {
			Graphics2D g2 = (Graphics2D) getContentPane().getGraphics();
			g2.setStroke(new BasicStroke(mouseClicked ? 3 : 1));
			g2.drawLine(e.getX(), e.getY(), e.getX(), e.getY());
		}

		private void handleMouseClicked(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON3) {
				Graphics2D g2 = (Graphics2D) getContentPane().getGraphics();
				g2.clearRect(0, 0, 300, 300);
			}
		}
	}

}
