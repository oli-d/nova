package com.dotc.nova.events.wrappers;

import com.dotc.nova.events.EventListener;

public interface NoParameterEventListener extends EventListener {

	void doHandle();

	@Override
	default void handle(Object... data) {
		doHandle();
	}

}
