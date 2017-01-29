/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova;

import ch.squaredesk.nova.events.CurrentThreadEventEmitter;
import ch.squaredesk.nova.events.EventDispatchConfig;
import ch.squaredesk.nova.events.EventLoopAwareEventEmitter;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

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
