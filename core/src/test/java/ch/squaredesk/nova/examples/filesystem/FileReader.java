/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.examples.filesystem;

import ch.squaredesk.nova.Nova;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * This example shows the two ways, how files can be read with the Nova API: synchronous and asyncronous.
 * 
 * @author oli
 * 
 */
public class FileReader extends JFrame {
    private JTextArea textArea;
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
         * *** Initilize Nova by creating a new instance of Nova               *** *
         * ***                                                                 *** *
         * *********************************************************************** *
         * *********************************************************************** *
         */
        nova = Nova.builder().build();

    }

    /**
     * <pre>
     * ************************************************************************* *
     * ************************************************************************* *
     * ***                                                                   *** *
     * *** 2nd step: Read files async and act appropriately when the results *** *
     * *** are pushed                                                        *** *
     * ***                                                                   *** *
     * ************************************************************************* *
     * ************************************************************************* *
     */
    private void readFileAsync(String pathToFile) {
        textArea.setText("Reading file " + pathToFile + "...");
        nova.filesystem.readTextFileFully(pathToFile).subscribe(
                s ->  textArea.setText(s),
                t ->  textArea.setText("Unable to read file.\nError: " + t));
    }

    private void initUI() {
        textArea = new JTextArea();

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
                    readFileAsync(fileChooser.getSelectedFile().getAbsolutePath());
                }
            }
        });

        // assemble UI
        JPanel panel = new JPanel(new FlowLayout());
        panel.add(buttonOpenFile);
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
