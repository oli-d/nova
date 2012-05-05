package com.dotc.nova.examples.events.mousemirror;

import java.awt.*;

import javax.swing.*;

public class SourceFrame extends JFrame {

	public SourceFrame() throws HeadlessException {
		super("Source Frame");

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
	}
}
