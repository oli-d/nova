/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the license described in the "LICENSE" file in the root directory
 * of the distribution package.
 */

package ch.squaredesk.nova;

import static org.junit.Assert.*;

import ch.squaredesk.nova.events.CurrentThreadEventEmitter;
import ch.squaredesk.nova.events.EventDispatchConfig;
import ch.squaredesk.nova.events.EventLoopAwareEventEmitter;
import org.junit.Test;

public class NovaBuilderTest {

	@Test
	public void testDefaultBuilsdWithEventLoopEmitter() {
		Nova nova = new Nova.Builder().build();
		assertTrue(nova.eventEmitter instanceof EventLoopAwareEventEmitter);
	}

	@Test
	public void testBuildWithCurrentThreadEmitter() {
		Nova nova = new Nova.Builder().setEventDispatchConfig(new EventDispatchConfig.Builder().setDispatchThreadStrategy(EventDispatchConfig.DispatchThreadStrategy.DISPATCH_IN_EMITTER_THREAD).build()).build();
		assertTrue(nova.eventEmitter instanceof CurrentThreadEventEmitter);
	}

	@Test
	public void testBuildWithEventLoopEmitter() {
		Nova nova = new Nova.Builder().setEventDispatchConfig(new EventDispatchConfig.Builder().setDispatchThreadStrategy(EventDispatchConfig.DispatchThreadStrategy.DISPATCH_IN_SPECIFIC_THREAD).build()).build();
		assertTrue(nova.eventEmitter instanceof EventLoopAwareEventEmitter);
	}

}
