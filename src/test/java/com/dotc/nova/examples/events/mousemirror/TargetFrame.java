package com.dotc.nova.examples.events.mousemirror;

import java.awt.Color;
import java.awt.HeadlessException;

import javax.swing.JFrame;

public class TargetFrame extends JFrame {

	public TargetFrame() throws HeadlessException {
		super("Target Frame");

		// some GUI stuff
		setBackground(Color.CYAN);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setResizable(false);
	}

}
