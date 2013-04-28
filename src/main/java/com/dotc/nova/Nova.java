package com.dotc.nova;

import com.dotc.nova.events.EventEmitter;
import com.dotc.nova.filesystem.Filesystem;
import com.dotc.nova.timers.Timers;

public class Nova {
	private final ProcessingLoop processingLoop;

	public final Timers timers;
	public final EventEmitter eventEmitter;
	public final com.dotc.nova.process.Process process;
	public final Filesystem filesystem;

	public Nova() {
		processingLoop = new ProcessingLoop();
		processingLoop.init();

		timers = new Timers(processingLoop);
		eventEmitter = new EventEmitter(processingLoop);
		process = new com.dotc.nova.process.Process(processingLoop);
		filesystem = new Filesystem(process);
	}

}
