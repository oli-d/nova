package com.dotc.nova;

import com.dotc.nova.events.EventEmitter;

public class Nova {
	private final ProcessingLoop eventDispatcher;

	private final EventEmitter eventEmitter;
	private final com.dotc.nova.process.Process process;

	public Nova() {
		eventDispatcher = new ProcessingLoop();
		eventEmitter = new EventEmitter(eventDispatcher);
		process = new com.dotc.nova.process.Process(eventDispatcher);
	}

	public EventEmitter getEventEmitter() {
		return eventEmitter;
	}

	public com.dotc.nova.process.Process getProcess() {
		return process;
	}

}
