/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.examples.events.mousemirror;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public abstract class MouseEventForwarder implements MouseListener, MouseMotionListener {

    public abstract void forwardEvent(MouseEvent event);

    @Override
    public void mouseReleased(MouseEvent e) {
        forwardEvent(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        forwardEvent(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        forwardEvent(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        forwardEvent(e);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        forwardEvent(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        forwardEvent(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        forwardEvent(e);
    }

}