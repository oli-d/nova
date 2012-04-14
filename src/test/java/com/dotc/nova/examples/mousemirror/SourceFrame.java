package com.dotc.nova.examples.mousemirror;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import com.dotc.nova.events.EventEmitter;

public class SourceFrame extends JFrame {
	private final EventEmitter eventEmitter;

	public SourceFrame(EventEmitter eventEmitter) throws HeadlessException {
		super("Source Frame");
		this.eventEmitter = eventEmitter;

		// some GUI stuff
		setBackground(Color.GREEN);
		JPanel infoPanel = new JPanel(new GridLayout(3, 1));
		infoPanel.add(new JLabel("Move the mouse to draw"));
		infoPanel.add(new JLabel("Click + move the mouse to draw a thick line"));
		infoPanel.add(new JLabel("Right-click to clear"));
		getContentPane().add(infoPanel, BorderLayout.CENTER);
		setSize(300, 300);
		setLocation(0, 0);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);

		// register listener, which forwards all mouse events to the Nova event queue
		getGlassPane().setVisible(true);
		ForwardingMouseEventListener mouseEventListener = new ForwardingMouseEventListener();
		getGlassPane().addMouseListener(mouseEventListener);
		getGlassPane().addMouseMotionListener(mouseEventListener);
	}

	private final class ForwardingMouseEventListener implements MouseListener, MouseMotionListener {
		/**
		 * Puts the passed event on the Nova event queue
		 */
		protected void forwardEvent(MouseEvent event) {
			eventEmitter.emit(event);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			forwardEvent(e);
		}

		@Override
		public void mousePressed(MouseEvent e) {
			forwardEvent(e);
		}

		@Override
		public void mouseExited(MouseEvent e) {
			forwardEvent(e);
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			forwardEvent(e);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			forwardEvent(e);
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			forwardEvent(e);
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			forwardEvent(e);
		}

	}
}
