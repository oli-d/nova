package com.dotc.nova.examples.mousemirror;

import org.apache.log4j.BasicConfigurator;

import com.dotc.nova.Nova;

public class MouseMirror {
	public static void main(String[] args) {
		// init logging
		BasicConfigurator.configure();

		// init Nova
		Nova nova = new Nova();

		// create the UI components
		TargetFrame targetFrame1 = new TargetFrame(nova.getEventEmitter());
		targetFrame1.setSize(300, 300);
		targetFrame1.setLocation(310, 0);
		targetFrame1.setVisible(true);

		TargetFrame targetFrame2 = new TargetFrame(nova.getEventEmitter());
		targetFrame2.setSize(300, 300);
		targetFrame2.setLocation(310, 330);
		targetFrame2.setVisible(true);

		SourceFrame sourceFrame = new SourceFrame(nova.getEventEmitter());
		sourceFrame.setSize(300, 300);
		sourceFrame.setLocation(0, 0);
		sourceFrame.setVisible(true);
	}
}
