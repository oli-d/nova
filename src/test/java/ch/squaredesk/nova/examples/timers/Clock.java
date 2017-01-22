/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the license described in the "LICENSE" file in the root directory
 * of the distribution package.
 */

package ch.squaredesk.nova.examples.timers;

import java.awt.BorderLayout;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JLabel;

import ch.squaredesk.nova.Nova;

public class Clock extends JFrame {
	private SimpleDateFormat dateFormatter = new SimpleDateFormat("HH:mm:ss");
	private JLabel label = new JLabel();

	public Clock() {
		super("Nova Timers example");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(200, 100);
		getContentPane().add(label, BorderLayout.CENTER);
		setLocationRelativeTo(null);
	}

	public static void main(String[] args) {
		Clock clock = new Clock();
		clock.setVisible(true);
		clock.tickCurrentTime();
	}

	private void tickCurrentTime() {
		/**
		 * <pre>
		 * *********************************************************************** * 
		 * *********************************************************************** * 
		 * ***                                                                 *** *
		 * *** 1st step:                                                       *** *
		 * *** Initilize Nova by creating a new instance of Nova *** *
		 * ***                                                                 *** *
		 * *********************************************************************** *
		 * *********************************************************************** *
		 */
		Nova nova = new Nova.Builder().build();

		/**
		 * <pre>
		 * *********************************************************************** * 
		 * *********************************************************************** * 
		 * ***                                                                 *** *
		 * *** 2nd step:                                                       *** *
		 * *** Register a callback, which will be executed periodically        *** *
		 * ***                                                                 *** *
		 * *********************************************************************** *
		 * *********************************************************************** *
		 */
		nova.timers.setInterval(new Runnable() {

			@Override
			public void run() {
				label.setText(dateFormatter.format(new Date()));
			}
		}, 1000); // 1000ms == 1sec
	}
}
