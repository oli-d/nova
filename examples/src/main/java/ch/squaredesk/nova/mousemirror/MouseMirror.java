/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.mousemirror;

import ch.squaredesk.nova.Nova;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * This simple example shows how to use the Nove event queue.
 * 
 * It creates a source JFrame and translates that Frame's MouseEvents to 2 target JFrames.
 * 
 * The translation is done by the source frame putting all MouseEvents onto the Nove event queue, and register one Nova event queue listener per target frame, which takes the MouseEvents and applies
 * them to the target frame.
 * 
 */
@SpringBootApplication
public class MouseMirror {
    @Autowired
    private Nova nova;

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(MouseMirror.class)
                .headless(false).run(args);

        EventQueue.invokeLater(() -> {
            MouseMirror ex = ctx.getBean(MouseMirror.class);
            ex.run();
        });
    }

    public void run() {
        // create the UI components
        JFrame sourceFrame = createSourceFrame();
        JFrame[] targetFrames = createTargetFrames(2);

        /**
         * <pre>
         * *********************************************************************** * 
         * *********************************************************************** * 
         * ***                                                                 *** *
         * *** 1st step:                                                       *** *
         * *** Initialize Nova by creating a new instance of Nova *** *
         * ***                                                                 *** *
         * *********************************************************************** *
         * *********************************************************************** *
         */
        final Nova nova = Nova.builder().build();

        /**
         * <pre>
         * *************************************************************************************** * 
         * *************************************************************************************** * 
         * ***                                                                                 *** *
         * *** 2nd step:                                                                       *** *
         * *** Register listener on source frame, which puts all MouseEvents on the Nova queue *** *
         * ***                                                                                 *** *
         * *************************************************************************************** *
         * *************************************************************************************** *
         */
        MouseEventForwarder eventForwarder = new MouseEventForwarder() {
            @Override
            public void forwardEvent(MouseEvent event) {
                nova.eventBus.emit("MouseEvent", event);
            }
        };
        sourceFrame.getGlassPane().addMouseListener(eventForwarder);
        sourceFrame.getGlassPane().addMouseMotionListener(eventForwarder);

        /**
         * <pre>
         * ********************************************************************************************** * 
         * ********************************************************************************************** * 
         * ***                                                                                        *** *
         * *** 3rd step:                                                                              *** *
         * *** For each target frame, we register a listener, which applies the forwarded MouseEvents *** *
         * ***                                                                                        *** *
         * ********************************************************************************************** *
         * ********************************************************************************************** *
         */
        for (JFrame targetFrame : targetFrames) {
            nova.eventBus.on("MouseEvent").subscribe(new MouseEventTranslator(targetFrame));
        }

    }

    private static JFrame[] createTargetFrames(int numberOfFrames) {
        JFrame[] returnValue = new JFrame[numberOfFrames];
        for (int i = 0; i < returnValue.length; i++) {
            returnValue[i] = new TargetFrame();
            returnValue[i].setSize(300, 300);
            returnValue[i].setLocation(300 + (i * 310), 0);
            returnValue[i].setVisible(true);
        }
        return returnValue;
    }

    private static JFrame createSourceFrame() {
        SourceFrame sourceFrame = new SourceFrame();
        sourceFrame.setSize(300, 300);
        sourceFrame.setLocation(0, 0);
        sourceFrame.setVisible(true);
        return sourceFrame;
    }
}
