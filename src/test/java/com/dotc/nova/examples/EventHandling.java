package com.dotc.nova.examples;

import java.util.concurrent.*;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.dotc.nova.Nova;
import com.dotc.nova.events.EventListener;

public class EventHandling {
	private static final Logger LOGGER = Logger.getRootLogger();
	private static int counter = 0;

	public static void main(String[] args) {
		BasicConfigurator.configure();

		final Nova nova = new Nova();

		LOGGER.info("Registering event listener...");
		nova.getEventEmitter().addListener(MyEvent.class, new MyEventListener());

		Runnable eventProducer = new Runnable() {

			@Override
			public void run() {
				nova.getEventEmitter().emit(new MyEvent(counter++));
			}
		};
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

		LOGGER.info("Starting event producer...");
		executor.scheduleWithFixedDelay(eventProducer, 0, 2, TimeUnit.SECONDS);
	}

	private static class MyEventListener implements EventListener<MyEvent> {

		@Override
		public void handle(MyEvent event) {
			LOGGER.info("Event listener invoked, count = " + event.count);
			if (counter == 10) {
				System.exit(0);
			}
		}

	}

	private static class MyEvent {
		public final int count;

		public MyEvent(int count) {
			this.count = count;
		}

	}
}
