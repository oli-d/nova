package com.dotc.nova.events.wrappers;

import com.dotc.nova.events.EventListener;

public interface TwoParametersEventListener<ParamOneType, ParamTwoType> extends EventListener {
	@Override
	default void handle(Object... params) {
		if (params == null || params.length == 0) {
			doHandle(null, null);
		} else if (params == null || params.length == 1) {
			doHandle((ParamOneType) params[0], null);
		} else {
			doHandle((ParamOneType) params[0], (ParamTwoType) params[1]);
		}
	}

	void doHandle(ParamOneType p1, ParamTwoType p2);
}
