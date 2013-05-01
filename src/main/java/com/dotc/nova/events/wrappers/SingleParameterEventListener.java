package com.dotc.nova.events.wrappers;

import com.dotc.nova.events.EventListener;

public abstract class SingleParameterEventListener<ParamType> implements EventListener {

	abstract void handle(ParamType param);

	@Override
	public void handle(Object... data) {
		if (data == null || data.length == 0) {
			handle((ParamType) null);
		}
		handle((ParamType) data[0]);
	}

}
