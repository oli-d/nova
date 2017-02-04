/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
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
		 * *** Initilize Nova by creating a new instance of Nova               *** *
		 * ***                                                                 *** *
		 * *********************************************************************** *
		 * *********************************************************************** *
		 */
		Nova nova = Nova.builder().build();

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
