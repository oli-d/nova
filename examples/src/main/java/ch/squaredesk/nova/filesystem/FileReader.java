/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.filesystem;

import ch.squaredesk.nova.Nova;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.swing.*;
import java.awt.*;

/**
 * This example shows how files can be read with the Nova API.
 * 
 * @author oli
 * 
 */
@SpringBootApplication
public class FileReader extends JFrame implements CommandLineRunner {
    private JTextArea textArea;
    private Nova nova;

    public static void main(String[] args) throws Exception {
        SpringApplication.run(FileReader.class, args);
    }

    public FileReader(Nova nova) {
        super("Nova File Reader example");

        this.nova = nova;

        initUI();
    }

    private void readFile(String pathToFile) {
        textArea.setText("Reading file " + pathToFile + "...");
        nova.filesystem.readTextFileFully(pathToFile).subscribe(
                s ->  textArea.setText(s),
                t ->  textArea.setText("Unable to read file.\nError: " + t));
    }

    private void initUI() {
        textArea = new JTextArea();

        // the load button
        JButton buttonOpenFile = new JButton("Open File...");
        buttonOpenFile.addActionListener(event -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
            fileChooser.setMultiSelectionEnabled(false);
            int returnVal = fileChooser.showOpenDialog(FileReader.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                readFile(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });

        // assemble UI
        JPanel panel = new JPanel(new FlowLayout());
        panel.add(buttonOpenFile);
        getContentPane().add(panel, BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(textArea), BorderLayout.CENTER);
    }

    @Override
    public void run(String... args) throws Exception {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 600);
        setVisible(true);
    }
}
