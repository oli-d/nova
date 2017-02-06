/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.events;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;

class InvocationContext {
	private Object event;
	private Consumer<Object[]> consumer;
	private Object[] data;
	private Object duplicateDetectionId;
	private Map<Object, Object[]> currentDataLookupMap;

	public InvocationContext(Object event, Consumer<Object[]> consumer, Object... data) {
		this.event = event;
		this.consumer = consumer;
		this.data = data;
	}

	public InvocationContext() {
	}

	void reset() {
		this.event = null;
		this.consumer = null;
		this.data = null;
		this.duplicateDetectionId = null;
		this.currentDataLookupMap = null;
	}

	public Object getEvent() {
		return event;
	}

	public Consumer<Object[]> getConsumer() {
		return consumer;
	}

	public Object[] getData() {
		if (duplicateDetectionId != null) {
			return currentDataLookupMap.remove(duplicateDetectionId);
		} else {
			return data;
		}
	}

	public void setEmitInfo(Object event, Consumer<Object[]> consumer, Object... data) {
		reset();
		this.event = event;
		this.consumer = consumer;
		this.data = data;
	}

	public void setEmitInfo(Object event, Consumer<Object[]> consumer, Object duplicateDetectionId,
							Map<Object, Object[]> currentDataLookupMap) {
		reset();
		this.event = event;
		this.consumer = consumer;
		this.duplicateDetectionId = duplicateDetectionId;
		this.currentDataLookupMap = currentDataLookupMap;
	}

	@Override
	public String toString() {
		return "InvocationContext [event=" + event + ", consumer=" + consumer
				+ (duplicateDetectionId == null ? ", data=" + Arrays.toString(data) : ", duplicateDetectionId=" + duplicateDetectionId)
				+ "]";
	}

}
