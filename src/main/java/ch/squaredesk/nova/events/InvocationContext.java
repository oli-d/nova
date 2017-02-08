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

class InvocationContext {
	public final Object event;
	private final Object[] data;
	public final Object duplicateDetectionId;
	public final Map<Object, Object[]> currentDataLookupMap;

	InvocationContext(Object event, Object[] data) {
		this(event, data, null, null);
	}

	InvocationContext(Object event, Object[] data, Object duplicateDetectionId, Map<Object, Object[]> currentDataLookupMap) {
		this.event = event;
		this.data = data;
		this.duplicateDetectionId = duplicateDetectionId;
		this.currentDataLookupMap = currentDataLookupMap;
	}

	public Object[] getData() {
		if (duplicateDetectionId != null) {
			return currentDataLookupMap.remove(duplicateDetectionId);
		} else {
			return data;
		}
	}


	@Override
	public String toString() {
		return "InvocationContext [event=" + event
				+ (duplicateDetectionId == null ? ", data=" + Arrays.toString(data) : ", duplicateDetectionId=" + duplicateDetectionId)
				+ "]";
	}

}
