package com.dotc.nova;

import static org.junit.Assert.*;

import org.junit.Test;

import com.dotc.nova.events.CurrentThreadEventEmitter;
import com.dotc.nova.events.EventLoopAwareEventEmitter;

public class NovaBuilderTest {

	@Test
	public void testDefaultBuilsdWithEventLoopEmitter() {
		Nova nova = new Nova.Builder().build();
		assertTrue(nova.eventEmitter instanceof EventLoopAwareEventEmitter);
	}

	@Test
	public void testBuildWithCurrentThreadEmitter() {
		Nova nova = new Nova.Builder().withCurrentThreadEventEmitter(true).build();
		assertTrue(nova.eventEmitter instanceof CurrentThreadEventEmitter);
	}

	@Test
	public void testBuildWithEventLoopEmitter() {
		Nova nova = new Nova.Builder().withCurrentThreadEventEmitter(false).build();
		assertTrue(nova.eventEmitter instanceof EventLoopAwareEventEmitter);
	}

}
