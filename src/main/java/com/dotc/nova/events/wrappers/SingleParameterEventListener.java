package com.dotc.nova.events.wrappers;

import com.dotc.nova.events.EventListener;

public interface SingleParameterEventListener<ParamType> extends EventListener {
	@Override
	default void handle(Object... params) {
		if (params == null || params.length == 0) {
			doHandle(null);
		} else {
			doHandle((ParamType) params[0]);
		}
	}

	void doHandle(ParamType p);
}
