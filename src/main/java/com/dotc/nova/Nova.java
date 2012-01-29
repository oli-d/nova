package com.dotc.nova;

import com.dotc.nova.events.EventEmitter;

public class Nova {
	private final ProcessingLoop processingLoop;

	private final EventEmitter eventEmitter;
	private final com.dotc.nova.process.Process process;

	public Nova() {
		processingLoop = new ProcessingLoop();
		processingLoop.init();

		eventEmitter = new EventEmitter(processingLoop);
		process = new com.dotc.nova.process.Process(processingLoop);
	}

	public EventEmitter getEventEmitter() {
		return eventEmitter;
	}

	public com.dotc.nova.process.Process getProcess() {
		return process;
	}

}
