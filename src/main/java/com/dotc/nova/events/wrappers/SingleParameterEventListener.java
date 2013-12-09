package com.dotc.nova.events.wrappers;

import com.dotc.nova.events.EventListener;

public abstract class SingleParameterEventListener<ParamType> implements EventListener<Object> {

	public abstract void handle(ParamType param);

	@SuppressWarnings("unchecked")
	@Override
	public void handle(Object... data) {
		if (data == null || data.length == 0) {
			handle((ParamType) null);
		} else {
			handle((ParamType) data[0]);
		}
	}

}
