package com.dotc.nova;

import static org.junit.Assert.*;

import org.junit.Test;

import com.dotc.nova.events.AsyncEventEmitter;
import com.dotc.nova.events.SyncEventEmitter;

public class NovaBuilderTest {

	@Test
	public void testDefaultBuilsdWithSyncEmitter() {
		Nova nova = new Nova.Builder().build();
		assertTrue(nova.eventEmitter instanceof SyncEventEmitter);
	}

	@Test
	public void testBuildWithSyncEmitter() {
		Nova nova = new Nova.Builder().withAsyncEventEmitter(false).build();
		assertTrue(nova.eventEmitter instanceof SyncEventEmitter);
	}

	@Test
	public void testBuildWithAsyncEmitter() {
		Nova nova = new Nova.Builder().withAsyncEventEmitter(true).build();
		assertTrue(nova.eventEmitter instanceof AsyncEventEmitter);
	}

}
