/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the license described in the "LICENSE" file in the root directory
 * of the distribution package.
 */

package ch.squaredesk.nova.examples.filesystem;

import ch.squaredesk.nova.Nova;
import ch.squaredesk.nova.filesystem.FileReadHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

/**
 * This example shows the two ways, how files can be read with the Nova API: synchronous and asyncronous.
 * 
 * @author oli
 * 
 */
public class FileReader extends JFrame {
	private JTextArea textArea;
	private JCheckBox checkBox;
	private Nova nova;

	public FileReader() {
		super("Nova File Reader example");

		initUI();

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
		nova = new Nova.Builder().build();

	}

	/**
	 * <pre>
	 * ********************************************************************* * 
	 * ********************************************************************* * 
	 * ***                                                               *** *
	 * *** Reading files async requires a handler callback to be passed, *** *
	 * *** which is invoked when either the file was fully read, or an   *** *
	 * *** exception was caught                                          *** *
	 * ***                                                               *** *
	 * ********************************************************************* *
	 * ********************************************************************* *
	 */
	private void readFileAsync(String pathToFile) {
		textArea.setText("Reading file " + pathToFile + "...");
		nova.filesystem.readFile(pathToFile, new FileReadHandler() {

			@Override
			public void fileRead(String fileContents) {
				textArea.setText(fileContents);
			}

			@Override
			public void errorOccurred(Throwable error) {
				textArea.setText("Unable to read file.\nError: " + error);
			}
		});
	}

	/**
	 * <pre>
	 * ********************************************************************* * 
	 * ********************************************************************* * 
	 * ***                                                               *** *
	 * *** Reading files sync only requires the file path, but you have  *** *
	 * *** to wrap the call in a try / catch block to handle an          *** *
	 * *** eventually occurring IOException                              *** *
	 * ***                                                               *** *
	 * ********************************************************************* *
	 * ********************************************************************* *
	 */
	private void readFileSync(String pathToFile) {
		textArea.setText("Reading file " + pathToFile + "...");
		try {
			textArea.setText(nova.filesystem.readFileSync(pathToFile));
		} catch (IOException e) {
			textArea.setText("Unable to read file.\nError: " + e);
		}
	}

	private void initUI() {
		textArea = new JTextArea();
		checkBox = new JCheckBox("Async");
		checkBox.setSelected(true);

		// the load button
		JButton buttonOpenFile = new JButton("Open File...");
		buttonOpenFile.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
				fileChooser.setMultiSelectionEnabled(false);
				int returnVal = fileChooser.showOpenDialog(FileReader.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					if (checkBox.isSelected()) {
						readFileAsync(fileChooser.getSelectedFile().getAbsolutePath());
					} else {
						readFileSync(fileChooser.getSelectedFile().getAbsolutePath());
					}
				}
			}
		});

		// assemble UI
		JPanel panel = new JPanel(new FlowLayout());
		panel.add(buttonOpenFile);
		panel.add(checkBox);
		getContentPane().add(panel, BorderLayout.NORTH);
		getContentPane().add(new JScrollPane(textArea), BorderLayout.CENTER);
	}

	public static void main(String[] args) {
		FileReader reader = new FileReader();
		reader.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		reader.setSize(300, 600);
		reader.setVisible(true);
	}
}
