package com.dotc.nova;

import org.easymock.Capture;

public class TestHelper {
	private static final long MAX_NUM_MS_TO_WAIT_FOR_CAPTURE_VALUE = 2000;
	private static final long DELAY_IN_MS_TO_WAIT_FOR_NEXT_CAPTURE_VALUE_CHECK = 100;

	public static <T> T getCaptureValue(Capture<T> capture) {
		return getCaptureValue(capture, MAX_NUM_MS_TO_WAIT_FOR_CAPTURE_VALUE, DELAY_IN_MS_TO_WAIT_FOR_NEXT_CAPTURE_VALUE_CHECK);
	}

	public static <T> T getCaptureValue(Capture<T> capture, long maxMillisToWait, long delayInMs) {
		T returnValue = null;
		long endTime = System.currentTimeMillis() + maxMillisToWait;
		while (returnValue == null && System.currentTimeMillis() <= endTime) {
			try {
				returnValue = capture.getValue();
			} catch (Throwable e) {
				// NOOP
			}
		}
		return returnValue;
	}
}
