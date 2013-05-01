package com.dotc.nova.events.wrappers;

import com.dotc.nova.events.EventListener;

public abstract class TwoParametersEventListener<ParamOneType, ParamTwoType> implements EventListener {

	abstract void handle(ParamOneType param1, ParamTwoType param2);

	@Override
	public void handle(Object... data) {
		ParamOneType param1 = null;
		ParamTwoType param2 = null;

		if (data != null) {
			if (data.length > 0) {
				param1 = (ParamOneType) data[0];
			}
			if (data.length > 1) {
				param2 = (ParamTwoType) data[1];
			}
		}

		handle(param1, param2);
	}
}
