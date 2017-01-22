/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the license described in the "LICENSE" file in the root directory
 * of the distribution package.
 */

package ch.squaredesk.nova.examples.events.mousemirror;

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
