package com.dotc.nova;

import com.dotc.nova.events.EventEmitter;
import com.dotc.nova.filesystem.Filesystem;
import com.dotc.nova.timers.Timers;

public class Nova {
	private final ProcessingLoop processingLoop;

	private final Timers timers;
	private final EventEmitter eventEmitter;
	private final com.dotc.nova.process.Process process;
	private final Filesystem filesystem;

	public Nova() {
		processingLoop = new ProcessingLoop();
		processingLoop.init();

		timers = new Timers(processingLoop);
		eventEmitter = new EventEmitter(processingLoop);
		process = new com.dotc.nova.process.Process(processingLoop);
		filesystem = new Filesystem(processingLoop);
	}

	public EventEmitter getEventEmitter() {
		return eventEmitter;
	}

	public com.dotc.nova.process.Process getProcess() {
		return process;
	}

	public Timers getTimers() {
		return timers;
	}

	public Filesystem getFilesystem() {
		return filesystem;
	}

}
