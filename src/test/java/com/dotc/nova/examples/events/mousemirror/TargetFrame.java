package com.dotc.nova.examples.events.mousemirror;

import java.awt.Color;

import javax.swing.JFrame;

public class TargetFrame extends JFrame {

	public TargetFrame() {
		super("Target Frame");

		// some GUI stuff
		setBackground(Color.CYAN);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setResizable(false);
	}

}
