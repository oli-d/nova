package com.dotc.nova.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.ExceptionHandler;

public class DefaultExceptionHandler implements ExceptionHandler {
	private Logger LOGGER = LoggerFactory.getLogger(DefaultExceptionHandler.class);

	@Override
	public void handleEventException(Throwable ex, long sequence, Object event) {
		String message = String.format("An unhandled exception was caught trying to dispatch element #%d in ring buffer: %s", sequence, event);
		LOGGER.error(message, ex);
	}

	@Override
	public void handleOnStartException(Throwable ex) {
		String message = "An unhandled exception was caught trying to start the ring buffer.";
		LOGGER.error(message, ex);
	}

	@Override
	public void handleOnShutdownException(Throwable ex) {
		String message = "An unhandled exception was caught trying to shutdown the ring buffer.";
		LOGGER.error(message, ex);
	}

}
