package com.dotc.nova.events.wrappers;

import com.dotc.nova.events.EventListener;

public abstract class NoParameterEventListener implements EventListener {

	public abstract void handle();

	@Override
	public void handle(Object... data) {
		handle();
	}

}
