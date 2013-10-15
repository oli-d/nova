package com.dotc.nova;

import static org.junit.Assert.*;

import org.junit.Test;

import com.dotc.nova.events.*;
import com.dotc.nova.events.EventDispatchConfig.DispatchThreadStrategy;

public class NovaBuilderTest {

	@Test
	public void testDefaultBuilsdWithEventLoopEmitter() {
		Nova nova = new Nova.Builder().build();
		assertTrue(nova.eventEmitter instanceof EventLoopAwareEventEmitter);
	}

	@Test
	public void testBuildWithCurrentThreadEmitter() {
		Nova nova = new Nova.Builder().withEventDispatchConfig(new EventDispatchConfig.Builder().withDispatchThreadStrategy(DispatchThreadStrategy.DISPATCH_IN_EMITTER_THREAD).build()).build();
		assertTrue(nova.eventEmitter instanceof CurrentThreadEventEmitter);
	}

	@Test
	public void testBuildWithEventLoopEmitter() {
		Nova nova = new Nova.Builder().withEventDispatchConfig(new EventDispatchConfig.Builder().withDispatchThreadStrategy(DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD).build()).build();
		assertTrue(nova.eventEmitter instanceof EventLoopAwareEventEmitter);
	}

}
