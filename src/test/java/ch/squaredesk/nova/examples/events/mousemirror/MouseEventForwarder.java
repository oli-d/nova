/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the license described in the "LICENSE" file in the root directory
 * of the distribution package.
 */

package ch.squaredesk.nova.examples.events.mousemirror;

import java.awt.event.*;

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