/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.mousemirror;

import javax.swing.*;
import java.awt.*;

public class TargetFrame extends JFrame {

    public TargetFrame() {
        super("Target Frame");

        // some GUI stuff
        setBackground(Color.CYAN);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(false);
    }

}
