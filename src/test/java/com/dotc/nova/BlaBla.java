package com.dotc.nova;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.dotc.nova.events.EventDispatchConfig;
import com.dotc.nova.events.EventDispatchConfig.DispatchThreadStrategy;
import com.dotc.nova.events.wrappers.SingleParameterEventListener;

public class BlaBla {

	@Test
	public void test() throws Exception {
		SingleParameterEventListener<String> listener = new SingleParameterEventListener<String>() {
			@Override
			public void handle(String param) {
				System.out.println("Got " + param + " in Thread " + Thread.currentThread());
			}
		};

		EventDispatchConfig edc = new EventDispatchConfig.Builder().setDispatchThreadStrategy(DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD).setNumberOfConsumers(1).build();
		final Nova nova = new Nova.Builder().setEventDispatchConfig(edc).build();
		nova.eventEmitter.on("event", listener);

		nova.timers.setTimeout(new Runnable() {

			@Override
			public void run() {
				nova.eventEmitter.emit("event", "from timeout");
			}
		}, 3, TimeUnit.SECONDS);

		nova.eventEmitter.emit("event", "from main");
		Thread.sleep(1000);
		nova.eventEmitter.emit("event", "from main");
		Thread.sleep(1000);
		nova.eventEmitter.emit("event", "from main");
		Thread.sleep(1000);
		nova.eventEmitter.emit("event", "from main");
		Thread.sleep(1000);
		nova.eventEmitter.emit("event", "from main");
		Thread.sleep(1000);
		nova.eventEmitter.emit("event", "from main");
		Thread.sleep(1000);
		nova.eventEmitter.emit("event", "from main");
		Thread.sleep(1000);
		nova.eventEmitter.emit("event", "from main");
		Thread.sleep(1000);
		nova.eventEmitter.emit("event", "from main");
		Thread.sleep(1000);
		nova.eventEmitter.emit("event", "from main");
		Thread.sleep(1000);
	}

}
